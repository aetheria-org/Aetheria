package io.hamlook.aetheria.features.profile.viewer.ui;

import io.hamlook.aetheria.core.config.gui.GuiTextures;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.profile.data.ProfileData;
import io.hamlook.aetheria.features.profile.viewer.PlayerProfile;
import io.hamlook.aetheria.features.profile.viewer.ProfileViewerAPI;
import io.hamlook.aetheria.features.profile.viewer.ui.modules.PVButton;
import io.hamlook.aetheria.features.profile.viewer.ui.modules.PlayerModule;
import io.hamlook.aetheria.features.profile.viewer.ui.modules.PVSearchBar;
import io.hamlook.aetheria.features.profile.viewer.ui.tabs.*;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ProfileViewerGUI extends GuiScreen {

    // UI Data
    public static ResourceLocation CONTAINER_BG = GuiTextures.CAPES_UI;
    public static float uiScale = 1f;
    private static int tab = 0;
    private int boxW;
    private int boxH;
    private int boxX;
    private int boxY;
    private final HashMap<Integer, Tab> tabs = new HashMap<>();

    // Player Data
    public String username;
    public int profileIndex = 0;
    public PlayerProfile playerProfile;
    public ProfileData activeProfileData;

    // State Trackers
    public boolean isFetching = true;
    public boolean hasError = false;
    public String errorMessage = "";

    // Dropdowns
    private boolean isDropdownOpen = false;
    private int dropX, dropY, dropW, dropH, itemHeight;

    private boolean isTabDropdownOpen = false;
    private int tabDropX, tabDropY, tabDropW, tabDropH, tabItemHeight;

    private List<String> tooltipToDraw = null;
    private int tooltipX, tooltipY;

    // Buttons & Inputs
    public PVButton profileButton;
    public PVButton tabButton;
    public PVSearchBar searchBar;

    public ProfileViewerGUI(String username) {
        this.username = username;
        uiScale = ATHRConfig.feature.overlays.pvScale * ResolutionUtils.getXStatic(1);
        ProfileViewerAPI.fetchPlayerListAsync();

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
                this.errorMessage = e.getMessage();
                this.hasError = true;
            } finally {
                this.isFetching = false;
            }
        }, "ATHR-GUI-FetchThread").start();
    }

    public static float getScaleHeader() {
        return Math.max(0.25f, getScaledF(1)) * 3f;
    }

    public static float getScaleText() {
        return Math.max(0.25f, getScaledF(1)) * 2f;
    }

    public void addTab(Tab tab) {
        this.tabs.put(tab.tabIndex, tab);
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        profileButton = null;
        tabButton = null;
        isDropdownOpen = false;
        isTabDropdownOpen = false;
        CONTAINER_BG = GuiTextures.storageBackground(1);
        uiScale = ATHRConfig.feature.overlays.pvScale * ResolutionUtils.getXStatic(1);
        addTab(new BasicInfoTab());
        addTab(new InventoryStorageInfoTab());
        addTab(new SkillInfoTab());
        addTab(new DungeonInfoTab());
        addTab(new SlayerInfoTab());
        addTab(new CollectionInfoTab());
        addTab(new ExtraWearableInfoTab());
        addTab(new HOTMInfoTab());
        addTab(new BagsInfoTab());
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    public void drawTooltip(List<String> textLines, int x, int y) {
        this.tooltipToDraw = textLines;
        this.tooltipX = x;
        this.tooltipY = y;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int maxWidth = (int)(this.width * 0.9f);
        boxW = Math.min(maxWidth, getScaled(900));
        boxH = (int)(boxW * 0.62f);
        boxX = (this.width / 2) - (boxW / 2);
        boxY = (this.height / 2) - (boxH / 2);

        int centerX = boxX + (boxW / 2);
        int centerY = boxY + (boxH / 2);

        if (isFetching) {
            NineSliceUtils.draw(CONTAINER_BG, boxX, boxY, boxW, boxH, 6, 18);
            String text = "Fetching data for " + this.username + "...";
            int textWidth = fontRendererObj.getStringWidth(text);
            drawString(fontRendererObj, text, centerX - (textWidth / 2), centerY - (fontRendererObj.FONT_HEIGHT / 2), 0xFFFFAA00);

            updateAndDrawSearchBar(boxX + boxW, boxY, mouseX, mouseY);

        } else if (hasError) {
            NineSliceUtils.draw(CONTAINER_BG, boxX, boxY, boxW, boxH, 6, 18);
            String text = "An error occurred while fetching data for " +  this.username + "!";
            drawCenteredString(fontRendererObj, text, centerX, centerY - (fontRendererObj.FONT_HEIGHT / 2), 0xFFFF5555);
            drawCenteredString(fontRendererObj,this.errorMessage,centerX, centerY + (2*fontRendererObj.FONT_HEIGHT),0xFFFF5555);
            updateAndDrawSearchBar(boxX + boxW, boxY, mouseX, mouseY);

        } else if (this.playerProfile == null) {
            NineSliceUtils.draw(CONTAINER_BG, boxX, boxY, boxW, boxH, 6, 18);
            String text = this.username + " (Not In Database)";
            int textWidth = fontRendererObj.getStringWidth(text);
            drawString(fontRendererObj, text, centerX - (textWidth / 2), centerY - (fontRendererObj.FONT_HEIGHT / 2), 0xFFAAAAAA);

            updateAndDrawSearchBar(boxX + boxW, boxY, mouseX, mouseY);

        } else {
            int leftBoxWidth = drawBasicBG(mouseX, mouseY);
            int rightBoxX = boxX + leftBoxWidth + getScaled(10);

            int profileW = getScaled(200);
            int profileH = getScaled(30);
            int profileX = boxX + (leftBoxWidth / 2) - (profileW / 2);
            int profileY = boxY + boxH - profileH - getScaled(12);

            int scale = getScaled(150);
            int playerX = boxX + (leftBoxWidth / 2);
            int playerY = profileY - getScaled(25);

            PlayerModule.draw(playerX, playerY, scale, this.username, mouseX, mouseY);

            String profile = "§aProfile: §f" + this.activeProfileData.baseData.playerProfile + " §7▼";
            if (profileButton == null) {
                profileButton = new PVButton(0, profileX, profileY, profileW, profileH, profile);
                this.buttonList.add(profileButton);
            } else {
                profileButton.xPosition = profileX;
                profileButton.yPosition = profileY;
                profileButton.width = profileW;
                profileButton.height = profileH;
                profileButton.displayString = profile;
            }

            int tabW = getScaled(200);
            int tabH = getScaled(30);
            int tabX = rightBoxX + getScaled(12);
            int tabY = boxY + getScaled(12);

            String tabName = "§a" + tabs.get(tab).name + " §7▼";
            if (tabButton == null) {
                tabButton = new PVButton(1, tabX, tabY, tabW, tabH, tabName);
                this.buttonList.add(tabButton);
            } else {
                tabButton.xPosition = tabX;
                tabButton.yPosition = tabY;
                tabButton.width = tabW;
                tabButton.height = tabH;
                tabButton.displayString = tabName;
            }


            float lineY = tabY + tabH + getScaledF(8);
            Gui.drawRect((rightBoxX + getScaled(12)), (int) lineY, (rightBoxX + boxW - getScaled(12)), (int) (lineY + Math.max(1, getScaledF(1))), new java.awt.Color(255, 255, 255, 40).getRGB());

            float contentY = lineY + getScaledF(8);
            int contentH = (boxY + boxH) - (int)contentY - getScaled(12);
            
            updateAndDrawSearchBar(rightBoxX + boxW, boxY, mouseX, mouseY);
            tabs.get(tab).draw(rightBoxX, contentY, boxW, contentH, activeProfileData, mc);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (isDropdownOpen && this.playerProfile != null && this.playerProfile.profiles != null) {
            drawProfileDropdown(mouseX, mouseY);
        }
        if (isTabDropdownOpen) {
            drawTabDropdown(mouseX, mouseY);
        }

        if (tooltipToDraw != null) {
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.translate(0, 0, 500);
            TextRenderUtils.drawHoveringText(tooltipToDraw, tooltipX, tooltipY, fontRendererObj);
            net.minecraft.client.renderer.GlStateManager.popMatrix();
            tooltipToDraw = null;
        }
    }

    private void updateAndDrawSearchBar(int rightEdgeX, int topEdgeY, int mouseX, int mouseY) {
        int searchW = getScaled(300);
        int searchH = getScaled(36);
        int searchX = rightEdgeX - searchW - getScaled(12);
        int searchY = topEdgeY + getScaled(12);

        if (searchBar == null) {
            searchBar = new PVSearchBar(searchX, searchY, searchW, searchH);
        } else {
            searchBar.x = searchX;
            searchBar.y = searchY;
            searchBar.width = searchW;
            searchBar.height = searchH;
        }
        searchBar.draw(mouseX, mouseY, uiScale);
    }

    private void drawProfileDropdown(int mouseX, int mouseY) {
        int numProfiles = this.playerProfile.profiles.size();
        itemHeight = getScaled(20);

        dropX = profileButton.xPosition;
        dropW = profileButton.width;
        dropH = itemHeight * numProfiles;
        dropY = profileButton.yPosition + profileButton.height;

        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.translate(0, 0, 300);

        NineSliceUtils.draw(CONTAINER_BG, dropX, dropY, dropW, dropH, 6, 18);

        for (int i = 0; i < numProfiles; i++) {
            ProfileData pData = this.playerProfile.profiles.get(i);
            String pName = pData.baseData.playerProfile;
            int itemY = dropY + (i * itemHeight);

            boolean isHovered = mouseX >= dropX && mouseX <= dropX + dropW &&
                    mouseY >= itemY && mouseY <= itemY + itemHeight;

            if (isHovered) {
                net.minecraft.client.gui.Gui.drawRect(dropX + 4, itemY, dropX + dropW - 4, itemY + itemHeight, 0x30FFFFFF);
            }

            float centerX = dropX + (dropW / 2.0f);
            float centerY = itemY + (itemHeight / 2.0f);

            String displayPrefix = (i == profileIndex) ? "§a> §f" : "§7";
            TextRenderUtils.drawCenteredStringScaleAware(displayPrefix + pName, centerX, centerY, (uiScale * 1.8f), false);
        }
        
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

    private void drawTabDropdown(int mouseX, int mouseY) {
        int numTabs = this.tabs.size();
        tabItemHeight = getScaled(20);

        tabDropX = tabButton.xPosition;
        tabDropW = tabButton.width;
        tabDropH = tabItemHeight * numTabs;
        tabDropY = tabButton.yPosition + tabButton.height;

        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.translate(0, 0, 300);

        NineSliceUtils.draw(CONTAINER_BG, tabDropX, tabDropY, tabDropW, tabDropH, 6, 18);

        List<Tab> sortedTabs = new ArrayList<>(tabs.values());
        sortedTabs.sort(Comparator.comparingInt(t -> t.tabIndex));

        for (int i = 0; i < sortedTabs.size(); i++) {
            Tab t = sortedTabs.get(i);
            int itemY = tabDropY + (i * tabItemHeight);

            boolean isHovered = mouseX >= tabDropX && mouseX <= tabDropX + tabDropW &&
                    mouseY >= itemY && mouseY <= itemY + tabItemHeight;

            if (isHovered) {
                net.minecraft.client.gui.Gui.drawRect(tabDropX + 4, itemY, tabDropX + tabDropW - 4, itemY + tabItemHeight, 0x30FFFFFF);
            }

            float centerX = tabDropX + (tabDropW / 2.0f);
            float centerY = itemY + (tabItemHeight / 2.0f);

            String displayPrefix = (t.tabIndex == tab) ? "§a> §f" : "§7";
            TextRenderUtils.drawCenteredStringScaleAware(displayPrefix + t.name, centerX, centerY, (uiScale * 1.8f), false);
        }
        
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchBar != null && searchBar.isFocused) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                searchBar.isFocused = false;
                return;
            }
            boolean pressedEnter = searchBar.keyTyped(typedChar, keyCode);

            if (pressedEnter) {
                String target = searchBar.text.trim();
                if (!target.isEmpty()) {
                    Minecraft.getMinecraft().displayGuiScreen(new ProfileViewerGUI(target));
                }
            }
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (searchBar != null) {
            String suggestionClicked = searchBar.mouseClicked(mouseX, mouseY, mouseButton);
            if (suggestionClicked != null) {
                Minecraft.getMinecraft().displayGuiScreen(new ProfileViewerGUI(suggestionClicked));
                return;
            }
        }

        if (mouseButton == 0) {
            if (isDropdownOpen) {
                if (mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= dropY && mouseY <= dropY + dropH) {
                    int clickedIndex = (mouseY - dropY) / itemHeight;
                    if (clickedIndex >= 0 && clickedIndex < playerProfile.profiles.size()) {
                        profileIndex = clickedIndex;
                        activeProfileData = playerProfile.profiles.get(profileIndex);
                        isDropdownOpen = false;
                        return;
                    }
                } else if (profileButton != null && !(mouseX >= profileButton.xPosition && mouseX <= profileButton.xPosition + profileButton.width &&
                        mouseY >= profileButton.yPosition && mouseY <= profileButton.yPosition + profileButton.height)) {
                    isDropdownOpen = false;
                }
            }

            if (isTabDropdownOpen) {
                if (mouseX >= tabDropX && mouseX <= tabDropX + tabDropW && mouseY >= tabDropY && mouseY <= tabDropY + tabDropH) {
                    int clickedRow = (mouseY - tabDropY) / tabItemHeight;

                    List<Tab> sortedTabs = new ArrayList<>(tabs.values());
                    sortedTabs.sort(Comparator.comparingInt(t -> t.tabIndex));

                    if (clickedRow >= 0 && clickedRow < sortedTabs.size()) {
                        tab = sortedTabs.get(clickedRow).tabIndex; // Switch tab!
                        isTabDropdownOpen = false;
                        return;
                    }
                } else if (tabButton != null && !(mouseX >= tabButton.xPosition && mouseX <= tabButton.xPosition + tabButton.width &&
                        mouseY >= tabButton.yPosition && mouseY <= tabButton.yPosition + tabButton.height)) {
                    isTabDropdownOpen = false;
                }
            }
            
            if (tabs.containsKey(tab) && !isDropdownOpen && !isTabDropdownOpen) {
                tabs.get(tab).mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (profileButton != null && button.id == profileButton.id) {
            isDropdownOpen = !isDropdownOpen;
            isTabDropdownOpen = false;
        } else if (tabButton != null && button.id == tabButton.id) {
            isTabDropdownOpen = !isTabDropdownOpen;
            isDropdownOpen = false;
        }
    }

    public int drawBasicBG(int mouseX, int mouseY) {
        String name = "§a" + this.username;

        String updateTimeText = this.playerProfile.update_time;
        String syncTimeText = this.playerProfile.updated_at;
        String updateDate = "";
        String syncDate = "";

        String updateHour = "";
        String syncHour = "";

        boolean parseUpd = true,parseSync = true;
        if(updateTimeText == null || updateTimeText.isEmpty()){
            updateDate = "Never";
            parseUpd = false;
        }
        if(syncTimeText == null || syncTimeText.isEmpty()){
            syncDate = "Never";
            parseSync = false;
        }

        ZoneId targetZone = ZoneId.systemDefault();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        if(parseUpd){
            Instant updateT = Instant.parse(updateTimeText);
            ZonedDateTime localizedUpd = updateT.atZone(targetZone);
            updateDate = "§8Uploaded: §f" + dateFormatter.format(localizedUpd);
            updateHour = "§7(" + timeFormatter.format(localizedUpd) + ")";

        }
        if(parseSync) {
            Instant syncT = Instant.parse(syncTimeText);
            ZonedDateTime localizedSync = syncT.atZone(targetZone);
            syncDate = "§8Sync: §f" + dateFormatter.format(localizedSync);
            syncHour = "§7(" + timeFormatter.format(localizedSync) + ")";
        }
        float textScale = Math.max(0.25f, getScaledF(1)) * 2.5f;
        float labelScale = textScale * 0.70f;
        float hourScale = textScale * 0.55f;

        float nameWidth = fontRendererObj.getStringWidth(name) * textScale;

        float updDateWidth = fontRendererObj.getStringWidth(updateDate) * labelScale;
        float updHourWidth = fontRendererObj.getStringWidth(updateHour) * hourScale;
        float fullUpdWidth = updDateWidth + getScaled(5) + updHourWidth;

        float syncDateWidth = fontRendererObj.getStringWidth(syncDate) * labelScale;
        float syncHourWidth = fontRendererObj.getStringWidth(syncHour) * hourScale;
        float fullSyncWidth = syncDateWidth + getScaled(5) + syncHourWidth;

        float maxTextWidth = Math.max(nameWidth, Math.max(fullUpdWidth, fullSyncWidth));

        int leftBoxWidth = (int)(maxTextWidth + getScaledF(20));
        int gap = getScaled(10);
        int totalCombinedWidth = leftBoxWidth + gap + boxW;

        boxX = (this.width / 2) - (totalCombinedWidth / 2);
        int rightBoxX = boxX + leftBoxWidth + gap;

        int textX = boxX + getScaled(10);
        int nameY = boxY + getScaled(12);

        int updateY = boxY + getScaled(32);
        int syncY = updateY + getScaled(14);

        NineSliceUtils.draw(CONTAINER_BG, boxX, boxY, leftBoxWidth, boxH, 6, 18);
        NineSliceUtils.draw(CONTAINER_BG, rightBoxX, boxY, boxW, boxH, 6, 18);

        TextRenderUtils.drawStringScaleAware(name, textX, nameY, textScale, false);

        float textHeight = fontRendererObj.FONT_HEIGHT * labelScale;

        boolean hoverUpd = mouseX >= textX && mouseX <= textX + fullUpdWidth && mouseY >= updateY && mouseY <= updateY + textHeight;
        boolean hoverSync = mouseX >= textX && mouseX <= textX + fullSyncWidth && mouseY >= syncY && mouseY <= syncY + textHeight;

        TextRenderUtils.drawStringScaleAware(updateDate, textX, updateY, labelScale, false);
        if (hoverUpd) {
            TextRenderUtils.drawStringScaleAware(updateHour, textX + updDateWidth + getScaled(5), updateY + (textHeight - fontRendererObj.FONT_HEIGHT * hourScale), hourScale, false);
        }

        TextRenderUtils.drawStringScaleAware(syncDate, textX, syncY, labelScale, false);
        if (hoverSync) {
            TextRenderUtils.drawStringScaleAware(syncHour, textX + syncDateWidth + getScaled(5), syncY + (textHeight - fontRendererObj.FONT_HEIGHT * hourScale), hourScale, false);
        }

        return leftBoxWidth;
    }

    public static int getScaled(double initial){
        return (int)(initial*uiScale);
    }

    public static float getScaledF(double initial){
        return (float) (initial*uiScale);
    }
}