package io.hamlook.aetheria.idea

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

internal data class SourceFileContext(
    val project: Project,
    val root: Path,
    val source: Path,
    val relativeSource: Path,
    val onlyTarget: String? = null,
    val currentTarget: String? = null
)

internal data class GeneratedFileVariant(
    val context: SourceFileContext,
    val target: String,
    val file: Path
)

internal object VersionVariantSourceSync {
    private val key = Key.create<Boolean>("aetheria.version.variant.source.sync")

    fun isSyncing(document: Document) = document.getUserData(key) == true

    fun <T> withSync(document: Document, block: () -> T): T {
        val wasSyncing = isSyncing(document)
        document.putUserData(key, true)
        return try { block() } finally { document.putUserData(key, wasSyncing) }
    }
}

internal object VersionFileVariants {
    private val logger = Logger.getInstance(VersionFileVariants::class.java)
    private val sessions = ConcurrentHashMap<String, SyncSession>()
    private val saveListeners = ConcurrentHashMap.newKeySet<Project>()
    private val nodePattern = Regex("val\\s+(\\w+)\\s*=\\s*createNode\\(\"([^\"]+)\"")
    private val targetPattern = Regex("createNode\\(\"([^\"]+)\"")
    private val linkPattern = Regex("""(\w+)\.link\(\s*(\w+)""")
    private val generatedPathPattern = Regex("^versions/([^/]+)/build/preprocessed/main/(.+)$")

    fun context(project: Project, file: VirtualFile): SourceFileContext? {
        val path = runCatching { file.toNioPath().toAbsolutePath().normalize() }.getOrNull() ?: return null
        val root = projectRoot(project, path) ?: return null
        val relative = root.relativize(path).toString().replace('\\', '/')
        val generated = generatedPathPattern.matchEntire(relative)
        if (generated != null) {
            val target = generated.groupValues[1]
            val relativeSource = Path.of(generated.groupValues[2])
            val versionSource = root.resolve("versions").resolve(target).resolve("src/main").resolve(relativeSource)
            val sharedSource = root.resolve("src/main").resolve(relativeSource)
            val hasVersionSource = versionSource.toFile().isFile
            val source = if (hasVersionSource) versionSource else sharedSource
            if (!source.toFile().isFile) return null
            return SourceFileContext(project, root, source, relativeSource, target.takeIf { hasVersionSource }, target)
        }

        val unix = relative
        val sharedPrefix = "src/main/"
        if (unix.startsWith(sharedPrefix)) {
            val relativeSource = Path.of(unix.removePrefix(sharedPrefix))
            return SourceFileContext(project, root, path, relativeSource)
        }
        val versionPrefix = Regex("^versions/([^/]+)/src/main/(.+)$").matchEntire(unix) ?: return null
        val target = versionPrefix.groupValues[1]
        return SourceFileContext(project, root, path, Path.of(versionPrefix.groupValues[2]), target, target)
    }

    fun variants(context: SourceFileContext): List<GeneratedFileVariant> {
        return variantsForTargets(context, supportedTargets(context.root))
    }

    /** Generation can target any configured loader/version node, including intermediate Forge/Fabric nodes. */
    fun generationVariants(context: SourceFileContext): List<GeneratedFileVariant> {
        return variantsForTargets(context, configuredTargets(context.root))
    }

    private fun variantsForTargets(context: SourceFileContext, targets: List<String>): List<GeneratedFileVariant> {
        return targets.asSequence()
            .filter { context.onlyTarget == null || context.onlyTarget == it }
            .map { target ->
                val path = context.root.resolve("versions").resolve(target)
                    .resolve("build/preprocessed/main").resolve(context.relativeSource)
                GeneratedFileVariant(context, target, path)
            }
            .toList()
    }

    private fun configuredTargets(root: Path): List<String> {
        val text = runCatching { root.resolve("root.gradle.kts").toFile().readText() }.getOrDefault("")
        return targetPattern.findAll(text).map { it.groupValues[1] }.toList()
    }

