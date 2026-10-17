package io.hamlook.aetheria.features.misc.itemList;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import io.hamlook.aetheria.utils.compat.BlockCompat;
import io.hamlook.aetheria.utils.compat.ItemCompatKt;
import io.hamlook.aetheria.utils.compat.NbtCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
//#if MC < 1.16
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagString;
//#else
//$$ import net.minecraft.item.Items;
//$$ import net.minecraft.item.Item;
//$$ import net.minecraft.nbt.NbtString;
//$$ import net.minecraft.registry.Registries;
//$$ import net.minecraft.util.Identifier;
//#endif
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.List;

public class SkyblockItem {

    public transient String skyblockID;

    @SerializedName("name")
    public String displayName;

    public String itemid;
    public int damage;
    public String texture;

    @SerializedName("lore")
    public List<String> baseLore;

    public String rarity;
    public String color;

    public List<JsonObject> recipes;
    public List<String> info;
    public String infoType;
    public String clickcommand;

    public transient boolean enchanted;
    public transient ItemStack itemStack;
    public transient String familyId;
    public transient String familyMemberLabel;
    public transient int amount = 1;

    // Filter and search cache tags
    public transient String itemType = "";
    public transient String itemRarity = "";
    public transient String cleanNameLower = "";
    public transient String idLower = "";

    public SkyblockItem clone() {
        SkyblockItem clone = new SkyblockItem();
        clone.skyblockID = this.skyblockID;
        clone.displayName = this.displayName;
        clone.itemid = this.itemid;
        clone.damage = this.damage;
        clone.texture = this.texture;
        clone.baseLore = this.baseLore;
        clone.rarity = this.rarity;
        clone.enchanted = this.enchanted;
        clone.itemStack = this.itemStack;
        clone.itemType = this.itemType;
        clone.itemRarity = this.itemRarity;
        clone.cleanNameLower = this.cleanNameLower;
        clone.idLower = this.idLower;
        clone.color = this.color;
        return clone;
    }

    public ItemStack getStack() {
        if (this.itemStack != null) return this.itemStack;
        if (this.itemid == null) return null;

        if (this.itemid.equals("minecraft:player_head") || this.itemid.equals("minecraft:skull")) {
            if(this.damage == 3) {
                if (this.texture != null && !this.texture.isEmpty()) {
                    this.itemStack = ItemUtils.createSkullWithTexture(this.texture);
                } else {
                    //#if MC < 1.16
                    this.itemStack = new ItemStack(Items.skull, 1, 3);
                    //#else
                    //$$ this.itemStack = new ItemStack(Items.PLAYER_HEAD, 1);
                    //#endif
                }
            }else {
                //#if MC < 1.16
                Item mcItem = Item.getByNameOrId(this.itemid);
                //#else
                //$$ Item mcItem = Registries.ITEM.getOrEmpty(Identifier.tryParse(this.itemid)).orElse(null);
                //#endif
                if (mcItem != null) {
                    //#if MC < 1.16
                    this.itemStack = new ItemStack(mcItem, 1, this.damage);
                    //#else
                    //$$ this.itemStack = new ItemStack(mcItem, 1);
                    //#endif
                } else {
                    this.itemStack = BlockCompat.getStoneItem();
                }
            }
        } else {
            //#if MC < 1.16
            Item mcItem = Item.getByNameOrId(this.itemid);
            //#else
            //$$ Item mcItem = Registries.ITEM.getOrEmpty(Identifier.tryParse(this.itemid)).orElse(null);
            //#endif
            if (mcItem != null) {
                //#if MC < 1.16
                this.itemStack = new ItemStack(mcItem, 1, this.damage);
                //#else
                //$$ this.itemStack = new ItemStack(mcItem, 1);
                //#endif
            } else {
                this.itemStack = BlockCompat.getStoneItem();
            }
        }
        //#if MC < 1.16
        this.itemStack.stackSize = this.amount > 0 ? this.amount : 1;
        //#else
        //$$ this.itemStack.setCount(this.amount > 0 ? this.amount : 1);
        //#endif
        NBTTagCompound tag = ItemUtils.getOrCreateTag(this.itemStack);
        NBTTagCompound display = NbtCompat.createCompound();
        if (this.displayName != null) {
            NbtCompat.setString(display, "Name", this.displayName);
        }
        if (this.baseLore != null && !this.baseLore.isEmpty()) {
            NBTTagList loreList = NbtCompat.createList();
            for (String line : this.baseLore) {
                //#if MC < 1.16
                loreList.appendTag(new NBTTagString(line));
                //#else
                //$$ loreList.add(NbtString.of(line));
                //#endif
            }
            NbtCompat.setTagList(display, "Lore", loreList);
        }
        NbtCompat.setTag(tag, "display", display);

        NBTTagCompound extra = NbtCompat.createCompound();
        if (this.skyblockID != null) {
            NbtCompat.setString(extra, "id", this.skyblockID);
        }
        NbtCompat.setTag(tag, "ExtraAttributes", extra);

        if (this.color != null && !this.color.isEmpty()) {
            try {
                int rgb = Integer.parseInt(this.color);
                if ("minecraft:potion".equals(this.itemid)) {
                    NbtCompat.setInt(tag, "CustomPotionColor", rgb);
                } else {
                    NbtCompat.setInt(display, "color", rgb);
                }
            } catch (NumberFormatException ignored) {}
        }

        if (this.enchanted) {
            NbtCompat.setTagList(tag, "ench", NbtCompat.createList());
        }
        NbtCompat.setTagCompound(this.itemStack, tag);

        return this.itemStack;
    }
}