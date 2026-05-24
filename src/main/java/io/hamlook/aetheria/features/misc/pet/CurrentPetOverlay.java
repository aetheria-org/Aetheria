package io.hamlook.aetheria.features.misc.pet;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.config.editors.ChromaColour;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.overlay.Overlay;
import io.hamlook.aetheria.utils.overlay.OverlayUtils;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

@RegisterEvents
public class CurrentPetOverlay extends Overlay {

    private static final int SKULL_SIZE = 16;
    private static final int GAP = 4;

    @Getter
    private static CurrentPetOverlay instance;

    public CurrentPetOverlay() {
        super(160, SKULL_SIZE + PADDING * 2);
        instance = this;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.misc.currentPet.currentPetPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.misc.currentPet.currentPetScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.misc.currentPet.currentPetBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.misc.currentPet.currentPetCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature.misc.currentPet.showCurrentPet && SkyblockData.isOnSkyblock();
    }

    @Override
    protected int getBaseWidth() {
        return SKULL_SIZE + GAP + Minecraft.getMinecraft().fontRendererObj.getStringWidth("§7[Lvl 100] §6Tiger") + PADDING * 2;
    }

    @Override
    public List<String> getLines(boolean preview) {
        return Collections.emptyList();
    }

    @Override
    public void render(boolean preview) {
        if (!preview && OverlayUtils.shouldHide()) return;

        Minecraft mc = Minecraft.getMinecraft();

        String formattedName;
        ItemStack skullItem = null;

        if (preview) {
            formattedName = "§7[Lvl 100] §6Tiger";
        } else {
            if (!CurrentPetApi.hasPet()) return;
            formattedName = CurrentPetApi.getDisplayName();
            if (formattedName.isEmpty()) formattedName = CurrentPetApi.getColoredName();
            String tex = CurrentPetApi.getTextureValue();
            if (!tex.isEmpty()) skullItem = ItemUtils.createSkullWithTexture(tex);
        }

        float scale = getScale();
        int textW = mc.fontRendererObj.getStringWidth(formattedName);
        int w = SKULL_SIZE + GAP + textW + PADDING * 2;
        int h = SKULL_SIZE + PADDING * 2;
        lastW = w;
        lastH = h;

        ScaledResolution sr = new ScaledResolution(mc);
        Position pos = getPosition();
        int x = pos.getAbsX(sr, (int) (w * scale));
        int y = pos.getAbsY(sr, (int) (h * scale));
        if (pos.isCenterX()) x -= (int) (w * scale / 2);
        if (pos.isCenterY()) y -= (int) (h * scale / 2);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale, scale, 1f);

        int bgColor = getBgColor();
        if ((bgColor >>> 24) != 0) drawRoundedRect(-PADDING, -PADDING, w, h - PADDING, getCornerRadius(), bgColor);

        if (skullItem != null) {
            ItemRenderUtils.renderItemWithEffects(mc, skullItem, 0, 0);
        } else {
            Gui.drawRect(0, 0, SKULL_SIZE, SKULL_SIZE, 0xFF555555);
        }

        int textY = (SKULL_SIZE - mc.fontRendererObj.FONT_HEIGHT) / 2;
        mc.fontRendererObj.drawStringWithShadow(formattedName, SKULL_SIZE + GAP, textY, 0xFFFFFF);

        GL11.glPopMatrix();
    }
}