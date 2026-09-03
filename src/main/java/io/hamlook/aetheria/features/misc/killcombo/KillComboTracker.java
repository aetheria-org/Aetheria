package io.hamlook.aetheria.features.misc.killcombo;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.chat.ChatFilter;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.TextCompat;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@RegisterEvents
public class KillComboTracker {

    private static final String EXPIRE_MARKER = "Your Kill Combo has expired!";
    private static final Pattern EXPIRE_COMBO = Pattern.compile("reached a (\\d+) Kill Combo");
    private static final Pattern COMBO_MSG = Pattern.compile("\\+(\\d+) Kill Combo");
    private static final Pattern VALUE_PATTERN = Pattern.compile("\\+(\\d+(?:\\.\\d+)?)");
    private static KillComboTracker INSTANCE;
    private final Map<String, ComboStat> stats = new HashMap<>();
    private int currentCombo = 0;
    private int highestCombo = 0;
    private boolean active = false;

    public void reset() {
        currentCombo = 0;
        highestCombo = 0;
        active = false;
        stats.clear();
    }

    public KillComboTracker() {
        INSTANCE = this;
        ChatFilter.hide("killCombo.messages", msg -> ATHRConfig.feature != null && ATHRConfig.feature.misc.killCombo.enabled && ATHRConfig.feature.misc.killCombo.hideChatMessages && (COMBO_MSG.matcher(msg).find() || msg.contains(EXPIRE_MARKER)));
    }

    public static KillComboTracker getInstance() {
        return INSTANCE;
    }

    private static boolean isPlayerMessage(String msg) {
        return ChatUtils.isPlayerMessage(msg) || ChatUtils.isPartyMessage(msg) || ChatUtils.isMsgReceived(msg) || ChatUtils.isMsgSent(msg) || ChatUtils.isDonateMessage(msg);
    }

    public static String getComboColor(int combo) {
        if (combo <= 10) return "§a";
        if (combo <= 15) return "§9";
        if (combo <= 25) return "§5";
        return "§6";
    }

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        currentCombo = 0;
        active = false;
    }

    @HandleEvent
    public void onChat(ASMChatEvent event) {
        if (!ATHRConfig.feature.misc.killCombo.enabled) return;
        if (!ChatUtils.isFromServer(event)) return;
        String stripped = ChatUtils.clean(event);
        if (isPlayerMessage(stripped)) return;

        String text = TextCompat.getFormattedText(event.message);

        if (text.contains(EXPIRE_MARKER)) {
            handleExpire(text);
            return;
        }

        Matcher m = COMBO_MSG.matcher(text);
        if (!m.find()) return;

        int combo = Integer.parseInt(m.group(1));

        if (!active) {
            stats.clear();
            active = true;
        }

        currentCombo = combo;
        if (combo > highestCombo) highestCombo = combo;

        if (combo > 30) return;

        int bonusStart = text.indexOf(" Kill Combo ");
        if (bonusStart == -1) return;
        bonusStart += " Kill Combo ".length();
        String bonusText = text.substring(bonusStart).trim();
        parseBonus(bonusText, combo);
    }

    private void handleExpire(String text) {
        Matcher m = EXPIRE_COMBO.matcher(text);
        if (m.find()) {
            int expired = Integer.parseInt(m.group(1));
            if (expired > highestCombo) highestCombo = expired;
        }
        currentCombo = 0;
        active = false;
        stats.clear();
    }

    private void parseBonus(String coloredText, int milestone) {
        String stripped = ColorUtils.stripColor(coloredText).trim();
        Matcher m = VALUE_PATTERN.matcher(stripped);
        if (!m.find()) return;

        double doubleVal = Double.parseDouble(m.group(1));
        int value = (int) doubleVal;

        String rest = stripped.substring(m.end()).trim();
        String statType = rest.replaceFirst("^%?\\s*", "").trim();

        if (statType.isEmpty()) return;

        ComboStat stat = stats.get(statType);
        if (stat == null) {
            stats.put(statType, new ComboStat(value, milestone));
        } else {
            stat.totalValue += value;
            if (milestone > stat.lastMilestone) stat.lastMilestone = milestone;
        }
    }

    public static class ComboStat {
        public int totalValue;
        public int lastMilestone;

        ComboStat(int totalValue, int lastMilestone) {
            this.totalValue = totalValue;
            this.lastMilestone = lastMilestone;
    }
}
}