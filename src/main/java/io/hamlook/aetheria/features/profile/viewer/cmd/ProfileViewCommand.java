package io.hamlook.aetheria.features.profile.viewer.cmd;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;

@RegisterEvents
public class ProfileViewCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("pv", builder -> {
            builder.description = "Open the profile viewer";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.legacyCallbackArgs(args -> {
                if (MinecraftCompat.getLocalPlayer() == null) return;
                if (args.length < 1) {
                    ATHRConfig.screenToOpen = new ProfileViewerGUI(MinecraftCompat.getMinecraft().getSession().getUsername());
                    return;
                }
                String user = args[0];
                ATHRConfig.screenToOpen = new ProfileViewerGUI(user);
            });
        });
    }
}