    /**
     * EGT node links form a chain from the oldest supported target through intermediate
     * mapping nodes to the newest supported target. Only the root and terminal nodes are
     * user-facing variants; intermediate nodes remain build/check targets.
     */
    private fun supportedTargets(root: Path): List<String> {
        val text = runCatching { root.resolve("root.gradle.kts").toFile().readText() }.getOrDefault("")
        val nodes = nodePattern.findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
        val links = linkPattern.findAll(text).map { it.groupValues[1] to it.groupValues[2] }.toList()
        if (nodes.isEmpty() || links.isEmpty()) return configuredTargets(root)
        val childNodes = links.map { it.first }.toSet()
        val parentNodes = links.map { it.second }.toSet()
        val endpoints = nodes.keys.filter { it !in childNodes || it !in parentNodes }.toSet()
        return nodes.filterKeys { it in endpoints }.values.toList()
    }

    fun countGeneratedSources(root: File, targets: List<String>): Int = targets.sumOf { target ->
        val directory = root.toPath().resolve("versions").resolve(target).resolve("build/preprocessed/main").toFile()
        if (!directory.isDirectory) 0 else directory.walkTopDown().count { file ->
            file.isFile && (file.extension.equals("java", true) || file.extension.equals("kt", true))
        }
    }

    fun open(variant: GeneratedFileVariant) {
        val context = variant.context
        if (!VersionVariantSyncState.isSynced(context.project)) {
            Messages.showInfoMessage(context.project, "Click Sync Files in the Aetheria Versions tool window before opening a version variant.", "Sync Files Required")
            return
        }
        if (!ensureGenerated(variant)) {
            Messages.showErrorDialog(context.project, "Could not create the ${variant.target} file variant from the raw source.", "Open File Variant")
            return
        }
        val generated = variant.file.toFile()
        val vfs = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(generated) ?: return
        connectSource(vfs, variant)
        val manager = FileEditorManager.getInstance(context.project)
        manager.openFile(vfs, true)
        val editor = (manager.getSelectedEditor(vfs) as? TextEditor)?.editor
        if (editor != null) VariantDirectiveFolding.apply(editor)
    }

    private fun ensureGenerated(variant: GeneratedFileVariant): Boolean {
        if (variant.file.toFile().isFile) return true
        val raw = runCatching { variant.context.source.toFile().readText() }.getOrNull() ?: return false
        val rendered = preprocessForTarget(raw, variant.target) ?: return false
        return runCatching {
            variant.file.toFile().parentFile.mkdirs()
            variant.file.toFile().writeText(rendered)
            true
        }.getOrDefault(false)
    }

    fun openRaw(context: SourceFileContext) {
        val file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(context.source.toFile()) ?: return
        FileEditorManager.getInstance(context.project).openFile(file, true)
    }

    fun generateFromVariant(context: SourceFileContext, target: String, startOffset: Int, endOffset: Int): Boolean {
        val variants = generationVariants(context)
        val variant = variants.firstOrNull { it.target == target } ?: return false
        val sourceFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(context.source.toFile()) ?: return false
        val sourceDocument = FileDocumentManager.getInstance().getDocument(sourceFile) ?: return false
        val sourcePsi = PsiManager.getInstance(context.project).findFile(sourceFile) ?: return false
        val startLine = sourceDocument.getLineNumber(startOffset.coerceIn(0, sourceDocument.textLength))
        val endLine = sourceDocument.getLineNumber((endOffset - 1).coerceAtLeast(startOffset).coerceIn(0, sourceDocument.textLength))
        val sourceLines = splitLines(sourceDocument.text)
        val first = minOf(startLine, endLine)
        val afterLast = maxOf(startLine, endLine) + 1
        if ((first until afterLast).any { sourceLines.getOrNull(it)?.trimStart()?.startsWith("//#") == true }) {
            Messages.showErrorDialog(context.project, "Select code lines inside one block. Preprocessor directives cannot be included in the selection.", "Generate from Variant")
            return false
        }
        if (!ensureGenerated(variant)) return false
        val generatedText = runCatching { variant.file.toFile().readText() }.getOrNull() ?: return false
        val generatedToSource = alignLines(sourceDocument.text, generatedText)
        val generatedLines = splitLines(generatedText)
        val mapped = generatedToSource.withIndex().filter { (_, sourceIndex) -> sourceIndex in first until afterLast }.map { it.index }
        if (mapped.isEmpty()) {
            Messages.showErrorDialog(context.project, "The selected lines do not appear in $target. Select code that exists in the source file, or open that variant and edit it directly.", "Generate from Variant")
            return false
        }
        // A valid selected block may contain target-only lines (or blank lines that the
        // alignment cannot pair). Keep the whole target span between its first and last
        // matched source lines instead of rejecting those edits as discontinuous.
        val variantBlock = generatedLines.subList(mapped.first(), mapped.last() + 1).joinToString("\n")
        val originalBlock = sourceLines.subList(first, afterLast).joinToString("\n")
        val predicates = targetPredicates(target, configuredTargets(context.root)) ?: run {
            Messages.showErrorDialog(context.project, "Could not determine a safe Minecraft/loader condition for $target.", "Generate from Variant")
            return false
        }
        val replacement = conditionalBlock(predicates, variantBlock, originalBlock)
        val start = sourceDocument.getLineStartOffset(first)
        val end = if (afterLast < sourceDocument.lineCount) sourceDocument.getLineStartOffset(afterLast) else sourceDocument.textLength
        WriteCommandAction.runWriteCommandAction(context.project, "Generate $target Version Variant", null, {
            sourceDocument.replaceString(start, end, replacement)
            PsiDocumentManager.getInstance(context.project).commitDocument(sourceDocument)
        }, sourcePsi)
        FileDocumentManager.getInstance().saveDocument(sourceDocument)
        VersionBuildRunner.scheduleAuto(context.project)
        return true
    }

