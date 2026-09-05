package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.util.CMMHelper;
import net.minecraft.client.gui.GuiScreen;

public final class CMMClipboard {
    private CMMClipboard() { }
    public static void copy(CMMElement element) {
        if (element != null) GuiScreen.setClipboardString(CMMHelper.GSON.toJson(element, CMMElement.class));
    }
    public static CMMElement paste() {
        try { String value = GuiScreen.getClipboardString(); return value == null || value.trim().isEmpty() ? null : CMMHelper.GSON.fromJson(value, CMMElement.class); }
        catch (RuntimeException ignored) { return null; }
    }
    public static void copyPreset(io.hamlook.aetheria.features.custommenu.CustomMMConfig config) {
        if (config != null) GuiScreen.setClipboardString(CMMHelper.GSON.toJson(config));
    }
    public static io.hamlook.aetheria.features.custommenu.CustomMMConfig pastePreset() {
        try { String value=GuiScreen.getClipboardString(); return value==null||value.trim().isEmpty()?null:CMMHelper.GSON.fromJson(value,io.hamlook.aetheria.features.custommenu.CustomMMConfig.class); }
        catch (RuntimeException ignored) { return null; }
    }
}
