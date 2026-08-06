package io.hamlook.aetheria.features.diana.party.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.WebSocketClient;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.diana.party.DianaPartyConnector;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Compact non-fullscreen party finder styled after ChatFilterGUI (BetterContainers
 * 9-slice background). Lists every active Diana party in a scrollable grid with a
 * Join button per party; your own party shows a Leave/Disband button instead, and
 * joining is blocked while you are already in a party.
 */
public class DPartyGUI extends GuiScreen {

    private static final int COLUMNS = 2;

    private int boxX, boxY, boxW, boxH;
    private int listX, listY, listW, listH;
    private int gridX;
    private int gridY;
    private int gridPadY;
    private int cardW, cardH, rowH, gapX;
    private int btnW, btnH;
    private float textScale;

    private final List<PartyEntry> parties = new ArrayList<>();
    private boolean loading = true;
    private String loadError = null;
    private int scrollY = 0;
    private int dragMode = 0;
    private int dragStartY = 0;
    private int dragStartScrollY = 0;

    private String toast = null;
    private long toastUntil = 0;

    private PartyEntry passwordPromptParty = null;
    private GuiTextField passwordField;
    private boolean busy = false;

    public static class PartyEntry {
        public String partyID;
        public String name;
        public String creator;
        public boolean hasPassword;
        public int memberCount;
        public List<String> members = new ArrayList<>();
    }

    public static void open() {
        ATHRConfig.screenToOpen = new DPartyGUI();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        passwordField = new GuiTextField(0, fontRendererObj, 0, 0, 180, 16);

        boxW = getScaledX(525);
        boxH = getScaledY(390);
        boxX = (width - boxW) / 2;
        boxY = (height - boxH) / 2;

        textScale = ResolutionUtils.getXStatic(1) * 2.6f;

        listX = boxX + getScaledX(10);
        listY = boxY + getScaledY(54);
        listW = boxW - getScaledX(20);
        listH = boxH - getScaledY(64);

        int gridPadX = getScaledX(6);
        gridPadY = getScaledY(6);
        gridX = listX + gridPadX;
        gridY = listY + gridPadY;

        gapX = getScaledX(12);
        cardW = (listW - gridPadX * 2 - gapX) / COLUMNS;
        cardH = getScaledY(54);
        rowH = cardH + getScaledY(10);

        btnW = getScaledX(76);
        btnH = getScaledY(26);

        DianaPartyConnector.setPartyListListener(this::applyParties);
        fetchParties();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        DianaPartyConnector.setPartyListListener(null);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        passwordField.updateCursorCounter();
    }

    private int getScaledX(double entry) {
        return (int) (ResolutionUtils.getXStatic(1) * entry * 2.0);
    }

    private int getScaledY(double entry) {
        return (int) (ResolutionUtils.getYStatic(1) * entry * 2.0);
    }

    private String myName() {
        return Minecraft.getMinecraft().getSession().getUsername().toLowerCase();
    }

    private boolean isMyParty(PartyEntry party) {
        if (party.creator != null && party.creator.equalsIgnoreCase(myName())) return true;
        for (String m : party.members) {
            if (m.equalsIgnoreCase(myName())) return true;
        }
        return false;
    }

    private PartyEntry findMyParty() {
        for (PartyEntry p : parties) {
            if (isMyParty(p)) return p;
        }
        return null;
    }

    private int gridRows() {
        return (parties.size() + COLUMNS - 1) / COLUMNS;
    }

    private int totalH() {
        return gridRows() * rowH + gridPadY * 2;
    }

    private int maxScroll() {
        return Math.max(0, totalH() - listH);
    }

