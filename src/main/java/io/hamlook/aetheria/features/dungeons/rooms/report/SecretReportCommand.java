package io.hamlook.aetheria.features.dungeons.rooms.report;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoom;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomDetector;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;

@RegisterEvents
public class SecretReportCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("report-secret", builder -> {
            builder.description = "Opens the secret report GUI for the current dungeon room";
            builder.setCategory(CommandCategory.USERS_ACTIVE);
            builder.simpleCallback(() -> {
                DungeonRoom room = DungeonRoomDetector.getCurrentRoom();
                if (room == null) {
                    ChatUtils.sendMessage("§cYou can only send secret-report in a valid Dungeon room!");
                    return;
                }
                ATHRConfig.screenToOpen = new SecretReportGUI(room);
            });
        });
    }
}
