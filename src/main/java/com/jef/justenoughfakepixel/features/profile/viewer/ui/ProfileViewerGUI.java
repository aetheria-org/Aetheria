package com.jef.justenoughfakepixel.features.profile.viewer.ui;

import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.core.config.gui.GuiTextures;
import com.jef.justenoughfakepixel.features.profile.data.ProfileData;
import com.jef.justenoughfakepixel.features.profile.viewer.PlayerProfile;
import com.jef.justenoughfakepixel.features.profile.viewer.ProfileViewerAPI;
import com.jef.justenoughfakepixel.features.profile.viewer.ui.modules.BaseDataModule;
import com.jef.justenoughfakepixel.features.profile.viewer.ui.modules.PVButton;
import com.jef.justenoughfakepixel.features.profile.viewer.ui.modules.PlayerModule;
import com.jef.justenoughfakepixel.features.profile.viewer.ui.util.StringDrawer;
import com.jef.justenoughfakepixel.utils.render.NineSliceUtils;
import com.jef.justenoughfakepixel.utils.render.ResolutionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class ProfileViewerGUI extends GuiScreen {

    // UI Data
    public static ResourceLocation CONTAINER_BG = GuiTextures.CAPES_UI;
    private static float uiScale = 1f;
    private static int page = 0;
    private int boxW;
    private int boxH;
    private int boxX;
    private int boxY;
    private int pad20;
    private int pad50;


    // Player Data
    public String username;
    public int profileIndex = 0;
    public PlayerProfile playerProfile;
    public ProfileData activeProfileData;

    // State Trackers
    public boolean isFetching = true;
    public boolean hasError = false;

    // Buttons
    public PVButton profileButton;
    public ProfileViewerGUI(String username) {
        this.username = username;

        new Thread(() -> {
            try {
                if (ProfileViewerAPI.profileHashMap.containsKey(username)) {
                    this.playerProfile = ProfileViewerAPI.profileHashMap.get(username);
                } else {
                    this.playerProfile = ProfileViewerAPI.fetchUser(username);
                    if (this.playerProfile != null) {
                        ProfileViewerAPI.profileHashMap.put(username, this.playerProfile);
                    }
                }

                if (this.playerProfile != null && this.playerProfile.profiles != null && !this.playerProfile.profiles.isEmpty()) {
                    this.activeProfileData = this.playerProfile.profiles.get(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.hasError = true;
            } finally {
                this.isFetching = false;
            }
        }, "JEF-GUI-FetchThread").start();
    }

    @Override
    public void initGui() {
        super.initGui();
        uiScale = JefConfig.feature.overlays.profileViewer.pvScale;
        float finalScale = uiScale * ResolutionUtils.getXStatic(1);
        pad20 = (int) (20 * finalScale);
        pad50 = (int) (50 * finalScale);
        CONTAINER_BG = GuiTextures.storageBackground(JefConfig.feature.storage.activeContainerStyle);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        boxW = (int) Math.min((Minecraft.getMinecraft().displayWidth * 90f)/100f,
                ResolutionUtils.getXStatic((int)(1200 * uiScale)));
        boxH = (int)(boxW * 0.55f);
        boxX = (this.width / 2) - (boxW / 2);
        boxY = (this.height / 2) - (boxH / 2);

        NineSliceUtils.draw(CONTAINER_BG, boxX, boxY, boxW, boxH, 6, 18);

        int centerX = boxX + (boxW / 2);
        int centerY = boxY + (boxH / 2);

        if (isFetching) {
            String text = "Fetching data...";
            int textWidth = fontRendererObj.getStringWidth(text);
            drawString(fontRendererObj, text, centerX - (textWidth / 2), centerY - (fontRendererObj.FONT_HEIGHT / 2), 0xFFFFAA00); // Yellow/Orange

        } else if (hasError) {
            String text = "An error occurred while fetching!";
            int textWidth = fontRendererObj.getStringWidth(text);
            drawString(fontRendererObj, text, centerX - (textWidth / 2), centerY - (fontRendererObj.FONT_HEIGHT / 2), 0xFFFF5555); // Light Red

        } else if (this.playerProfile == null) {
            String text = this.username + " (Not In Database)";
            int textWidth = fontRendererObj.getStringWidth(text);
            drawString(fontRendererObj, text, centerX - (textWidth / 2), centerY - (fontRendererObj.FONT_HEIGHT / 2), 0xFFAAAAAA); // Gray

        } else {
            GlStateManager.pushMatrix();
            GlStateManager.translate(boxX, boxY, 0);
            StringDrawer.drawString("§a" + this.playerProfile.player_name + " §8(Fetched)", 5, 5, uiScale * 3.0f);
            GlStateManager.popMatrix();
            float scaleDisplay = ResolutionUtils.getXStatic(1);

            int buttonW = boxW / 5;
            int buttonX = boxX + buttonW + pad20;
            int buttonY = boxY + (int)(5 * uiScale * scaleDisplay);
            int buttonH = (int)(50 * uiScale * scaleDisplay);

            if (profileButton == null) {
                profileButton = new PVButton(1, buttonX, buttonY, buttonW, buttonH, "§a" + this.activeProfileData.baseData.playerProfile);
                this.buttonList.add(profileButton);
            } else {
                profileButton.xPosition = buttonX;
                profileButton.yPosition = buttonY;
                profileButton.width = buttonW;
                profileButton.height = buttonH;
            }
            if (page == 0) {
                drawPageZero(mouseX, mouseY);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if(button.id == 1){
            int max = this.playerProfile.profiles.size() -1;
            profileIndex++;
            if(profileIndex > max) profileIndex = 0;
            this.activeProfileData = this.playerProfile.profiles.get(profileIndex);
            if (this.profileButton != null) {
                this.profileButton.displayString = "§a" + this.activeProfileData.baseData.playerProfile;
            }
        }
    }

    public void drawPageZero(int mouseX, int mouseY){
        // Player Model
        PlayerModule.draw(boxX,boxY,boxW,boxH,uiScale,mc,username, mouseX, mouseY,false,true);
        int pad10 = (int) (10 * uiScale);
        int pad25 = (int) (25 * uiScale);
        int pad2 = (int) (2 * uiScale);

        int baseY = boxY + (2* pad50) + pad10;
        int baseX = (boxX + pad20 + boxW / 5 + pad2);
        int baseW = (int)(boxW / 3.5);
        int baseH = (int)(baseW * 0.8);

        // BaseData Model
        BaseDataModule.draw(this.activeProfileData.baseData, baseX,
                baseY, baseW, baseW, uiScale);

        // TODO: Draw the rest of fetched profile UI

    }


}