    /** Applies a live party-list push from the server (called on the MC thread). */
    private void applyParties(List<JsonObject> fetched) {
        this.parties.clear();
        for (JsonObject o : fetched) {
            PartyEntry p = new PartyEntry();
            p.partyID = o.has("partyID") ? o.get("partyID").getAsString() : "";
            p.name = o.has("name") ? o.get("name").getAsString() : "Unknown";
            p.creator = o.has("creator") ? o.get("creator").getAsString() : "";
            p.hasPassword = o.has("hasPassword") && o.get("hasPassword").getAsBoolean();
            p.memberCount = o.has("memberCount") ? o.get("memberCount").getAsInt() : 0;
            if (o.has("members") && o.get("members").isJsonArray()) {
                for (JsonElement m : o.getAsJsonArray("members")) p.members.add(m.getAsString());
            }
            this.parties.add(p);
        }
        this.parties.sort(Comparator.comparingInt(p -> isMyParty(p) ? 0 : 1));
        loading = false;
        loadError = null;
        scrollY = Math.max(0, Math.min(scrollY, maxScroll()));
    }

    private void fetchParties() {
        if (!WebSocketClient.isConnected) {
            loading = false;
            loadError = "Not connected to the API. Run /dparty again to reconnect.";
            DianaPartyConnector.connectToAPI();
            return;
        }
        loading = true;
        loadError = null;
        CompletableFuture<String> future = DianaPartyConnector.listParties();
        if (future == null) {
            loading = false;
            loadError = "Not connected to the API.";
            return;
        }
        future.whenComplete((response, ex) -> mc.addScheduledTask(() -> {
            if (ex != null) {
                loading = false;
                loadError = "Could not load parties.";
                return;
            }
            try {
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonObject data = json.getAsJsonObject("data");
                int code = data.get("code").getAsInt();
                if (code == 200 && data.has("parties") && data.get("parties").isJsonArray()) {
                    List<JsonObject> fetched = new ArrayList<>();
                    for (JsonElement e : data.getAsJsonArray("parties")) {
                        if (e.isJsonObject()) fetched.add(e.getAsJsonObject());
                    }
                    applyParties(fetched);
                } else {
                    loading = false;
                    loadError = "Could not load parties (code " + code + ")";
                }
            } catch (Exception parseErr) {
                loading = false;
                loadError = "Could not load parties.";
            }
        }));
    }

    // ---------------------------------------------------------------- input

    private int[] refreshButtonRect() {
        return new int[]{boxX + boxW - getScaledX(125), boxY + getScaledY(12), getScaledX(80), getScaledY(28)};
    }

    private int[] closeButtonRect() {
        int s = refreshButtonRect()[3];
        return new int[]{boxX + boxW - s - getScaledX(10), boxY + getScaledY(12), s, s};
    }

    private int[] buttonRect(int cardX, int cardY) {
        return new int[]{cardX + cardW - btnW - getScaledX(10), cardY + (cardH - btnH) / 2, btnW, btnH};
    }