    private fun targetPredicates(target: String, targets: List<String>): List<String>? {
        val parts = target.split('-')
        if (parts.size < 2) return null
        val version = parts[0].split('.')
        if (version.size < 2 || version.take(2).any { it.toIntOrNull() == null }) return null
        val mc = "${version[0]}.${version[1]}"
        val ordered = targets.mapNotNull { name ->
            val v = name.substringBefore('-').split('.')
            if (v.size < 2 || v.take(2).any { it.toIntOrNull() == null }) null
            else (v[0].toInt() * 10000 + v[1].toInt()) to name
        }.distinctBy { it.first }.sortedBy { it.first }
        val value = version[0].toInt() * 10000 + version[1].toInt()
        val previous = ordered.lastOrNull { it.first < value }?.first
        val next = ordered.firstOrNull { it.first > value }?.first
        val predicates = mutableListOf<String>()
        if (previous != null) predicates += "MC >= $mc"
        if (next != null) predicates += "MC < ${next / 10000}.${next % 10000}"
        val loader = parts[1].uppercase()
        if (loader == "FORGE" || loader == "FABRIC") predicates += loader
        return predicates.ifEmpty { null }
    }

    private fun conditionalBlock(predicates: List<String>, variantBlock: String, fallback: String): String {
        var block = variantBlock.lines().joinToString("\n")
        for (predicate in predicates.asReversed()) {
            block = "//#if $predicate\n$block\n//#else\n${fallback.lines().joinToString("\n", transform = ::encodeFallbackLine)}\n//#endif"
        }
        return block
    }

    private data class ConditionalFrame(
        val parentActive: Boolean,
        var branchTaken: Boolean,
        var active: Boolean
    )

    private fun preprocessForTarget(source: String, target: String): String? {
        val version = target.substringBefore('-').split('.').map { it.toIntOrNull() ?: return null }
        val forge = target.substringAfter('-', "").equals("forge", ignoreCase = true)
        val stack = mutableListOf<ConditionalFrame>()
        val output = StringBuilder()

        fun active() = stack.lastOrNull()?.active ?: true
        fun condition(value: String): Boolean? {
            val normalized = value.trim()
            if (normalized == "FORGE") return forge
            if (normalized == "FABRIC") return !forge
            val match = Regex("""^MC\s*(<=|>=|<|>)\s*([0-9.]+)$""").matchEntire(normalized) ?: return null
            val bound = match.groupValues[2].split('.').map { it.toIntOrNull() ?: return null }
            val comparison = compareVersions(version, bound)
            return when (match.groupValues[1]) {
                "<" -> comparison < 0
                "<=" -> comparison <= 0
                ">" -> comparison > 0
                ">=" -> comparison >= 0
                else -> null
            }
        }

        for (line in source.split('\n')) {
            val directive = line.trim()
            when {
                directive.startsWith("//#if ") -> {
                    val selected = condition(directive.removePrefix("//#if ")) ?: return null
                    val parent = active()
                    stack.add(ConditionalFrame(parent, selected, parent && selected))
                }
                directive.startsWith("//#elseif ") -> {
                    val frame = stack.lastOrNull() ?: return null
                    val selected = condition(directive.removePrefix("//#elseif ")) ?: return null
                    frame.active = frame.parentActive && !frame.branchTaken && selected
                    frame.branchTaken = frame.branchTaken || selected
                }
                directive == "//#else" -> {
                    val frame = stack.lastOrNull() ?: return null
                    frame.active = frame.parentActive && !frame.branchTaken
                    frame.branchTaken = true
                }
                directive == "//#endif" -> if (stack.isEmpty()) return null else stack.removeAt(stack.lastIndex)
                active() -> {
                    val indent = line.takeWhile { it == ' ' || it == '\t' }
                    val content = line.substring(indent.length)
                    val decoded = if (content.startsWith("//\$\$")) {
                        val remainder = content.removePrefix("//\$\$")
                        indent + remainder.removePrefix(" ")
                    } else line
                    output.append(decoded).append('\n')
                }
            }
        }
        if (stack.isNotEmpty()) return null
        return output.toString()
    }

