package io.hamlook.aetheria.features.custommenu.editor;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;

/** Shared native image picker used by backgrounds and image elements. */
public final class CMMImagePicker {
    private CMMImagePicker() { }
    public static String pick() {
        FileDialog dialog = new FileDialog((Frame) null, "Select Custom Main Menu Image", FileDialog.LOAD);
        dialog.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp");
        });
        dialog.setVisible(true);
        if (dialog.getFile() == null || dialog.getDirectory() == null) return null;
        return new File(dialog.getDirectory(), dialog.getFile()).getAbsolutePath();
    }
}
