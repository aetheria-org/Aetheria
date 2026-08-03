package io.hamlook.aetheria.features.diana.party.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.WebSocketClient;
import io.hamlook.aetheria.features.diana.party.DianaPartyConnector;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Lists every active Diana party (name, creator, members) fetched from the
 * cape API, with a Join button per party. Password-protected parties open a
 * small password prompt; open parties join directly.
 */
public class DPartyGUI extends GuiScreen {

    private static final int HEADER_H = 30;
    private static final int ROW_H = 58;
    private static final int PADDING = 10;
    private static final int JOIN_W = 62;

    private final List<PartyEntry> parties = new ArrayList<>();
    private boolean loading = true;
    private String loadError = null;
    private int scroll = 0;

    private String toast = null;
    private long toastUntil = 0;

    private PartyEntry passwordPromptParty = null;
    private GuiTextField passwordField;
    private boolean joining = false;

    public static class PartyEntry {
        public String partyID;
        public String name;
        public String creator;
        public boolean hasPassword;
        public int memberCount;
        public List<String> members = new ArrayList<>();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        passwordField = new GuiTextField(0, fontRendererObj, 0, 0, 180, 16);
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

    public static void open() {
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(new DPartyGUI());
    }

    /** Applies a live party-list push from the server (called on the MC thread). */
    private void applyParties(List<JsonObject> parties) {
        this.parties.clear();
        for (JsonObject o : parties) {
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
        loading = false;
        loadError = null;
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

        int closeX = width - 24;
        if (mouseX >= closeX && mouseX < closeX + 16 && mouseY >= 7 && mouseY < 23) {
            mc.displayGuiScreen(null);
            return;
        }
        if (mouseX >= width - 74 && mouseX <= width - 40 && mouseY >= 8 && mouseY <= 22) {
            fetchParties();
            return;
        }

        int listTop = HEADER_H;
        int areaW = width - PADDING * 2;
        int y = listTop - scroll;
        for (PartyEntry party : parties) {
            if (y + ROW_H > listTop && y < height - PADDING) {
                int bx = width - PADDING - JOIN_W - 8;
                int by = y + (ROW_H - 22) / 2;
                if (mouseX >= bx && mouseX <= bx + JOIN_W && mouseY >= by && mouseY <= by + 22) {
                    if (party.hasPassword) {
                        passwordPromptParty = party;
                        passwordField.setText("");
                        passwordField.setFocused(true);
                    } else {
                        joinParty(party, "");
                    }
                    break;
                }
            }
            y += ROW_H + 6;
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
        int total = parties.size() * (ROW_H + 6);
        int maxScroll = Math.max(0, total - (height - HEADER_H - PADDING));
        scroll = Math.max(0, Math.min(maxScroll, scroll + (wheel > 0 ? -24 : 24)));
    }

    private void joinParty(PartyEntry party, String password) {
        if (joining) return;
        joining = true;
        CompletableFuture<String> future = DianaPartyConnector.joinParty(party.partyID, password);
        if (future == null) {
            joining = false;
            flashToast("§cNot connected to the API. Reconnecting...");
            passwordPromptParty = null;
            DianaPartyConnector.connectToAPI();
            return;
        }
        future.whenComplete((response, ex) -> mc.addScheduledTask(() -> {
            joining = false;
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

    private void flashToast(String text) {
        toast = text;
        toastUntil = System.currentTimeMillis() + 4000;
    }

    // ---------------------------------------------------------------- render

    private int[] passwordBoxRect() {
        int bw = 320, bh = 110;
        return new int[]{(width - bw) / 2, (height - bh) / 2, bw, bh};
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xFF313338);
        drawHeader(mouseX, mouseY);
        drawPartyList(mouseX, mouseY);

        if (passwordPromptParty != null) {
            drawPasswordPrompt(mouseX, mouseY);
        }

        if (toast != null && System.currentTimeMillis() < toastUntil) {
            int tw = fontRendererObj.getStringWidth(toast);
            int tx = Math.max(PADDING, (width - tw) / 2 - 12);
            drawRect(tx - 6, height - 30, tx + tw + 12, height - 12, 0xE62B2D31);
            fontRendererObj.drawStringWithShadow(toast, tx, height - 25, 0xFFFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawHeader(int mouseX, int mouseY) {
        drawRect(0, 0, width, HEADER_H, 0xFF2B2D31);
        drawRect(0, HEADER_H - 1, width, HEADER_H, 0xFF1E1F22);
        fontRendererObj.drawStringWithShadow("Diana Parties", PADDING, (HEADER_H - fontRendererObj.FONT_HEIGHT) / 2f, 0xFFFFFFFF);

        boolean refreshHover = mouseX >= width - 74 && mouseX <= width - 40 && mouseY >= 8 && mouseY <= 22;
        drawRect(width - 74, 8, width - 40, 22, refreshHover ? 0xFF404249 : 0xFF35373C);
        fontRendererObj.drawStringWithShadow("Refresh", width - 68, 12, 0xFFB5BAC1);

        int closeX = width - 24;
        boolean closeHover = mouseX >= closeX && mouseX < closeX + 16 && mouseY >= 7 && mouseY < 23;
        fontRendererObj.drawStringWithShadow("X", closeX + 4, 10, closeHover ? 0xFFFFFFFF : 0xFF949BA4);
    }

    private void drawPartyList(int mouseX, int mouseY) {
        int listTop = HEADER_H;
        int areaW = width - PADDING * 2;

        if (loading) {
            fontRendererObj.drawStringWithShadow("Loading parties...", PADDING, listTop + 14, 0xFF949BA4);
            return;
        }
        if (loadError != null) {
            fontRendererObj.drawStringWithShadow(loadError, PADDING, listTop + 14, 0xFFFF6B6B);
            return;
        }
        if (parties.isEmpty()) {
            fontRendererObj.drawStringWithShadow("No Diana parties are active right now.", PADDING, listTop + 14, 0xFF949BA4);
            return;
        }

        int y = listTop - scroll;
        for (PartyEntry party : parties) {
            if (y + ROW_H > listTop && y < height - PADDING) {
                drawPartyRow(party, y, areaW, mouseX, mouseY);
            }
            y += ROW_H + 6;
        }
    }

    private void drawPartyRow(PartyEntry party, int y, int areaW, int mouseX, int mouseY) {
        drawRect(PADDING, y, PADDING + areaW, y + ROW_H, 0xFF2B2D31);
        drawRect(PADDING, y, PADDING + 3, y + ROW_H, party.hasPassword ? 0xFFF2C94C : 0xFF5865F2);

        int textW = areaW - JOIN_W - 24;

        String title = party.name + (party.hasPassword ? " \uD83D\uDD12" : "");
        fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(title, textW), PADDING + 12, y + 6, 0xFFFFFFFF);

        String creator = "by " + (party.creator == null ? "unknown" : party.creator);
        fontRendererObj.drawStringWithShadow(creator, PADDING + 12, y + 18, 0xFF949BA4);

        String membersText;
        if (party.members.isEmpty()) {
            membersText = party.memberCount > 0 ? party.memberCount + " member(s)" : "0 members";
        } else {
            membersText = party.memberCount + " member(s): " + String.join(", ", party.members);
        }
        fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(membersText, textW), PADDING + 12, y + 30, 0xFFB5BAC1);

        int bx = width - PADDING - JOIN_W - 8;
        int by = y + (ROW_H - 22) / 2;
        boolean hover = mouseX >= bx && mouseX <= bx + JOIN_W && mouseY >= by && mouseY <= by + 22;
        drawRect(bx, by, bx + JOIN_W, by + 22, hover ? 0xFF5865F2 : 0xFF404249);
        fontRendererObj.drawStringWithShadow(party.hasPassword ? "Join \uD83D\uDD12" : "Join",
                bx + (JOIN_W - fontRendererObj.getStringWidth(party.hasPassword ? "Join \uD83D\uDD12" : "Join")) / 2f,
                by + 7, 0xFFFFFFFF);
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
        drawCenteredString(fontRendererObj, joining ? "..." : "Join", joinX + 37, joinY + 6, 0xFFFFFFFF);

        boolean cancelHover = mouseX >= joinX + 82 && mouseX <= joinX + 156 && mouseY >= joinY && mouseY <= joinY + 20;
        drawRect(joinX + 82, joinY, joinX + 156, joinY + 20, cancelHover ? 0xFF35373C : 0xFF2B2D31);
        drawCenteredString(fontRendererObj, "Cancel", joinX + 119, joinY + 6, 0xFFFFFFFF);
    }
}
