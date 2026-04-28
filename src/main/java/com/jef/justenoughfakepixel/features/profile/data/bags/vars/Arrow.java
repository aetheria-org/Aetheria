package com.jef.justenoughfakepixel.features.profile.data.bags.vars;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Arrow {

    FLINT("Flint Arrow"),
    IRON("Reinforced Iron Arrow"),
    GOLD("Gold-tipped Arrow"),
    REDSTONE("Redstone-tipped Arrow"),
    EMERALD("Emerald-tipped Arrow"),
    BOUNCY("Bouncy Arrow"),
    ICY("Icy Arrow"),
    ARMORSHRED("Armorshred Arrow"),
    EXPLOSIVE("Explosive Arrow"),
    GLUE("Glue Arrow"),
    NANSORB("Nansorb Arrow"),
    MAGMA("Magma Arrow"),
    POISON("Toxic Arrow Poison")
    ;
    public final String itemName;

    public static Arrow getArrow(String name) {
        for(Arrow a : values()){
            if(a.itemName.equalsIgnoreCase(name)){
                return a;
            }
        }
        return null;
    }
}
