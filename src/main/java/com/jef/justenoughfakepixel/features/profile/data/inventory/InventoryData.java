package com.jef.justenoughfakepixel.features.profile.data.inventory;

import com.jef.justenoughfakepixel.features.profile.data.ItemData;
import com.jef.justenoughfakepixel.features.profile.vars.EquipmentSlot;
import lombok.AllArgsConstructor;

import java.util.HashMap;

@AllArgsConstructor
public class InventoryData {

    public HashMap<EquipmentSlot, ItemData> armorData;
    public HashMap<Integer, ItemData> invData;

}
