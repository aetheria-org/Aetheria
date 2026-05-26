package io.hamlook.aetheria.utils.chat;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.diana.DianaTracker;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class PartyCommands {

    private static final long HELP_COOLDOWN_MS = 10_000L;
    private final Minecraft mc = Minecraft.getMinecraft();
    private long lastHelpMs = 0L;

    private static String getATHRVersion() {
        return "Aetheria v" + Aetheria.VERSION;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (ChatUtils.isFromServer(event)) return;
        String msg = ChatUtils.clean(event);
        if (!ChatUtils.isPartyMessage(msg)) return;

        String body = ChatUtils.getPartyBody(msg);
        if (body == null) return;
        body = body.toLowerCase();

        if (body.startsWith("!pb")) {
            String[] parts = body.split("\\s+");
            String arg1 = parts.length >= 2 ? parts[1] : null;
            String arg2 = parts.length >= 3 ? parts[2] : null;
            if (arg1 == null) {
                ChatUtils.sendMessage("§6[ATHR] §cMissing floor argument");
                ChatUtils.sendMessage("§6[ATHR] §eUsage:");
                ChatUtils.sendMessage("§6[ATHR]   §f!pb <floor> §7- Total run time");
                ChatUtils.sendMessage("§6[ATHR]   §f!pb <floor> br §7- Blood rush time");
                ChatUtils.sendMessage("§6[ATHR]   §f!pb <floor> p1-p5 §7- Phase times");
                ChatUtils.sendMessage("§6[ATHR] §eExamples:");
                ChatUtils.sendMessage("§6[ATHR]   §f!pb f7 §7- F7 total PB");
                ChatUtils.sendMessage("§6[ATHR]   §f!pb m7 p4 §7- M7 P4 (Necron) PB");
                ChatUtils.sendMessage("§6[ATHR]   §f!pb f2 p1 §7- F2 P1 (Scarf) PB");
                return;
            }
            String result = DungeonStats.getFormattedPb(arg1, arg2);
            if (result != null) respond(result);
            return;
        }

        switch (body) {
            case "!athr":
                respond(getATHRVersion());
                break;
            case "!burrows":
                respond(DianaTracker.getBorrowsMessage());
                break;
            case "!inq":
                respond(DianaTracker.getInqMessage());
                break;
            case "!mobs":
                respond(DianaTracker.getMobsMessage());
                break;
            case "!time":
                respond(DianaTracker.getTimeMessage());
                break;
            case "!chim":
                respond(DianaTracker.getChimMessage());
                break;
            case "!stick":
                respond(DianaTracker.getStickMessage());
                break;
            case "!relic":
                respond(DianaTracker.getRelicMessage());
                break;
            case "!loot":
                respond(DianaTracker.getLootMessage());
                break;
            case "!help": {
                long now = System.currentTimeMillis();
                if (now - lastHelpMs < HELP_COOLDOWN_MS) break;
                lastHelpMs = now;
                ChatUtils.sendMultilineMessage(DianaTracker.getHelpMessage());
                break;
            }
        }
    }

    private void respond(String msg) {
        ChatUtils.sendPartyMessage(msg);
    }
}