    private boolean inRect(int mouseX, int mouseY, int[] r) {
        return mouseX >= r[0] && mouseX <= r[0] + r[2] && mouseY >= r[1] && mouseY <= r[1] + r[3];
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (passwordPromptParty != null) {
            passwordField.mouseClicked(mouseX, mouseY, mouseButton);
            int[] box = passwordBoxRect();
            int joinX = box[0] + box[2] / 2 - 78, joinY = box[1] + box[3] - 30;
            if (mouseX >= joinX && mouseX <= joinX + 74 && mouseY >= joinY && mouseY <= joinY + 20) {
                joinParty(passwordPromptParty, passwordField.getText().trim());
            } else if (mouseX >= joinX + 82 && mouseX <= joinX + 156 && mouseY >= joinY && mouseY <= joinY + 20) {
                passwordPromptParty = null;
            }
            return;
        }

        if (inRect(mouseX, mouseY, refreshButtonRect())) {
            fetchParties();
            return;
        }
        if (inRect(mouseX, mouseY, closeButtonRect())) {
            mc.displayGuiScreen(null);
            return;
        }

        if (tryStartScrollbarDrag(mouseX, mouseY, boxX + boxW - getScaledX(18), listY, listH, totalH(), scrollY, maxScroll())) {
            dragMode = 1;
            dragStartY = mouseY;
            dragStartScrollY = scrollY;
            return;
        }

        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            PartyEntry mine = findMyParty();
            for (int r = 0; r < gridRows(); r++) {
                int cy = gridY + r * rowH - scrollY;
                if (mouseY < cy || mouseY > cy + cardH) continue;
                for (int c = 0; c < COLUMNS; c++) {
                    int index = r * COLUMNS + c;
                    if (index >= parties.size()) break;
                    PartyEntry party = parties.get(index);
                    int cx = gridX + c * (cardW + gapX);
                    if (inRect(mouseX, mouseY, buttonRect(cx, cy))) {
                        if (isMyParty(party)) {
                            if (party.creator != null && party.creator.equalsIgnoreCase(myName())) {
                                disbandParty();
                            } else {
                                leaveParty();
                            }
                        } else if (mine != null) {
                            flashToast("§cYou're already in a Diana Party. Leave or disband it first.");
                        } else if (party.hasPassword) {
                            passwordPromptParty = party;
                            passwordField.setText("");
                            passwordField.setFocused(true);
                        } else {
                            joinParty(party, "");
                        }
                        return;
                    }
                }
            }
            if (maxScroll() > 0) {
                dragMode = 2;
                dragStartY = mouseY;
                dragStartScrollY = scrollY;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (passwordPromptParty != null) {
            if (passwordField.textboxKeyTyped(typedChar, keyCode)) return;
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                joinParty(passwordPromptParty, passwordField.getText().trim());
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                passwordPromptParty = null;
            }
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || passwordPromptParty != null) return;
        scrollY = Math.max(0, Math.min(maxScroll(), scrollY + (wheel > 0 ? -getScaledY(24) : getScaledY(24))));
    }

    private void joinParty(PartyEntry party, String password) {
        if (busy) return;
        if (findMyParty() != null) {
            passwordPromptParty = null;
            flashToast("§cYou're already in a Diana Party. Leave or disband it first.");
            return;
        }
        busy = true;
        CompletableFuture<String> future = DianaPartyConnector.joinParty(party.partyID, password);
        if (future == null) {
            busy = false;
            flashToast("§cNot connected to the API. Reconnecting...");
            passwordPromptParty = null;
            DianaPartyConnector.connectToAPI();
            return;
        }
        future.whenComplete((response, ex) -> mc.addScheduledTask(() -> {
            busy = false;
            passwordPromptParty = null;
            if (ex != null) {
                flashToast("§cCould not join party.");
                return;
            }
            try {
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonObject data = json.getAsJsonObject("data");
                int code = data.get("code").getAsInt();
                if (code == 200) {
                    String name = data.has("partyName") ? data.get("partyName").getAsString() : party.name;
                    ChatUtils.sendMessage("§aSuccessfully Joined Diana Party: " + name);
                    flashToast("§aJoined " + name + "!");
                    fetchParties();
                } else {
                    String msg = data.has("message") ? data.get("message").getAsString() : "Unknown error";
                    flashToast("§c" + msg);
                }
            } catch (Exception parseErr) {
                flashToast("§cCould not join party.");
            }
        }));
    }

    private void leaveParty() {
        if (busy) return;
        busy = true;
        CompletableFuture<String> future = DianaPartyConnector.leaveParty();
        if (future == null) {
            busy = false;
            flashToast("§cNot connected to the API. Reconnecting...");
            DianaPartyConnector.connectToAPI();
            return;
        }
        future.whenComplete((response, ex) -> mc.addScheduledTask(() -> {
            busy = false;
            if (ex != null) {
                flashToast("§cCould not leave party.");
                return;
            }
            try {
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonObject data = json.getAsJsonObject("data");
                int code = data.get("code").getAsInt();
                if (code == 200) {
                    flashToast("§aYou left the party.");
                    fetchParties();
                } else {
                    String msg = data.has("message") ? data.get("message").getAsString() : "Could not leave party.";
                    flashToast("§c" + msg);
                }
            } catch (Exception parseErr) {
                flashToast("§cCould not leave party.");
            }
        }));
    }

