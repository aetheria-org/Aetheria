package io.hamlook.aetheria.features.chestanimations.caseopening;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.DebugLogger;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chestanimations.ChestAnimations;
import io.hamlook.aetheria.features.chestanimations.ChestListener;
import io.hamlook.aetheria.features.chestanimations.CitManager;
import io.hamlook.aetheria.features.chestanimations.TextureData;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomDropAnimationGui extends GuiScreen {

    private static final ResourceLocation FADE_SIDE  = Resources.CASE_FADE_SIDE;
    // AUDIO is a Minecraft sound event ResourceLocation – kept inline below where used
    private static final ResourceLocation AUDIO = new ResourceLocation("gui.button.press");

    private final Minecraft mc = Minecraft.getMinecraft();
    private final DungeonDropData.Rule rewardItem;
    private final ItemStack rewardStack;
    private final DungeonDropData.Floor floor;
    private final DungeonDropData.CaseMaterial material;

    private final List<DungeonDropData.Rule> carouselItems = new ArrayList<>();
    private final int itemCount = 50;
    private final int rewardSlot = 44;
    private final long guiOpenStartTime;
    private final long animationDuration = 300_000_000L;
    private final float randstop;
    private final float randslow;
    private final boolean resultHandled = false;
    private int scaleFactor, screenWidth, screenHeight;
    private int lastScaleFactor = -1, lastScreenWidth = -1, lastScreenHeight = -1;
    private float lastSpacing = -1f;
    private Framebuffer frameBufferLayer1, frameBufferLayer2;
    private ShaderGroup blurShader;
    private long lastFrameTime = 0L;
    private float currentScrollSpeed;
    private float offsetX = 0f;
    private float itemBoxWidth, itemBoxHeight, itemBoxPadding, spacing;
    private float centerX, centerY;
    private float stopPoint, slowPoint;
    private int lastBoxDistance = 0;
    private boolean hasShownResult = false;

    public CustomDropAnimationGui(ContainerChest container, DungeonDropData.Floor floor, DungeonDropData.CaseMaterial material) {
        this.mc.getSoundHandler().playSound(PositionedSoundRecord.create(AUDIO, 1.0F));
        this.floor = floor;
        this.material = material;
        this.rewardItem = ChestAnimations.findBestReward(container, floor, material);
        this.rewardStack = findRewardStack(container, rewardItem);
        this.randstop = (float) randStopPoint();
        this.randslow = (float) ThreadLocalRandom.current().nextDouble(-2, 2);
        this.guiOpenStartTime = System.nanoTime();
        this.lastFrameTime = System.nanoTime();

        if (this.rewardItem == null) {
            Aetheria.logger.warning("[CustomDropAnimationGui] No matching reward found (floor=" + floor + ", material=" + material + ") — returning to chest GUI");
            Minecraft.getMinecraft().displayGuiScreen(ChestListener.originalGui);
            return;
        }
        Aetheria.logger.info("[CustomDropAnimationGui] Animation started — reward=" + rewardItem.item.name() + " (rarity " + rewardItem.rarity + "), floor=" + floor + ", material=" + material);
        buildCarousel(rewardItem);
    }

    private static double randStopPoint() {
        double rand = ThreadLocalRandom.current().nextDouble(0, 100);
        if (rand < 40) return ThreadLocalRandom.current().nextDouble(0, 0.2);
        if (rand < 60) return ThreadLocalRandom.current().nextDouble(0.2, 0.8);
        return ThreadLocalRandom.current().nextDouble(0.8, 1.0);
    }

    private static ItemStack findRewardStack(ContainerChest container, DungeonDropData.Rule reward) {
        if (container == null || reward == null) return null;
        IInventory lower = ContainerUtils.getLowerInventory(container);
        if (lower == null) return null;
        for (int i = 0; i < lower.getSizeInventory(); i++) {
            ItemStack s = lower.getStackInSlot(i);
            if (s == null || s.getItem() == null) continue;
            if (reward.item.getId().equals(ItemUtils.getEffectiveItemId(s))) return s;
        }
        return null;
    }

    private void buildCarousel(DungeonDropData.Rule rewardItem) {
        List<DungeonDropData.Rule> allDrops = DungeonDropData.getDrops(material, floor);
        List<DungeonDropData.Rule> books = new ArrayList<>();
        List<DungeonDropData.Rule> nonBooks = new ArrayList<>();
        for (DungeonDropData.Rule r : allDrops) {
            if (r.item.isBook()) books.add(r);
            else nonBooks.add(r);
        }

        int rolls = itemCount - 1;
        int bookCap = Math.min(books.size(), Math.max(rolls / 4, 1));

        List<DungeonDropData.Rule> pool = new ArrayList<>();
        int[] tierWeight = {2, 5, 12, 25, 60, 100, 140};
        for (DungeonDropData.Rule r : nonBooks) {
            if (r.rarity <= 3 && r.item.equals(rewardItem.item)) continue;
            int w = tierWeight[Math.max(0, Math.min(6, r.rarity - 1))];
            for (int k = 0; k < w; k++) pool.add(r);
        }

        Map<DungeonDropData.Rule, Integer> picks = new HashMap<>();
        int nonBookSlots = pool.isEmpty() ? 0 : rolls - bookCap;
        int filled = 0;
        for (int j = 0; j < nonBookSlots; j++) {
            DungeonDropData.Rule picked = pickWeighted(pool, picks);
            if (picked == null) break;
            carouselItems.add(picked);
            picks.merge(picked, 1, Integer::sum);
            filled++;
        }

        int bookSlots = pool.isEmpty() ? rolls : bookCap + (nonBookSlots - filled);
        if (books.isEmpty()) {
            for (int j = 0; j < bookSlots; j++)
                carouselItems.add(allDrops.get(ThreadLocalRandom.current().nextInt(allDrops.size())));
        } else {
            for (int j = 0; j < bookSlots; j++)
                carouselItems.add(books.get(ThreadLocalRandom.current().nextInt(books.size())));
        }
        carouselItems.add(rewardSlot, rewardItem);
    }

    private DungeonDropData.Rule pickWeighted(List<DungeonDropData.Rule> pool, Map<DungeonDropData.Rule, Integer> picks) {
        double total = 0;
        List<Double> cum = new ArrayList<>();
        for (DungeonDropData.Rule r : pool) {
            int p = picks.getOrDefault(r, 0);
            double mult = p == 0 ? 1.0 : p == 1 ? 0.5 : 0.0;
            total += mult;
            cum.add(total);
        }
        if (total <= 0) return null;
        double roll = ThreadLocalRandom.current().nextDouble(total);
        for (int i = 0; i < pool.size(); i++) {
            if (roll < cum.get(i)) return pool.get(i);
        }
        return pool.get(pool.size() - 1);
    }

    private double velocityFromX(double distanceToStop) {
        double X = Math.max(stopPoint - slowPoint, spacing);
        double T = Math.max(ATHRConfig.feature.dungeons.caseOpening.caseOpeningSlowTime, 0.1);
        double fullSpeed = X * 3 / T;

        if (distanceToStop <= 0) return 0;          // reached or overshot stop
        if (distanceToStop >= X) return fullSpeed;   // before slow zone: full speed

        double normX = (X - distanceToStop) / X;
        double t = T * (1 - Math.cbrt(1 - normX));
        return X * 3 * Math.pow(1 - t / T, 2) / T;
    }

    private void updateLayout() {
        this.itemBoxWidth = screenWidth / 5f;
        this.itemBoxHeight = screenHeight / 4f;
        this.itemBoxPadding = screenWidth / 64f;
        this.spacing = itemBoxWidth + itemBoxPadding;

        if (lastSpacing > 0f) offsetX *= (spacing / lastSpacing);
        this.lastSpacing = spacing;
        this.lastScaleFactor = scaleFactor;
        this.lastScreenWidth = screenWidth;
        this.lastScreenHeight = screenHeight;
        this.centerX = screenWidth / 2f;
        this.centerY = screenHeight / 2f;
        this.stopPoint = 41 * spacing + randstop * itemBoxWidth;
        this.slowPoint = stopPoint - ((ATHRConfig.feature.dungeons.caseOpening.caseOpeningSlowDistance + randslow) * spacing);

        frameBufferLayer1 = new Framebuffer(screenWidth, screenHeight, true);
        frameBufferLayer2 = new Framebuffer(screenWidth, screenHeight, true);
        GL11.glPushMatrix();
        GL11.glScalef(1f / scaleFactor, 1f / scaleFactor, 1f);
        setupStencil(frameBufferLayer1);
        setupStencil(frameBufferLayer2);
        GL11.glPopMatrix();
        if (blurShader != null) blurShader.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
    }

    private void setupStencil(Framebuffer fb) {
        fb.setFramebufferColor(0, 0, 0, 0);
        fb.enableStencil();
        fb.bindFramebuffer(true);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glColorMask(false, false, false, false);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilMask(0xFF);
        drawCircleMask(centerX, centerY, centerY * 2 / 3, 0xFFFFFFFF);
        GL11.glColorMask(true, true, true, true);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    @Override
    public void initGui() {
        super.initGui();
        DebugLogger.log("[ATHR ANIMATION] initGui called - initializing animation GUI");
        try {
            blurShader = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), Resources.CASE_BLUR_SHADER);
            blurShader.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
            DebugLogger.log("[ATHR ANIMATION] Blur shader initialized successfully");
        } catch (Exception e) {
            DebugLogger.log("[ATHR ANIMATION] ERROR: Failed to initialize blur shader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (rewardItem == null) {
            Aetheria.logger.warning("[CustomDropAnimationGui] drawScreen skipped: rewardItem is null");
            return;
        }
        if (lastFrameTime == 0L) {
            Aetheria.logger.info("[CustomDropAnimationGui] First drawScreen — reward=" + rewardItem.item.name()
                    + " blurShader=" + (blurShader == null ? "null" : "ok")
                    + " fb1=" + (frameBufferLayer1 == null ? "null" : "ok"));
        }
        // Clear the screen to prevent black background with OptiFine
        drawDefaultBackground();

        float progress = Math.min((System.nanoTime() - guiOpenStartTime) / (float) animationDuration, 1.0f);
        progress = 1 - (1 - progress) * (1 - progress);

        ScaledResolution scaled = new ScaledResolution(mc);
        scaleFactor = scaled.getScaleFactor();
        screenWidth = scaled.getScaledWidth() * scaleFactor;
        screenHeight = scaled.getScaledHeight() * scaleFactor;
        if (scaleFactor != lastScaleFactor || screenWidth != lastScreenWidth || screenHeight != lastScreenHeight)
            updateLayout();

        long now = System.nanoTime();
        float deltaTime = Math.min((now - lastFrameTime) / 1_000_000_000f, 0.05f);
        lastFrameTime = now;

        if (!hasShownResult) {
            float distanceToStop = stopPoint - offsetX;
            currentScrollSpeed = (float) velocityFromX(distanceToStop);
            offsetX += currentScrollSpeed * deltaTime;


            if (Math.abs(distanceToStop) < 0.1f || currentScrollSpeed == 0) {
                offsetX = stopPoint;
                hasShownResult = true;
                DebugLogger.log("Animation reached stop point, showing result now.");
            }
        }

        if (hasShownResult) {
            String sound;
            switch (rewardItem.rarity) {
                case 7:
                    sound = "dig.grass";
                    break;
                case 6:
                    sound = "liquid.splash";
                    break;
                case 5:
                    sound = "random.orb";
                    break;
                case 4:
                    sound = "fireworks.launch";
                    break;
                case 3:
                case 1:
                    sound = "random.levelup";
                    break;
                case 2:
                    sound = "mob.wither.spawn";
                    break;
                default:
                    return;
            }

            mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("minecraft", sound), 1.0F));
            if (rewardItem.rarity == 1) {
                mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("minecraft", "fireworks.launch"), 1.0F));
                mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("minecraft", "mob.wither.spawn"), 1.0F));
            }
            mc.displayGuiScreen(ChestListener.originalGui);
        }

        GL11.glPushMatrix();
        GL11.glScalef(1f / scaleFactor, 1f / scaleFactor, 1f);
        renderLayer(frameBufferLayer1, progress, false);
        if (blurShader != null) blurShader.loadShaderGroup(partialTicks);
        renderLayer(frameBufferLayer2, progress, true);
        renderJudgementLine();
        renderItemNames(progress);
        GL11.glPopMatrix();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderLayer(Framebuffer fb, float progress, boolean inner) {
        fb.bindFramebuffer(true);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        if (inner) {
            GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
            GlStateManager.color(1f, 1f, 1f, 1f);
            drawCircleMask(centerX, centerY, centerY * 2 / 3 * progress, 0x3F000000);
        } else {
            GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0xFF);
        }
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glStencilMask(0x00);
        drawCarousel(centerX, centerY, inner ? progress : progress * 0.8f);

        if (!inner) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_ALPHA);
            mc.getTextureManager().bindTexture(FADE_SIDE);
            Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, 1920, 1080, screenWidth, screenHeight);
            GlStateManager.disableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }

        GL11.glDisable(GL11.GL_STENCIL_TEST);
        mc.getFramebuffer().bindFramebuffer(true);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        fb.framebufferRenderExt(screenWidth, screenHeight, false);
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawCarousel(float cx, float cy, float size) {
        for (int i = 0; i < carouselItems.size(); i++) {
            float x = cx - (offsetX - (i - 3) * spacing) * size;
            float y = cy - itemBoxHeight / 2 * size;
            if (x + itemBoxWidth < 0 || x > screenWidth || y + itemBoxHeight < 0 || y > screenHeight) continue;

            DropRarity rarity = DropRarity.fromIndex(carouselItems.get(i).rarity);
            if (rarity == null) continue;
            int boxColor = getBoxColor(rarity);

            float y1 = y + itemBoxHeight * size * 15 / 16f;
            float y2 = y + itemBoxHeight * size;

            GlStateManager.color(1f, 1f, 1f, 1f);
            drawRect((int) x, (int) y, (int) (x + itemBoxWidth * size), (int) (y + itemBoxHeight * size), 0x3F888888);
            drawRect((int) x, (int) y1, (int) (x + itemBoxWidth * size), (int) y2, boxColor);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            drawGradientRect((int) x, (int) (y2 - itemBoxHeight * size), (int) (x + itemBoxWidth * size), (int) y2, (boxColor & 0x00FFFFFF), (boxColor & 0x00FFFFFF) | 0xCC000000);
            GlStateManager.disableBlend();

            renderItemImage(i, x, y, size);
        }
    }

    private void renderItemNames(float size) {
        if (!ATHRConfig.feature.dungeons.caseOpening.caseOpeningAllowText) return;
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        float textScale = ATHRConfig.feature.dungeons.caseOpening.caseOpeningTextScale;
        float maxRadius = centerY * 2f / 3f;
        for (int i = 0; i < carouselItems.size(); i++) {
            float x = centerX - (offsetX - (i - 3) * spacing) * size;
            float y = centerY - itemBoxHeight / 2 * size;
            if (x + itemBoxWidth < 0 || x > screenWidth || y + itemBoxHeight < 0 || y > screenHeight) continue;

            float boxCx = x + itemBoxWidth * size / 2;
            float boxCy = y + itemBoxHeight * size / 2;
            float dx = boxCx - centerX, dy = boxCy - centerY;
            if (dx * dx + dy * dy > maxRadius * maxRadius) continue;

            DropRarity rarity = DropRarity.fromIndex(carouselItems.get(i).rarity);
            if (rarity == null) continue;
            int boxColor = getBoxColor(rarity);

            float textX = x + itemBoxWidth * size / 2;
            float textY = y + itemBoxHeight * size + 8;
            float factor = textScale * 3f;
            String text = normalizeString(carouselItems.get(i).item.name());
            float textWidth = mc.fontRendererObj.getStringWidth(text);
            TextRenderUtils.drawStringScaled(text, mc.fontRendererObj, textX - textWidth * factor / 2f, textY, true, boxColor, factor);
        }
    }

    private void renderItemImage(int slot, float x, float y, float size) {
        float imageSize = 0.8f * itemBoxHeight * size;
        float imageX = x + (itemBoxWidth * size) / 2 - imageSize / 2;
        float imageY = y + (itemBoxHeight * size) / 2 - imageSize / 2;

        ItemStack renderStack = null;
        if (slot == rewardSlot && rewardStack != null) {
            renderStack = rewardStack;
        } else if (carouselItems.get(slot).item.isBook()) {
            renderStack = new ItemStack(Items.enchanted_book);
        }

        if (renderStack != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(imageX, imageY, 0.0f);
            GlStateManager.scale(imageSize / 16f, imageSize / 16f, 1.0f);
            mc.getRenderItem().renderItemAndEffectIntoGUI(renderStack, 0, 0);
            GlStateManager.popMatrix();
            return;
        }

        String name = carouselItems.get(slot).item.name();
        TextureData tex = CitManager.getTextureData(name);
        if (tex.getRl() == null) return;

        int frameHeight = 16;
        float frameDuration = tex.getFrameTime() * 50f;
        int frameIndex = (int) ((System.currentTimeMillis() % 10000L / frameDuration) % tex.getFrames());
        float v = frameIndex * frameHeight;

        try {
            mc.getTextureManager().bindTexture(tex.getRl());
        } catch (Exception e) {
            mc.getTextureManager().bindTexture(TextureMap.LOCATION_MISSING_TEXTURE);
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
        drawScaledCustomSizeModalRect((int) imageX, (int) imageY, 0, v, 16, 16, (int) imageSize, (int) imageSize, 16f, frameHeight * tex.getFrames());
    }

    private void renderJudgementLine() {
        float lineWidth = screenWidth / 512f;
        float lineHeight = screenHeight / 4f;
        drawRect((int) (centerX - lineWidth / 2), (int) (centerY - lineHeight / 3 * 2.5), (int) (centerX + lineWidth), (int) (centerY + lineHeight), 0xFFFFA500);

        int boxDistance = (int) offsetX % (int) spacing;
        if (boxDistance < lastBoxDistance) mc.getSoundHandler().playSound(PositionedSoundRecord.create(AUDIO, 1.0F));
        lastBoxDistance = boxDistance;
    }

    private int getBoxColor(DropRarity rarity) {
        switch (rarity) {
            case PRAYTORNG:
                return getRainbowColor();
            case DIVINE:
                return 0xFF4EEBEB;
            case MYTHIC:
                return 0xFFF953F9;
            case LEGENDARY:
                return 0xFFEF9E01;
            case EPIC:
                return 0xFFAA00AA;
            case FISH:
                return 0xFF4C4BE2;
            case COMMON:
                return 0xFFE0DFE0;
            default:
                return 0xFF000000;
        }
    }

    private int getRainbowColor() {
        float hue = (float) (System.currentTimeMillis() % 10000L / 1000.0);
        int rgb = Color.HSBtoRGB(hue, 0.6f, 1.0f);
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    private void drawCircleMask(float cx, float cy, float radius, int color) {
        float a = (color >> 24 & 255) / 255f, r = (color >> 16 & 255) / 255f, g = (color >> 8 & 255) / 255f, b = (color & 255) / 255f;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(r, g, b, a);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= 64; i++) {
            double angle = 2.0 * Math.PI * i / 64;
            GL11.glVertex2f((float) (cx + Math.cos(angle) * radius), (float) (cy + Math.sin(angle) * radius));
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private String normalizeString(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (String part : input.split("_")) {
            if (part.isEmpty()) continue;
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase()).append(" ");
        }
        return result.toString().trim();
    }

    @Override
    public void onGuiClosed() {
        if (mc.entityRenderer != null) mc.entityRenderer.stopUseShader();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            Aetheria.logger.info("[CustomDropAnimationGui] ESC pressed — returning to chest GUI. originalGui=" + (ChestListener.originalGui == null ? "NULL" : ChestListener.originalGui.getClass().getSimpleName()));
            mc.displayGuiScreen(ChestListener.originalGui);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}