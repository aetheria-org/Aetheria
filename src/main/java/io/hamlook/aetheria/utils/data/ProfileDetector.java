package io.hamlook.aetheria.utils.data;

import io.hamlook.aetheria.features.diana.DianaStats;
import io.hamlook.aetheria.features.fishing.trophy.TrophyFishStorage;
import io.hamlook.aetheria.features.mining.powder.PowderStats;
import io.hamlook.aetheria.features.mining.pristine.PristineStats;
import io.hamlook.aetheria.features.misc.ghosttracker.GhostStats;
import io.hamlook.aetheria.features.misc.pet.CurrentPetTracker;
import io.hamlook.aetheria.features.misc.pet.PetCache;
import io.hamlook.aetheria.features.storage.data.StorageData;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class ProfileDetector {

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String msg = ChatUtils.clean(event);
        if (ChatUtils.isPartyMessage(msg) || ChatUtils.isPlayerMessage(msg) || ChatUtils.isMsgReceived(msg) || ChatUtils.isMsgSent(msg) || ChatUtils.isDonateMessage(msg))
            return;

        if (msg.startsWith("You are playing on profile:")) {
            String newProfile = msg.substring("You are playing on profile: ".length()).trim();
            String oldProfile = SkyblockData.getCurrentProfile();

            if (!oldProfile.isEmpty()) {
                CurrentPetTracker.getInstance().save();
                PetCache.getInstance().save();
                TrophyFishStorage.getInstance().save();
                PowderStats.getInstance().save();
                PristineStats.getInstance().save();
                GhostStats.getInstance().save();
                DianaStats.getInstance().save();
                StorageData.saveContainers();
            }

            SkyblockData.setCurrentProfile(newProfile);

            CurrentPetTracker.getInstance().load();
            PetCache.getInstance().load();
            TrophyFishStorage.getInstance().load();
            PowderStats.getInstance().load();
            PristineStats.getInstance().load();
            GhostStats.getInstance().load();
            DianaStats.getInstance().load();
            StorageData.containers.clear();
            StorageData.loadContainers();
        }
    }
}
