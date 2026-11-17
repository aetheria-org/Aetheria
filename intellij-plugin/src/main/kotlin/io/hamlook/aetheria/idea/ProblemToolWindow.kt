package io.hamlook.aetheria.idea

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.io.File
import java.net.URI
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean

class ProblemToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = VersionProblemsPanel(project)
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel.component, "Build problems", false))
        VersionProblemsPanelRegistry.register(project, panel)
    }
}

private object VersionProblemsPanelRegistry {
    private val panels = java.util.WeakHashMap<Project, VersionProblemsPanel>()
    fun register(project: Project, panel: VersionProblemsPanel) { panels[project] = panel }
    fun get(project: Project) = panels[project]
}

internal object VersionProblemsPanelAccess {
    fun show(project: Project) {
        // The widget's click opens the tool window; the factory has created the panel by this callback.
        VersionProblemsPanelRegistry.get(project)?.showGlobalProblems()
    }
}

private class VersionProblemsPanel(private val project: Project) {
    private val output = JTextArea().apply { isEditable = false; lineWrap = false }
    private val verbose = JCheckBox("Verbose Gradle logs", false)
    private val globalProblemModel = DefaultListModel<GlobalProblem>()
    private val globalProblems = JList(globalProblemModel).apply {
        addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(event: java.awt.event.MouseEvent) {
                if (event.clickCount != 2) return
                val problem = selectedValue ?: return
                val path = problem.filePath ?: return
                val file = runCatching { File(URI(path)) }.getOrElse { File(path) }
                val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file) ?: return
                OpenFileDescriptor(project, virtualFile, (problem.line - 1).coerceAtLeast(0), (problem.column - 1).coerceAtLeast(0)).navigate(true)
            }
        })
    }
    private val syncFilesButton = JButton("Sync Files")
    private val switchVariantButton = JButton("Switch Variant...")
    private val generateVariantButton = JButton("Generate from Variant...")
    private val logTabs = JTabbedPane()
    private val queuedLogs = StringBuilder()
    private val logFlushQueued = AtomicBoolean(false)
    private val problemsByKey = LinkedHashMap<String, GlobalProblem>()
    val isVerbose get() = verbose.isSelected
    val component = JPanel(BorderLayout()).apply {
        logTabs.addTab("Build Log", JScrollPane(output))
        logTabs.addTab("Global Problems", JScrollPane(globalProblems))
        add(logTabs, BorderLayout.CENTER)
        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JPanel().apply {
                add(syncFilesButton.apply { addActionListener { runSyncFiles() } })
                add(switchVariantButton.apply { addActionListener { switchActiveVariant() } })
                add(generateVariantButton.apply { addActionListener { generateFromActiveVariant() } })
            })
            add(JPanel().apply {
                add(JButton("Check all versions").apply { addActionListener { runCheck() } })
                add(JButton("Stop").apply { addActionListener { VersionBuildRunner.cancel(project) } })
                add(JCheckBox("Auto-check after source edits", AutoCheckPreference.enabled(project)).apply {
                    addActionListener { AutoCheckPreference.setEnabled(project, isSelected) }
                })
                add(verbose)
            })
        }, BorderLayout.NORTH)
    }

    init { setFilesSynced(VersionVariantSyncState.isSynced(project)) }

    fun setFilesSynced(synced: Boolean, syncing: Boolean = false) = SwingUtilities.invokeLater {
        syncFilesButton.text = if (syncing) "Syncing Files..." else if (synced) "Sync Files (synced)" else "Sync Files"
        syncFilesButton.isEnabled = !syncing
        switchVariantButton.isEnabled = synced && !syncing
        generateVariantButton.isEnabled = synced && !syncing
    }

    private fun runSyncFiles() {
        val root = locateProjectRoot(project)
        if (root == null) {
            Messages.showErrorDialog(project, "Could not find root.gradle.kts for the Aetheria multi-version project.", "Sync Files")
            return
        }
        VersionBuildRunner.run(project, root, isVerbose, syncFiles = true)
    }

    private fun switchActiveVariant() {
        if (!VersionVariantSyncState.isSynced(project)) {
            Messages.showInfoMessage(project, "Click Sync Files first. It builds the configured Minecraft targets and updates their preprocessed files.", "Sync Files Required")
            return
        }
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val file = editor?.let { FileDocumentManager.getInstance().getFile(it.document) }
        val context = if (file != null) VersionFileVariants.context(project, file) else null
        if (context == null) {
            Messages.showInfoMessage(project, "Open a Java or Kotlin file from the Aetheria source tree first.", "Switch Variant")
            return
        }
        val available = VersionFileVariants.variants(context)
        if (available.isEmpty()) {
            Messages.showInfoMessage(project, "No target variants are available for this file yet.", "Switch Variant")
            return
        }
        val choices = (listOf("Raw File") + available.map { it.target }).toTypedArray()
        val selected = Messages.showChooseDialog(project, "Choose which version of the active file to open.", "Switch Variant", null, choices, choices.first())
        when (choices.getOrNull(selected)) {
            "Raw File" -> VersionFileVariants.openRaw(context)
            else -> available.firstOrNull { it.target == choices.getOrNull(selected) }?.let { VersionFileVariants.open(it) }
        }
    }

    private fun generateFromActiveVariant() {
        if (!VersionVariantSyncState.isSynced(project)) {
            Messages.showInfoMessage(project, "Click Sync Files first. It builds the configured Minecraft targets and updates their preprocessed files.", "Sync Files Required")
            return
        }
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null || !editor.selectionModel.hasSelection()) {
            Messages.showInfoMessage(project, "Select one or more lines in the raw source file first.", "Generate from Variant")
            return
        }
        val file = FileDocumentManager.getInstance().getFile(editor.document)
        val context = if (file != null) VersionFileVariants.context(project, file) else null
        val isRawSource = context != null && file?.let {
            runCatching { it.toNioPath().toAbsolutePath().normalize() == context.source.toAbsolutePath().normalize() }
                .getOrDefault(false)
        } == true
        if (!isRawSource || context == null) {
            Messages.showInfoMessage(project, "Open the Raw File, select the block to replace, then run Generate from Variant again.", "Generate from Variant")
            return
        }
        val variants = VersionFileVariants.generationVariants(context)
        if (variants.isEmpty()) {
            Messages.showInfoMessage(project, "No target variants are available for this file yet.", "Generate from Variant")
            return
        }
        val targets = variants.map { it.target }.toTypedArray()
        val selected = Messages.showChooseDialog(
            project,
            "Choose the target whose code should replace the selected block. The current block remains the fallback for other targets.",
            "Generate from Variant",
            null,
            targets,
            targets.first()
        )
        val target = targets.getOrNull(selected) ?: return
        VersionFileVariants.generateFromVariant(context, target, editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd)
    }

    fun append(line: String) {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        synchronized(queuedLogs) {
            queuedLogs.append("[$time] ").append(line).append('\n')
            if (queuedLogs.length > 2_000_000) {
                queuedLogs.delete(0, queuedLogs.length - 1_500_000)
                queuedLogs.insert(0, "[log] Older output trimmed to keep the IDE responsive.\n")
            }
        }
        scheduleLogFlush()
    }
    fun clear() {
        SwingUtilities.invokeLater {
            output.text = ""
            clearGlobalProblems()
        }
    }

    fun clearGlobalProblems() = SwingUtilities.invokeLater {
        synchronized(problemsByKey) { problemsByKey.clear() }
        globalProblemModel.clear()
        GlobalProblemsIndicator.setCount(project, 0)
    }

    fun addProblem(problem: GlobalProblem) = SwingUtilities.invokeLater {
        val key = "${problem.target}|${problem.filePath}|${problem.line}|${problem.column}|${problem.message}"
        val count = synchronized(problemsByKey) {
            if (!problemsByKey.containsKey(key)) {
                problemsByKey[key] = problem
                globalProblemModel.addElement(problem)
            }
            problemsByKey.size
        }
        GlobalProblemsIndicator.setCount(project, count)
        logTabs.setTitleAt(1, "Global Problems ($count)")
    }

    fun showGlobalProblems() = SwingUtilities.invokeLater { logTabs.selectedIndex = 1 }

    private fun scheduleLogFlush() {
        if (!logFlushQueued.compareAndSet(false, true)) return
        SwingUtilities.invokeLater {
            val batch = synchronized(queuedLogs) {
                val value = queuedLogs.toString()
                queuedLogs.setLength(0)
                value
            }
            if (batch.isNotEmpty()) {
                output.append(batch)
                output.caretPosition = output.document.length
            }
            logFlushQueued.set(false)
            synchronized(queuedLogs) { if (queuedLogs.isNotEmpty()) scheduleLogFlush() }
        }
    }
    private fun runCheck() = VersionBuildRunner.run(project, verbose = isVerbose)
}

