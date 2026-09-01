package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.events.ASMWorldLoadEvent;
import io.hamlook.aetheria.events.ASMServerDisconnectEvent;
import io.hamlook.aetheria.events.ASMServerJoinEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.event.world.WorldEvent;

@RegisterEvents
public class WorldBridge {

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
}
