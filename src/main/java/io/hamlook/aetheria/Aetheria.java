package io.hamlook.aetheria;

import io.hamlook.aetheria.features.chatfilters.ChatFilterManager;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.data.ApiHandler;
import io.hamlook.aetheria.features.capes.CapeManager;
import io.hamlook.aetheria.features.dungeons.caseopening.CitManager;
import io.hamlook.aetheria.features.misc.protect.ProtectedItemStorage;
import io.hamlook.aetheria.features.misc.invbuttons.SkyblockItemCache;
import io.hamlook.aetheria.features.misc.pet.PetCache;
import io.hamlook.aetheria.features.profile.GuiWaiter;
import io.hamlook.aetheria.init.EventRegistrar;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.logging.Logger;

@Mod(modid = Aetheria.MODID, name = Aetheria.NAME, version = Aetheria.VERSION, clientSideOnly = true, guiFactory = "io.hamlook.aetheria.GuiFactory")
public class Aetheria {

    public static final String MODID = "aetheria";
    public static final String NAME = "Aetheria";
    public static final String VERSION = "1.0.1-alpha";

    public static ATHRConfig config;
    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ATHRConfig.init();
        ATHRRepo.init();
        logger = Logger.getLogger("[ATHR] ");
        StorageManager.initAll(ATHRConfig.configDirectory);
        // ProtectedItemStorage uses .init() rather than .initFile(), so it stays manual for now.
        ProtectedItemStorage.INSTANCE.init(ATHRConfig.configDirectory);
        CapeManager.initialise(false);
    }

    @Mod.EventHandler
    public void clientInit(FMLInitializationEvent event) {
        ATHRConfig.register();
        StorageManager.loadAll();
        StorageManager.startAutoSave();
        SkyblockItemCache.getInstance().loadAsync();
        ItemRegistry.initialise();
        ChatFilterManager.initialise();
        new CitManager();
        if (ATHRConfig.feature.misc.currentPet.showCurrentPet) PetCache.getInstance().warmupTextures();

        MinecraftForge.EVENT_BUS.register(GuiWaiter.INSTANCE);
        MinecraftForge.EVENT_BUS.register(this);
        EventRegistrar.registerAll();
    }

    @SubscribeEvent
    public void onServerJoin(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        RepoHandler.refresh(ATHRRepo.KEY_PLAYERSIZES);
        RepoHandler.refresh(ATHRRepo.KEY_TIMERS);
        RepoHandler.refresh(ATHRRepo.KEY_UPDATE);
        ApiHandler.onServerJoin();
    }
}