private data class GlobalProblem(
    val target: String,
    val filePath: String?,
    val line: Int,
    val column: Int,
    val message: String
) {
    override fun toString(): String {
        val location = filePath?.substringAfterLast('/')?.substringAfterLast('\\')?.let { "$it:$line:$column" } ?: target
        return "[$target] $location — $message"
    }
}

private val kotlinDiagnosticPattern = Regex("^e:\\s+(file:///.*\\.(?:kt|java)):(\\d+):(\\d+)\\s+(.*)$")
private val javaDiagnosticPattern = Regex("^(.+\\.(?:java|kt)):(\\d+)(?::(\\d+))?:\\s*(?:error:\\s*)?(.*)$")

private fun parseCompilerProblem(line: String): GlobalProblem? = runCatching { parseCompilerProblemUnsafe(line) }.getOrNull()

private fun parseCompilerProblemUnsafe(line: String): GlobalProblem? {
    val kotlin = kotlinDiagnosticPattern.matchEntire(line)
    if (kotlin != null) {
        val path = kotlin.groupValues[1]
        return GlobalProblem(targetFromPath(path), path, kotlin.groupValues[2].toInt(), kotlin.groupValues[3].toInt(), kotlin.groupValues[4])
    }
    val java = javaDiagnosticPattern.matchEntire(line) ?: return null
    val path = java.groupValues[1]
    return GlobalProblem(targetFromPath(path), path, java.groupValues[2].toInt(), java.groupValues[3].ifBlank { "1" }.toInt(), java.groupValues[4])
}