    private void disbandParty() {
        if (busy) return;
        busy = true;
        CompletableFuture<String> future = DianaPartyConnector.disbandParty();
        if (future == null) {
            busy = false;
            flashToast("§cNot connected to the API. Reconnecting...");
            DianaPartyConnector.connectToAPI();
            return;
        }
        future.whenComplete((response, ex) -> mc.addScheduledTask(() -> {
            busy = false;
            if (ex != null) {
                flashToast("§cCould not disband party.");
                return;
            }
            try {
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                JsonObject data = json.getAsJsonObject("data");
                int code = data.get("code").getAsInt();
                if (code == 200) {
                    flashToast("§aParty disbanded.");
                    fetchParties();
                } else {
                    String msg = data.has("message") ? data.get("message").getAsString() : "Could not disband party.";
                    flashToast("§c" + msg);
                }
            } catch (Exception parseErr) {
                flashToast("§cCould not disband party.");
            }
        }));
    }

    private void flashToast(String text) {
        toast = text;
        toastUntil = System.currentTimeMillis() + 4000;
    }

    // ---------------------------------------------------------------- render

    private int[] passwordBoxRect() {
        int bw = 320, bh = 110;
        return new int[]{(width - bw) / 2, (height - bh) / 2, bw, bh};
    }

    private ResourceLocation bcNineSlice() {
        return Resources.betterContainerNineSlice(ATHRConfig.feature.qol.betterContainers.style);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GlStateManager.color(0.18f, 0.18f, 0.18f, 1f);
        NineSliceUtils.draw(bcNineSlice(), boxX, boxY, boxW, boxH, 6, 18);
        GlStateManager.color(1f, 1f, 1f, 1f);

        drawHeader(mouseX, mouseY);

        GlStateManager.color(0.12f, 0.12f, 0.12f, 1f);
        NineSliceUtils.draw(bcNineSlice(), listX, listY, listW, listH, 6, 18);
        GlStateManager.color(1f, 1f, 1f, 1f);

        if (loading) {
            TextRenderUtils.drawStringScaleAware("Loading parties...", listX + getScaledX(14), listY + getScaledY(14), textScale * 0.85f, false);
        } else if (loadError != null) {
            TextRenderUtils.drawStringScaled(loadError, fontRendererObj, listX + getScaledX(14), listY + getScaledY(14), true, 0xFFFF6B6B, textScale * 0.85f);
        } else if (parties.isEmpty()) {
            TextRenderUtils.drawStringScaleAware("No Diana parties are active right now.", listX + getScaledX(14), listY + getScaledY(14), textScale * 0.85f, false);
        } else {
            drawPartyGrid(mouseX, mouseY);
        }

        drawScrollbar(boxX + boxW - getScaledX(18), listY, listH, totalH(), scrollY, maxScroll());

        if (passwordPromptParty != null) {
            drawPasswordPrompt(mouseX, mouseY);
        }

        if (toast != null && System.currentTimeMillis() < toastUntil) {
            int tw = fontRendererObj.getStringWidth(toast);
            int tx = Math.max(getScaledX(10), (width - tw) / 2 - 12);
            drawRect(tx - 6, height - getScaledY(30), tx + tw + 12, height - getScaledY(12), 0xE62B2D31);
            fontRendererObj.drawStringWithShadow(toast, tx, height - getScaledY(25), 0xFFFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawHeader(int mouseX, int mouseY) {
        TextRenderUtils.drawStringScaleAware("Diana Parties", boxX + getScaledX(20), boxY + getScaledY(16), textScale * 0.9f, false);

        int[] refresh = refreshButtonRect();
        boolean refreshHover = inRect(mouseX, mouseY, refresh);
        GlStateManager.color(refreshHover ? 0.27f : 0.21f, refreshHover ? 0.27f : 0.21f, refreshHover ? 0.31f : 0.21f, 1f);
        NineSliceUtils.draw(bcNineSlice(), refresh[0], refresh[1], refresh[2], refresh[3], 6, 18);
        GlStateManager.color(1f, 1f, 1f, 1f);
        TextRenderUtils.drawCenteredStringScaleAware("Refresh", refresh[0] + refresh[2] / 2f, refresh[1] + refresh[3] / 2f, textScale * 0.7f, false);

        int[] close = closeButtonRect();
        boolean closeHover = inRect(mouseX, mouseY, close);
        GlStateManager.color(closeHover ? 0.88f : 0.21f, closeHover ? 0.35f : 0.21f, closeHover ? 0.35f : 0.21f, 1f);
        NineSliceUtils.draw(bcNineSlice(), close[0], close[1], close[2], close[3], 6, 18);
        GlStateManager.color(1f, 1f, 1f, 1f);
        TextRenderUtils.drawCenteredStringScaleAware("✕", close[0] + close[2] / 2f, close[1] + close[3] / 2f, textScale * 0.85f, false);
    }

    private void drawPartyGrid(int mouseX, int mouseY) {
        startScissor(listX, listY, listW, listH);

        for (int r = 0; r < gridRows(); r++) {
            int cy = gridY + r * rowH - scrollY;
            if (cy + cardH < listY || cy > listY + listH) continue;
            for (int c = 0; c < COLUMNS; c++) {
                int index = r * COLUMNS + c;
                if (index >= parties.size()) break;
                drawPartyCard(parties.get(index), gridX + c * (cardW + gapX), cy, mouseX, mouseY);
            }
        }
        stopScissor();
    }

    private void drawPartyCard(PartyEntry party, int cx, int cy, int mouseX, int mouseY) {
        boolean mine = isMyParty(party);

        GlStateManager.color(mine ? 0.24f : 0.22f, mine ? 0.24f : 0.22f, mine ? 0.28f : 0.22f, 1f);
        NineSliceUtils.draw(bcNineSlice(), cx, cy, cardW, cardH, 6, 18);
        GlStateManager.color(1f, 1f, 1f, 1f);

        int textMaxW = cardW - btnW - getScaledX(24);
        int textX = cx + getScaledX(12);

        String title = party.name + (party.hasPassword ? " \uD83D\uDD12" : "");
        String creator = "by " + (party.creator == null ? "unknown" : party.creator);

        String membersText;
        if (mine) {
            membersText = party.creator != null && party.creator.equalsIgnoreCase(myName())
                    ? "§6You created this party"
                    : "§aYou are in this party";
        } else if (party.members.isEmpty()) {
            membersText = party.memberCount > 0 ? party.memberCount + " member(s)" : "No members yet";
        } else {
            membersText = party.memberCount + ": " + String.join(", ", party.members);
        }

        float titleScale = textScale * 1.1f;
        float subScale = textScale * 0.75f;
        title = fontRendererObj.trimStringToWidth(title, (int) (textMaxW / titleScale));
        creator = fontRendererObj.trimStringToWidth(creator, (int) (textMaxW / subScale));
        membersText = fontRendererObj.trimStringToWidth(membersText, (int) (textMaxW / subScale));

        TextRenderUtils.drawStringScaled(title, fontRendererObj, textX, cy + getScaledY(8), true, 0xFFFFFFFF, titleScale);
        TextRenderUtils.drawStringScaled(creator, fontRendererObj, textX, cy + getScaledY(22), true, 0xFF949BA4, subScale);
        TextRenderUtils.drawStringScaled(membersText, fontRendererObj, textX, cy + getScaledY(36), true, 0xFFB5BAC1, subScale);

        int[] btn = buttonRect(cx, cy);
        boolean onBtn = inRect(mouseX, mouseY, btn);

        String label;
        float r, g, b;
        if (mine) {
            boolean isCreator = party.creator != null && party.creator.equalsIgnoreCase(myName());
            label = isCreator ? "Disband" : "Leave";
            r = 0.55f;
            g = 0.23f;
            b = 0.23f;
            if (onBtn) {
                r = 0.88f;
                g = 0.35f;
                b = 0.35f;
            }
        } else {
            label = party.hasPassword ? "Join \uD83D\uDD12" : "Join";
            r = 0.25f;
            g = 0.26f;
            b = 0.29f;
            if (onBtn) {
                r = 0.35f;
                g = 0.40f;
                b = 0.95f;
            }
        }
        GlStateManager.color(r, g, b, 1f);
        NineSliceUtils.draw(bcNineSlice(), btn[0], btn[1], btnW, btnH, 6, 18);
        GlStateManager.color(1f, 1f, 1f, 1f);
        TextRenderUtils.drawCenteredStringScaleAware("§" + (mine ? "c" : "a" ) +label, btn[0] + btnW / 2f, btn[1] + btnH / 2f, textScale * 0.7f, false);
    }

    private void drawPasswordPrompt(int mouseX, int mouseY) {
        int[] box = passwordBoxRect();
        drawRect(0, 0, width, height, 0x66000000);
        drawRect(box[0], box[1], box[0] + box[2], box[1] + box[3], 0xFF313338);
        drawRect(box[0], box[1], box[0] + box[2], box[1] + 2, 0xFFF2C94C);

        drawCenteredString(fontRendererObj, "Password required", box[0] + box[2] / 2, box[1] + 16, 0xFFFFFFFF);
        drawCenteredString(fontRendererObj, fontRendererObj.trimStringToWidth(passwordPromptParty.name, box[2] - 24),
                box[0] + box[2] / 2, box[1] + 28, 0xFF949BA4);

        passwordField.xPosition = box[0] + (box[2] - 180) / 2;
        passwordField.yPosition = box[1] + 44;
        passwordField.drawTextBox();

        int joinX = box[0] + box[2] / 2 - 78, joinY = box[1] + box[3] - 30;
        boolean joinHover = mouseX >= joinX && mouseX <= joinX + 74 && mouseY >= joinY && mouseY <= joinY + 20;
        drawRect(joinX, joinY, joinX + 74, joinY + 20, joinHover ? 0xFF5865F2 : 0xFF404249);
        drawCenteredString(fontRendererObj, busy ? "..." : "Join", joinX + 37, joinY + 6, 0xFFFFFFFF);

        boolean cancelHover = mouseX >= joinX + 82 && mouseX <= joinX + 156 && mouseY >= joinY && mouseY <= joinY + 20;
        drawRect(joinX + 82, joinY, joinX + 156, joinY + 20, cancelHover ? 0xFF35373C : 0xFF2B2D31);
        drawCenteredString(fontRendererObj, "Cancel", joinX + 119, joinY + 6, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------ scrollbar

    private void startScissor(int x, int y, int width, int height) {
        ScaledResolution res = new ScaledResolution(mc);
        int scale = res.getScaleFactor();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, mc.displayHeight - (y + height) * scale, width * scale, height * scale);
    }

    private void stopScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawScrollbar(int trackX, int listY, int listH, int totalH, int scrollY, int maxScroll) {
        if (maxScroll <= 0) return;
        int barW = getScaledX(5);
        drawRect(trackX, listY, trackX + barW, listY + listH, 0x44000000);
        int thumbH = Math.max(getScaledY(20), listH * listH / Math.max(listH, totalH));
        int thumbY = listY + (int) ((float) scrollY / maxScroll * (listH - thumbH));
        drawRect(trackX, thumbY, trackX + barW, thumbY + thumbH, 0xFFAAAAAA);
    }

    private boolean tryStartScrollbarDrag(int mouseX, int mouseY, int trackX, int listY, int listH, int totalH, int scrollY, int maxScroll) {
        if (maxScroll <= 0) return false;
        int barW = getScaledX(5);
        int thumbH = Math.max(getScaledY(20), listH * listH / Math.max(listH, totalH));
        int thumbY = listY + (int) ((float) scrollY / maxScroll * (listH - thumbH));
        return mouseX >= trackX - getScaledX(4) && mouseX <= trackX + barW + getScaledX(4) && mouseY >= thumbY && mouseY <= thumbY + thumbH;
    }

    private int clampScroll(int scroll, int maxScroll) {
        return Math.max(0, Math.min(scroll, maxScroll));
    }
}
