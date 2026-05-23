package io.hamlook.aetheria.features.misc.itemList;

import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonObject;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
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
                    this.itemStack = new ItemStack(Items.skull, 1, 3);
                }
            }else {
                Item mcItem = Item.getByNameOrId(this.itemid);
                if (mcItem != null) {
                    this.itemStack = new ItemStack(mcItem, 1, this.damage);
                } else {
                    this.itemStack = new ItemStack(Blocks.stone);
                }
            }
        } else {
            Item mcItem = Item.getByNameOrId(this.itemid);
            if (mcItem != null) {
                this.itemStack = new ItemStack(mcItem, 1, this.damage);
            } else {
                this.itemStack = new ItemStack(Blocks.stone);
            }
        }

        NBTTagCompound tag = ItemUtils.getOrCreateTag(this.itemStack);
        NBTTagCompound display = new NBTTagCompound();
        if (this.displayName != null) {
            display.setString("Name", this.displayName);
        }
        if (this.baseLore != null && !this.baseLore.isEmpty()) {
            NBTTagList loreList = new NBTTagList();
            for (String line : this.baseLore) {
                loreList.appendTag(new NBTTagString(line));
            }
            display.setTag("Lore", loreList);
        }
        tag.setTag("display", display);

        NBTTagCompound extra = new NBTTagCompound();
        if (this.skyblockID != null) {
            extra.setString("id", this.skyblockID);
        }
        tag.setTag("ExtraAttributes", extra);

        if (this.color != null && !this.color.isEmpty()) {
            try {
                int rgb = Integer.parseInt(this.color);
                if ("minecraft:potion".equals(this.itemid)) {
                    tag.setInteger("CustomPotionColor", rgb);
                } else {
                    display.setInteger("color", rgb);
                }
            } catch (NumberFormatException ignored) {}
        }

        if (this.enchanted) {
            tag.setTag("ench", new NBTTagList());
        }

        if (this.color != null && !this.color.isEmpty()) {
            try {
                int rgb = Integer.parseInt(this.color);
                display.setInteger("color", rgb);
            } catch (NumberFormatException ignored) {}
        }

        return this.itemStack;
    }
}