private fun targetFromPath(path: String): String = Regex("/versions/([^/\\\\]+)/")
    .find(path.replace('\\', '/'))?.groupValues?.get(1) ?: "Gradle"

internal object AutoCheckPreference {
    private const val KEY = "aetheria.multiversion.autoCheck"
    fun enabled(project: Project) = PropertiesComponent.getInstance(project).getBoolean(KEY, true)
    fun setEnabled(project: Project, enabled: Boolean) = PropertiesComponent.getInstance(project).setValue(KEY, enabled)
}

/** Deliberately session scoped: each project open requires a fresh generated-source sync. */
internal object VersionVariantSyncState {
    private val syncedProjects = java.util.Collections.newSetFromMap(java.util.WeakHashMap<Project, Boolean>())
    @Synchronized fun isSynced(project: Project) = syncedProjects.contains(project)
    @Synchronized fun setSynced(project: Project, synced: Boolean) {
        if (synced) syncedProjects.add(project) else syncedProjects.remove(project)
    }
}

class CheckAllVersionsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project).getToolWindow("Aetheria Versions")
        val root = locateProjectRoot(project)
        if (root == null) {
            Messages.showErrorDialog(project, "Could not find root.gradle.kts for the Aetheria multi-version project.", "Aetheria Version Check")
            return
        }
        if (toolWindow != null) {
            toolWindow.show {
                VersionProblemsPanelRegistry.get(project)?.clear()
                VersionBuildRunner.run(project, root, VersionProblemsPanelRegistry.get(project)?.isVerbose ?: false)
            }
        } else VersionBuildRunner.run(project, root)
    }
}

private fun locateProjectRoot(project: Project): File? {
    val base = project.basePath?.let(::File) ?: return null
    return sequenceOf(base, base.parentFile).filterNotNull().firstOrNull {
        File(it, "root.gradle.kts").isFile && File(it, "versions").isDirectory
    }
}

