package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.features.events.EventIcons;
import io.hamlook.aetheria.features.events.EventNotifierTracker;
import io.hamlook.aetheria.features.events.EventUtils;
import io.hamlook.aetheria.features.events.SkyblockEvent;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * /asmeventtestall [count] queues a countdown popup for the next {@code count} soonest-starting
 * *still-upcoming* events currently sitting in {@link EventUtils#cachedEvents} (the actual
 * live/cached API response), using each entry's real type, crop list, and — unlike
 * {@code AsmEventNotifTestCommand}'s synthetic 1-minute toasts — its actual real remaining time
 * until it starts, so a contest 43 minutes out genuinely reads "starts in 43 Minutes" rather than
 * a fake "starts in 60 Seconds". Already-past entries are filtered out before sorting/limiting —
 * {@code cachedEvents} keeps stale entries around indefinitely once they elapse (harmless for the
 * real notifier, which independently checks each entry's own threshold window), but showing one
 * here as "next" would be actively misleading. Omitting {@code count} queues every still-upcoming
 * cached event. Lets rendering/icons be checked against whatever Fakepixel's API is really
 * returning right now, including anything that only shows up in real data (extra entries,
 * unexpected crop names, etc).
 */
@RegisterCommand
public class AsmEventTestAllCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmeventtestall";
    }

    @Override
    public String getUsage() {
        return "/asmeventtestall [count]";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        List<SkyblockEvent> events = new ArrayList<>(EventUtils.cachedEvents);
        if (events.isEmpty()) {
            ChatUtils.sendMessage(EnumChatFormatting.RED + "No cached events yet — nothing fetched from the API so far.");
            return;
        }

        Integer limit = null;
        if (args.length > 0) {
            try {
                limit = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                ChatUtils.sendMessage(EnumChatFormatting.RED + "Usage: " + getUsage());
                return;
            }
        }

        Instant now = Instant.now();
        events.removeIf(e -> e == null || e.event == null || e.event.event == null || e.start == null
                || !Instant.parse(e.start).isAfter(now));
        events.sort(Comparator.comparing(e -> Instant.parse(e.start)));

        if (events.isEmpty()) {
            ChatUtils.sendMessage(EnumChatFormatting.RED + "No still-upcoming cached events — everything cached has already elapsed.");
            return;
        }

        int fired = 0;
        for (SkyblockEvent event : events) {
            if (limit != null && fired >= limit) break;
            EventNotifierTracker.debugFireCountdown(event.event.event, EventIcons.iconsFor(event.event), Instant.parse(event.start));
            fired++;
        }
        ChatUtils.sendMessage(EnumChatFormatting.GREEN + "Queued the next " + fired + " upcoming cached event(s) with their real remaining time.");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return Collections.emptyList();
    }
}
