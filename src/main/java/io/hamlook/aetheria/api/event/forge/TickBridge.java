package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.init.RegisterEvents;
//#if FORGE
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
//#else
//$$ import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//#endif

@RegisterEvents
public class TickBridge {

    //#if FORGE
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        TickPhase phase = event.phase == TickEvent.Phase.START ? TickPhase.START : TickPhase.END;
        new ASMTickEvent(phase).post();
    }
    //#else
    //$$ static {
    //$$     ClientTickEvents.START_CLIENT_TICK.register(client -> new ASMTickEvent(TickPhase.START).post());
    //$$     ClientTickEvents.END_CLIENT_TICK.register(client -> new ASMTickEvent(TickPhase.END).post());
    //$$ }
    //#endif
}
