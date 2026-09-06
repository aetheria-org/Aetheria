package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.features.custommenu.CustomMMConfig;
import io.hamlook.aetheria.features.custommenu.util.CMMHelper;
import io.hamlook.aetheria.features.custommenu.ui.sprites.Sprite;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.features.custommenu.util.PercentageUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Background source picker. All decoded media continues through GCImage. */
public class CMMBackgroundEditor extends AetheriaBaseScreen {
    private final CustomMMConfig config;
    private final GuiScreen parent;
    private final Sprite sprite;
    private GuiTextField url;
    private String message = "";
    private final ResourceLocation[] resources = {Resources.CMM_DEFAULT_BG};
    private int logicalWidth() { return PercentageUtils.width(100f); }
    private int logicalHeight() { return PercentageUtils.height(100f); }
    private int px(float percent) { return PercentageUtils.x(percent); }
    private int py(float percent) { return PercentageUtils.y(percent); }
    private int pw(float percent) { return PercentageUtils.width(percent); }
    private int ph(float percent) { return PercentageUtils.height(percent); }

    public CMMBackgroundEditor(CustomMMConfig config, GuiScreen parent) { this.config = config; this.parent = parent; this.sprite = null; }
    public CMMBackgroundEditor(Sprite sprite, GuiScreen parent) { this.config = null; this.parent = parent; this.sprite = sprite; }

    @Override protected void onInitGui() {
        ScreenHelper.updateScreenDimensions(width, height);
        url = new GuiTextField(0, MinecraftCompat.getFontRenderer(), px(28.92f), py(18.75f), pw(42.15f), ph(4.17f));
        url.setMaxStringLength(2048);
        if (sprite != null && sprite.image != null && sprite.image.url != null) url.setText(sprite.image.url);
        else if (config != null && config.background != null && config.background.url != null && config.background.url.startsWith("http")) url.setText(config.background.url);
        url.setFocused(true);
    }
    @Override public void onResize(net.minecraft.client.Minecraft mc, int w, int h) { super.onResize(mc, w, h); ScreenHelper.updateScreenDimensions(w, h); if (url != null) { url.xPosition=px(28.92f); url.yPosition=py(18.75f); url.width=pw(42.15f); url.height=ph(4.17f); } }

    @Override protected void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        int sw=logicalWidth(), sh=logicalHeight(); drawRect(0, 0, sw, sh, 0xEE121218);
        TextRenderUtils.drawCenteredStringScaleAware("Edit Background", sw / 2f, py(7.3f), 0xFFFFFFFF, 2f, true);
        TextRenderUtils.drawCenteredStringScaleAware("Paste a direct image link, upload a file, or choose a mod resource", sw / 2f, py(11.9f), 0xFFB8B8C8, 1f, false);
        drawField();
        button(px(28.92f), py(26.04f), pw(42.15f), ph(5f), "Apply Link", mouseX, mouseY);
        button(px(28.92f), py(32.92f), pw(20.49f), ph(5f), "Upload File", mouseX, mouseY);
        button(px(50.59f), py(32.92f), pw(20.49f), ph(5f), "Use Default Resource", mouseX, mouseY);
        button(px(28.92f), py(90.63f), pw(42.15f), ph(5f), "Back", mouseX, mouseY);
        if (!message.isEmpty()) TextRenderUtils.drawCenteredStringScaleAware(message, sw / 2f, py(86.5f), 0xFFFFAA55, 1f, false);
    }

    private void drawField() {
        drawRect(url.xPosition - 1, url.yPosition - 1, url.xPosition + url.width + 1, url.yPosition + url.height + 1, 0xFFFFFFFF);
        drawRect(url.xPosition, url.yPosition, url.xPosition + url.width, url.yPosition + url.height, 0xFF202026);
        url.drawTextBox();
    }

    private void button(int x, int y, int w, int h, String text, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        drawRect(x, y, x + w, y + h, hover ? 0xFF3B6982 : 0xFF292932);
        TextRenderUtils.drawCenteredStringScaleAware(text, x + w / 2f, y + h / 2f, 0xFFFFFFFF, 1f, false);
    }

    @Override protected void onMouseClicked(int mouseX, int mouseY, int button) {
        url.mouseClicked(mouseX, mouseY, button);
        if (button != 0) return;
        if (inside(mouseX, mouseY, px(28.92f), py(26.04f), pw(42.15f), ph(5f))) applyLink();
        else if (inside(mouseX, mouseY, px(28.92f), py(32.92f), pw(20.49f), ph(5f))) upload();
        else if (inside(mouseX, mouseY, px(50.59f), py(32.92f), pw(20.49f), ph(5f))) applyResource(resources[0]);
        else if (inside(mouseX, mouseY, px(28.92f), py(90.63f), pw(42.15f), ph(5f))) MinecraftCompat.getMinecraft().displayGuiScreen(parent);
    }

    private void applyLink() {
        String value = url.getText() == null ? "" : url.getText().trim();
        if (value.length() > GCImage.MAX_URL_LENGTH || !GCImage.looksLikeImageUrl(value)) { message = "Enter a valid supported image URL under 2048 characters."; return; }
        String id = GCImage.createGCImage(value);
        if (id.isEmpty()) { message = "The URL was rejected."; return; }
        if (sprite != null) sprite.image = ImageManager.images.get(id); else config.background = ImageManager.images.get(id);
        if (config != null) CMMHelper.savePreset(config);
        message = "Background loading started.";
    }

    private void upload() {
        File chosen = chooseFile();
        if (chosen == null) return;
        if (!GCImage.looksLikeImageUrl(chosen.getName()) || chosen.length() > GCImage.MAX_REMOTE_BYTES) { message = "Unsupported or oversized image file."; return; }
        try {
            File dir = new File(CMMHelper.CONFIG_FOLDER, "assets");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Could not create asset folder");
            String clean = chosen.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
            File destination = new File(dir, System.currentTimeMillis() + "_" + clean);
            Files.copy(chosen.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String id = GCImage.createGCImageFromFile(destination.getAbsolutePath());
            if (sprite != null) sprite.image = ImageManager.images.get(id); else config.background = ImageManager.images.get(id);
            if (config != null) CMMHelper.savePreset(config);
            message = "Background copied and loading.";
        } catch (Exception e) { message = "Could not copy the selected file."; }
    }

    private File chooseFile() {
        FileDialog dialog = new FileDialog((Frame) null, "Choose CMM Background", FileDialog.LOAD);
        dialog.setFilenameFilter((dir, name) -> GCImage.looksLikeImageUrl(name));
        dialog.setVisible(true);
        if (dialog.getFile() == null || dialog.getDirectory() == null) return null;
        return new File(dialog.getDirectory(), dialog.getFile());
    }

    private void applyResource(ResourceLocation resource) {
        String id = GCImage.createGCImageFromResource(resource);
        if (sprite != null) { sprite.image = null; sprite.imageLocal = resource; } else config.background = ImageManager.images.get(id);
        if (config != null) CMMHelper.savePreset(config);
        message = "Resource background loading started.";
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }

    @Override protected void onKeyTyped(char typedChar, int keyCode) { if (url.textboxKeyTyped(typedChar, keyCode)) return; if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) MinecraftCompat.getMinecraft().displayGuiScreen(parent); }
}
