package io.hamlook.aetheria.features.dungeons.reward;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.price.PriceMap;
import io.hamlook.aetheria.features.profile.ProfileParser;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@RegisterEvents
public class DungeonRewardProfitEstimator {

    public static HashMap<String,RewardEstimate> cache = new HashMap<>();

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) {
        cache.clear();
    }


    @SubscribeEvent
    public void onDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
        if(!ATHRConfig.feature.dungeons.priceEstimator.rewardProfitEstimator) return;
        if(!(event.gui instanceof GuiContainer)) return;
        GuiContainer container = (GuiContainer) event.gui;
        if(!(container.inventorySlots instanceof ContainerChest)) return;
        ContainerChest chest = (ContainerChest) container.inventorySlots;
        String title = ContainerUtils.getTitle(chest);
        if(!title.endsWith("Chest")) return;
        String chestID = (title.replace("Chest","").trim()).toLowerCase();
        if(getChestHeader(chestID).equals("unknown")) return;

        List<DungeonReward> rewardList = new ArrayList<>();
        long chestPrice = -1;

        if(cache.containsKey(chestID)){
            drawOverlay(container,cache.get(chestID));
        }
        for(int i = 0; i< chest.getLowerChestInventory().getSizeInventory(); i++) {
            Slot slot = chest.getSlot(i);
            if(slot == null || !slot.getHasStack()) continue;

            ItemStack stack = slot.getStack();
            if(stack == null || Objects.equals(stack.getItem().getRegistryName(), Item.getItemFromBlock(Blocks.stained_glass_pane).getRegistryName())) continue;

            if(ColorUtils.stripColor(stack.getDisplayName()).trim().
            startsWith("Open Reward Chest")){
                List<String> lore = ItemUtils.getLoreLinesWithoutColor(stack);
                for(String s : lore){
                    if(s.startsWith("Cost")){
                        int index = lore.indexOf(s) + 1;
                        if(index >= lore.size()) break;
                        String cost = lore.get(index);
                        if(cost.contains("Free")){
                            chestPrice = 0;
                        }else {
                            cost = cost.replaceAll("[^0-9,]", "");
                            chestPrice = ProfileParser.parseRawNumber(cost);
                        }
                    }
                }
                continue;
            }

            String itemID = ItemUtils.getEffectiveItemId(stack);
            if(itemID == null || itemID.isEmpty()) continue;

            double itemPrice = PriceMap.Cached.getDPrice(itemID);;

            DungeonReward reward = new DungeonReward(stack,itemPrice);
            rewardList.add(reward);
        }
        RewardEstimate rewardEstimate = new RewardEstimate(chestPrice,rewardList,chestID);
        cache.put(chestID,rewardEstimate);
    }

    private void drawOverlay(GuiContainer chest, RewardEstimate reward) {
        GlStateManager.pushMatrix();
        GlStateManager.color(1f,1f,1f,1f);
        GlStateManager.disableAlpha();
        GlStateManager.disableLighting();
        GlStateManager.disableBlend();
        ResourceLocation texture = Resources.betterContainerNineSlice(
                ATHRConfig.feature.qol.betterContainers.style
        );
        int xPos = chest.guiLeft + chest.xSize;
        if(ATHRConfig.feature.misc.invButtons.enableInvButtons){
            xPos += 24;
        }
        int yPos = chest.guiTop;
        int width = 100;
        int height = 40;

        if(reward == null || reward.getRewards().isEmpty()){
            if(reward != null) cache.remove(reward.getChestID());
            GlStateManager.popMatrix();
            return;
        }
        for(DungeonReward reward1 : reward.getRewards()){
            String name = reward1.getText();
            int tWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(name);
            tWidth += 20;
            if(width < tWidth) width = tWidth;
            height+= Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT + 4;
        }
        height += 20;
        int xCenter = xPos + (width / 2);

        if(reward.getPrice() < 0 || reward.getChestID().isEmpty()){
            NineSliceUtils.draw(texture,xPos,yPos,width,height,6,18);
            TextRenderUtils.drawCenteredStringScaleAware("Could not Get data for this chest.",xCenter,yPos + 5,2f,false);
            cache.remove(reward.getChestID());
            GlStateManager.popMatrix();
            return;
        }
        String profitString = Utils.shortNumberFormat(reward.getProfit(),0);
        double profit = reward.getProfit();

        NineSliceUtils.draw(texture,xPos,yPos,width,height,6,18);

        TextRenderUtils.drawCenteredStringScaleAware(getChestHeader(reward.getChestID()),xCenter,yPos+ 15,2f,false);
        int y = yPos + 35;
        for(DungeonReward reward2 : reward.getRewards()){
            String name = reward2.getText();
            TextRenderUtils.drawStringScaleAware(name,xPos + 5,y,1f,false);
            y += Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT + 4;
        }
        TextRenderUtils.drawStringScaleAware("§6PROFIT: " +
                        (profit > 0 ? "§a" + profitString + " coins" : "§c" + profit + " coins."),
                xPos + 5,
                y,1f,false
        );
        y += Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT + 4;
        TextRenderUtils.drawStringScaleAware("§7Prices may not always be accurate, the mod does not take responsibility for this.",
                xPos + 5,y,0.5f,false);
        GlStateManager.popMatrix();
    }

    public static String getChestHeader(String chestID) {
        switch (chestID.toLowerCase()){
            case "wood": return "§8Wood Chest";
            case "gold": return "§6Gold Chest";
            case "diamond": return "§bDiamond Chest";
            case "obsidian": return "§0Obsidian Chest";
            case "bedrock": return "§0Bedrock Chest";
        }
        return "unknown";
    }


}
