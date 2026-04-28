package com.jef.justenoughfakepixel.features.profile.data.bags.vars;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Bait {

    CARROT("Carrot Bait"),
    MINNOW("Minnow Bait"),
    FISH("Fish Bait"),
    LIGHT("Light Bait"),
    DARK("Dark Bait"),
    SPOOKY("Spooky Bait"),
    SPIKED("Spiked Bait"),
    BLESSED("Blessed Bait"),
    ICE("Ice Bait"),
    WHALE("Whale Bait"),
    SHARK("Shark Bait"),
    CORRUPTED("Corrupted Bait"),
    FROZEN("Frozen Bait"),
    OBF_COMMON(""),
    OBF_UNCOMMON(""),
    ;
    public final String itemName;

    public static Bait getBait(String itemName){
        for(Bait b : values()){
            if(b.itemName.equalsIgnoreCase(itemName)){
                return b;
            }
        }
        return null;
    }
}