    private fun compareVersions(left: List<Int>, right: List<Int>): Int {
        for (index in 0 until maxOf(left.size, right.size)) {
            val comparison = (left.getOrElse(index) { 0 }).compareTo(right.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    fun attachIfGenerated(project: Project, file: VirtualFile) {
        val context = context(project, file) ?: return
        val target = context.currentTarget ?: return
        connectSource(file, GeneratedFileVariant(context, target, file.toNioPath()))
        ApplicationManager.getApplication().invokeLater {
            FileEditorManager.getInstance(project).getEditors(file).filterIsInstance<TextEditor>()
                .forEach { VariantDirectiveFolding.apply(it.editor) }
        }
    }

    private fun connectSource(generatedFile: VirtualFile, variant: GeneratedFileVariant) {
        val project = variant.context.project
        val generatedDocument = FileDocumentManager.getInstance().getDocument(generatedFile) ?: return
        val sourceFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(variant.context.source.toFile()) ?: return
        val sourceDocument = FileDocumentManager.getInstance().getDocument(sourceFile) ?: return
        val sourcePsi = PsiManager.getInstance(project).findFile(sourceFile) ?: return
        val key = "${generatedFile.url}->${sourceFile.url}"
        val old = sessions[key]
        if (old != null) return
        val session = SyncSession(
            project, sourceDocument, generatedDocument, sourcePsi, generatedDocument.text, sourceDocument.text,
            alignLines(sourceDocument.text, generatedDocument.text), variant.target
        )
        if (sessions.putIfAbsent(key, session) != null) return

        // Wait for an explicit save before writing back to the source. This coalesces fast
        // typing and paste operations into one diff, instead of racing a source write for
        // every intermediate document state.
        if (saveListeners.add(project)) {
            project.messageBus.connect(project).subscribe(FileDocumentManagerListener.TOPIC, object : FileDocumentManagerListener {
                override fun beforeDocumentSaving(document: Document) {
                    val pending = sessions.values.filter { it.project === project && it.generatedDocument === document && it.dirty }
                    if (pending.isEmpty()) return
                    ApplicationManager.getApplication().invokeLater({
                        if (!project.isDisposed) pending.forEach { if (it.dirty && !it.syncing) syncEdit(it) }
                    }, ModalityState.any())
                }
            })
        }

        generatedDocument.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (session.syncing) return
                session.dirty = true
            }
        })
    }

