package io.hamlook.aetheria.utils.item;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class NBTFormatter {

    private NBTFormatter() {
    }

    public static String format(NBTBase nbt) {
        return format(nbt, 0);
    }

    private static String format(NBTBase nbt, int indent) {
        StringBuilder builder = new StringBuilder();
        String spaces = new String(new char[indent]).replace("\0", "  ");

        if (nbt instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) nbt;
            builder.append("{\n");
            for (String key : compound.getKeySet()) {
                NBTBase tag = compound.getTag(key);
                builder.append(spaces).append("  \"").append(key).append("\": ");
                builder.append(format(tag, indent + 1)).append(",\n");
            }
            builder.append(spaces).append("}");
        } else if (nbt instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) nbt;
            builder.append("[\n");
            for (int i = 0; i < list.tagCount(); i++) {
                builder.append(spaces).append("  ");
                builder.append(format(list.get(i), indent + 1)).append(",\n");
            }
            builder.append(spaces).append("]");
        } else {
            builder.append(nbt.toString());
        }

        return builder.toString().replaceAll(",\n\\s*([]}])", "\n$1");
    }
}
