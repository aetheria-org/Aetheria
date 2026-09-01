package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class TickBridge {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        new ASMTickEvent(event.phase).post();
    }
}
