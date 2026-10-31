package io.hamlook.aetheria.idea

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import javax.swing.Timer
import java.util.concurrent.ConcurrentHashMap

class VariantChangeTipListener : EditorFactoryListener {
    private val observedDocuments = ConcurrentHashMap.newKeySet<Document>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val document = editor.document
        val project = editor.project
        val virtualFile = FileDocumentManager.getInstance().getFile(document)
        if (project != null && virtualFile != null) VersionFileVariants.attachIfGenerated(project, virtualFile)
        if (!observedDocuments.add(document)) return
        var pendingCharacters = 0
        var lastOffset = 0
        var timer: Timer? = null
        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val sourceFile = FileDocumentManager.getInstance().getFile(document)
                val project = EditorFactory.getInstance().getEditors(document).firstOrNull { !it.isDisposed }?.project
                val path = sourceFile?.path.orEmpty().replace('\\', '/')
                if (project != null && !VersionVariantSourceSync.isSyncing(document) &&
                    (path.contains("/src/main/") || path.contains("/versions/") && path.contains("/src/"))
                ) {
                    VersionBuildRunner.scheduleAuto(project)
                }
                if (event.document.isInBulkUpdate || !isInsideConditionalVariant(event.document.text, event.offset)) return
                pendingCharacters += maxOf(event.oldLength, event.newLength)
                lastOffset = event.offset
                if (pendingCharacters < 24) return
                timer?.stop()
                timer = Timer(900) {
                    pendingCharacters = 0
                    val activeEditor = EditorFactory.getInstance().getEditors(document).firstOrNull { !it.isDisposed }
                    if (activeEditor != null && isInsideConditionalVariant(document.text, lastOffset)) {
                        HintManager.getInstance().showInformationHint(
                            activeEditor,
                            "This change is in the active Minecraft variant. Repeat it in the other variant when needed."
                        )
                    }
                }.apply { isRepeats = false; start() }
            }
        })
    }

    private fun isInsideConditionalVariant(text: String, offset: Int): Boolean {
        data class Frame(val marked: Boolean, var depth: Int, var bodyStart: Int, var elseSeen: Boolean = false, val ranges: MutableList<IntRange> = mutableListOf())
        val stack = mutableListOf<Frame>()
        val variantRanges = mutableListOf<IntRange>()
        var cursor = 0
        for (line in text.lineSequence()) {
            val lineStart = cursor
            val lineEnd = lineStart + line.length
            if (line.trimStart().startsWith("//#if")) {
                stack.add(Frame(true, 1, lineEnd + 1))
            } else if (line.trim() == "//#else" && stack.isNotEmpty()) {
                val frame = stack.last()
                if (frame.depth == 1 && !frame.elseSeen) {
                    frame.ranges.add(frame.bodyStart until lineStart)
                    frame.elseSeen = true
                    frame.bodyStart = lineEnd + 1
                }
            } else if (line.trim() == "//#endif" && stack.isNotEmpty()) {
                val frame = stack.removeAt(stack.lastIndex)
                if (frame.elseSeen) {
                    frame.ranges.add(frame.bodyStart until lineStart)
                    if (frame.marked) variantRanges.addAll(frame.ranges)
                }
            }
            cursor = lineEnd + 1
        }
        return variantRanges.any { offset in it }
    }
}
