package io.hamlook.aetheria.features.misc.SkyblockExp;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.events.ActionBarUpdateEvent;
import io.hamlook.aetheria.events.ActionBarXpGainEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraft.util.StringUtils;
import io.hamlook.aetheria.api.event.HandleEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.hamlook.aetheria.events.ASMChatEvent;


@RegisterEvents
public class ActionBarDispatcher {

    private static final byte ACTION_BAR_TYPE = 2;

    private static final Pattern SB_XP_STRIPPED = Pattern.compile("\\+(\\d+) SkyBlock XP");

    private static final Pattern SB_XP_FORMATTED = Pattern.compile("(\\+.*?SkyBlock XP)");

    public static String lastActionBarFormatted = "";
    public static String lastActionBarStripped = "";

    private String lastXpAmount = null;

    public ActionBarDispatcher() {
    }

    @HandleEvent
    public void onActionBar(ASMChatEvent event) {
        if (event.type != ACTION_BAR_TYPE) return;

        String stripped = StringUtils.stripControlCodes(event.message.getUnformattedText());
        String formatted = event.message.getFormattedText();

        lastActionBarFormatted = formatted;
        lastActionBarStripped = stripped;

        new ActionBarUpdateEvent(stripped).post();

        Matcher strippedMatcher = SB_XP_STRIPPED.matcher(stripped);
        if (!strippedMatcher.find()) {
            lastXpAmount = null;
            return;
        }

        String amount = strippedMatcher.group(1);
        if (amount.equals(lastXpAmount)) return; // same gain still showing, don't repeat
        lastXpAmount = amount;

        Matcher formattedMatcher = SB_XP_FORMATTED.matcher(formatted);
        String xpText = formattedMatcher.find() ? formattedMatcher.group(1) : ("+" + amount + " SkyBlock XP");

        new ActionBarXpGainEvent(xpText).post();
    }
}