package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.features.events.EventIcons;
import io.hamlook.aetheria.features.events.EventInfo;
import io.hamlook.aetheria.features.events.EventNotifierTracker;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /asmeventtest all queues a synthetic 1-minute countdown popup for one of every known SkyBlock
 * event type, one after another, so the notifier's rendering, icons, theming and queue sequencing
 * can be previewed on demand without waiting on real API timing or flipping the per-type config
 * toggles.
 */
@RegisterCommand
public class AsmEventNotifTestCommand extends ASMCommand {

    private static final List<EventInfo> TEST_EVENTS = Arrays.asList(
            eventInfo("Election Booth Opens!", null),
            eventInfo("Traveling Zoo", null),
            eventInfo("Dark Auction", null),
            eventInfo("Farming Contest", Arrays.asList("Wheat", "Potato", "Red Mushroom")),
            eventInfo("Spooky Festival", null),
            eventInfo("Jerry Workshop Opens", null),
            eventInfo("Mining Fiesta", null),
            eventInfo("Fishing Festival", null),
            eventInfo("New Year", null)
    );

    private static EventInfo eventInfo(String type, List<String> crops) {
        EventInfo info = new EventInfo();
        info.event = type;
        info.crops = crops;
        return info;
    }

    @Override
    public String getName() {
        return "asmeventtest";
    }

    @Override
    public String getUsage() {
        return "/asmeventtest all";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmevent");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length != 1 || !"all".equalsIgnoreCase(args[0])) {
            ChatUtils.sendMessage(EnumChatFormatting.RED + "Usage: " + getUsage());
            return;
        }

        for (EventInfo info : TEST_EVENTS) {
            EventNotifierTracker.debugFireCountdown(info.event, EventIcons.iconsFor(info), 60);
        }
        ChatUtils.sendMessage(EnumChatFormatting.GREEN + "Queued all " + TEST_EVENTS.size() + " event type popups (1-minute countdown each).");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return args.length == 1 ? Collections.singletonList("all") : Collections.emptyList();
    }
}
