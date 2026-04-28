package com.jef.justenoughfakepixel;

import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.data.ApiHandler;
import com.jef.justenoughfakepixel.features.capes.CapeManager;
import com.jef.justenoughfakepixel.features.diana.DianaStats;
import com.jef.justenoughfakepixel.features.dungeons.caseopening.CitManager;
import com.jef.justenoughfakepixel.features.fishing.trophy.TrophyFishStorage;
import com.jef.justenoughfakepixel.features.misc.ProtectedItemStorage;
import com.jef.justenoughfakepixel.features.mining.powder.PowderStats;
import com.jef.justenoughfakepixel.features.misc.invbuttons.InventoryButtonStorage;
import com.jef.justenoughfakepixel.features.misc.invbuttons.SkyblockItemCache;
import com.jef.justenoughfakepixel.features.misc.pet.CurrentPetTracker;
import com.jef.justenoughfakepixel.features.misc.pet.PetCache;
import com.jef.justenoughfakepixel.features.profile.GuiWaiter;
import com.jef.justenoughfakepixel.features.scoreboard.MaxwellPowerSync;
import com.jef.justenoughfakepixel.features.waypoints.WaypointStorage;
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
    public static final String VERSION = "1.2.7";

    public static JefConfig config;
    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        JefConfig.init();
        JefRepo.init();
        logger = Logger.getLogger("[JEF] ");
        WaypointStorage.getInstance().initFile(JefConfig.configDirectory);
        InventoryButtonStorage.getInstance().initFile(JefConfig.configDirectory);
        DianaStats.getInstance().initFile(JefConfig.configDirectory);
        PowderStats.getInstance().initFile(JefConfig.configDirectory);
        MaxwellPowerSync.getInstance().initFile(JefConfig.configDirectory);
        PetCache.getInstance().initFile(JefConfig.configDirectory);
        CurrentPetTracker.getInstance().initFile(JefConfig.configDirectory);
        TrophyFishStorage.getInstance().initFile(JefConfig.configDirectory);
        ProtectedItemStorage.INSTANCE.init(JefConfig.configDirectory);
        CapeManager.initialise(false);
    }


    @Mod.EventHandler
    public void clientInit(FMLInitializationEvent event) {
        JefConfig.register();
        WaypointStorage.getInstance().load();
        InventoryButtonStorage.getInstance().load();
        SkyblockItemCache.getInstance().loadAsync();
        DianaStats.getInstance().load();
        PowderStats.getInstance().load();
        MaxwellPowerSync.getInstance().load();
        PetCache.getInstance().load();
        CurrentPetTracker.getInstance().load();
        TrophyFishStorage.getInstance().load();

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