package io.hamlook.aetheria.features.diana;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.diana.overlays.DianaEventOverlay;
import io.hamlook.aetheria.features.diana.overlays.DianaLootOverlay;
import io.hamlook.aetheria.features.diana.overlays.DianaMobHealthOverlay;
import io.hamlook.aetheria.features.diana.overlays.InquisitorOverlay;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class GuiDianaOverlayEditor extends AetheriaBaseScreen {

    private final GuiScreen parentScreen;
    private final Runnable saveCallback;
    private final OverlayEntry[] overlays;
    private int draggedIndex = -1;
    private int focusedIndex = -1;
    private int grabbedX, grabbedY;
    public GuiDianaOverlayEditor(GuiScreen parent, Runnable saveCallback) {
        this.parentScreen = parent;
        this.saveCallback = saveCallback;

        DianaEventOverlay event = DianaEventOverlay.getInstance();
        DianaLootOverlay loot = DianaLootOverlay.getInstance();
        InquisitorOverlay inq = InquisitorOverlay.getInstance();
        DianaMobHealthOverlay mob = DianaMobHealthOverlay.getInstance();

        overlays = new OverlayEntry[]{new OverlayEntry("Event", ATHRConfig.feature.diana.eventOverlay.eventOverlayPos, event::getOverlayWidth, event::getOverlayHeight, () -> ATHRConfig.feature.diana.eventOverlay.eventScale, () -> event.render(true)),

                new OverlayEntry("Loot", ATHRConfig.feature.diana.lootOverlay.lootOverlayPos, loot::getOverlayWidth, loot::getOverlayHeight, () -> ATHRConfig.feature.diana.lootOverlay.lootScale, () -> loot.render(true)),

                new OverlayEntry(" ", ATHRConfig.feature.diana.inquisitorHp.inqHealthPos, inq::getOverlayWidth, inq::getOverlayHeight, () -> ATHRConfig.feature.diana.inquisitorHp.inqScale, () -> inq.render(true)),

                new OverlayEntry(" ", ATHRConfig.feature.diana.dianaMobHp.dianaMobHealthPos, mob::getOverlayWidth, mob::getOverlayHeight, () -> ATHRConfig.feature.diana.dianaMobHp.mobScale, () -> mob.render(true)),};
    }

    @Override
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        int[] m = KeybindHelper.getMouseCoords(sr);
        mouseX = m[0];
        mouseY = m[1];
        this.width = sr.getScaledWidth();
        this.height = sr.getScaledHeight();

        drawDefaultBackground();

        if (draggedIndex >= 0) {
            OverlayEntry e = overlays[draggedIndex];
            grabbedX += e.position.moveX(mouseX - grabbedX, e.scaledW(), sr);
            grabbedY += e.position.moveY(mouseY - grabbedY, e.scaledH(), sr);
        }

        for (OverlayEntry e : overlays) {
            e.renderer.run();
            int x = e.position.getAbsX(sr, e.scaledW());
            int y = e.position.getAbsY(sr, e.scaledH());
            if (e.position.isCenterX()) x -= e.scaledW() / 2;
            if (e.position.isCenterY()) y -= e.scaledH() / 2;
            Gui.drawRect(x, y, x + e.scaledW(), y + e.scaledH(), 0x80404040);
            MinecraftCompat.getFontRenderer().drawStringWithShadow(e.label, x + 2, y + 2, 0xFFFFFF);
        }

        Utils.drawStringCentered("Diana Overlay Editor", MinecraftCompat.getFontRenderer(), width / 2, 8, true, 0xFFFFFF);
        Utils.drawStringCentered("Drag overlays to move | R = reset all | ESC = back", MinecraftCompat.getFontRenderer(), width / 2, 18, true, 0xAAAAAA);
    }

    @Override
    protected void onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        int[] m = KeybindHelper.getMouseCoords(width, height);
        mouseX = m[0];
        mouseY = m[1];

        for (int i = 0; i < overlays.length; i++) {
            OverlayEntry e = overlays[i];
            int x = e.position.getAbsX(sr, e.scaledW());
            int y = e.position.getAbsY(sr, e.scaledH());
            if (e.position.isCenterX()) x -= e.scaledW() / 2;
            if (e.position.isCenterY()) y -= e.scaledH() / 2;
            if (mouseX >= x && mouseX <= x + e.scaledW() && mouseY >= y && mouseY <= y + e.scaledH()) {
                draggedIndex = focusedIndex = i;
                grabbedX = mouseX;
                grabbedY = mouseY;
                break;
            }
        }
    }

    @Override
    protected void onMouseReleased(int mouseX, int mouseY, int state) {
        if (draggedIndex >= 0) {
            saveCallback.run();
            draggedIndex = -1;
        }
    }

    @Override
    protected void onMouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggedIndex < 0) return;

        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        int[] m = KeybindHelper.getMouseCoords(width, height);
        mouseX = m[0];
        mouseY = m[1];

        OverlayEntry e = overlays[draggedIndex];
        grabbedX += e.position.moveX(mouseX - grabbedX, e.scaledW(), sr);
        grabbedY += e.position.moveY(mouseY - grabbedY, e.scaledH(), sr);
        saveCallback.run();
    }

    @Override
    protected void onKeyTyped(char typedChar, int keyCode) {
        KeyboardCompat.enableRepeatEvents(true);

        if (keyCode == Keyboard.KEY_ESCAPE) {
            saveCallback.run();
            mc.displayGuiScreen(parentScreen);
            return;
        }

        if (keyCode == Keyboard.KEY_R) {
            for (OverlayEntry e : overlays) e.position.set(e.originalPosition);
            saveCallback.run();
        }

        if (focusedIndex >= 0) {
            OverlayEntry e = overlays[focusedIndex];
            ScaledResolution sr = GuiScreenUtils.getScaledResolution();
            int dist = (KeyboardCompat.isKeyDown(Keyboard.KEY_LSHIFT) || KeyboardCompat.isKeyDown(Keyboard.KEY_RSHIFT)) ? 10 : 1;
            if (keyCode == Keyboard.KEY_DOWN) e.position.moveY(dist, e.scaledH(), sr);
            else if (keyCode == Keyboard.KEY_UP) e.position.moveY(-dist, e.scaledH(), sr);
            else if (keyCode == Keyboard.KEY_LEFT) e.position.moveX(-dist, e.scaledW(), sr);
            else if (keyCode == Keyboard.KEY_RIGHT) e.position.moveX(dist, e.scaledW(), sr);
            saveCallback.run();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static class OverlayEntry {
        final String label;
        final Position position;
        final Position originalPosition;
        final IntSupplier w, h;
        final Supplier<Float> scaleSupplier;
        final Runnable renderer;

        OverlayEntry(String label, Position position, IntSupplier w, IntSupplier h, Supplier<Float> scaleSupplier, Runnable renderer) {
            this.label = label;
            this.position = position;
            this.originalPosition = position.clone();
            this.w = w;
            this.h = h;
            this.scaleSupplier = scaleSupplier;
            this.renderer = renderer;
        }

        float scale() {
            return scaleSupplier.get();
        }

        int scaledW() {
            return (int) (w.getAsInt() * scale());
        }

        int scaledH() {
            return (int) (h.getAsInt() * scale());
        }
    }
}