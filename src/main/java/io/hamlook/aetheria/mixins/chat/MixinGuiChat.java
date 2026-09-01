package io.hamlook.aetheria.mixins.chat;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.chat.PlayerButtonsConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.chat.ChatLineHook;
import io.hamlook.aetheria.features.chat.GuiChatHook;
import io.hamlook.aetheria.features.chat.GuiNewChatHook;
import io.hamlook.aetheria.features.chat.PlayerNameButtonListener;
import io.hamlook.aetheria.features.chat.emoji.EmojiSuggestionBar;
import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI;
import io.hamlook.aetheria.features.qol.ChatStateManager;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen implements GuiChatHook {

    @Shadow
    protected GuiTextField inputField;

    @Unique
    private static boolean athr$disabled() {
        return ATHRConfig.feature == null || !ATHRConfig.feature.chat.emojiConfig.enabled || !ATHRConfig.feature.chat.emojiConfig.suggestionsEnabled;
    }

    @Override
    public boolean athr$isTypingMode() {
        return inputField != null && inputField.isFocused();
    }

    // ── Player name buttons ──────────────────────────────────────────────────

    @Unique private static final String[] athr$MENU_LABELS = {
            "Whisper", "View Profile", "Auction House", "Party Invite", "Add Friend", "Ignore", "Visit Island", "Visit Garden"
    };

    /** Index of the one entry that pastes into the chat box instead of running immediately. */
    @Unique private static final int athr$MENU_PASTE_INDEX = 0;

    /** Index of the View Profile entry, which can open the ASM GUI directly instead of a command. */
    @Unique private static final int athr$MENU_VIEW_PROFILE_INDEX = 1;

    @Unique private static final int athr$MENU_ROW_H = 14;
    @Unique private static final int athr$MENU_HEADER_H = 14;
    @Unique private static final int athr$MENU_WIDTH = 110;

    @Unique private String athr$menuPlayer = null;
    @Unique private int athr$menuX = 0;
    @Unique private int athr$menuY = 0;

    @Unique
    private static String athr$commandFor(int index, String name) {
        switch (index) {
            case 0: return "/msg " + name;
            case 1: return "/viewprofile " + name;
            case 2: return "/ah " + name;
            case 3: return "/p invite " + name;
            case 4: return "/f add " + name;
            case 5: return "/ignore add " + name;
            case 6: return "/visit " + name;
            case 7: return "/visitgarden " + name;
            default: return null;
        }
    }

    @Unique
    private int athr$menuHeight() {
        return athr$MENU_HEADER_H + athr$MENU_LABELS.length * athr$MENU_ROW_H + 4;
    }

    @Unique
    private int athr$menuLeft() {
        return Math.max(2, Math.min(athr$menuX, this.width - athr$MENU_WIDTH - 2));
    }

    @Unique
    private int athr$menuTop() {
        return Math.max(2, Math.min(athr$menuY, this.height - athr$menuHeight() - 2));
    }

    @Unique
    private static int athr$menuBackgroundColor() {
        String color = ATHRConfig.feature != null
                ? ATHRConfig.feature.chat.playerButtons.backgroundColor
                : PlayerButtonsConfig.DEFAULT_BACKGROUND_COLOR;
        return ChromaColour.specialToChromaRGB(color);
    }

    @Unique
    private static int athr$menuAccentColor() {
        String color = ATHRConfig.feature != null
                ? ATHRConfig.feature.chat.playerButtons.accentColor
                : PlayerButtonsConfig.DEFAULT_ACCENT_COLOR;
        return ChromaColour.specialToChromaRGB(color);
    }

    @Unique
    private int athr$cogLeft() {
        return athr$menuLeft() + athr$MENU_WIDTH - athr$MENU_HEADER_H;
    }

    /** Draws the popup menu on top of everything else once a name has been clicked. */
    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void athr$drawPlayerMenu(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (athr$menuPlayer == null) return;

        int bx = athr$menuLeft();
        int by = athr$menuTop();
        int boxH = athr$menuHeight();
        int cogX = athr$cogLeft();

        int bg = athr$menuBackgroundColor();
        int bgAlpha = (bg >>> 24) & 0xFF;
        int bgFade = ((bgAlpha / 2) << 24) | (bg & 0x00FFFFFF);
        int accent = athr$menuAccentColor();
        int hoverColor = (accent & 0x00FFFFFF) | 0x80000000;

        RenderUtils.drawGradientRect(0, bx, by, bx + athr$MENU_WIDTH, by + boxH, bg, bgFade);
        drawRect(bx, by, bx + athr$MENU_WIDTH, by + athr$MENU_HEADER_H, accent);
        drawCenteredString(fontRendererObj, athr$menuPlayer, bx + (athr$MENU_WIDTH - athr$MENU_HEADER_H) / 2, by + 3, 0xFFFFFFFF);

        boolean cogHovered = mouseX >= cogX && mouseX <= bx + athr$MENU_WIDTH && mouseY >= by && mouseY < by + athr$MENU_HEADER_H;
        if (cogHovered) drawRect(cogX, by, bx + athr$MENU_WIDTH, by + athr$MENU_HEADER_H, hoverColor);
        drawCenteredString(fontRendererObj, "⚙", cogX + athr$MENU_HEADER_H / 2, by + 3, 0xFFFFFFFF);

        for (int i = 0; i < athr$MENU_LABELS.length; i++) {
            int ry = by + athr$MENU_HEADER_H + 2 + i * athr$MENU_ROW_H;
            boolean hovered = mouseX >= bx && mouseX <= bx + athr$MENU_WIDTH && mouseY >= ry && mouseY < ry + athr$MENU_ROW_H;
            if (hovered) drawRect(bx + 1, ry, bx + athr$MENU_WIDTH - 1, ry + athr$MENU_ROW_H, hoverColor);
            drawString(fontRendererObj, athr$MENU_LABELS[i], bx + 6, ry + 3, hovered ? 0xFFFFFFFF : 0xFFB5BAC1);
        }
    }

    /** Close the popup with Escape instead of letting it fall through to closing chat. */
    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void athr$onKeyTypedMenu(char typedChar, int keyCode, CallbackInfo ci) {
        if (athr$menuPlayer != null && keyCode == Keyboard.KEY_ESCAPE) {
            athr$menuPlayer = null;
            ci.cancel();
        }
    }

    /**
     * Declared before the copy-on-click handler below so it always gets first
     * refusal on a click: while the menu is open it consumes every click
     * (selecting a row or dismissing), and otherwise it looks for a click on a
     * name tagged by {@link PlayerNameButtonListener} to open the menu.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void athr$onPlayerMenuClick(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (athr$menuPlayer != null) {
            if (mouseButton == 0) {
                int bx = athr$menuLeft();
                int by = athr$menuTop();
                int cogX = athr$cogLeft();

                if (mouseX >= cogX && mouseX <= bx + athr$MENU_WIDTH && mouseY >= by && mouseY < by + athr$MENU_HEADER_H) {
                    ATHRConfig.openSubcategory("Chat Utils", "playerButtons");
                } else {
                    for (int i = 0; i < athr$MENU_LABELS.length; i++) {
                        int ry = by + athr$MENU_HEADER_H + 2 + i * athr$MENU_ROW_H;
                        if (mouseX >= bx && mouseX <= bx + athr$MENU_WIDTH && mouseY >= ry && mouseY < ry + athr$MENU_ROW_H) {
                            if (i == athr$MENU_VIEW_PROFILE_INDEX
                                    && !(ATHRConfig.feature != null && ATHRConfig.feature.chat.playerButtons.useIngameViewProfile)) {
                                ATHRConfig.screenToOpen = new ProfileViewerGUI(athr$menuPlayer);
                            } else {
                                String cmd = athr$commandFor(i, athr$menuPlayer);
                                if (cmd != null) {
                                    if (i == athr$MENU_PASTE_INDEX) {
                                        inputField.setText(cmd);
                                        inputField.setFocused(true);
                                    } else {
                                        ChatUtils.sendChatCommand(cmd);
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
            athr$menuPlayer = null;
            ci.cancel();
            return;
        }

        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.playerButtons.enabled) return;
        if (mouseButton != 0) return;

        // GuiNewChat#getChatComponent expects raw LWJGL mouse coords (like vanilla's own
        // call in this method), not the already-GUI-scaled mouseX/mouseY parameters.
        IChatComponent comp = Minecraft.getMinecraft().ingameGUI.getChatGUI()
                .getChatComponent(Mouse.getX(), Mouse.getY());
        if (comp == null) return;

        ChatStyle style = comp.getChatStyle();
        ClickEvent click = style.getChatClickEvent();
        if (click == null || click.getAction() != ClickEvent.Action.SUGGEST_COMMAND) return;

        String value = click.getValue();
        if (value == null || !value.startsWith(PlayerNameButtonListener.MARKER_PREFIX)) return;

        athr$menuPlayer = value.substring(PlayerNameButtonListener.MARKER_PREFIX.length());
        athr$menuX = mouseX;
        athr$menuY = mouseY;
        ci.cancel();
    }

    // ── Copy on click ─────────────────────────────────────────────────────────

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ATHR$onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (!athr$disabled() && inputField != null && EmojiSuggestionBar.handleMouseClick(mouseX, mouseY, mouseButton, inputField)) {
            ci.cancel();
            return;
        }
        if (ATHRConfig.feature == null || !ATHRConfig.feature.chat.chatCopyEnabled) return;
        if (mouseButton != 0) return;
        if (!GuiScreen.isShiftKeyDown() && !GuiScreen.isCtrlKeyDown()) return;
        GuiNewChatHook chatGUI = (GuiNewChatHook) Minecraft.getMinecraft().ingameGUI.getChatGUI();
        ChatLine line = chatGUI.athr$getCurrentHoveredLine();
        if (line == null) return;
        boolean formatted = ATHRConfig.feature.chat.chatCopyFormatted;
        String text;
        if (GuiScreen.isCtrlKeyDown()) {
            String raw = line.getChatComponent().getFormattedText();
            text = formatted ? raw : EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        } else {
            IChatComponent fullMsg = ((ChatLineHook) line).athr$getFullMessage();
            IChatComponent src = (fullMsg != null) ? fullMsg : line.getChatComponent();
            String raw = src.getFormattedText();
            text = formatted ? raw : EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        }
        GuiScreen.setClipboardString(text);
        ci.cancel();
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void ATHR$onKeyTypedHead(char typedChar, int keyCode, CallbackInfo ci) {
        if (athr$disabled() || inputField == null) return;
        if (EmojiSuggestionBar.handleKeyTypedPre(keyCode, inputField)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyTyped", at = @At("RETURN"))
    private void ATHR$onKeyTypedReturn(char typedChar, int keyCode, CallbackInfo ci) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.qol.chatStateRestore) {
            if (keyCode != Keyboard.KEY_ESCAPE && keyCode != Keyboard.KEY_RETURN) {
                ChatStateManager.getInstance().updateState(inputField.getText());
            } else {
                ChatStateManager.getInstance().resetState();
            }
        }
        if (!athr$disabled() && inputField != null) {
            EmojiSuggestionBar.handleKeyTypedPost(keyCode, inputField);
        }
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"), cancellable = true)
    private void ATHR$onHandleMouseInput(CallbackInfo ci) {
        if (athr$disabled()) return;
        if (EmojiSuggestionBar.handleMouseWheel(Mouse.getEventDWheel())) {
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void ATHR$drawSuggestions(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (athr$disabled()) return;
        EmojiSuggestionBar.render(inputField, mouseX, mouseY);
        EmojiSuggestionBar.tickDrag(mouseY);
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    public void ATHR$chatStateInit(CallbackInfo ci) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.chatStateRestore) return;
        if (ChatStateManager.getInstance().shouldRestore()) {
            inputField.setText(ChatStateManager.getInstance().getSavedText());
        }
    }
}