    private fun syncEdit(session: SyncSession) {
        val oldGenerated = session.generatedBase
        val newGenerated = session.generatedDocument.text
        val oldSource = session.sourceBase
        val oldLines = splitLines(oldGenerated)
        val sourceLines = splitLines(oldSource)
        val newLines = splitLines(newGenerated)
        var prefix = 0
        while (prefix < oldLines.size && prefix < newLines.size && oldLines[prefix] == newLines[prefix]) prefix++
        var suffix = 0
        while (suffix < oldLines.size - prefix && suffix < newLines.size - prefix &&
            oldLines[oldLines.lastIndex - suffix] == newLines[newLines.lastIndex - suffix]
        ) suffix++
        val oldLimit = oldLines.size - suffix
        val newLimit = newLines.size - suffix
        if (prefix == oldLimit && prefix == newLimit) {
            session.dirty = false
            return
        }

        val mappedSourceLines = (prefix until oldLimit).map { session.generatedToSource.getOrNull(it) ?: -1 }
        if (mappedSourceLines.any { it < 0 } || mappedSourceLines.zipWithNext().any { (a, b) -> b != a + 1 }) {
            logger.warn("Ignored a ${session.target} edit that crosses generated-only or removed EGT source lines")
            restoreGenerated(session)
            return
        }

        val sourceStartLine: Int
        val sourceEndLine: Int
        if (mappedSourceLines.isEmpty()) {
            val previous = session.generatedToSource.getOrNull(prefix - 1)?.takeIf { it >= 0 }
            val next = session.generatedToSource.getOrNull(prefix)?.takeIf { it >= 0 }
            if (previous != null && next != null && next != previous + 1) {
                logger.warn("Ignored a ${session.target} insertion at an ambiguous preprocessor boundary")
                restoreGenerated(session)
                return
            }
            sourceStartLine = next ?: (previous?.plus(1) ?: 0)
            sourceEndLine = sourceStartLine
        } else {
            sourceStartLine = mappedSourceLines.first()
            sourceEndLine = mappedSourceLines.last() + 1
        }

        val touchedGenerated = oldLines.subList(prefix, oldLimit)
        val touchedSource = sourceLines.subList(sourceStartLine, sourceEndLine.coerceAtMost(sourceLines.size))
        if (touchedGenerated.any { it.trimStart().startsWith("//\$\$") || it.trimStart().startsWith("//#") } ||
            touchedSource.any { it.trimStart().startsWith("//#") }
        ) {
            logger.info("Ignored an edit that crossed inactive code or an EGT directive in ${session.target}")
            restoreGenerated(session)
            return
        }

        val newBlock = blockText(newGenerated, prefix, newLimit)
        val newBlockLines = splitLines(newBlock)
        val replacementLines = newBlockLines.mapIndexed { index, line ->
            val generatedLineIndex = prefix + index
            val sourceLineIndex = session.generatedToSource.getOrNull(generatedLineIndex)?.takeIf { it >= 0 }
                ?: mappedSourceLines.lastOrNull()
                ?: sourceStartLine.takeIf { it < sourceLines.size }
                ?: (sourceStartLine - 1).coerceAtLeast(0)
            val oldSourceLine = sourceLines.getOrNull(sourceLineIndex).orEmpty()
            val commentIndent = when {
                isCommentEncoded(oldSourceLine) -> commentMarkerIndent(oldSourceLine)
                oldSourceLine.isBlank() || oldSourceLine.trimStart().startsWith("//#") ->
                    commentEncodingIndent(sourceLines, sourceLineIndex)
                else -> null
            }
            if (commentIndent != null) {
                encodeCommentLine(line, commentIndent)
            } else line
        }
        val replacement = replacementLines.joinToString("\n")
        val start = lineStartOffset(oldSource, sourceStartLine)
        val end = lineStartOffset(oldSource, sourceEndLine)
        if (start > oldSource.length || end > oldSource.length) {
            restoreGenerated(session)
            return
        }

        session.syncing = true
        try {
            VersionVariantSourceSync.withSync(session.sourceDocument) {
                WriteCommandAction.runWriteCommandAction(session.project, "Edit ${session.target} File Variant", null, {
                    session.sourceDocument.replaceString(start, end, replacement)
                    PsiDocumentManager.getInstance(session.project).commitDocument(session.sourceDocument)
                }, session.sourcePsi)
                FileDocumentManager.getInstance().saveDocument(session.sourceDocument)
            }
            runCatching { FileDocumentManager.getInstance().saveDocument(session.generatedDocument) }
            session.sourceBase = session.sourceDocument.text
            session.generatedBase = newGenerated
            session.generatedToSource = alignLines(session.sourceBase, session.generatedBase)
            session.dirty = false
            VersionBuildRunner.scheduleAuto(session.project)
        } catch (failure: Throwable) {
            logger.error("Failed to sync ${session.target} generated source to the EGT source file", failure)
            restoreGenerated(session)
        } finally {
            session.syncing = false
        }
    }

    private fun restoreGenerated(session: SyncSession) {
        session.syncing = true
        try {
            session.generatedDocument.setText(session.generatedBase)
            session.dirty = false
        } finally { session.syncing = false }
    }

