package io.hamlook.aetheria.features.misc.ghosttracker;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.events.ActionBarUpdateEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;

@RegisterEvents
public class GhostTrackerListener {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    private static long lastAutoSave = 0L;
    private static float previousXp = -1f;

    private static boolean isPlayerOrPartyMessage(String msg) {
        return ChatUtils.isPlayerMessage(msg) || ChatUtils.isPartyMessage(msg) || ChatUtils.isMsgReceived(msg) || ChatUtils.isMsgSent(msg) || ChatUtils.isDonateMessage(msg);
    }

    private static void handleRareDrop(Matcher matcher) {
        try {
            String dropName = matcher.group("drop").trim();
            int mf = NUMBER_FORMAT.parse(matcher.group("mf")).intValue();

            GhostStats gs = GhostStats.getInstance();
            gs.addDrop(dropName);
            gs.recordMagicFind(mf);
        } catch (Exception e) {
        }
    }

    private static boolean isValidKillContext() {
        boolean onSkyblock = SkyblockData.isOnSkyblock();
        boolean inDwarven = SkyblockData.getCurrentLocation() == SkyblockData.Location.DWARVEN;
        boolean inMist = SkyblockData.isInMist();
        Minecraft mc = Minecraft.getMinecraft();
        boolean posYValid = mc.thePlayer != null && mc.thePlayer.posY <= 100;

        return onSkyblock && inDwarven && inMist && posYValid;
    }

    private static void handleKillDetection(Matcher matcher) throws Exception {
        float xpGain = NUMBER_FORMAT.parse(matcher.group("gained")).floatValue();
        float currentXp = NUMBER_FORMAT.parse(matcher.group("progress")).floatValue();

        if (previousXp == -1f) {
            previousXp = currentXp;
            return;
        }

        float xpDelta = currentXp - previousXp;
        previousXp = currentXp;

        if (xpDelta <= 0 || xpGain >= 1000) return;

        int killsGained = Math.round(xpDelta / xpGain);
        if (killsGained <= 0 || killsGained > 15) return;

        GhostStats gs = GhostStats.getInstance();
        gs.addKill(killsGained);
        gs.addXp((long) (xpGain * killsGained));
        PurseTracker.recordKill();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        GhostStats.getInstance().timerTick();
        PurseTracker.tick();
        autoSaveIfNeeded();
    }

    private void autoSaveIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastAutoSave >= GhostTrackerConstants.AUTOSAVE_INTERVAL) {
            GhostStats.getInstance().save();
            lastAutoSave = now;
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!ChatUtils.isFromServer(event)) return;
        String msg = ChatUtils.clean(event);
        if (isPlayerOrPartyMessage(msg)) return;

        if (GhostTrackerConstants.COIN_DROP_MESSAGE.equals(event.message.getFormattedText())) {
            GhostStats.getInstance().addDrop("Coins");
            return;
        }

        Matcher matcher = GhostTrackerConstants.RARE_DROP_PATTERN.matcher(event.message.getFormattedText());
        if (!matcher.find()) return;

        handleRareDrop(matcher);
    }

    @SubscribeEvent
    public void onActionBar(ActionBarUpdateEvent event) {
        String msg = event.getText();
        
        Matcher matcher = GhostTrackerConstants.COMBAT_XP_PATTERN.matcher(msg);
        if (!matcher.find()) {
            return;
        }
        
        if (!isValidKillContext()) {
            return;
        }

        try {
            handleKillDetection(matcher);
        } catch (Exception e) {
        }
    }
}
