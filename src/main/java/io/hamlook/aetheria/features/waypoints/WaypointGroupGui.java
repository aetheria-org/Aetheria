package io.hamlook.aetheria.features.waypoints;

import io.hamlook.aetheria.core.moulconfig.gui.GuiElement;
import io.hamlook.aetheria.features.waypoints.ui.EditorTab;
import io.hamlook.aetheria.features.waypoints.ui.HelpTab;
import io.hamlook.aetheria.features.waypoints.ui.MenuTab;
import io.hamlook.aetheria.features.waypoints.ui.Modals;
import io.hamlook.aetheria.features.waypoints.ui.RoutesTab;
import io.hamlook.aetheria.features.waypoints.ui.SettingsTab;
import io.hamlook.aetheria.features.waypoints.ui.WaypointField;
import io.hamlook.aetheria.features.waypoints.ui.WaypointUi;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.utils.LerpUtils;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class WaypointGroupGui extends GuiElement {

    public static final int TAB_ROUTES = 0;
    public static final int TAB_EDITOR = 1;

    private static final int MAX_W = 640;
    private static final int MAX_H = 420;
    private static final int TOAST_H = 14;
    private static final long TOAST_MS = 3000L;

    private static WaypointGroupGui INSTANCE;

    private final MenuTab[] tabs = {new RoutesTab(), new EditorTab(), new SettingsTab(), new HelpTab()};
    private final WaypointUi.HitList hits = new WaypointUi.HitList();
    private final List<WaypointField> fields = new ArrayList<>();
    private final List<Modals.Modal> modals = new ArrayList<>();

    private int activeTab = TAB_ROUTES;
    private String selectedRoute;
    private WaypointField activeField;

    private String toastText;
    private boolean toastOk;
    private long toastUntil;

    private WaypointUi.Hit tooltipSource;
    private long tooltipSince;
    private boolean tooltipSuppressed;
    private WaypointUi.SliderDrag sliderDrag;

    private LerpUtils.LerpingFloat tabHighlightX;
    private LerpUtils.LerpingFloat tabHighlightW;
    private LerpUtils.LerpingFloat progressFill;
    private float progressTarget = -1f;
    private LerpUtils.LerpingFloat enabledAnim;
    private LerpUtils.LerpingFloat setupAnim;
    private LerpUtils.LerpingFloat toastAlpha;
    private LerpUtils.LerpingFloat panelFade;
    private LerpUtils.LerpingFloat modalFade;

    private int px, py, pw, ph;

    public WaypointGroupGui() {
        INSTANCE = this;
        WaypointActions.clearUndo();
        KeyboardCompat.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        KeyboardCompat.enableRepeatEvents(false);
    }

    public static WaypointGroupGui current() {
        return INSTANCE;
    }

    private void layout() {
        int sw = GuiScreenUtils.getScaledWindowWidth();
        int sh = GuiScreenUtils.getScaledWindowHeight();
        pw = Math.min(MAX_W, sw - 20);
        ph = Math.min(MAX_H, sh - 20);
        px = (sw - pw) / 2;
        py = (sh - ph) / 2;
    }

    public int contentX() {
        return px + WaypointUi.SIDE_PAD;
    }

    public int contentY() {
        return py + WaypointUi.HEADER_H + WaypointUi.ACTIVE_H + WaypointUi.TAB_H + WaypointUi.PAD;
    }

    public int contentW() {
        return pw - WaypointUi.SIDE_PAD * 2;
    }

    public int contentH() {
        return py + ph - WaypointUi.FOOT_H - TOAST_H - WaypointUi.PAD - contentY();
    }

    public FontRenderer font() {
        return MinecraftCompat.getFontRenderer();
    }

    public String selectedRouteName() {
        return selectedRoute;
    }

    public WaypointGroup selectedRoute() {
        WaypointStorage storage = WaypointStorage.getInstance();
        WaypointGroup g = selectedRoute == null ? null : storage.getGroup(selectedRoute);
        if (g != null) return g;
        WaypointState state = WaypointState.getInstance();
        if (state.loadedGroup != null) g = storage.getGroup(state.loadedGroup.name);
        if (g == null) {
            for (WaypointGroup candidate : storage.getGroups().values()) {
                g = candidate;
                break;
            }
        }
        selectedRoute = g == null ? null : g.name;
        return g;
    }

    public void selectRoute(String name) {
        WaypointGroup g = name == null ? null : WaypointStorage.getInstance().getGroup(name);
        selectedRoute = g == null ? null : g.name;
    }

    public boolean isRouteActive(WaypointGroup g) {
        return WaypointActions.isActive(g);
    }

    public int activeIndex() {
        WaypointState state = WaypointState.getInstance();
        return state.hasGroup() ? state.currentIndex : -1;
    }

    public void openTab(int tab) {
        if (tab < 0 || tab >= tabs.length || tab == activeTab) return;
        WaypointActions.clearUndo();
        closeField();
        activeTab = tab;
        tabs[tab].onShow(this);
    }

    public void requestNewRoute() {
        openTab(TAB_ROUTES);
        ((RoutesTab) tabs[TAB_ROUTES]).requestNewRoute(this);
    }

    public void openEditorTab(String routeName, boolean rename) {
        selectRoute(routeName);
        openTab(TAB_EDITOR);
        if (selectedRoute() == null) return;
        if (rename) ((EditorTab) tabs[TAB_EDITOR]).requestRename();
    }

    public void toast(WaypointResult result) {
        if (result == null) return;
        toast(result.message, result.ok);
    }

    public void toast(String message, boolean ok) {
        toastText = message;
        toastOk = ok;
        toastUntil = System.currentTimeMillis() + TOAST_MS;
    }

    public WaypointField field(MenuTab owner, String tag) {
        for (WaypointField f : fields) if (f.matches(owner, tag)) return f;
        WaypointField f = new WaypointField(owner, tag);
        fields.add(f);
        return f;
    }

    public void activateField(WaypointField field) {
        if (field == null) return;
        for (WaypointField f : fields) if (f != field) f.deactivate();
        activeField = field;
        field.activate();
    }

    public void closeField() {
        if (activeField != null) activeField.deactivate();
        activeField = null;
    }

    private void commitField() {
        if (activeField == null) return;
        boolean close = activeField.submit();
        if (close) closeField();
    }

    private void cancelField() {
        if (activeField == null) return;
        WaypointField field = activeField;
        closeField();
        field.cancel();
    }

    public void openModal(Modals.Modal modal) {
        if (modal == null) return;
        closeField();
        modals.add(modal);
        modalFade = new LerpUtils.LerpingFloat(0, 150);
        modalFade.resetTimer();
    }

    public void closeModal() {
        if (!modals.isEmpty()) modals.remove(modals.size() - 1);
    }

    private Modals.Modal topModal() {
        return modals.isEmpty() ? null : modals.get(modals.size() - 1);
    }

    public void startSliderDrag(WaypointUi.SliderDrag drag, int mouseX) {
        sliderDrag = drag;
        if (drag != null) drag.applyAt(mouseX);
    }

    @Override
    public void render() {
        layout();
        FontRenderer fr = font();
        int[] mouse = KeybindHelper.getMouseCoords(GuiScreenUtils.getScaledWindowWidth(),
                GuiScreenUtils.getScaledWindowHeight());
        int mx = mouse[0], my = mouse[1];
        boolean modalOpen = topModal() != null;
        hits.clear(modalOpen ? -1 : mx, modalOpen ? -1 : my);

        if (sliderDrag != null) {
            if (MouseCompat.isButtonDown(0)) sliderDrag.applyAt(mx);
            else sliderDrag = null;
        }

        if (panelFade == null) {
            panelFade = new LerpUtils.LerpingFloat(0, 150);
            panelFade.resetTimer();
        }
        panelFade.setTarget(1);
        panelFade.tick();
        float pa = panelFade.getValue();

        WaypointUi.rect(0, 0, GuiScreenUtils.getScaledWindowWidth(),
                GuiScreenUtils.getScaledWindowHeight(), WaypointUi.withAlpha(WaypointUi.DIM, pa));
        WaypointUi.rect(px - 1, py - 1, pw + 2, ph + 2, WaypointUi.withAlpha(WaypointUi.EDGE, pa));
        WaypointUi.rect(px, py, pw, ph, WaypointUi.withAlpha(WaypointUi.PANEL, pa));
        WaypointUi.border(px, py, pw, ph, WaypointUi.LINE);

        renderHeader(fr, hits);
        renderActiveBar(fr, hits);
        renderTabs(fr, hits);

        tabs[activeTab].render(this, fr, hits, mx, my);

        if (activeField != null) activeField.render();

        renderFooter(fr, hits);
        tabs[activeTab].renderOverlay(this, fr);

        if (modalOpen) hits.setMouse(mx, my);
        renderModals(fr, hits);

        WaypointUi.Hit hovered = hits.topAt(mx, my);
        long now = System.currentTimeMillis();
        if (hovered != tooltipSource) {
            tooltipSource = hovered;
            tooltipSince = now;
            tooltipSuppressed = false;
        }
        if (hovered != null && hovered.tooltip != null && !tooltipSuppressed && now - tooltipSince >= 300
                && !tabs[activeTab].hasOverlay()) {
            List<String> lines = new ArrayList<>(1);
            lines.add(hovered.tooltip);
            TextRenderUtils.drawHoveringText(lines, mx, my, GuiScreenUtils.getScaledWindowWidth(),
                    GuiScreenUtils.getScaledWindowHeight(), 220, fr);
        }
    }

    private void renderHeader(FontRenderer fr, WaypointUi.HitList hits) {
        int y = py + (WaypointUi.HEADER_H - 8) / 2;
        WaypointUi.text(fr, Strings.HEADER_TITLE, px + WaypointUi.SIDE_PAD, y, WaypointUi.TEXT);
        WaypointUi.divider(px + WaypointUi.SIDE_PAD, py + WaypointUi.HEADER_H - 1,
                pw - WaypointUi.SIDE_PAD * 2);

        WaypointState state = WaypointState.getInstance();
        if (setupAnim == null) setupAnim = new LerpUtils.LerpingFloat(state.setupMode ? 1 : 0, 100);
        if (setupAnim.getTarget() != (state.setupMode ? 1f : 0f)) {
            setupAnim.setTarget(state.setupMode ? 1f : 0f);
            setupAnim.resetTimer();
        }
        setupAnim.tick();
        if (enabledAnim == null) enabledAnim = new LerpUtils.LerpingFloat(state.enabled ? 1 : 0, 100);
        if (enabledAnim.getTarget() != (state.enabled ? 1f : 0f)) {
            enabledAnim.setTarget(state.enabled ? 1f : 0f);
            enabledAnim.resetTimer();
        }
        enabledAnim.tick();
        int right = px + pw - WaypointUi.SIDE_PAD;
        right = headerSwitch(fr, hits, Strings.TOGGLE_SETUP, setupAnim.getValue(), right,
                Strings.TOOLTIP_SETUP,
                () -> toast(WaypointActions.setSetupMode(!state.setupMode)));
        headerSwitch(fr, hits, Strings.TOGGLE_ENABLED, enabledAnim.getValue(), right,
                Strings.TOOLTIP_ENABLED,
                () -> toast(WaypointActions.setEnabled(!state.enabled)));
    }

    private int headerSwitch(FontRenderer fr, WaypointUi.HitList hits, String label, float frac,
                             int right, String tooltip, Runnable action) {
        int toggleX = right - 30;
        int labelRight = toggleX - 6;
        int labelW = WaypointUi.width(fr, label);
        int y = py + (WaypointUi.HEADER_H - 16) / 2;
        WaypointUi.textRight(fr, label, labelRight, y + 4, WaypointUi.TEXT);
        WaypointUi.toggle(hits, toggleX, y, frac, action, tooltip);
        hits.add(labelRight - labelW, y, labelW, 16, tooltip, action);
        return labelRight - labelW - 16;
    }

    private void renderActiveBar(FontRenderer fr, WaypointUi.HitList hits) {
        int x = px + WaypointUi.SIDE_PAD;
        int y = py + WaypointUi.HEADER_H;
        int w = pw - WaypointUi.SIDE_PAD * 2;
        int h = WaypointUi.ACTIVE_H;
        WaypointUi.rect(x, y, w, h, WaypointUi.INSET);
        WaypointUi.border(x, y, w, h, WaypointUi.LINE);

        WaypointState state = WaypointState.getInstance();
        int lineY = y + 12;

        if (!state.hasGroup()) {
            WaypointUi.text(fr, Strings.ACTIVE_BAR_NONE, x + 8, lineY, WaypointUi.MUTED);
            return;
        }

        int right = x + w - 8;
        int btnY = y + (h - WaypointUi.SMALL_H) / 2 - 2;
        right = barButton(fr, hits, Strings.BTN_UNLOAD, right, btnY, true,
                () -> toast(WaypointActions.unloadRoute()), Strings.TOOLTIP_UNLOAD);
        right = barButton(fr, hits, Strings.BTN_RESTART, right, btnY, false,
                () -> toast(WaypointActions.restartRoute()), Strings.TOOLTIP_RESTART);
        right = barButton(fr, hits, Strings.BTN_NEXT, right, btnY, false,
                () -> toast(WaypointActions.skip(1)), Strings.TOOLTIP_NEXT);
        right = barButton(fr, hits, Strings.BTN_PREV, right, btnY, false,
                () -> toast(WaypointActions.skip(-1)), Strings.TOOLTIP_PREV);

        int nameX = x + 8;
        String pos = (state.currentIndex + 1) + " / " + state.size();
        int posW = WaypointUi.width(fr, pos);
        int nameMax = right - 12 - nameX - posW;
        WaypointUi.text(fr, WaypointUi.fit(fr, state.loadedGroup.name, nameMax), nameX, lineY, WaypointUi.TEXT);
        WaypointUi.textRight(fr, pos, right - 6, lineY, WaypointUi.MUTED);

        float target = (state.currentIndex + 1) / (float) Math.max(1, state.size());
        if (progressFill == null) {
            progressFill = new LerpUtils.LerpingFloat(target, 200);
            progressTarget = target;
        }
        if (progressTarget != target) {
            progressFill.setTarget(target);
            progressFill.resetTimer();
            progressTarget = target;
        }
        progressFill.tick();
        int progressLine = h - 2;
        WaypointUi.rect(x + 1, y + progressLine, w - 2, 1, WaypointUi.LINE);
        int filled = (int) ((w - 2) * progressFill.getValue());
        WaypointUi.rect(x + 1, y + progressLine, Math.max(0, filled), 1, WaypointUi.ACCENT);
    }

    private int barButton(FontRenderer fr, WaypointUi.HitList hits, String label, int right, int y,
                          boolean danger, Runnable action, String tooltip) {
        int w = WaypointUi.buttonWidth(fr, label);
        int x = right - w;
        WaypointUi.button(hits, fr, label, x, y, w, WaypointUi.SMALL_H, false, danger, action, tooltip);
        return x - 6;
    }

    private void renderTabs(FontRenderer fr, WaypointUi.HitList hits) {
        int y = py + WaypointUi.HEADER_H + WaypointUi.ACTIVE_H;
        int x = px + WaypointUi.SIDE_PAD;
        WaypointUi.divider(x, y + WaypointUi.TAB_H - 1, pw - WaypointUi.SIDE_PAD * 2);

        int activeX = x, activeW = 0, cursor = x;
        for (int i = 0; i < tabs.length; i++) {
            int w = Math.max(48, WaypointUi.width(fr, tabs[i].title()) + 24);
            if (i == activeTab) {
                activeX = cursor;
                activeW = w;
            }
            cursor += w;
        }
        if (tabHighlightX == null) {
            tabHighlightX = new LerpUtils.LerpingFloat(activeX, 120);
            tabHighlightW = new LerpUtils.LerpingFloat(activeW, 120);
        }
        if ((int) tabHighlightX.getTarget() != activeX) {
            tabHighlightX.setTarget(activeX);
            tabHighlightX.resetTimer();
        }
        tabHighlightX.tick();
        if ((int) tabHighlightW.getTarget() != activeW) {
            tabHighlightW.setTarget(activeW);
            tabHighlightW.resetTimer();
        }
        tabHighlightW.tick();
        int highlightX = Math.round(tabHighlightX.getValue());
        int highlightW = Math.round(tabHighlightW.getValue());
        WaypointUi.rect(highlightX, y, highlightW, WaypointUi.TAB_H - 1, WaypointUi.ACCENT);

        for (int i = 0; i < tabs.length; i++) {
            int w = Math.max(48, WaypointUi.width(fr, tabs[i].title()) + 24);
            boolean hover = hits.hover(x, y, w, WaypointUi.TAB_H - 1);
            if (i != activeTab && hover) {
                WaypointUi.rect(x, y, w, 1, WaypointUi.LINE);
            }
            WaypointUi.textCenter(fr, tabs[i].title(), x + w / 2, y + 7,
                    i == activeTab ? WaypointUi.ACCENT_ON : WaypointUi.MUTED);
            int index = i;
            hits.add(x, y, w, WaypointUi.TAB_H - 1, null, () -> openTab(index));
            x += w;
        }
    }

    private void renderFooter(FontRenderer fr, WaypointUi.HitList hits) {
        int footerTop = py + ph - WaypointUi.FOOT_H;

        if (toastAlpha == null) toastAlpha = new LerpUtils.LerpingFloat(0, 150);
        long now = System.currentTimeMillis();
        boolean live = toastText != null && now < toastUntil;
        float target = live ? 1f : 0f;
        if (toastAlpha.getTarget() != target) {
            toastAlpha.setTarget(target);
            toastAlpha.resetTimer();
        }
        toastAlpha.tick();
        float ta = toastAlpha.getValue();
        int ta255 = (int) (ta * 255);
        if (toastText != null) {
            if (ta255 >= 8) {
                int color = WaypointUi.withAlpha(toastOk ? WaypointUi.SUCCESS : WaypointUi.DANGER, ta);
                int x = px + WaypointUi.SIDE_PAD;
                int max = pw - WaypointUi.SIDE_PAD * 2 - (WaypointActions.hasUndo() ? 44 : 0);
                WaypointUi.text(fr, WaypointUi.fit(fr, toastText, max), x, footerTop - 12, color);
            }
            if (!live && ta <= 0.02f) toastText = null;
        }

        if (WaypointActions.hasUndo()) {
            int undoW = WaypointUi.buttonWidth(fr, Strings.BTN_UNDO);
            WaypointUi.button(hits, fr, Strings.BTN_UNDO, px + pw - WaypointUi.SIDE_PAD - undoW,
                    footerTop - 15, undoW, WaypointUi.SMALL_H, false, false,
                    () -> toast(WaypointActions.undo()), Strings.TOOLTIP_UNDO);
        }

        if (!WaypointLinks.hasSite9()) return;

        WaypointUi.divider(px + WaypointUi.SIDE_PAD, footerTop - 2, pw - WaypointUi.SIDE_PAD * 2);
        int y = footerTop + 6;
        int x = px + WaypointUi.SIDE_PAD;
        String lead = Strings.FOOTER_LEAD;
        WaypointUi.text(fr, lead, x, y, WaypointUi.MUTED);
        int linkX = x + WaypointUi.width(fr, lead);
        String link = Strings.SITE_9_TEXT;
        int linkW = WaypointUi.width(fr, link);
        boolean hover = hits.hover(linkX, y, linkW, WaypointUi.LINE_H);
        WaypointUi.text(fr, link, linkX, y, WaypointUi.ACCENT);
        if (hover) WaypointUi.rect(linkX, y + 9, linkW, 1, WaypointUi.ACCENT);
        hits.add(linkX, y, linkW, WaypointUi.LINE_H, Strings.TOOLTIP_OPEN_SITE9,
                () -> toast(WaypointLinks.openSite9()));
    }

    private void renderModals(FontRenderer fr, WaypointUi.HitList hits) {
        Modals.Modal modal = topModal();
        if (modal == null) return;
        if (modalFade == null) {
            modalFade = new LerpUtils.LerpingFloat(0, 150);
            modalFade.resetTimer();
        }
        modalFade.setTarget(1);
        modalFade.tick();
        float ma = modalFade.getValue();
        WaypointUi.rect(0, 0, GuiScreenUtils.getScaledWindowWidth(),
                GuiScreenUtils.getScaledWindowHeight(), WaypointUi.withAlpha(WaypointUi.OVERLAY, ma));
        hits.add(0, 0, GuiScreenUtils.getScaledWindowWidth(), GuiScreenUtils.getScaledWindowHeight(),
                null, this::closeModal);
        modal.render(this, fr, hits);
    }

    public float modalAlpha() {
        return modalFade == null ? 1 : modalFade.getValue();
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY) {
        if (MouseCompat.getEventButton() == -1) {
            int wheel = MouseCompat.getEventDWheel();
            if (wheel == 0) return false;
            return handleScroll(mouseX, mouseY, wheel);
        }
        return handleMouse(mouseX, mouseY, MouseCompat.getEventButton(), MouseCompat.getEventButtonState());
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY, int button, boolean pressed) {
        if (button == -1) {
            int wheel = MouseCompat.getEventDWheel();
            if (wheel == 0) return false;
            return handleScroll(mouseX, mouseY, wheel);
        }
        return handleMouse(mouseX, mouseY, button, pressed);
    }

    private boolean handleScroll(int mouseX, int mouseY, int wheel) {
        tooltipSuppressed = true;
        Modals.Modal modal = topModal();
        if (modal != null) return modal.scroll(this, wheel);
        if (activeField != null) return false;
        if (!inContent(mouseX, mouseY)) return false;
        return tabs[activeTab].scroll(this, wheel);
    }

    private boolean handleMouse(int mouseX, int mouseY, int button, boolean pressed) {
        if (!pressed) return false;
        tooltipSuppressed = true;

        if (topModal() != null) {
            hits.click(mouseX, mouseY);
            return true;
        }

        if (activeField != null) {
            if (activeField.inside(mouseX, mouseY)) {
                activeField.click(mouseX, mouseY, button);
                return true;
            }
            if (button == 0) {
                commitField();
                if (activeField != null) return true;
            }
        }

        if (tabs[activeTab].mouse(this, mouseX, mouseY, button, pressed)) return true;
        return hits.click(mouseX, mouseY);
    }

    private boolean inContent(int mouseX, int mouseY) {
        return mouseX >= contentX() && mouseX < contentX() + contentW()
                && mouseY >= contentY() && mouseY < contentY() + contentH();
    }

    @Override
    public boolean keyboardInput() {
        if (!KeyboardCompat.getEventKeyState()) return false;
        return keyboardInput(KeyboardCompat.getEventCharacter(), KeyboardCompat.getEventKey());
    }

    @Override
    public boolean keyboardInput(char typedChar, int keyCode) {
        Modals.Modal modal = topModal();
        if (modal != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) return modal.escape(this);
            return modal.key(this, typedChar, keyCode);
        }

        if (activeField != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                cancelField();
                return true;
            }
            if (keyCode == Keyboard.KEY_RETURN) {
                commitField();
                return true;
            }
            if (keyCode == Keyboard.KEY_TAB) {
                boolean handled = activeField.tab();
                if (!handled) closeField();
                return true;
            }
            activeField.type(typedChar, keyCode);
            return true;
        }

        if (tabs[activeTab].key(this, typedChar, keyCode)) return true;
        if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_4) {
            openTab(keyCode - Keyboard.KEY_1);
            return true;
        }
        if (keyCode == Keyboard.KEY_DOWN) return tabs[activeTab].scroll(this, -1);
        if (keyCode == Keyboard.KEY_UP) return tabs[activeTab].scroll(this, 1);
        return false;
    }

    @Override
    public boolean capturesEscape() {
        return topModal() != null || activeField != null || tabs[activeTab].capturesEscape();
    }
}