    private fun encodeCommentLine(line: String, markerIndent: String): String {
        if (line.isBlank()) return line
        val generatedIndent = line.takeWhile { it == ' ' || it == '\t' }
        val markerPrefix = markerIndent.take(generatedIndent.length)
        val innerIndent = generatedIndent.removePrefix(markerPrefix)
        return markerPrefix + "//\$\$ " + innerIndent + line.substring(generatedIndent.length)
    }

    private fun encodeFallbackLine(line: String): String {
        if (line.isBlank()) return line
        val indent = line.takeWhile { it == ' ' || it == '\t' }.length
        return line.substring(0, indent) + "//\$\$ " + line.substring(indent)
    }

    private fun isCommentEncoded(line: String) = line.trimStart().startsWith("//\$\$")

    private fun commentMarkerIndent(line: String): String? {
        val indentLength = line.takeWhile { it == ' ' || it == '\t' }.length
        val content = line.substring(indentLength)
        return if (content.startsWith("//\$\$")) line.substring(0, indentLength) else null
    }

    private fun commentEncodingIndent(lines: List<String>, index: Int): String? {
        val candidates = sequenceOf(index - 1, index + 1, index - 2, index + 2)
        return candidates.mapNotNull { lines.getOrNull(it) }
            .filter { it.isNotBlank() && !it.trimStart().startsWith("//#") }
            .mapNotNull(::commentMarkerIndent)
            .firstOrNull()
    }

    private fun projectRoot(project: Project, path: Path): Path? {
        var cursor: Path? = if (path.toFile().isDirectory) path else path.parent
        while (cursor != null) {
            if (cursor.resolve("root.gradle.kts").toFile().isFile && cursor.resolve("versions").toFile().isDirectory) return cursor
            cursor = cursor.parent
        }
        val base = project.basePath?.let { Path.of(it).toAbsolutePath().normalize() } ?: return null
        return base.takeIf { it.resolve("root.gradle.kts").toFile().isFile && it.resolve("versions").toFile().isDirectory }
    }

    private fun splitLines(text: String): List<String> = text.split('\n')
    private fun alignLines(source: String, generated: String): IntArray {
        val sourceLines = splitLines(source)
        val generatedLines = splitLines(generated)
        // Empty lines are common across unrelated methods and should not become global
        // alignment anchors (which can make a selected method appear spread across a file).
        val sourceNormalized = sourceLines.mapIndexed { index, line ->
            normalizeForAlignment(line).ifEmpty { "\u0000source:$index" }
        }
        val generatedNormalized = generatedLines.mapIndexed { index, line ->
            normalizeForAlignment(line).ifEmpty { "\u0000generated:$index" }
        }
        if (sourceLines.size.toLong() * generatedLines.size.toLong() > 8_000_000L) {
            return greedyAlign(sourceNormalized, generatedNormalized)
        }
        val lcs = Array(sourceLines.size + 1) { IntArray(generatedLines.size + 1) }
        for (i in sourceLines.lastIndex downTo 0) {
            for (j in generatedLines.lastIndex downTo 0) {
                lcs[i][j] = if (sourceNormalized[i] == generatedNormalized[j]) {
                    lcs[i + 1][j + 1] + 1
                } else maxOf(lcs[i + 1][j], lcs[i][j + 1])
            }
        }
        val map = IntArray(generatedLines.size) { -1 }
        var i = 0
        var j = 0
        while (i < sourceLines.size && j < generatedLines.size) {
            if (sourceNormalized[i] == generatedNormalized[j]) {
                map[j] = i
                i++
                j++
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) i++ else j++
        }
        return map
    }

    private fun greedyAlign(source: List<String>, generated: List<String>): IntArray {
        val positions = mutableMapOf<String, ArrayDeque<Int>>()
        source.forEachIndexed { index, line -> positions.getOrPut(line) { ArrayDeque() }.add(index) }
        val map = IntArray(generated.size) { -1 }
        var previous = -1
        generated.forEachIndexed { index, line ->
            val position = positions[line]?.firstOrNull { it > previous }
            if (position != null) {
                map[index] = position
                previous = position
            }
        }
        return map
    }

    private fun normalizeForAlignment(line: String): String {
        var normalized = line.trimStart()
        if (normalized.startsWith("//\$\$")) normalized = normalized.removePrefix("//\$\$").trimStart()
        return normalized
    }

