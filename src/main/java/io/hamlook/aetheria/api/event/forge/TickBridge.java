package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class TickBridge {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        TickPhase phase = event.phase == TickEvent.Phase.START ? TickPhase.START : TickPhase.END;
        new ASMTickEvent(phase).post();
    }
}
