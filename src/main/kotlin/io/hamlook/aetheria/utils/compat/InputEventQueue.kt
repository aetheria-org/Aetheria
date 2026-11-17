package io.hamlook.aetheria.utils.compat

/**
 * Modern (GLFW-callback) input event queue behind the LWJGL-style
 * MouseCompat/KeyboardCompat API.
 *
 * On 1.8.9 LWJGL owns the queue: `next()` advances one event and the
 * `getEvent*` getters read the current event. On modern there is no LWJGL
 * queue, so the version twins ([MixinMouse], [MixinKeyboard] equivalents
 * under versions/1.21.1-fabric/src) push GLFW callbacks here and `next()`
 * polls them back out. Call sites are unchanged on both versions.
 *
 * Model notes (all deliberate):
 * - Mouse and keyboard have separate queues, mirroring the separate
 *   `Mouse.next()` / `Keyboard.next()` iterators.
 * - The latch mirrors LWJGL "current event" reads: after a poll the getters
 *   return that event's values; non-button events latch button=-1/false.
 * - Wheel uses a consuming accumulator (`consumeWheel`): every producer push
 *   is delivered exactly once across the three wheel getters. Call sites all
 *   read at most once per handling, so this is exact; a same-tick double
 *   read would see (N, 0) instead of (N, N) — no such site exists today.
 * - Scroll amounts are scaled x120 to legacy wheel units (SkyHanni parity:
 *   GLFW reports ~1.0 per notch, legacy ~120).
 * - Key codes are raw GLFW on modern. Source-level `Keyboard.KEY_*`
 *   comparisons keep working because the mapping already renames them to
 *   `GLFW.GLFW_KEY_*`; persisted legacy ints are a data-migration concern,
 *   not a queue concern.
 * - The queue is bounded (128) with move-event coalescing: only the twins
 *   produce, and nothing drains outside transition handling, so an uncapped
 *   queue would grow on cursor motion forever.
 * - Char input merges into a preceding key event without a char, matching
 *   LWJGL's bundled key+character events.
 */
object InputEventQueue {

    private const val MAX_QUEUE = 128
    private const val WHEEL_SCALE = 120

    private const val KIND_BUTTON = 0
    private const val KIND_SCROLL = 1
    private const val KIND_MOVE = 2

    private data class MouseEvent(
        val kind: Int,
        val button: Int = -1,
        val pressed: Boolean = false,
        val dx: Int = 0,
        val dy: Int = 0,
        val nanos: Long = 0L,
    )

    private val mouseEvents: ArrayDeque<MouseEvent> = ArrayDeque()

    @JvmStatic var mouseButton: Int = -1
        private set
    @JvmStatic var mouseButtonState: Boolean = false
        private set
    @JvmStatic var mouseDX: Int = 0
        private set
    @JvmStatic var mouseDY: Int = 0
        private set
    @JvmStatic var mouseNanos: Long = 0L
        private set

    private var wheelAccum: Int = 0
    private var lastCursorX: Double = Double.NaN
    private var lastCursorY: Double = Double.NaN

    @JvmStatic
    fun pushMouseButton(button: Int, pressed: Boolean) {
        offer(mouseEvents, MouseEvent(KIND_BUTTON, button, pressed, nanos = System.nanoTime()))
    }

    @JvmStatic
    fun pushMouseScroll(amount: Double) {
        wheelAccum += (amount * WHEEL_SCALE).toInt()
        offer(mouseEvents, MouseEvent(KIND_SCROLL, nanos = System.nanoTime()))
    }

    @JvmStatic
    fun pushMouseMoveAbs(x: Double, y: Double) {
        val dx = if (lastCursorX.isNaN()) 0 else (x - lastCursorX).toInt()
        val dy = if (lastCursorY.isNaN()) 0 else (y - lastCursorY).toInt()
        lastCursorX = x
        lastCursorY = y
        val tail = mouseEvents.lastOrNull()
        val event = MouseEvent(KIND_MOVE, dx = dx, dy = dy, nanos = System.nanoTime())
        if (tail != null && tail.kind == KIND_MOVE) {
            mouseEvents[mouseEvents.size - 1] = event
        } else {
            offer(mouseEvents, event)
        }
    }

    @JvmStatic
    fun pollMouse(): Boolean {
        val event = mouseEvents.removeFirstOrNull() ?: return false
        when (event.kind) {
            KIND_BUTTON -> {
                mouseButton = event.button
                mouseButtonState = event.pressed
            }
            KIND_SCROLL, KIND_MOVE -> {
                mouseButton = -1
                mouseButtonState = false
            }
        }
        if (event.kind == KIND_MOVE) {
            mouseDX = event.dx
            mouseDY = event.dy
        }
        mouseNanos = event.nanos
        return true
    }

    @JvmStatic
    fun consumeWheel(): Int {
        val value = wheelAccum
        wheelAccum = 0
        return value
    }

    private data class KeyEvent(
        val key: Int = 0,
        val scancode: Int = 0,
        val pressed: Boolean = false,
        val repeat: Boolean = false,
        val char: Char = '\u0000',
        val nanos: Long = 0L,
    )

    private val keyEvents: ArrayDeque<KeyEvent> = ArrayDeque()

    @JvmStatic var eventKey: Int = 0
        private set
    @JvmStatic var eventKeyState: Boolean = false
        private set
    @JvmStatic var eventRepeat: Boolean = false
        private set
    @JvmStatic var eventChar: Char = '\u0000'
        private set
    @JvmStatic var keyNanos: Long = 0L
        private set

    @JvmStatic
    fun pushKey(key: Int, scancode: Int, pressed: Boolean, repeat: Boolean) {
        offer(keyEvents, KeyEvent(key, scancode, pressed, repeat, nanos = System.nanoTime()))
    }

    @JvmStatic
    fun pushChar(char: Char) {
        val tail = keyEvents.lastOrNull()
        if (tail != null && tail.char == '\u0000') {
            keyEvents[keyEvents.size - 1] = tail.copy(char = char)
        } else {
            offer(keyEvents, KeyEvent(char = char, pressed = true, nanos = System.nanoTime()))
        }
    }

    @JvmStatic
    fun pollKey(): Boolean {
        val event = keyEvents.removeFirstOrNull() ?: return false
        eventKey = event.key
        eventKeyState = event.pressed
        eventRepeat = event.repeat
        eventChar = event.char
        keyNanos = event.nanos
        return true
    }

    private fun <T> offer(queue: ArrayDeque<T>, event: T) {
        if (queue.size >= MAX_QUEUE) queue.removeFirst()
        queue.addLast(event)
    }
}
