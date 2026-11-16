package io.hamlook.aetheria.features.waypoints.ui;

import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.render.TextFieldUtils;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import java.util.function.Consumer;
import java.util.function.Function;

public final class WaypointField {

    private final MenuTab owner;
    private final String tag;

    private GuiTextField field;
    private int x, y, w, h;
    private String placeholder;
    private int collapsedWidth;
    @Getter
    private boolean active;
    @Setter
    private boolean error;
    private LerpUtils.LerpingFloat openProgress;
    private int scrollX;
    private boolean closing;

    private Function<String, Boolean> onSubmit;
    private Function<String, Boolean> onTab;
    private Runnable onCancel;
    private Consumer<String> onChange;

    public WaypointField(MenuTab owner, String tag) {
        this.owner = owner;
        this.tag = tag;
    }

    public boolean matches(MenuTab owner, String tag) {
        return this.owner == owner && this.tag.equals(tag);
    }

    public WaypointField bounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = Math.max(20, w);
        this.h = h;
        return this;
    }

    public WaypointField placeholder(String text) {
        this.placeholder = text;
        return this;
    }

    public WaypointField collapsedWidth(int width) {
        this.collapsedWidth = Math.max(8, width);
        return this;
    }

    public WaypointField text(String value) {
        ensure().setText(value == null ? "" : value);
        GuiScreenUtils.cursorTextFieldToEnd(field);
        return this;
    }

    public WaypointField maxLength(int length) {
        GuiScreenUtils.setTextFieldMaxLength(ensure(), length);
        return this;
    }

    public WaypointField onSubmit(Function<String, Boolean> handler) {
        this.onSubmit = handler;
        return this;
    }

    public WaypointField onTab(Function<String, Boolean> handler) {
        this.onTab = handler;
        return this;
    }

    public WaypointField onCancel(Runnable handler) {
        this.onCancel = handler;
        return this;
    }

    public WaypointField onChange(Consumer<String> handler) {
        this.onChange = handler;
        return this;
    }

    public boolean isOpen() {
        return active || closing;
    }

    public boolean isClosing() {
        return closing;
    }

    public String value() {
        return field == null ? "" : field.getText();
    }

    public boolean submit() {
        if (onSubmit == null) return true;
        return onSubmit.apply(value());
    }

    public void cancel() {
        if (onCancel != null) onCancel.run();
    }

    public boolean tab() {
        return onTab != null && onTab.apply(value());
    }

    private GuiTextField ensure() {
        if (field == null) {
            FontRenderer fr = MinecraftCompat.getFontRenderer();
            field = GuiScreenUtils.createTextField(fr, x, y, w, h);
            GuiScreenUtils.setTextFieldBackground(field, false);
            GuiScreenUtils.setTextFieldCanLoseFocus(field, true);
            GuiScreenUtils.setTextFieldMaxLength(field, 64);
        }
        return field;
    }

    public void activate() {
        GuiTextField f = ensure();
        GuiScreenUtils.setX(f, x);
        GuiScreenUtils.setY(f, y);
        GuiScreenUtils.setW(f, w);
        GuiScreenUtils.setH(f, h);
        f.setFocused(true);
        GuiScreenUtils.cursorTextFieldToEnd(f);
        scrollX = 0;
        active = true;
        error = false;
        if (openProgress == null) openProgress = new LerpUtils.LerpingFloat(0, 150);
        if (closing) {
            closing = false;
        } else {
            openProgress.setValue(0);
        }
        openProgress.setTarget(1);
        openProgress.resetTimer();
    }

    public void deactivate() {
        if (field != null) {
            GuiScreenUtils.setTextFieldSelection(field, GuiScreenUtils.getTextFieldCursor(field));
            field.setFocused(false);
        }
        error = false;
        boolean collapse = collapsedWidth > 0 && openProgress != null && openProgress.getValue() > 0.005f;
        closing = collapse;
        if (closing) {
            openProgress.setTarget(0);
            openProgress.resetTimer();
        }
        active = false;
    }

    public void render() {
        if (!isOpen()) return;
        GuiTextField f = ensure();
        if (openProgress == null) {
            openProgress = new LerpUtils.LerpingFloat(closing ? 0 : 1, 150);
            openProgress.resetTimer();
        }
        openProgress.tick();
        float value = openProgress.getValue();
        boolean done = closing && value <= 0.005f;
        if (done) {
            openProgress.setValue(0);
            value = 0;
        }
        float e = closing ? value * value * value : 1f - (float) Math.pow(1f - value, 3);
        int basis = collapsedWidth > 0 ? Math.min(collapsedWidth, w) : w;
        int drawW = Math.max(8, Math.round(basis + (w - basis) * e));
        GuiScreenUtils.setX(f, x);
        GuiScreenUtils.setY(f, y);
        GuiScreenUtils.setW(f, drawW);
        GuiScreenUtils.setH(f, h);
        WaypointUi.rect(x, y, drawW, h, WaypointUi.INSET);
        int border;
        if (error) border = WaypointUi.DANGER;
        else if (closing) border = WaypointUi.mixArgb(WaypointUi.LINE, WaypointUi.ACCENT, e);
        else border = WaypointUi.ACCENT;
        WaypointUi.border(x, y, drawW, h, border);
        FontRenderer fr = MinecraftCompat.getFontRenderer();
        scrollX = TextFieldUtils.computeScrollX(fr, f.getText(), GuiScreenUtils.getTextFieldCursor(f), drawW, scrollX);
        DrawContextUtils.enableScissor(x, y, drawW, h);
        try {
            TextFieldUtils.renderInput(fr, f, x, y, drawW, h, scrollX);
            String text = f.getText();
            if ((text == null || text.isEmpty()) && placeholder != null && drawW > 24) {
                WaypointUi.text(fr, placeholder, x + 4, y + (h - 8) / 2, WaypointUi.MUTED);
            }
        } finally {
            DrawContextUtils.disableScissor();
        }
        if (done) closing = false;
    }

    public boolean click(int mouseX, int mouseY, int button) {
        if (!active) return false;
        if (button != 0) return false;
        ensure().mouseClicked(mouseX, mouseY, button);
        return inside(mouseX, mouseY);
    }

    public boolean type(char typedChar, int keyCode) {
        if (!active) return false;
        boolean used = GuiScreenUtils.typeTextField(ensure(), typedChar, keyCode);
        if (used) {
            error = false;
            if (onChange != null) onChange.accept(value());
        }
        return used;
    }

    public boolean inside(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
