package com.jef.justenoughfakepixel.features.profile.data;

import com.jef.justenoughfakepixel.features.profile.data.bags.BagsData;
import com.jef.justenoughfakepixel.features.profile.data.base.BaseData;
import com.jef.justenoughfakepixel.features.profile.data.collection.CollectionsData;
import com.jef.justenoughfakepixel.features.profile.data.dungeon.DungeonData;
import com.jef.justenoughfakepixel.features.profile.data.inventory.InventoryData;
import com.jef.justenoughfakepixel.features.profile.data.pets.PetsData;
import com.jef.justenoughfakepixel.features.profile.data.skills.SkillsData;
import com.jef.justenoughfakepixel.features.profile.data.slayer.SlayersData;
import com.jef.justenoughfakepixel.features.profile.data.storage.StorageData;
import com.jef.justenoughfakepixel.features.profile.data.wardrobe.WardrobeData;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public class ProfileData {

    public BaseData baseData;
    public InventoryData inventoryData;
    public SkillsData skillData;
    public HOTMData hotmData;
    public DungeonData dungeonData;
    public SlayersData slayersData;
    public WardrobeData wardrobeData;
    public PetsData petsData;
    public StorageData storageData;
    public BagsData bagsData;
    public CollectionsData collectionData;

}
