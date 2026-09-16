package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.events.ASMServerDisconnectEvent;
import io.hamlook.aetheria.events.ASMServerJoinEvent;
import io.hamlook.aetheria.events.ASMWorldLoadEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.init.RegisterEvents;
//#if FORGE
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
//#else
//$$ import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
//$$ import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
//#endif

@RegisterEvents
public class WorldBridge {

    //#if FORGE
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        new ASMWorldLoadEvent().post();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        new ASMWorldUnloadEvent().post();
    }

    @SubscribeEvent
    public void onServerJoin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        new ASMServerJoinEvent(event.manager).post();
    }

    @SubscribeEvent
    public void onServerDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        new ASMServerDisconnectEvent().post();
    }
    //#else
    //$$ static {
    //$$     ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
    //$$         new ASMServerJoinEvent(handler.getConnection()).post());
    //$$     ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
    //$$         new ASMServerDisconnectEvent().post());
    //$$     ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
    //$$         if (world == null) new ASMWorldUnloadEvent().post();
    //$$         else new ASMWorldLoadEvent().post();
    //$$     });
    //$$ }
    //#endif
}
