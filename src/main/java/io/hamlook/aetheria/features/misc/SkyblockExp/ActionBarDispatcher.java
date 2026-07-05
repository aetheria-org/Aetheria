package io.hamlook.aetheria.features.misc.SkyblockExp;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.events.ActionBarUpdateEvent;
import io.hamlook.aetheria.events.ActionBarXpGainEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RegisterEvents
public class ActionBarDispatcher {

    private static final byte ACTION_BAR_TYPE = 2;

    private static final Pattern SB_XP_STRIPPED = Pattern.compile("\\+(\\d+) SkyBlock XP");

    private static final Pattern SB_XP_FORMATTED = Pattern.compile("(\\+.*?SkyBlock XP)");

    private String lastXpAmount = null;

    public ActionBarDispatcher() {
        Aetheria.logger.severe("[ActionBarDispatcher] Initialized - CLASSPATH SCAN FOUND THIS CLASS");
    }

    @SubscribeEvent
    public void onActionBar(ClientChatReceivedEvent event) {
        if (event.type != ACTION_BAR_TYPE) return;

        String stripped = StringUtils.stripControlCodes(event.message.getUnformattedText());
        String formatted = event.message.getFormattedText();

        // Only log when in mist
        boolean inMist = io.hamlook.aetheria.utils.data.SkyblockData.isInMist();
        if (inMist) {
            Aetheria.logger.severe("[ActionBarDispatcher] onActionBar RECEIVED: " + stripped);
        }

        MinecraftForge.EVENT_BUS.post(new ActionBarUpdateEvent(stripped));

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

        MinecraftForge.EVENT_BUS.post(new ActionBarXpGainEvent(xpText));
    }
}