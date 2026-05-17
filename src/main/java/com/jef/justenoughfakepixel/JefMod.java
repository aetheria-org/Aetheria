package com.jef.justenoughfakepixel;

import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.core.JefStorageManager;
import com.jef.justenoughfakepixel.data.ApiHandler;
import com.jef.justenoughfakepixel.features.capes.CapeManager;
import com.jef.justenoughfakepixel.features.dungeons.caseopening.CitManager;
import com.jef.justenoughfakepixel.features.misc.protect.ProtectedItemStorage;
import com.jef.justenoughfakepixel.features.misc.invbuttons.SkyblockItemCache;
import com.jef.justenoughfakepixel.features.misc.pet.PetCache;
import com.jef.justenoughfakepixel.features.profile.GuiWaiter;
import com.jef.justenoughfakepixel.init.JefEventRegistrar;
import com.jef.justenoughfakepixel.mixins.MixinMinecraft;
import com.jef.justenoughfakepixel.repo.JefRepo;
import com.jef.justenoughfakepixel.repo.RepoHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.logging.Logger;

@Mod(modid = JefMod.MODID, name = JefMod.NAME, version = JefMod.VERSION, clientSideOnly = true, guiFactory = "com.jef.justenoughfakepixel.JefGuiFactory")
public class JefMod {

    public static final String MODID = "justenoughfakepixel";
    public static final String NAME = "JustEnoughFakepixel";
    public static final String VERSION = "1.2.8";

    public static JefConfig config;
    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        JefConfig.init();
        JefRepo.init();
        logger = Logger.getLogger("[JEF] ");
        JefStorageManager.initAll(JefConfig.configDirectory);
        // ProtectedItemStorage uses .init() rather than .initFile(), so it stays manual for now.
        ProtectedItemStorage.INSTANCE.init(JefConfig.configDirectory);
        CapeManager.initialise(false);
    }

    @Mod.EventHandler
    public void clientInit(FMLInitializationEvent event) {
        JefConfig.register();
        JefStorageManager.loadAll();
        JefStorageManager.startAutoSave();
        SkyblockItemCache.getInstance().loadAsync();

        new CitManager();
        if (JefConfig.feature.misc.currentPet.showCurrentPet) PetCache.getInstance().warmupTextures();

        MinecraftForge.EVENT_BUS.register(GuiWaiter.INSTANCE);
        MinecraftForge.EVENT_BUS.register(this);
        JefEventRegistrar.registerAll();
    }

    @SubscribeEvent
    public void onServerJoin(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        RepoHandler.refresh(JefRepo.KEY_PLAYERSIZES);
        RepoHandler.refresh(JefRepo.KEY_TIMERS);
        RepoHandler.refresh(JefRepo.KEY_UPDATE);
        ApiHandler.onServerJoin();
    }
}