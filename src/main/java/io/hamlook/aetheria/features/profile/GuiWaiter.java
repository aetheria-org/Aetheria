package io.hamlook.aetheria.features.profile;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.compat.InventoryCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public class GuiWaiter {

    public static final GuiWaiter INSTANCE = new GuiWaiter();
    private final Deque<PendingWait> queue = new ArrayDeque<>();
    private GuiWaiter() {}

    // ── Helper to resolve negative slot indexes from the end of the container ──

    private static int resolveSlot(ContainerChest container, int slot) {
        int temp = slot + 1;
        if (temp == 0) return -1;  // -1 means no slot/action is required
        if (temp > 0) return slot; // Normal forward index

        // Negative index: offset "temp" amount from the very last slot
        int lastSlot = container.getLowerChestInventory().getSizeInventory() - 1;

        // ADD + 1 HERE so that entering -2 targets the exact last slot!
        return lastSlot + temp + 1;
    }

    // ── Standard single-page wait ─────────────────────────────────────────────

    public static void waitFor(String expectedTitle, int tickDelay, int pressSlot,
                               Consumer<ContainerChest> callback) {
        // // ATHRMod.logger.info("[GuiWaiter] Queued wait for: '" + expectedTitle + "' (queue size now " + (INSTANCE.queue.size() + 1) + ")");
        INSTANCE.queue.add(new PendingWait(expectedTitle, tickDelay, pressSlot, callback, null, null, -1));
    }

    public static void waitFor(String expectedTitle, int tickDelay, int pressSlot,
                               String returnTitle, Consumer<ContainerChest> callback,
                               Consumer<ContainerChest> onReturn) {
        // // ATHRMod.logger.info("[GuiWaiter] Queued wait for: '" + expectedTitle + "' with return to '" + returnTitle + "' (queue size now " + (INSTANCE.queue.size() + 1) + ")");
        INSTANCE.queue.add(new PendingWait(expectedTitle, tickDelay, pressSlot, callback, returnTitle, onReturn, -1));
    }

    // ── Automatic Paged Wait ──────────────────────────────────────────────────

    public static void waitForPaged(String expectedTitle, int tickDelay,
                                    int nextPageSlot, String nextPageItemName,
                                    int backSlot, String returnTitle,
                                    Consumer<ContainerChest> onPage,
                                    Consumer<ContainerChest> onReturn) {
        // // ATHRMod.logger.info("[GuiWaiter] Queued paged wait for: '" + expectedTitle + "'");
        INSTANCE.queue.add(new PendingWait(expectedTitle, tickDelay, -1,
                container -> INSTANCE.handlePage(container, expectedTitle, tickDelay,
                        nextPageSlot, nextPageItemName, backSlot, returnTitle, onPage, onReturn),
                null, null, -1));
    }

    public static void waitForUserAction(String expectedTitle, int ignoreWindowId, Consumer<ContainerChest> callback) {
        Aetheria.logger.info("[GuiWaiter] Highlighted a slot. Waiting for user to click and reload '" + expectedTitle + "'...");
        WaiterLogs.addLog("[GuiWaiter] Highlighted a slot. Waiting for user to click and reload '" + expectedTitle + "'...");
        PendingWait wait = new PendingWait(expectedTitle, 2, -1, callback, null, null, ignoreWindowId);
        wait.maxPollTicks = 400;
        INSTANCE.queue.addFirst(wait);
    }

    private void handlePage(ContainerChest container, String expectedTitle, int tickDelay,
                            int nextPageSlot, String nextPageItemName,
                            int backSlot, String returnTitle,
                            Consumer<ContainerChest> onPage,
                            Consumer<ContainerChest> onReturn) {
        // Parse this page
        onPage.accept(container);

        if (container == null) {
            if (returnTitle != null && onReturn != null) {
                Aetheria.logger.info("[GuiWaiter] Paged GUI was empty, returning directly to: '" + returnTitle + "'");
                WaiterLogs.addLog("[GuiWaiter] Paged GUI was empty, returning directly to: '" + returnTitle + "'");
                queue.addFirst(new PendingWait(returnTitle, 2, -1, onReturn, null, null, -1));
            }
            return;
        }

        Minecraft mc = MinecraftCompat.getMinecraft();
        int actualNextSlot = resolveSlot(container, nextPageSlot);
        boolean hasNextPage = false;

        if (actualNextSlot >= 0) {
            ItemStack nextPageItem = container.getSlot(actualNextSlot).getStack();
            hasNextPage = nextPageItem != null
                    && ColorUtils.stripColor(nextPageItem.getDisplayName()).contains(nextPageItemName);
        }

        if (hasNextPage) {
            //ATHRMod.logger.info("[GuiWaiter] Paged GUI: clicking next page (slot " + actualNextSlot + ")");
            InventoryCompat.windowClick(container.windowId, actualNextSlot, 0, 0, MinecraftCompat.getLocalPlayer());

            queue.addFirst(new PendingWait(expectedTitle, tickDelay, -1,
                    next -> handlePage(next, expectedTitle, tickDelay,
                            nextPageSlot, nextPageItemName, backSlot, returnTitle, onPage, onReturn),
                    null, null, container.windowId));
        } else {
            int actualBackSlot = resolveSlot(container, backSlot);
            Aetheria.logger.info("[GuiWaiter] Paged GUI: last page reached, clicking back (slot " + actualBackSlot + ")");
            WaiterLogs.addLog("[GuiWaiter] Paged GUI: last page reached, clicking back (slot " + actualBackSlot + ")");
            if (actualBackSlot >= 0) {
                InventoryCompat.windowClick(container.windowId, actualBackSlot, 0, 0, MinecraftCompat.getLocalPlayer());
                //ATHRMod.logger.info("[GuiWaiter] Page GUI: clicked on slot " + actualBackSlot + " | " + container.windowId);
            }
            if (returnTitle != null && onReturn != null) {
                Aetheria.logger.info("[GuiWaiter] Queuing return wait for: '" + returnTitle + "'");
                WaiterLogs.addLog("[GuiWaiter] Queuing return wait for: '" + returnTitle + "'");
                queue.addFirst(new PendingWait(returnTitle, 2, -1, onReturn, null, null, container.windowId));
            }
        }
    }

    // ── Manual Paged Wait ─────────────────────────────────────────────────────

    public static void waitForManualPaged(String expectedTitle, int tickDelay,
                                          int nextPageSlot, String nextPageItemName,
                                          int backSlot, String returnTitle,
                                          Consumer<ContainerChest> onPage,
                                          Consumer<ContainerChest> onReturn) {
        // ATHRMod.logger.info("[GuiWaiter] Queued manual paged wait for: '" + expectedTitle + "'");
        INSTANCE.queue.add(new PendingWait(expectedTitle, tickDelay, -1,
                container -> INSTANCE.handleManualPage(container, expectedTitle, tickDelay,
                        nextPageSlot, nextPageItemName, backSlot, returnTitle, onPage, onReturn),
                null, null, -1));
    }

    private void handleManualPage(ContainerChest container, String expectedTitle, int tickDelay,
                                  int nextPageSlot, String nextPageItemName,
                                  int backSlot, String returnTitle,
                                  Consumer<ContainerChest> onPage,
                                  Consumer<ContainerChest> onReturn) {
        // Parse this page
        onPage.accept(container);

        if (container == null) {
            if (returnTitle != null && onReturn != null) {
                Aetheria.logger.info("[GuiWaiter] Manual Paged GUI was empty, returning directly to: '" + returnTitle + "'");
                WaiterLogs.addLog("[GuiWaiter] Manual Paged GUI was empty, returning directly to: '" + returnTitle + "'");
                queue.addFirst(new PendingWait(returnTitle, 2, -1, onReturn, null, null, -1));
            }
            return;
        }

        int actualNextSlot = resolveSlot(container, nextPageSlot);
        boolean hasNextPage = false;

        if (actualNextSlot >= 0) {
            ItemStack nextPageItem = container.getSlot(actualNextSlot).getStack();
            hasNextPage = nextPageItem != null
                    && ColorUtils.stripColor(nextPageItem.getDisplayName()).contains(nextPageItemName);
        }

        if (hasNextPage) {
            // Queue a wait with a 10-second timeout (200 ticks) waiting for the user to click the next page
            PendingWait manualWait = new PendingWait(expectedTitle, tickDelay, -1,
                    next -> handleManualPage(next, expectedTitle, tickDelay,
                            nextPageSlot, nextPageItemName, backSlot, returnTitle, onPage, onReturn),
                    null, null, container.windowId);
            manualWait.maxPollTicks = 200; // 10 seconds timeout
            queue.addFirst(manualWait);

        } else {
            int actualBackSlot = resolveSlot(container, backSlot);
            Aetheria.logger.info("[GuiWaiter] Manual Paged GUI: last page reached, waiting for user to click back (slot " + actualBackSlot + ")");
            WaiterLogs.addLog("[GuiWaiter] Manual Paged GUI: last page reached, waiting for user to click back (slot " + actualBackSlot + ")");

            if (returnTitle != null && onReturn != null) {
                PendingWait returnWait = new PendingWait(returnTitle, 2, -1, onReturn, null, null, container.windowId);
                returnWait.maxPollTicks = 200;
                queue.addFirst(returnWait);
            }
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    public static void clearQueue() {
        INSTANCE.queue.clear();
    }

    // ── Chat Interceptor (For Empty Storage Aborts) ───────────────────────────

    @HandleEvent
    public void onChatReceived(ASMChatEvent event) {
        if (event.type == 2) return; // Ignore Action bar messages
        if (queue.isEmpty()) return;

        PendingWait head = queue.peek();
        // If we are already processing the opened GUI, ignore chat messages
        if (head.guiReceived) return;

        String msg = ColorUtils.stripColor(TextCompat.getUnformattedText(event.message)).trim();
        String lowerMsg = msg.toLowerCase();

        if ((lowerMsg.contains("is empty") || lowerMsg.contains("empty!")) && !lowerMsg.contains(": ")) {
            Aetheria.logger.info("[GuiWaiter] Intercepted empty container chat: '" + msg + "'. Aborting wait for '" + head.expectedTitle + "'");
            WaiterLogs.addLog("[GuiWaiter] Intercepted empty container chat: '" + msg + "'. Aborting wait for '" + head.expectedTitle + "'");

            queue.poll(); // Remove the stalled wait from the queue
            head.callback.accept(null); // Safely pass null

            if (head.returnTitle != null && head.onReturn != null) {
                // ATHRMod.logger.info("[GuiWaiter] Queueing return wait for: '" + head.returnTitle + "' after empty intercept.");
                queue.addFirst(new PendingWait(head.returnTitle, 2, -1, head.onReturn, null, null, -1));
            }
        }
    }

    // ── Tick handler ──────────────────────────────────────────────────────────

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (queue.isEmpty()) return;

        PendingWait head = queue.peek();

        if (!head.guiReceived) {
            ContainerChest chest = getOpenChest(head.expectedTitle, head.ignoreWindowId);
            if (chest == null) {
                head.pollTicks++;

                // Re-click pressSlot every 100 ticks (attempts at 100, 200, 300, 400, 500)
                if (head.pressSlot > 0 && head.pollTicks % 100 == 0 && head.pollTicks <= 500) {
                    Aetheria.logger.info("[GuiWaiter] Retry " + (head.pollTicks / 100) + "/5 for '" + head.expectedTitle + "'");
                    WaiterLogs.addLog("[GuiWaiter] Retry " + (head.pollTicks / 100) + "/5 for '" + head.expectedTitle + "'");
                    Minecraft mc = MinecraftCompat.getMinecraft();
                    ContainerChest current = getOpenChest("View Profile", -1);
                    if (current != null) {
                        InventoryCompat.windowClick(current.windowId, head.pressSlot - 1, 0, 0, MinecraftCompat.getLocalPlayer());
                    }
                }

                if (head.pollTicks % 100 == 0) {
                    String current = getCurrentTitle();
                    Aetheria.logger.info("[GuiWaiter] Still waiting for '" + head.expectedTitle
                            + "' — current screen title: '" + current + "' (" + head.pollTicks + " ticks)");
                    WaiterLogs.addLog("[GuiWaiter] Still waiting for '" + head.expectedTitle
                            + "' — current screen title: '" + current + "' (" + head.pollTicks + " ticks)");
                }
                if (head.pollTicks >= 520) {
                    Aetheria.logger.info("[GuiWaiter] TIMEOUT waiting for '" + head.expectedTitle + "' — cancelling remaining queue (" + queue.size() + " items)");
                    WaiterLogs.addLog("[GuiWaiter] TIMEOUT waiting for '" + head.expectedTitle + "' — cancelling remaining queue (" + queue.size() + " items)");
                    queue.clear();
                    ProfileParser.parsing = false;
                }
                return;
            }
            // ATHRMod.logger.info("[GuiWaiter] GUI matched: '" + head.expectedTitle + "' (Window ID: " + chest.windowId + ") — starting " + head.ticksRemaining + "-tick delay");
            head.container   = chest;
            head.guiReceived = true;
            return;
        }

        if (--head.ticksRemaining > 0) return;

        // ATHRMod.logger.info("[GuiWaiter] Firing callback for: '" + head.expectedTitle + "'");
        queue.poll();

        // Failsafe: Re-fetch the container right before callback just in case it updated during the tickDelay
        ContainerChest currentChest = getOpenChest(head.expectedTitle, -1);
        if (currentChest != null) {
            head.container = currentChest;
        }

        head.callback.accept(head.container);

        int actualPressSlot = resolveSlot(head.container, head.pressSlot);
        if (actualPressSlot >= 0) {
            // ATHRMod.logger.info("[GuiWaiter] Clicking slot " + actualPressSlot + " to navigate away from '" + head.expectedTitle + "'");
            Minecraft mc = MinecraftCompat.getMinecraft();
            InventoryCompat.windowClick(
                    head.container.windowId, actualPressSlot, 0, 0, MinecraftCompat.getLocalPlayer()
            );
        }

        if (head.returnTitle != null && head.onReturn != null) {
            // ATHRMod.logger.info("[GuiWaiter] Queuing return wait for: '" + head.returnTitle + "'");
            queue.addFirst(new PendingWait(head.returnTitle, 2, -1, head.onReturn, null, null, head.container.windowId));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String getCurrentTitle() {
        if (!(MinecraftCompat.getCurrentScreen() instanceof GuiContainer)) {
            return MinecraftCompat.getCurrentScreen() == null ? "null"
                    : MinecraftCompat.getCurrentScreen().getClass().getSimpleName();
        }
        Container container = InventoryCompat.getContainer((GuiContainer) MinecraftCompat.getCurrentScreen());
        if (!(container instanceof ContainerChest)) return "(non-chest container)";
        return ColorUtils.stripColor(
                TextCompat.getUnformattedText(((ContainerChest) container).getLowerChestInventory()
                        .getDisplayName())
        ).trim();
    }

    private static ContainerChest getOpenChest(String expectedTitle, int ignoreWindowId) {
        if (!(MinecraftCompat.getCurrentScreen() instanceof GuiContainer)) return null;
        Container container = InventoryCompat.getContainer((GuiContainer) MinecraftCompat.getCurrentScreen());
        if (!(container instanceof ContainerChest)) return null;

        if (container.windowId == ignoreWindowId) return null;

        if ("*".equals(expectedTitle)) return (ContainerChest) container;
        // -------------------------------------------------------------------------

        String title = ColorUtils.stripColor(
                TextCompat.getUnformattedText(((ContainerChest) container).getLowerChestInventory()
                        .getDisplayName())
        ).trim();
        return title.equals(expectedTitle) ? (ContainerChest) container : null;
    }
    // ── Internal state ────────────────────────────────────────────────────────

    private static class PendingWait {
        final String                   expectedTitle;
        final Consumer<ContainerChest> callback;
        final String                   returnTitle;
        final Consumer<ContainerChest> onReturn;
        final int                      pressSlot;
        final int                      ignoreWindowId;
        int                            ticksRemaining;
        int                            pollTicks = 0;
        int                            maxPollTicks = 400;
        ContainerChest                 container;
        boolean                        guiReceived = false;

        PendingWait(String expectedTitle, int tickDelay, int pressSlot,
                    Consumer<ContainerChest> callback,
                    String returnTitle, Consumer<ContainerChest> onReturn,
                    int ignoreWindowId) {
            this.expectedTitle  = expectedTitle;
            this.ticksRemaining = Math.max(tickDelay, 1);
            this.pressSlot      = pressSlot;
            this.callback       = callback;
            this.returnTitle    = returnTitle;
            this.onReturn       = onReturn;
            this.ignoreWindowId = ignoreWindowId;
        }
    }
}