    private fun lineStartOffset(text: String, lineIndex: Int): Int {
        if (lineIndex <= 0) return 0
        var currentLine = 0
        for (i in text.indices) if (text[i] == '\n' && ++currentLine == lineIndex) return i + 1
        return text.length
    }

    private fun blockText(text: String, firstLine: Int, afterLastLine: Int): String {
        val start = lineStartOffset(text, firstLine)
        val end = lineStartOffset(text, afterLastLine)
        return text.substring(start.coerceAtMost(text.length), end.coerceAtLeast(start).coerceAtMost(text.length))
    }

    private data class SyncSession(
        val project: Project,
        val sourceDocument: Document,
        val generatedDocument: Document,
        val sourcePsi: com.intellij.psi.PsiFile,
        var generatedBase: String,
        var sourceBase: String,
        var generatedToSource: IntArray,
        val target: String,
        var syncing: Boolean = false,
        var dirty: Boolean = false
    )
}

private object VariantDirectiveFolding {
    private data class Block(
        val ifStart: Int,
        val firstBodyStart: Int,
        val children: MutableList<Block> = mutableListOf(),
        var elseStart: Int = -1,
        var secondBodyStart: Int = -1,
        var endifStart: Int = -1,
        var endifEnd: Int = -1
    )

    fun apply(editor: Editor) {
        val text = editor.document.text
        val roots = mutableListOf<Block>()
        val stack = mutableListOf<Block>()
        var offset = 0
        text.split('\n').forEach { line ->
            val trimmed = line.trim()
            val lineEnd = offset + line.length
            when {
                trimmed.startsWith("//#if ") -> {
                    val block = Block(offset, lineEnd + 1)
                    (stack.lastOrNull()?.children ?: roots).add(block)
                    stack.add(block)
                }
                trimmed == "//#else" && stack.isNotEmpty() -> {
                    stack.last().elseStart = offset
                    stack.last().secondBodyStart = lineEnd + 1
                }
                trimmed == "//#endif" && stack.isNotEmpty() -> {
                    stack.removeAt(stack.lastIndex).apply {
                        endifStart = offset
                        endifEnd = lineEndAfter(text, offset)
                    }
                }
            }
            offset = lineEnd + 1
        }

        val operations = mutableListOf<Pair<Int, Int>>()
        fun process(block: Block) {
            if (block.endifStart < 0) return
            val firstEnd = if (block.elseStart >= 0) block.elseStart else block.endifStart
            val secondEnd = if (block.elseStart >= 0) block.endifStart else -1
            val firstActive = score(text, block.firstBodyStart, firstEnd) >=
                (if (secondEnd >= 0) score(text, block.secondBodyStart, secondEnd) else -1)

            if (firstActive) {
                operations += block.ifStart to block.firstBodyStart
                operations += (if (block.elseStart >= 0) block.elseStart else block.endifStart) to block.endifEnd
                block.children.filter { it.ifStart >= block.firstBodyStart && it.ifStart < firstEnd }.forEach(::process)
            } else {
                operations += block.ifStart to block.secondBodyStart
                operations += block.endifStart to block.endifEnd
                block.children.filter { it.ifStart >= block.secondBodyStart && it.ifStart < secondEnd }.forEach(::process)
            }
        }
        roots.forEach(::process)

        editor.foldingModel.runBatchFoldingOperation {
            operations.distinct().sortedByDescending { it.first }.forEach { (start, end) ->
                if (start >= 0 && end > start && end <= editor.document.textLength) {
                    runCatching { editor.foldingModel.addFoldRegion(start, end, "")?.isExpanded = false }
                }
            }
        }
    }

    private fun score(text: String, start: Int, end: Int): Int {
        if (start < 0 || end < start) return 0
        return text.substring(start.coerceAtMost(text.length), end.coerceAtMost(text.length))
            .lineSequence().count { line ->
                val trimmed = line.trimStart()
                line.isNotBlank() && !trimmed.startsWith("//\$\$") && !trimmed.startsWith("//#") &&
                    !trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")
            }
    }

    private fun lineEndAfter(text: String, offset: Int): Int {
        if (offset < 0) return offset
        val next = text.indexOf('\n', offset)
        return if (next < 0) text.length else next + 1
    }
}
