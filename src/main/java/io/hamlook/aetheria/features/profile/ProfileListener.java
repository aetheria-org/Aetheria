package io.hamlook.aetheria.features.profile;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMGuiBackgroundDrawEvent;
import io.hamlook.aetheria.events.ASMGuiOpenEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraft.client.gui.inventory.GuiContainer;

@RegisterEvents
public class ProfileListener {

    public static volatile boolean isParsing = false;

    @HandleEvent
    public void onGuiDraw(ASMGuiBackgroundDrawEvent e) throws Exception {
        if(isParsing || !(e.gui instanceof GuiContainer))return;
        if(!ModulesManager.initialised){
            ModulesManager.init();
        }
        GuiContainer container = (GuiContainer) e.gui;
        for(ProfileModule module : ModulesManager.modules){
            if(module.isInitialGUI(container)){
                isParsing = true;
                module.parse(container);
            }
        }
    }

    @HandleEvent
    public void onGuiClose(ASMGuiOpenEvent e){
        isParsing = false;
    }

}
