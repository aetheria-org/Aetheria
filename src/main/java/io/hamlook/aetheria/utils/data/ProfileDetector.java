package io.hamlook.aetheria.utils.data;

import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.features.storage.data.StorageData;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class ProfileDetector {

    public static void saveAllProfileData() {
        StorageManager.saveAllProfileData();
        StorageData.saveContainers();
    }

    public static void loadAllProfileData() {
        StorageManager.loadAllProfileData();
        StorageData.loadContainers();
    }

    public static void onEnvironmentChanged(SkyblockData.Environment oldEnvironment, SkyblockData.Environment newEnvironment) {
        if (SkyblockData.getCurrentProfile().isEmpty()) return;
        saveAllProfileData();
        SkyblockData.setEnvironment(newEnvironment);
        loadAllProfileData();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String msg = ChatUtils.clean(event);
        if (ChatUtils.isPartyMessage(msg) || ChatUtils.isPlayerMessage(msg) || ChatUtils.isMsgReceived(msg) || ChatUtils.isMsgSent(msg) || ChatUtils.isDonateMessage(msg))
            return;

        if (msg.startsWith("You are playing on profile:")) {
            String newProfile = msg.substring("You are playing on profile: ".length()).trim();
            String oldProfile = SkyblockData.getCurrentProfile();

            if (!oldProfile.isEmpty()) {
                saveAllProfileData();
            }

            SkyblockData.setCurrentProfile(newProfile);

            StorageData.containers.clear();
            loadAllProfileData();
        }
    }
}
