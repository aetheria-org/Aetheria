package io.hamlook.aetheria.features.chat.globalchat.ui;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Toast overlay for Global Chat notifications (mentions, @everyone/@here,
 * punishments). Renders every active {@link Notification} stacked at the
 * configured position (default top right); the position and the whole toast
 * system are toggled from the Global Chat config.
 */
@RegisterEvents
public class NotificationOverlay extends Overlay {

    @Getter
    private static NotificationOverlay instance;

    /** Max pixel width of a toast body line before wrapping. */
    private static final int MAX_LINE_WIDTH = 180;
    private static final int BG_COLOR = 0xAA15151B;
    private static final int CORNER_RADIUS = 4;

    public NotificationOverlay() {
        super(MAX_LINE_WIDTH, 40);
        instance = this;
    }

    @Override
    protected int getBaseWidth() {
        return MAX_LINE_WIDTH;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.network.globalChatConfig.notificationsPosition;
    }

    @Override
    public float getScale() {
        return 1f;
    }

    @Override
    public int getBgColor() {
        return BG_COLOR;
    }

    @Override
    public int getCornerRadius() {
        return CORNER_RADIUS;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature != null
                && ATHRConfig.feature.network.globalChatConfig.notificationsEnabled
                && !GlobalChat.notifications.isEmpty();
    }

    // Toasts must stay visible while the chat GUI, tab list or debug screen is up.
    @Override
    protected boolean hideOnChat()   { return false; }
    @Override
    protected boolean hideOnTab()    { return false; }
    @Override
    protected boolean hideOnDebug()  { return false; }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> lines = new ArrayList<>();
        if (preview) {
            lines.add("§a§lMention");
            lines.add("§7§l#general §8· §7Steve: hello there!");
            return lines;
        }
        List<Notification> snapshot;
        synchronized (GlobalChat.notifications) {
            snapshot = new ArrayList<>(GlobalChat.notifications);
        }
        for (Notification notification : snapshot) {
            if (notification == null) continue;
            String header = notification.header == null || notification.header.isEmpty()
                    ? "Notification" : notification.header;
            lines.add(headerColor(header) + "§l" + header);
            if (notification.message != null && !notification.message.isEmpty()) {
                for (String line : wrap(notification.message)) lines.add(line);
            }
        }
        return lines;
    }

    private String headerColor(String header) {
        switch (header.toLowerCase()) {
            case "mention": return "§a";
            case "reply": return "§b";
            case "@everyone":
            case "@here": return "§c";
            default: return "§e";
        }
    }

    private List<String> wrap(String text) {
        List<String> out = new ArrayList<>();
        while (!text.isEmpty() && out.size() < 3) {
            String line = MinecraftCompat.getFontRenderer().trimStringToWidth(text, MAX_LINE_WIDTH);
            if (line.isEmpty()) line = text;
            out.add(line);
            if (line.length() >= text.length()) break;
            text = text.substring(line.length()).trim();
        }
        return out;
    }
}
