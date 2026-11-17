package io.hamlook.aetheria.features.qol;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMActionPerformedEvent;
import io.hamlook.aetheria.events.ASMGuiDrawEvent;
import io.hamlook.aetheria.events.ASMGuiInitEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;

@RegisterEvents
public class ConfirmDisconnect {

    private boolean confirm = false;
    private long lastClick = 0L;
    private GuiButton disconnectButton = null;

    @HandleEvent
    public void onGuiInit(ASMGuiInitEvent event) {
        if (!(event.gui instanceof GuiIngameMenu)) return;

        confirm = false;
        lastClick = 0L;
        disconnectButton = null;

        for (Object obj : event.buttonList) {
            GuiButton btn = (GuiButton) obj;
            if ("Disconnect".equals(GuiScreenUtils.getButtonMessage(btn))) {
                disconnectButton = btn;
                break;
            }
        }
    }

    @HandleEvent
    public void onAction(ASMActionPerformedEvent event) {
        if (!(event.gui instanceof GuiIngameMenu)) return;

        if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.confirmDisconnect) return;

        if (event.button != disconnectButton) return;

        if (System.currentTimeMillis() - lastClick > 3000L) {
            confirm = false;
        }

        if (!confirm) {
            event.cancel();

            confirm = true;
            lastClick = System.currentTimeMillis();

            GuiScreenUtils.setButtonMessage(event.button, "§cPress again to confirm");
        } else {
            confirm = false;
        }
    }

    @HandleEvent
    public void onDraw(ASMGuiDrawEvent event) {
        if (!(event.gui instanceof GuiIngameMenu)) return;

        if (confirm && System.currentTimeMillis() - lastClick > 2000L) {
            confirm = false;

            if (disconnectButton != null) {
                GuiScreenUtils.setButtonMessage(disconnectButton, "Disconnect");
            }
        }
    }
}