internal object VersionBuildRunner {
    private val running = java.util.concurrent.ConcurrentHashMap<Project, Process>()
    private val inProgress = java.util.concurrent.ConcurrentHashMap.newKeySet<Project>()
    private val scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "Aetheria version check debounce").apply { isDaemon = true }
    }
    private val scheduled = java.util.concurrent.ConcurrentHashMap<Project, java.util.concurrent.ScheduledFuture<*>>()
    private val lastStarted = java.util.concurrent.ConcurrentHashMap<Project, Long>()

    fun cancel(project: Project) { running[project]?.destroyForcibly() }

    fun scheduleAuto(project: Project) {
        if (!AutoCheckPreference.enabled(project) || project.isDisposed) return
        scheduled.remove(project)?.cancel(false)
        scheduled[project] = scheduler.schedule({
            val cooldown = 5 * 60 * 1000L
            val remaining = cooldown - (System.currentTimeMillis() - (lastStarted[project] ?: 0L))
            if (remaining > 0) {
                scheduled[project] = scheduler.schedule({ startAutoCheck(project) }, remaining, java.util.concurrent.TimeUnit.MILLISECONDS)
            } else startAutoCheck(project)
        }, 30, java.util.concurrent.TimeUnit.SECONDS)
    }

    private fun startAutoCheck(project: Project) {
        if (!AutoCheckPreference.enabled(project) || project.isDisposed) return
        lastStarted[project] = System.currentTimeMillis()
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            val toolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project).getToolWindow("Aetheria Versions") ?: return@invokeLater
            toolWindow.show {
                VersionProblemsPanelRegistry.get(project)?.clear()
                run(project, verbose = VersionProblemsPanelRegistry.get(project)?.isVerbose ?: false)
            }
        }
    }

    fun run(project: Project, explicitRoot: File? = null, verbose: Boolean = false, syncFiles: Boolean = false) {
        val panel = VersionProblemsPanelRegistry.get(project)
        val root = explicitRoot ?: locateProjectRoot(project)
        if (root == null) {
            panel?.append("ERROR: Could not find root.gradle.kts.")
            return
        }
        val config = runCatching { File(root, "root.gradle.kts").readText() }.getOrElse {
            panel?.append("ERROR reading root.gradle.kts: ${it.message ?: it.javaClass.simpleName}")
            return
        }
        val targets = Regex("createNode\\(\\\"([^\\\"]+)\\\"").findAll(config).map { it.groupValues[1] }.toList()
        if (targets.isEmpty()) {
            panel?.append("ERROR: No createNode targets found in root.gradle.kts.")
            return
        }
        val wrapper = File(root, if (System.getProperty("os.name").startsWith("Windows", true)) "gradlew.bat" else "gradlew")
        if (!wrapper.isFile) {
            panel?.append("ERROR: Gradle wrapper not found at ${wrapper.absolutePath}")
            return
        }
        if (!inProgress.add(project)) {
            panel?.append("A version check is already in progress; continuing the current run.")
            return
        }
        // Gradle reads source from disk, while IntelliJ editors may still contain
        // unsaved changes. Match a terminal build by flushing those documents first.
        FileDocumentManager.getInstance().saveAllDocuments()
        // Every run compiles the configured targets, which also refreshes their
        // generated preprocessed sources. Either button therefore performs a sync.
        VersionVariantSyncState.setSynced(project, false)
        panel?.setFilesSynced(false, syncing = true)
        Thread({
            panel?.append(if (syncFiles) "Syncing preprocessed files for ${targets.size} Minecraft targets from ${root.absolutePath}" else "Checking ${targets.size} Minecraft targets from ${root.absolutePath}")
            panel?.append("Targets: ${targets.joinToString()}")
            panel?.clearGlobalProblems()
            var diagnosticCount = 0
            val gradleArgs = mutableListOf("--console=plain")
            if (verbose) gradleArgs += "--info"
            val command = if (wrapper.name.endsWith(".bat", true)) {
                listOf("cmd.exe", "/c", wrapper.absolutePath) + gradleArgs + targets.map { ":$it:build" }
            } else listOf(wrapper.absolutePath) + gradleArgs + targets.map { ":$it:build" }
            try {
                panel?.append("Gradle working directory: ${root.absolutePath}")
                panel?.append("Gradle command: ${command.joinToString(" ")}")
                val process = ProcessBuilder(command).directory(root).redirectErrorStream(true).start()
                running[project] = process
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        panel?.append(line)
                        val diagnostic = parseCompilerProblem(line)
                        if (diagnostic != null) {
                            diagnosticCount++
                            // 1.16.5 is an intermediate mapping node, not one of the
                            // user-facing Minecraft variants, so keep its diagnostics in
                            // the build log without listing them in Global Problems.
                            if (!diagnostic.target.startsWith("1.16.5-")) panel?.addProblem(diagnostic)
                        }
                    }
                }
                val code = process.waitFor()
                val generatedSourceCount = VersionFileVariants.countGeneratedSources(root, targets)
                panel?.append("Found $generatedSourceCount preprocessed Java/Kotlin target files.")
                panel?.append("Collected $diagnosticCount compiler diagnostics.")
                if (code != 0 && diagnosticCount == 0) {
                    panel?.addProblem(GlobalProblem("All targets", null, 0, 0, "Gradle version check failed with exit code $code"))
                }
                panel?.append(if (code == 0) "BUILD SUCCESSFUL: all targets compiled." else "BUILD FAILED (exit $code). See Gradle output above or rerun Gradle for full diagnostics.")
                VersionVariantSyncState.setSynced(project, code == 0)
                panel?.setFilesSynced(code == 0)
                if (code == 0) {
                    panel?.append("Preprocessed files are synced. You can now switch to and edit file variants.")
                } else if (syncFiles) {
                    panel?.append("File sync failed. Fix the build errors, then click Sync Files again.")
                } else {
                    panel?.append("Build failed, so variant switching remains locked. Fix the errors and run Check All Versions or Sync Files again.")
                }
            } catch (t: Throwable) {
                panel?.append("ERROR starting Gradle: ${t.message}")
                VersionVariantSyncState.setSynced(project, false)
                panel?.setFilesSynced(false)
            } finally {
                running.remove(project)
                inProgress.remove(project)
            }
        }, "Aetheria multi-version check").apply { isDaemon = true }.start()
    }
}
