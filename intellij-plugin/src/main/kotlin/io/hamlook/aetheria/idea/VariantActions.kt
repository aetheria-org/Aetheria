package io.hamlook.aetheria.idea

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager

class SwitchVariantAction : ActionGroup(
    "Open File Variant",
    "Open the generated Java or Kotlin source for a Minecraft target",
    null
) {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val project = e?.project ?: return emptyArray()
        if (!VersionVariantSyncState.isSynced(project)) return emptyArray()
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return emptyArray()
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return emptyArray()
        val context = VersionFileVariants.context(project, virtualFile) ?: return emptyArray()
        val raw = object : AnAction("Raw File", "Open the editable source with preprocessor directives", null) {
            override fun actionPerformed(event: AnActionEvent) = VersionFileVariants.openRaw(context)
        }
        return (listOf(raw) + VersionFileVariants.variants(context).map { variant ->
            object : AnAction(variant.target) {
                override fun actionPerformed(event: AnActionEvent) {
                    VersionFileVariants.open(variant)
                }
            }
        }).toTypedArray()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        val editor = e.getData(CommonDataKeys.EDITOR)
        val virtualFile = editor?.let { FileDocumentManager.getInstance().getFile(it.document) }
        val context = if (project != null && virtualFile != null) VersionFileVariants.context(project, virtualFile) else null
        e.presentation.isEnabledAndVisible = VersionVariantSyncState.isSynced(project) &&
            context != null && VersionFileVariants.variants(context).isNotEmpty()
    }
}

class GenerateFromVariantAction : AnAction("Generate from Variant", "Replace the selected source block with code from a Minecraft variant", null) {
    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = editor?.let { com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(it.document) }
        val context = if (project != null && file != null) VersionFileVariants.context(project, file) else null
        val isRawSource = context != null && file?.let { runCatching { it.toNioPath().toAbsolutePath().normalize() == context.source.toAbsolutePath().normalize() }.getOrDefault(false) } == true
        e.presentation.isEnabledAndVisible = VersionVariantSyncState.isSynced(project) && isRawSource &&
            editor?.selectionModel?.hasSelection() == true && VersionFileVariants.generationVariants(context!!).isNotEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (e.project?.let(VersionVariantSyncState::isSynced) != true) return
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        if (!editor.selectionModel.hasSelection()) return
        val file = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(editor.document) ?: return
        val context = VersionFileVariants.context(project, file) ?: return
        if (runCatching { file.toNioPath().toAbsolutePath().normalize() != context.source.toAbsolutePath().normalize() }.getOrDefault(true)) return
        val variants = VersionFileVariants.generationVariants(context)
        if (variants.isEmpty()) return
        val targets = variants.map { it.target }.toTypedArray()
        val chosenIndex = com.intellij.openapi.ui.Messages.showChooseDialog(
            project,
            "Choose the target variant whose selected code should replace this block. The current block becomes the fallback for other targets.",
            "Generate from Variant",
            null,
            targets,
            targets.first()
        )
        val chosen = targets.getOrNull(chosenIndex) ?: return
        VersionFileVariants.generateFromVariant(context, chosen, editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd)
    }
}
