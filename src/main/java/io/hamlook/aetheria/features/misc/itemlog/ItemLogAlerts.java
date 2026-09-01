package io.hamlook.aetheria.features.misc.itemlog;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.misc.ItemLogAlertsConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.SoundUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import io.hamlook.aetheria.api.event.HandleEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import io.hamlook.aetheria.events.ASMTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.events.ASMRenderOverlayEvent;

@RegisterEvents
public class ItemLogAlerts {

    private static final int ALERT_DURATION_MS = 2000;
    private final Set<String> firstTimeAlerted = new HashSet<>();
    private String displayText = "";
    private long endTime = 0;
    private boolean registered = false;

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!registered) {
            ItemPickupLog log = ItemPickupLog.getInstance();
            if (log != null) {
                log.addRichItemChangeListener(this::onItemChange);
                registered = true;
            }
        }
    }

    private void onItemChange(String internalId, String displayName, int delta) {
        if (delta <= 0) return;
        ItemLogAlertsConfig config = ATHRConfig.feature.misc.itemLogAlerts;
        if (!config.enabled) return;
        Map<String, ItemLogAlertsConfig.AlertEntry> alerts = config.alerts;
        if (alerts == null) return;
        String id = internalId.toLowerCase();
        ItemLogAlertsConfig.AlertEntry entry = alerts.get(id);
        if (entry == null) return;
        if (config.alertMode == 1) {
            if (firstTimeAlerted.contains(id)) return;
            firstTimeAlerted.add(id);
        }

        String text;
        if (config.useDisplayName || entry.customText.isEmpty()) {
            text = displayName;
        } else {
            text = entry.customText.replace("&", "§");
        }

        displayText = text;
        endTime = System.currentTimeMillis() + ALERT_DURATION_MS;
        if (config.playSound) {
            SoundUtils.playSound("note.pling", 2.0f, 2.0f);
        }
    }

    @HandleEvent
    public void onRenderOverlay(ASMRenderOverlayEvent event) {
        if (event.type != 11) return;
        if (System.currentTimeMillis() > endTime) return;
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();
        int textWidth = fr.getStringWidth(displayText);
        float scale = Math.min(6.0f, (screenWidth - 20f) / textWidth);
        scale = Math.max(1.0f, scale);
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
        int x = (int) ((screenWidth / scale - textWidth) / 2);
        int y = (int) ((screenHeight / scale / 2) - 15);
        fr.drawStringWithShadow(displayText, x, y, 0xFF5555);
        GlStateManager.popMatrix();
    }

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        displayText = "";
        firstTimeAlerted.clear();
    }
}
