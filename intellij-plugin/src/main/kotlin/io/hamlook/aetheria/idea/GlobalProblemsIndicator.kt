package io.hamlook.aetheria.idea

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.InspectionWidgetActionProvider
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.Icon

/** Adds the global build-problem indicator to IntelliJ's editor inspection widget (top-right). */
class GlobalProblemsInspectionWidgetActionProvider : InspectionWidgetActionProvider {
    override fun createAction(editor: Editor): AnAction? {
        val project = editor.project ?: return null
        return GlobalProblemsWidgetAction(project)
    }
}

private class GlobalProblemsWidgetAction(private val project: Project) : AnAction() {
    override fun update(event: AnActionEvent) {
        val count = GlobalProblemsIndicator.count(project)
        event.presentation.isVisible = count > 0
        event.presentation.isEnabled = count > 0
        event.presentation.text = "$count Aetheria global problems"
        event.presentation.description = event.presentation.text
        event.presentation.icon = GlobalProblemsIcon(count)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Aetheria Versions") ?: return
        toolWindow.show { VersionProblemsPanelAccess.show(project) }
    }
}

private class GlobalProblemsIcon(private val count: Int) : Icon {
    private val countText = count.toString()
    override fun getIconWidth() = 8 + countText.length * 7
    override fun getIconHeight() = 16

    override fun paintIcon(component: Component?, graphics: Graphics, x: Int, y: Int) {
        val g = graphics.create() as Graphics2D
        try {
            g.color = Color(0xF2, 0xA0, 0x32)
            g.stroke = BasicStroke(1.5f)
            g.drawLine(x + 2, y + 2, x + 2, y + 14)
            g.fillPolygon(intArrayOf(x + 3, x + 10, x + 7, x + 3), intArrayOf(y + 3, y + 4, y + 8, y + 7), 4)
            g.font = g.font.deriveFont(Font.BOLD, 10f)
            g.drawString(countText, x + 11, y + 11)
        } finally {
            g.dispose()
        }
    }
}

internal object GlobalProblemsIndicator {
    private val counts = java.util.WeakHashMap<Project, Int>()

    @Synchronized
    fun count(project: Project) = counts[project] ?: 0

    fun setCount(project: Project, count: Int) {
        synchronized(this) { counts[project] = count }
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            FileEditorManager.getInstance(project).allEditors.filterIsInstance<TextEditor>()
                .forEach { refreshInspectionAction(it.editor.component) }
        }
    }

    private fun refreshInspectionAction(component: Component) {
        if (component is ActionToolbar && component.actions.any { it is GlobalProblemsWidgetAction }) {
            component.updateActionsImmediately()
        }
        if (component is Container) component.components.forEach(::refreshInspectionAction)
    }
}
