package com.jef.justenoughfakepixel.features.profile;

import com.google.gson.Gson;
import com.jef.justenoughfakepixel.JefMod;
import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.features.profile.data.*;

import java.util.*;

import com.jef.justenoughfakepixel.features.profile.data.bags.AccessoryData;
import com.jef.justenoughfakepixel.features.profile.data.bags.BagsData;
import com.jef.justenoughfakepixel.features.profile.data.bags.FishingData;
import com.jef.justenoughfakepixel.features.profile.data.bags.QuiverData;
import com.jef.justenoughfakepixel.features.profile.data.bags.vars.Arrow;
import com.jef.justenoughfakepixel.features.profile.data.bags.vars.Bait;
import com.jef.justenoughfakepixel.features.profile.data.base.BaseData;
import com.jef.justenoughfakepixel.features.profile.data.base.NetworthData;
import com.jef.justenoughfakepixel.features.profile.data.base.Statistics;
import com.jef.justenoughfakepixel.features.profile.data.dungeon.DungeonData;
import com.jef.justenoughfakepixel.features.profile.data.dungeon.Floor;
import com.jef.justenoughfakepixel.features.profile.data.dungeon.FloorData;
import com.jef.justenoughfakepixel.features.profile.data.inventory.InventoryData;
import com.jef.justenoughfakepixel.features.profile.data.pets.Pet;
import com.jef.justenoughfakepixel.features.profile.data.pets.PetsData;
import com.jef.justenoughfakepixel.features.profile.data.skills.Skill;
import com.jef.justenoughfakepixel.features.profile.data.skills.SkillData;
import com.jef.justenoughfakepixel.features.profile.data.skills.SkillsData;
import com.jef.justenoughfakepixel.features.profile.data.slayer.Slayer;
import com.jef.justenoughfakepixel.features.profile.data.slayer.SlayerData;
import com.jef.justenoughfakepixel.features.profile.data.slayer.SlayersData;
import com.jef.justenoughfakepixel.features.profile.data.storage.ContainerData;
import com.jef.justenoughfakepixel.features.profile.data.storage.StorageData;
import com.jef.justenoughfakepixel.features.profile.data.wardrobe.WardrobeData;
import com.jef.justenoughfakepixel.features.profile.data.wardrobe.WardrobeSet;
import com.jef.justenoughfakepixel.features.profile.saving.SupabaseHandler;
import com.jef.justenoughfakepixel.features.profile.vars.EquipmentSlot;
import com.jef.justenoughfakepixel.features.profile.vars.ProfileMode;
import com.jef.justenoughfakepixel.utils.ColorUtils;
import com.jef.justenoughfakepixel.utils.RomanNumeralParser;
import com.jef.justenoughfakepixel.utils.item.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ProfileParser {

    public static void save(){
        if(!parsing) {
            JefMod.logger.info("[ProfileParser] Not Parsing cause one data is null");
            JefMod.logger.info("[ProfileParser] Data: Inventory: " +
                    (inventory[0] != null) + " | Skills: " + (skill[0] != null) +
                    " | HOTM: "+ (mountain[0] != null) + " | Dungeon: " + (dungeonData[0] != null)
                    + " | Slayer: " + (slayerData[0] != null) + " | Wardrobe: " + (wardrobeData[0] != null)
                    + " | Pets: " + (petsData[0] != null) + " | Storage: " + (storageData[0] != null)
            + " | Bags: " + (bagsData[0] != null));
            return;
        }
        ProfileData profile = new ProfileData(base, inventory[0], skill[0], mountain[0],
                dungeonData[0], slayerData[0],wardrobeData[0],petsData[0],storageData[0],
                bagsData[0]);
        profileData.put(base.playerName, profile);
        writeToJson(profile);
        SupabaseHandler.pushProfileAsync(base.playerName, profile);
        parsing = false;
        JefMod.logger.info("[ProfileParser] Saved profile: " + base.playerName);
    }

    public static HashMap<String, ProfileData> profileData = new HashMap<>();
    public static String lastCachedProfile = "";
    public static boolean parsing = false;
    public static int windowID = -1;
    public static Gson GSON = new Gson();

    public static final InventoryData[] inventory = new InventoryData[1];
    public static final SkillsData[] skill = new SkillsData[1];
    public static final HOTMData[] mountain = new HOTMData[1];
    public static final DungeonData[] dungeonData = new DungeonData[1];
    public static final SlayersData[] slayerData = new SlayersData[1];
    public static final WardrobeData[] wardrobeData = new WardrobeData[1];
    public static final PetsData[] petsData = new PetsData[1];
    public static final StorageData[] storageData = new StorageData[1];
    public static final BagsData[] bagsData = new BagsData[1];
    public static final FishingData[] fishingData = new FishingData[1];
    public static BaseData base;


    public static void parse(String player, Container container) {
        base = parseBasicInfo(container);
        if (base == null) return;
        base.playerProfile = lastCachedProfile;
        parsing = true;

        Minecraft mc = Minecraft.getMinecraft();

        JefMod.logger.info("[ProfileParser] Pending inventory for: " + base.playerName);
        windowID = container.windowId;
        mc.playerController.windowClick(
                windowID, 19, 0, 0, Minecraft.getMinecraft().thePlayer
        );

        // Inventory
        GuiWaiter.waitFor("View Inventory",2,8,"View Profile",inv -> {
            inventory[0] = parseInvData(inv);
            if (inventory[0] == null) {
                JefMod.logger.info("[ProfileParser] InventoryData was null for: " + base.playerName);
                parsing = false;
                return;
            }
            JefMod.logger.info("[ProfileParser] InventoryData parsed for: " + base.playerName);
        },prof1 -> {
            windowID = prof1.windowId;
            mc.playerController.windowClick(windowID,21,0,0,mc.thePlayer);

            // Skills
            GuiWaiter.waitFor("View Skills",2,49,"View Profile",skills -> {
                skill[0] = parseSkills(skills);
                if(skill[0] == null){
                    JefMod.logger.info("[ProfileParser] SkillData was null for: " + base.playerName);
                    parsing = false;
                    return;
                }
                JefMod.logger.info("[ProfileParser] SkillData parsed for: " + base.playerName);
            },prof2 -> {
                windowID = prof2.windowId;
                mc.playerController.windowClick(windowID,41,0,0,mc.thePlayer);

                // HOTM
                GuiWaiter.waitFor("View HOTM", 2,31,"View Profile",hotm -> {
                    mountain[0] = parseHOTM(hotm);
                    if(mountain[0] == null){
                        JefMod.logger.info("[ProfileParser] HOTMData was null for: " + base.playerName);
                        parsing = false;
                        return;
                    }
                    JefMod.logger.info("[ProfileParser] HOTMData parsed for: " + base.playerName);
                },prof3 -> {
                    windowID = prof3.windowId;
                    mc.playerController.windowClick(windowID,43,0,0,mc.thePlayer);

                    // Dungeon
                    GuiWaiter.waitFor("View Dungeon Stats",2,49,"View Profile",dungeon -> {
                        dungeonData[0] = parseDungeon(dungeon);
                        if(dungeonData[0] == null){
                            JefMod.logger.info("[ProfileParser] DungeonData was null for: " + base.playerName);
                            parsing = false;
                            return;
                        }
                        JefMod.logger.info("[ProfileParser] DungeonData parsed for: " + base.playerName);
                    },prof4 -> {

                        windowID = prof4.windowId;
                        mc.playerController.windowClick(windowID,33,0,0,mc.thePlayer);
                        // Slayers
                        GuiWaiter.waitFor("View Slayers",2,31,"View Profile", slayers -> {
                            slayerData[0] = parseSlayer(slayers);
                            if(slayerData[0] == null){
                                JefMod.logger.info("[ProfileParser] SlayersData was null for: " + base.playerName);
                                parsing = false;
                                return;
                            }
                            JefMod.logger.info("[ProfileParser] SlayersData parsed for: " + base.playerName);
                        },prof5 -> {
                            windowID = prof5.windowId;
                            mc.playerController.windowClick(windowID,31,0,0,mc.thePlayer);

                            // Wardrobe
                            HashMap<Integer,WardrobeSet> wardrobe = new HashMap<>();
                            GuiWaiter.waitForPaged("View Wardrobe", 2, 53, "Next Page", 48, "View Profile", chest -> wardrobe.putAll(parseWardrobe(chest)),
                                    prof6 -> {
                                        JefMod.logger.info("[ProfileParser] WardrobeData parsed for: " + base.playerName);
                                        wardrobeData[0] = new WardrobeData(wardrobe);

                                        windowID = prof6.windowId;
                                        mc.playerController.windowClick(windowID,29,0,0,mc.thePlayer);

                                        // Pets
                                        HashMap<Integer,Pet> pets = new HashMap<>();
                                        GuiWaiter.waitForPaged("View Pets",2,53,"Next Page",49,"View Profile",
                                                chest -> pets.putAll(parsePets(chest)),
                                                prof7 -> {
                                                    JefMod.logger.info("[ProfileParser] PetsData parsed for: " + base.playerName);
                                                    petsData[0] = new PetsData(pets);

                                                    windowID = prof7.windowId;
                                                    mc.playerController.windowClick(windowID,23,0,0,mc.thePlayer);

                                                    // Storage
                                                    GuiWaiter.waitFor("View Storage",2,-1,bags -> {
                                                        windowID = bags.windowId;
                                                        List<Integer> slotsToCheck = new ArrayList<>();

                                                        for(int i = 0; i < 9;i++){
                                                            ItemStack stack = bags.getSlot(i).getStack();
                                                            if(stack == null)continue;
                                                            String name = ColorUtils.stripColor(stack.getDisplayName()).trim();
                                                            if(name.equals("Blocked Page.")) continue;
                                                            if(name.startsWith("Ender Chest")){
                                                                slotsToCheck.add(i);
                                                            }
                                                        }
                                                        for(int i = 18;i < 36;i++){
                                                            ItemStack stack = bags.getSlot(i).getStack();
                                                            if(stack == null)continue;
                                                            String name = ColorUtils.stripColor(stack.getDisplayName()).trim();
                                                            if(name.equals("Empty Backpack slot")) continue;
                                                            if(name.endsWith("Backpack")){
                                                                slotsToCheck.add(i);
                                                            }
                                                        }

                                                        List<ContainerData> containers = new ArrayList<>();
                                                        parseNext(0, slotsToCheck, containers, windowID);
                                                    });
                                                });
                                    });
                        });
                    });
                });
            });
        });
    }


    private static void parseNext(int index, List<Integer> slotsToCheck, List<ContainerData> containers, int currentWindowId) {
        if (index >= slotsToCheck.size()) {
            if (containers.isEmpty()) {
                JefMod.logger.info("[ProfileParser] StorageData was null for: " + base.playerName);
                parsing = false;
                return;
            }
            JefMod.logger.info("[ProfileParser] StorageData parsed for: " + base.playerName);
            storageData[0] = new StorageData(containers);

            Minecraft mc = Minecraft.getMinecraft();
            mc.playerController.windowClick(currentWindowId,49,0,0,mc.thePlayer);


            GuiWaiter.waitFor("View Profile",2,-1,prof -> {
                windowID = prof.windowId;
                mc.playerController.windowClick(windowID,39,0,0,mc.thePlayer);

                GuiWaiter.waitFor("View Bags",2,-1,bags -> {
                    windowID = bags.windowId;
                    mc.playerController.windowClick(windowID,11,0,0,mc.thePlayer);
                    GuiWaiter.waitFor("Show Contents",2,-6,"View Bags",fishing -> {
                        fishingData[0] = parseFishing(fishing);
                        JefMod.logger.info("[ProfileParser] FishingData parsed for: " + base.playerName);

                     },bags1 -> {
                        windowID = bags1.windowId;
                        List<ItemData> accessories =  new ArrayList<>();
                        mc.playerController.windowClick(windowID,15,0,0,mc.thePlayer);
                        GuiWaiter.waitForPaged("Show Contents",2,-2,"Next Page",
                                -6,"View Bags",accessory -> accessories.addAll(parseAccessory(accessory)),
                                bags2 -> {
                            AccessoryData accessoryData = new AccessoryData(accessories);
                            JefMod.logger.info("[ProfileParser] AccessorryData parsed for: " + base.playerName);

                            windowID = bags2.windowId;
                            mc.playerController.windowClick(windowID,13,0,0,mc.thePlayer);
                            GuiWaiter.waitFor("Show Contents",2,-6,"View Bags",quiver -> {
                                QuiverData quiverData = parseQuiver(quiver);
                                JefMod.logger.info("[ProfileParser] QuiverData parsed for: " + base.playerName);
                                bagsData[0] = new BagsData(accessoryData,fishingData[0],quiverData);
                            },bags3 -> {
                                windowID = bags3.windowId;
                                mc.playerController.windowClick(windowID,31,0,0,mc.thePlayer);
                                save();
                            });
                        });
                    });
                });
            });
            return;
        }

        int slotToClick = slotsToCheck.get(index);
        Minecraft mc = Minecraft.getMinecraft();
        mc.playerController.windowClick(currentWindowId, slotToClick, 0, 0, mc.thePlayer);

        String id = slotToClick <= 9 ? "echest-" + slotToClick : "bag-" + (slotToClick - 18);

        GuiWaiter.waitFor("Show Contents", 2, -6, "View Storage",
                storage -> {
                    if (storage == null) {
                        JefMod.logger.info("Empty Container: " + id);
                        containers.add(new ContainerData(id, new HashMap<>()));

                    } else {
                        containers.add(parseStorage(id, storage));
                    }
                },
                viewStorageChest -> parseNext(index + 1, slotsToCheck, containers, viewStorageChest.windowId)
        );
    }

    private static QuiverData parseQuiver(ContainerChest container) {
        EnumMap<Arrow,Integer> quiver = new EnumMap<>(Arrow.class);
        if (container == null) return new QuiverData(quiver);

        String title = ColorUtils.stripColor(
                container.getLowerChestInventory().getDisplayName().getUnformattedText()
        ).trim();
        if (!title.equals("Show Contents")) return new QuiverData(quiver);

        for(int i = 0; i < container.getLowerChestInventory().getSizeInventory();i++){
            ItemStack stack = container.getLowerChestInventory().getStackInSlot(i);
            if(stack == null)continue;
            String name = ColorUtils.stripColor(stack.getDisplayName()).trim();
            if(name.isEmpty() || name.equals("Go Back")) continue;

            Arrow arrow = Arrow.getArrow(name);

            if(arrow == null) continue;
            int currentAmount = quiver.getOrDefault(arrow, 0);
            quiver.put(arrow, currentAmount + stack.stackSize);

        }
        return new QuiverData(quiver);
    }

    private static Collection<? extends ItemData> parseAccessory(ContainerChest container) {
        List<ItemData> accessory = new ArrayList<>();
        if (container == null) return accessory;
        String title = ColorUtils.stripColor(
                container.getLowerChestInventory().getDisplayName().getUnformattedText()
        ).trim();
        if (!title.equals("Show Contents")) return accessory;

        for(int i = 0; i < container.getLowerChestInventory().getSizeInventory();i++){
            ItemStack stack = container.getLowerChestInventory().getStackInSlot(i);
            if(stack == null)continue;
            String name = ColorUtils.stripColor(stack.getDisplayName()).trim();
            if(name.isEmpty() || name.equals("Go Back")) continue;
            accessory.add(parseItemData(stack));
        }
        return accessory;
    }

    private static FishingData parseFishing(ContainerChest container) {
        EnumMap<Bait,Integer> baits = new EnumMap<>(Bait.class);

        if (container == null) return new FishingData(baits);
        String title = ColorUtils.stripColor(
                container.getLowerChestInventory().getDisplayName().getUnformattedText()
        ).trim();
        if (!title.equals("Show Contents")) return new FishingData(baits);

        for(int i = 0; i < container.getLowerChestInventory().getSizeInventory();i++){
            ItemStack stack = container.getLowerChestInventory().getStackInSlot(i);
            if(stack == null)continue;
            String name = ColorUtils.stripColor(stack.getDisplayName()).trim();
            if(name.isEmpty() || name.equals("Go Back")) continue;

            Bait bait = Bait.getBait(name);
            if(name.endsWith("DIAMOND") || name.endsWith("BRONZE") ||
                    name.endsWith("GOLD") || name.endsWith("SILVER")){
                if(ItemUtils.getInternalName(stack).startsWith("common")){
                    bait = Bait.OBF_COMMON;
                }else {
                    bait = Bait.OBF_UNCOMMON;
                }
            }
            if(bait == null) continue;
            int currentAmount = baits.getOrDefault(bait, 0);
            baits.put(bait, currentAmount + stack.stackSize);

        }
        return new FishingData(baits);
    }


    public static ContainerData parseStorage(String id, ContainerChest storage) {
        HashMap<Integer,ItemData> data = new HashMap<>();
        if (storage == null) return new ContainerData(id, data);
        String title = ColorUtils.stripColor(
                storage.getLowerChestInventory().getDisplayName().getUnformattedText()
        ).trim();
        if (!title.equals("Show Contents")) return new ContainerData(id, data);

        for(int i = 0; i < storage.getLowerChestInventory().getSizeInventory();i++){
            ItemStack stack = storage.getLowerChestInventory().getStackInSlot(i);
            if(stack == null)continue;
            String name = ColorUtils.stripColor(stack.getDisplayName()).trim();
            if(name.isEmpty() || name.equals("Go Back")) continue;
            data.put(i, parseItemData(stack));
        }
        return new ContainerData(id,data);
    }

    public static HashMap<Integer, Pet> parsePets(ContainerChest container){
        HashMap<Integer, Pet> set = new HashMap<>();
        if (container == null) return set;
        String title = ColorUtils.stripColor(
                container.getLowerChestInventory().getDisplayName().getUnformattedText()
        ).trim();
        if (!title.equals("View Pets")) return set;

        ItemStack pageItem = container.getSlot(53).getStack();
        int add = -1;
        if(pageItem == null) return set;
        if(pageItem.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane)){
            pageItem = container.getSlot(45).getStack();
            if(pageItem == null) return set;
            add = 1;
        }
        int page = -1;
        List<String> lore = getLore(pageItem);
        for(String s : lore) {
            if (s.startsWith("Page")) {
                page = Integer.parseInt(s.replace("Page", "").trim());
            }
        }
        page += add;
        if(page < 0) return set;
        int petIndex = 0;
        for(int i = 10; i < 44;i++){
            if(i % 9 == 0 || (i + 1) % 9 == 0) continue;
            ItemStack stack = container.getSlot(i).getStack();
            if(stack == null)break;
            int petSlot = petIndex + ((page -1)* 28);
            petIndex++;
            List<String> slore = getLore(stack);
            boolean equipped = slore.contains("Click to despawn!");
            set.put(petSlot,new Pet(parseItemData(stack),page,equipped));
        }
        return set;
    }

    public static HashMap<Integer, WardrobeSet> parseWardrobe(ContainerChest container){
        HashMap<Integer, WardrobeSet> set = new HashMap<>();
        if (container == null) return set;
        String title = ColorUtils.stripColor(
                container.getLowerChestInventory().getDisplayName().getUnformattedText()
        ).trim();
        if (!title.equals("View Wardrobe")) return set;

        HashMap<Integer, Integer> parsingSlots = new HashMap<>();
        int equippedSlot = -1;

        for(int i = 36; i < 45; i++){
            ItemStack stack = container.getSlot(i).getStack();
            if(stack == null) continue;

            String name = ColorUtils.stripColor(stack.getDisplayName()).trim();

            int realSlotNumber = (i - 36) + 1;
            try {
                String[] words = name.split("[: ]");
                for (String w : words) {
                    if (w.matches("\\d+")) {
                        realSlotNumber = Integer.parseInt(w);
                        break;
                    }
                }
            } catch (Exception ignored) {}

            if(name.endsWith("Ready")){
                parsingSlots.put(realSlotNumber, i - 36);
            }
            if(name.endsWith("Equipped")){
                parsingSlots.put(realSlotNumber, i - 36);
                equippedSlot = realSlotNumber;
            }
        }

        for(Map.Entry<Integer, Integer> entry : parsingSlots.entrySet()){
            int realSlot = entry.getKey();
            int localCol = entry.getValue();

            ItemStack helm = container.getSlot(localCol).getStack();
            ItemStack chestplate = container.getSlot(localCol + 9).getStack();
            ItemStack leggings = container.getSlot(localCol + 18).getStack();
            ItemStack boots = container.getSlot(localCol + 27).getStack();

            if(helm == null || chestplate == null || leggings == null || boots == null) continue;

            String hName = ColorUtils.stripColor(helm.getDisplayName()).trim();
            String cName = ColorUtils.stripColor(chestplate.getDisplayName()).trim();
            String lName = ColorUtils.stripColor(leggings.getDisplayName()).trim();
            String bName = ColorUtils.stripColor(boots.getDisplayName()).trim();

            ItemData helmData = null;
            ItemData chestData = null;
            ItemData legData = null;
            ItemData bootData = null;

            if(!hName.startsWith("Slot ")){
                helmData = parseItemData(helm);
            }
            if(!cName.startsWith("Slot ")){
                chestData = parseItemData(chestplate);
            }
            if(!lName.startsWith("Slot ")){
                legData = parseItemData(leggings);
            }
            if(!bName.startsWith("Slot ")){
                bootData = parseItemData(boots);
            }

            set.put(realSlot, new WardrobeSet(helmData, chestData, legData, bootData,
                    equippedSlot == realSlot));
        }
        return set;
    }
    public static SlayersData parseSlayer(ContainerChest container) {
        if (container == null) return null;
        String title = ColorUtils.stripColor(
                container.getLowerChestInventory().getDisplayName().getUnformattedText()
        ).trim();
        if (!title.equals("View Slayers")) return null;
        EnumMap<Slayer, SlayerData> slayers = new EnumMap<>(Slayer.class);
        for(Slayer slayer : Slayer.values()){
            JefMod.logger.info("[Slayer1] " + slayer);
            ItemStack stack = container.getSlot(slayer.itemSlot).getStack();
            if(stack == null) { JefMod.logger.info("[Slayer] Null stack at slot " + slayer.itemSlot + " for " + slayer + " — skipping"); continue; }
            if(!ColorUtils.stripColor(stack.getDisplayName()).equals(slayer.itemName)) {
                JefMod.logger.info("[Slayer] Name mismatch for " + slayer + " at slot " + slayer.itemSlot + " — got: " + ColorUtils.stripColor(stack.getDisplayName()) + ", expected: " + slayer.itemName + " — skipping");
                continue;
            }
            List<String> lore = getLore(stack);

            int curLevel = -1,t1Kills = -1,t2Kills = -1,t3Kills = -1,t4Kills = -1,t5Kills = -1;
            long curExp = -1,reqExp = -1;
            HashMap<String,Integer> drops = new HashMap<>();
            int dropIndex = -1;
            try {
                for (String s : lore) {
                    if (s.startsWith("Level:")) {
                        curLevel = Integer.parseInt(s.split(":")[1].trim());
                    }
                    if(s.endsWith("XP") && s.contains("/")){
                        String[] words = s.split(" ");
                        String[] exp = words[0].split("/");
                        curExp = parseRawNumber(exp[0].trim());
                        reqExp = parseRawNumber(exp[1].trim());
                    }
                    if(s.startsWith("T1 Kills:")){
                        t1Kills = (int)parseRawNumber(s.split(":")[1].trim());
                    }
                    if(s.startsWith("T2 Kills:")){
                        t2Kills = (int)parseRawNumber(s.split(":")[1].trim());
                    }
                    if(s.startsWith("T3 Kills:")){
                        t3Kills = (int)parseRawNumber(s.split(":")[1].trim());
                    }
                    if(s.startsWith("T4 Kills:")){
                        t4Kills = (int)parseRawNumber(s.split(":")[1].trim());
                    }
                    if (s.startsWith("T5 Kills:")){
                        t5Kills = (int)parseRawNumber(s.split(":")[1].trim());
                    }
                    if(s.startsWith("Rare Drops:")){
                        dropIndex = lore.indexOf(s) + 1;
                    }
                }
                for(int i = dropIndex;i < lore.size();i++){
                    String line = lore.get(i);
                    if(!line.contains(":")) break;
                    String[] words = line.split(":");
                    drops.put(words[0].trim(),(int)parseRawNumber(words[1].trim()));
                }
                if(curLevel < 0) { JefMod.logger.info("[Slayer] No level found for " + slayer + " — skipping"); continue; }
                JefMod.logger.info("[Slayer2] " + slayer + " | " + slayers.size());
                slayers.put(slayer,new SlayerData(
                        curLevel,curExp,reqExp,t1Kills,t2Kills,t3Kills,t4Kills,t5Kills,drops
                ));
                JefMod.logger.info("[Slayer3] " + slayer + " | " + slayers.size());
            }catch (NumberFormatException e){
                JefMod.logger.info(e.getMessage());
            }
        }
        return new SlayersData(slayers);
    }
    public static DungeonData parseDungeon(ContainerChest container){
        if (container == null) return null;
        String title = ColorUtils.stripColor(
                container.getLowerChestInventory().getDisplayName().getUnformattedText()
        ).trim();
        if (!title.equals("View Dungeon Stats")) return null;

        ItemStack skill = container.getSlot(4).getStack();
        if(skill == null) return null;
        String name = ColorUtils.stripColor(skill.getDisplayName()).trim();
        String[] words = name.split(" ");
        int level,hLevel = -1,mLevel = -1,aLevel = -1,bLevel = -1,tLevel = -1;
        long curPorgress = -1,reqProgress = -1;
        level = Integer.parseInt(words[words.length-1]);

        List<String> lore = getLore(skill);
        for(String line : lore){
            if(line.startsWith("Healer")) {
                try {
                    hLevel = RomanNumeralParser.parse(line.split(" ")[1].trim());
                }catch (IllegalArgumentException e){
                    hLevel = 0;
                }
            }
            if(line.startsWith("Mage")){
                try {
                    mLevel = RomanNumeralParser.parse(line.split(" ")[1].trim());
                }catch (IllegalArgumentException e){
                    mLevel = 0;
                }
            }
            if(line.startsWith("Archer")) {
                try {
                    aLevel = RomanNumeralParser.parse(line.split(" ")[1].trim());
                }catch (IllegalArgumentException e){
                    aLevel = 0;
                }
            }
            if(line.startsWith("Berserk")) {
                try {
                    bLevel = RomanNumeralParser.parse(line.split(" ")[1].trim());
                }catch (IllegalArgumentException e){
                    bLevel = 0;
                }
            }
            if(line.startsWith("Tank")) {
                try {
                    tLevel = RomanNumeralParser.parse(line.split(" ")[1].trim());
                }catch (IllegalArgumentException e){
                    tLevel = 0;
                }
            }
            if (line.contains("/") && !line.contains(" ")) {
                try {
                    String[] parts = line.split("/");
                    curPorgress = parseRawNumber(parts[0]);
                    reqProgress = parseRawNumber(parts[1]);
                } catch (Exception ignored) {
                }
            }
        }
        if(level < 0 || hLevel < 0 || mLevel < 0 || aLevel < 0 || bLevel < 0 || tLevel < 0)return null;

        EnumMap<Floor, FloorData> floorData = new EnumMap<>(Floor.class);
        int[] floorStats = new int[]{19,20,21,22,23,24,25,31};
        for(int i : floorStats){
            FloorData fData = parseFloor(container.getSlot(i).getStack());
            if(fData == null) continue;
            floorData.put(fData.floor,fData);
        }
        if(floorData.isEmpty()) return null;

        return new DungeonData(level,hLevel,mLevel,aLevel,bLevel,tLevel,curPorgress,reqProgress,floorData);
    }

    public static FloorData parseFloor(ItemStack stack){
        if(stack == null)return null;

        Floor floor = Floor.getFloor(ColorUtils.stripColor(stack.getDisplayName()));
        if(floor == null) return null;

        int bossKills = -1,bestScore = -1,totalEnemiesKilled = -1,mostEnemiesKilled = -1;
        long mHDamage = -1,mMDamage = -1,mADamage = -1,mBDamage = -1,mTDamage = -1,mLDamage = -1;
        long fastestTime = -1,fastestSTime = -1,fastestSPlusTime = -1;

        List<String> lore = getLore(stack);
        try {
            for (String s : lore) {
                if (s.startsWith(floor.bossName + " Kills:")) {
                    bossKills = Integer.parseInt(s.split(":")[1].trim());
                }
                if (s.startsWith("Best Score:")) {
                    bestScore = Integer.parseInt(s.split(":")[1].trim());
                }
                if (s.startsWith("Total Enemies Killed:")) {
                    totalEnemiesKilled = Integer.parseInt(s.split(":")[1].replace(",","").trim());
                }
                if (s.startsWith("Most Enemies Killed:")) {
                    mostEnemiesKilled = Integer.parseInt(s.split(":")[1].replace(",","").trim());
                }
                if (s.startsWith("Fastest Time:")) {
                    fastestTime = parseFinishTimeToSeconds(s.split(":")[1].trim());
                }
                if (s.startsWith("Fastest Time (S):")) {
                    fastestSTime = parseFinishTimeToSeconds(s.split(":")[1].trim());
                }
                if (s.startsWith("Fastest Time (S+):")) {
                    fastestSPlusTime = parseFinishTimeToSeconds(s.split(":")[1].trim());
                }
                if (s.startsWith("Most")) {
                    String amount = s.split(":")[1].trim();
                    if (s.contains("Healer")) {
                        if (!amount.equals("N/A")) {
                            mHDamage = parseRawNumber(amount);
                        }
                    }
                    if (s.contains("Mage")) {
                        if (!amount.equals("N/A")) {
                            mMDamage = parseRawNumber(amount);
                        }
                    }
                    if (s.contains("Archer")) {
                        if (!amount.equals("N/A")) {
                            mADamage = parseRawNumber(amount);
                        }
                    }
                    if (s.contains("Berserk")) {
                        if (!amount.equals("N/A")) {
                            mBDamage = parseRawNumber(amount);
                        }
                    }
                    if (s.contains("Tank")) {
                        if (!amount.equals("N/A")) {
                            mTDamage = parseRawNumber(amount);
                        }
                    }
                    if (s.contains("Ally")) {
                        if (!amount.equals("N/A")) {
                            mLDamage = parseRawNumber(amount);
                        }
                    }
                }
            }
        }catch (NumberFormatException ignored){}
        if(bossKills < 0) return null;
        return new FloorData(floor,bossKills,fastestTime,fastestSTime,fastestSPlusTime,bestScore,mHDamage,mMDamage,mADamage,mBDamage,mTDamage,mLDamage,totalEnemiesKilled,mostEnemiesKilled);
    }
    public static HOTMData parseHOTM(ContainerChest container) {
        if (container == null) return null;
        String title = ColorUtils.stripColor(
                container.getLowerChestInventory().getDisplayName().getUnformattedText()
        ).trim();
        if (!title.equals("View HOTM")) return null;

        ItemStack hotm = container.getSlot(11).getStack();
        if(hotm == null) return null;
        List<String> lore = getLore(hotm);
        if(lore.isEmpty()) return null;
        int tier = -1,tokens = -1,mithril = -1,gemstone = -1,commisions = -1;
        long curProgress = -1, reqProgress = -1;
        for(String s : lore){
            if(s.startsWith("Tier:")){
                String[] words = s.split(" ");
                if(words.length < 2)continue;
                tier = Integer.parseInt(words[1].trim().replace(",",""));
            }
            if(s.startsWith("Total Token of the Mountain: ")){
                String[] words = s.split(":");
                if(words.length < 2)continue;
                tokens = Integer.parseInt(words[1].trim().replace(",",""));
            }
            if(s.startsWith("Total Mithril Powder:")){
                String[] words = s.split(":");
                if (words.length < 2)continue;
                mithril = Integer.parseInt(words[1].trim().replace(",",""));
            }
            if(s.startsWith("Total Gemstone Powder:")){
                String[] words = s.split(":");
                if (words.length < 2)continue;
                gemstone = Integer.parseInt(words[1].trim().replace(",",""));
            }
            if(s.startsWith("Commissions Complete:")){
                String[]  words = s.split(":");
                if (words.length < 2)continue;
                commisions = Integer.parseInt(words[1].trim().replace(",",""));
            }
            if (s.contains("/") && !s.contains(" ")) {
                try {
                    String[] parts = s.split("/");
                    JefMod.logger.info("[ProfileParser] Parsing " + parts[0] + " " + parts[1]);
                    curProgress = parseRawNumber(parts[0]);
                    reqProgress = parseRawNumber(parts[1]);
                } catch (Exception ignored) {
                }
            }
        }
        if(tier < 0 || tokens < 0 || mithril < 0 || gemstone < 0 || commisions < 0){
            JefMod.logger.info("Tier: " + tier + " | Tokens: " + tokens + " | Mithril: " + mithril + " | Gemstone: " + gemstone + " | Comms: " + commisions);
            return null;
        }
        return new HOTMData(tier,tokens,mithril,gemstone,commisions,curProgress,reqProgress);
    }

    public static InventoryData parseInvData(ContainerChest container) {
        if (container == null) return null;
        String title = ColorUtils.stripColor(
                container.getLowerChestInventory().getDisplayName().getUnformattedText()
        ).trim();
        if (!title.equals("View Inventory")) return null;

        HashMap<EquipmentSlot, ItemData> armorData = new HashMap<>();
        HashMap<Integer, ItemData>       invData   = new HashMap<>();

        ItemStack helmet     = container.getSlot(2).getStack();
        ItemStack chestplate = container.getSlot(3).getStack();
        ItemStack leggings   = container.getSlot(4).getStack();
        ItemStack boots      = container.getSlot(5).getStack();
        ItemStack necklace   = container.getSlot(11).getStack();
        ItemStack cloak      = container.getSlot(12).getStack();
        ItemStack belt       = container.getSlot(13).getStack();
        ItemStack gloves     = container.getSlot(14).getStack();

        if (helmet     != null) armorData.put(EquipmentSlot.HELMET,     parseItemData(helmet));
        if (chestplate != null) armorData.put(EquipmentSlot.CHESTPLATE, parseItemData(chestplate));
        if (leggings   != null) armorData.put(EquipmentSlot.LEGGINGS,   parseItemData(leggings));
        if (boots      != null) armorData.put(EquipmentSlot.BOOTS,      parseItemData(boots));

        if (necklace != null && !ColorUtils.stripColor(necklace.getDisplayName()).equals("Necklace"))
            armorData.put(EquipmentSlot.NECKLACE, parseItemData(necklace));
        if (cloak    != null && !ColorUtils.stripColor(cloak.getDisplayName()).equals("Cloak"))
            armorData.put(EquipmentSlot.CLOAK,    parseItemData(cloak));
        if (belt     != null && !ColorUtils.stripColor(belt.getDisplayName()).equals("Belt"))
            armorData.put(EquipmentSlot.BELT,     parseItemData(belt));
        if (gloves   != null && !ColorUtils.stripColor(gloves.getDisplayName()).equals("Gloves"))
            armorData.put(EquipmentSlot.GLOVES,   parseItemData(gloves));

        for (int i = 0; i < 36; i++) {
            ItemStack stack = container.getSlot(i + 18).getStack();
            if (stack != null) invData.put(i, parseItemData(stack));
        }

        return new InventoryData(armorData, invData);
    }

    public static ItemData parseItemData(ItemStack stack) {
        if (stack == null) return null;
        String displayName = stack.getDisplayName();
        String skyblockID = ItemUtils.getInternalName(stack);
        List<String> lore = getLoreColored(stack);
        return new ItemData(displayName, lore, skyblockID);
    }


    public static ItemStack itemFromNBT(String data) {
        try {
            return ItemStack.loadItemStackFromNBT(JsonToNBT.getTagFromJson(data));
        } catch (NBTException e) {
            JefMod.logger.info("Error parsing item from NBT: " + data);
            return null;
        }
    }

    public static void parseName(ItemStack stack) {
        if (stack == null) return;
        for (String line : getLore(stack)) {
            if (line.startsWith("Profile:")) {
                String[] words = line.split(" ");
                if (words.length >= 2) {
                    lastCachedProfile = words[1];
                    JefMod.logger.info("Profile: " + lastCachedProfile);
                }
            }
        }
    }


    public static BaseData parseBasicInfo(Container container) {
        ItemStack basicInfo = container.getSlot(4).getStack();
        if (basicInfo == null) return null;

        List<String> lore = getLore(basicInfo);
        if (lore.isEmpty()) return null;

        String playerName = ColorUtils.stripColor(basicInfo.getDisplayName()).split("'")[0];
        int level = -1;
        long profileAge = 0L;
        ProfileMode mode = ProfileMode.NORMAL;
        long purse = 0L, bank = 0L;
        int bits = 0;
        long totalNetworth = 0L, itemNetworth = 0L, armorNetworth = 0L,
                petNetworth = 0L, accessoriesNetworth = 0L;
        long playtime = 0L;
        int kills = 0, deaths = 0;
        long highCrit = 0L;

        for (String raw : lore) {
            String line = raw.trim();
            try {
                if (line.startsWith("SkyBlock Level:")) {
                    int open = line.indexOf('['), close = line.indexOf(']');
                    if (open != -1 && close != -1)
                        level = Integer.parseInt(line.substring(open + 1, close).trim());
                } else if (line.startsWith("Profile Age:")) {
                    profileAge = parseTimeToSeconds(line.substring("Profile Age:".length()).trim());
                } else if (line.startsWith("Profile Mode:")) {
                    mode = line.substring("Profile Mode:".length()).trim().equalsIgnoreCase("Ironman")
                            ? ProfileMode.IRONMAN : ProfileMode.NORMAL;
                } else if (line.startsWith("Purse:")) {
                    purse = parseCoins(line.substring("Purse:".length()));
                } else if (line.startsWith("Bank:")) {
                    String bankPart = line.substring("Bank:".length()).trim();
                    int slash = bankPart.indexOf('/');
                    bank = parseCoins(slash != -1 ? bankPart.substring(0, slash) : bankPart);
                } else if (line.startsWith("Bits:")) {
                    bits = (int) parseLongNumber(line.substring("Bits:".length()).replace("Bits", ""));
                } else if (line.startsWith("Estimate Networth:")) {
                    totalNetworth = parseCoins(line.substring("Estimate Networth:".length()));
                } else if (line.startsWith("Items:")) {
                    itemNetworth = parseCoins(line.substring("Items:".length()));
                } else if (line.startsWith("Armor:")) {
                    armorNetworth = parseCoins(line.substring("Armor:".length()));
                } else if (line.startsWith("Pets:")) {
                    petNetworth = parseCoins(line.substring("Pets:".length()));
                } else if (line.startsWith("Accessories:")) {
                    accessoriesNetworth = parseCoins(line.substring("Accessories:".length()));
                } else if (line.startsWith("Playtime:")) {
                    playtime = parseTimeToSeconds(line.substring("Playtime:".length()).trim());
                } else if (line.startsWith("Kills:")) {
                    kills = (int) parseLongNumber(line.substring("Kills:".length()));
                } else if (line.startsWith("Deaths:")) {
                    deaths = (int) parseLongNumber(line.substring("Deaths:".length()));
                } else if (line.startsWith("Highest Critical Damage:")) {
                    highCrit = parseLongNumber(line.substring("Highest Critical Damage:".length()));
                }
            } catch (Exception ignored) {}
        }

        if (level == -1) return null;

        return new BaseData(
                playerName, "",level, profileAge, mode,
                (int) purse, (int) bank, bits,
                new NetworthData((int) totalNetworth, (int) itemNetworth,
                        (int) armorNetworth, (int) petNetworth, (int) accessoriesNetworth),
                new Statistics(playtime, kills, deaths, (int) highCrit)
        );
    }

    private static List<String> getLore(ItemStack stack) {
        List<String> lore = new ArrayList<>();
        if (stack == null || !stack.hasTagCompound()) return lore;
        NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
        if (!display.hasKey("Lore", 9)) return lore;
        NBTTagList loreList = display.getTagList("Lore", 8);
        for (int i = 0; i < loreList.tagCount(); i++)
            lore.add(ColorUtils.stripColor(loreList.getStringTagAt(i)).trim());
        return lore;
    }

    private static List<String> getLoreColored(ItemStack stack) {
        List<String> lore = new ArrayList<>();
        if (stack == null || !stack.hasTagCompound()) return lore;
        NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
        if (!display.hasKey("Lore", 9)) return lore;
        NBTTagList loreList = display.getTagList("Lore", 8);
        for (int i = 0; i < loreList.tagCount(); i++)
            lore.add(loreList.getStringTagAt(i));
        return lore;
    }

    private static long parseCoins(String raw) {
        String cleaned = raw.trim().replace("Coins", "").replace(",", "").trim();
        int dot = cleaned.indexOf('.');
        if (dot != -1) cleaned = cleaned.substring(0, dot);
        return cleaned.isEmpty() ? 0L : Long.parseLong(cleaned);
    }

    private static long parseLongNumber(String raw) {
        String cleaned = raw.trim().replace(",", "").trim();
        int dot = cleaned.indexOf('.');
        if (dot != -1) cleaned = cleaned.substring(0, dot);
        cleaned = cleaned.split("\\s+")[0];
        return cleaned.isEmpty() ? 0L : Long.parseLong(cleaned);
    }

    private static long parseTimeToSeconds(String raw) {
        long total = 0L;
        String[][] units = {
                {"year",   String.valueOf(365L * 24 * 60 * 60)},
                {"day",    String.valueOf(24L  * 60 * 60)},
                {"hour",   String.valueOf(60L  * 60)},
                {"minute", String.valueOf(60L)},
                {"second", String.valueOf(1L)},
        };
        String text = raw.toLowerCase().replace(",", "").trim();
        for (String[] unit : units) {
            int idx = text.indexOf(unit[0]);
            if (idx == -1) continue;
            String[] tokens = text.substring(0, idx).trim().split("\\s+");
            if (tokens.length == 0) continue;
            try { total += Long.parseLong(tokens[tokens.length - 1]) * Long.parseLong(unit[1]); }
            catch (NumberFormatException ignored) {}
        }
        return total;
    }

    private static long parseFinishTimeToSeconds(String raw){
        long total = 0L;
        String[][] units = {
                {"y",   String.valueOf(365L * 24 * 60 * 60)},
                {"d",    String.valueOf(24L  * 60 * 60)},
                {"h",   String.valueOf(60L  * 60)},
                {"m", String.valueOf(60L)},
                {"s", String.valueOf(1L)},
        };
        String text = raw.toLowerCase().replace(",","").trim();
        for(String[] unit : units){
            int idx = text.indexOf(unit[0]);
            if (idx == -1) continue;
            String[] tokens = text.substring(0,idx).trim().split("\\s+");
            if(tokens.length == 0) continue;
            try { total += Long.parseLong(tokens[tokens.length - 1]) * Long.parseLong(unit[1]); }
            catch (NumberFormatException ignored) {}
        }
        return total;
    }



    public static SkillsData parseSkills(ContainerChest container) {
        EnumMap<Skill, SkillData> result = new EnumMap<>(Skill.class);
        if (container == null) return null;

        String title = ColorUtils.stripColor(
                container.getLowerChestInventory().getDisplayName().getUnformattedText()
        ).trim();
        if (!title.equals("View Skills")) return null;

        int[] slots = {19,20,21,22,23,24,25,30,31,32};

        for (int slot : slots) {
            ItemStack stack = container.getSlot(slot).getStack();
            if (stack == null) continue;

            Skill skill = Skill.get(ColorUtils.stripColor(stack.getDisplayName().split(" ")[0]));
            if (skill == null) continue;
            List<String> lore = getLore(stack);
            if (lore.isEmpty()) continue;

            int currentLevel = -1;
            long currentXp = 0L;
            long requiredXp = -1L;

            for (String line : lore) {
                if (line.startsWith("Progress to Level")) {
                    try {
                        String afterLevel = line.substring(line.indexOf("Level ") + 6);
                        String levelStr = afterLevel.substring(0, afterLevel.indexOf(':')).trim();
                        int nextLevel = Integer.parseInt(levelStr);
                        currentLevel = nextLevel - 1;
                    } catch (Exception ignored) {
                    }
                }

                if (line.contains("/") && !line.contains(" ")) {
                    try {
                        String[] parts = line.split("/");
                        currentXp = parseRawNumber(parts[0]);
                        requiredXp = parseRawNumber(parts[1]);
                    } catch (Exception ignored) {
                    }
                }
            }

            if (currentLevel == -1) {
                for (String line : lore) {
                    if (line.startsWith("Level ")) {
                        try {
                            currentLevel = Integer.parseInt(line.substring(6).trim());
                        } catch (Exception ignored) {
                        }
                        break;
                    }
                }
            }

            if (currentLevel < 0) continue;

            result.put(skill, new SkillData(skill, currentLevel, currentXp, requiredXp));
        }
        if(result.isEmpty()) return null;
        return new SkillsData(result);
    }

    private static long parseRawNumber(String raw) {
        String s = raw.trim().replace(",", "");
        if (s.isEmpty()) return 0L;
        char suffix = Character.toUpperCase(s.charAt(s.length() - 1));
        long multiplier = 1L;
        if (suffix == 'K') { multiplier = 1_000L;         s = s.substring(0, s.length() - 1); }
        else if (suffix == 'M') { multiplier = 1_000_000L;     s = s.substring(0, s.length() - 1); }
        else if (suffix == 'B') { multiplier = 1_000_000_000L; s = s.substring(0, s.length() - 1); }
        return (long) (Double.parseDouble(s) * multiplier);
    }

    public static void writeToJson(ProfileData data) {
        if (data == null) return;
        File file = new File(JefConfig.configDirectory, "profile.json");
        if (!file.exists()) {
            try { file.createNewFile(); }
            catch (IOException e) { JefMod.logger.info("Error creating profile.json"); return; }
        }
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
            writer.flush();
        } catch (IOException e) {
            JefMod.logger.info("Error writing to profile.json");
        }
    }
}