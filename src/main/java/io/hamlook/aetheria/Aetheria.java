package io.hamlook.aetheria;

import io.hamlook.aetheria.features.chat.chatfilters.ChatFilterManager;
import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.features.custommenu.util.CMMHelper;
import io.hamlook.aetheria.features.diana.party.DianaPartyConnector;
import io.hamlook.aetheria.features.chat.emoji.EmojiManager;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.data.ApiHandler;
import io.hamlook.aetheria.features.capes.CapeManager;
import io.hamlook.aetheria.features.dungeons.caseopening.CitManager;
import io.hamlook.aetheria.features.farming.visitors.VisitorShoppingList;
import io.hamlook.aetheria.features.misc.pet.PetCache;
import io.hamlook.aetheria.features.profile.GuiWaiter;
import io.hamlook.aetheria.features.trackers.TrackerManager;
import io.hamlook.aetheria.command.brigadier.CommandsRegistry;
import io.hamlook.aetheria.init.EventRegistrar;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.utils.ElectionUtils;
import io.hamlook.aetheria.utils.placeholders.PlaceholderManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import io.hamlook.aetheria.api.event.AetheriaEventBus;
import io.hamlook.aetheria.api.event.HandleEvent;

import java.util.logging.Logger;
import io.hamlook.aetheria.events.ASMServerJoinEvent;

@Mod(modid = Aetheria.MODID, name = Aetheria.NAME, version = Aetheria.VERSION, clientSideOnly = true, guiFactory = "io.hamlook.aetheria.GuiFactory")
public class Aetheria {

    public static final String MODID = "aetheria";
    public static final String NAME = "Aetheria";
    public static final String VERSION = "1.4.0-alpha";

    public static ATHRConfig config;
    public static Logger logger;
    public static WebSocketClient webSocketClient;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = Logger.getLogger("[ATHR] ");
        ATHRConfig.init();
        ATHRRepo.init();
        EmojiManager.init();
        StorageManager.initAll(ATHRConfig.configDirectory);
        CapeManager.initialise(false);
        TesterWhitelist.init(VERSION);
        PlaceholderManager.initialise();
        webSocketClient = new WebSocketClient();
        if (ATHRConfig.feature == null || !ATHRConfig.feature.network.smartSocketLifecycle) {
            webSocketClient.connect();
        }
    }

    @Mod.EventHandler
    public void clientInit(FMLInitializationEvent event) {
        ATHRConfig.register();
        StorageManager.loadAll();
        StorageManager.startAutoSave();
        ItemRegistry.initialise();
        ChatFilterManager.initialise();
        ElectionUtils.initialise();
        DianaPartyConnector.initialise();
        GlobalChat.initialise();
        ImageManager.initialise();
        TrackerManager.initialise();
        new CitManager();
        if (ATHRConfig.feature.misc.currentPet.showCurrentPet) PetCache.getInstance().warmupTextures();
        MinecraftForge.EVENT_BUS.register(GuiWaiter.INSTANCE);
        AetheriaEventBus.INSTANCE.register(GuiWaiter.INSTANCE);
        MinecraftForge.EVENT_BUS.register(this);
        AetheriaEventBus.INSTANCE.register(this);
        EventRegistrar.registerAll();
        CommandsRegistry.INSTANCE.registerAll();
        CMMHelper.initialise();
    }

    @HandleEvent
    public void onServerJoin(ASMServerJoinEvent e) {
        RepoHandler.refresh(ATHRRepo.KEY_PLAYERSIZES);
        RepoHandler.refresh(ATHRRepo.KEY_TIMERS);
        RepoHandler.refresh(ATHRRepo.KEY_UPDATE);
        ApiHandler.onServerJoin();
        VisitorShoppingList.onServerJoined(e);
    }
}
