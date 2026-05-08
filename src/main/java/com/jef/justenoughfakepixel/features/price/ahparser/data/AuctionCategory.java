package com.jef.justenoughfakepixel.features.price.ahparser.data;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AuctionCategory {

    WEAPON(1),
    ARMOR(11),
    ACCESSORY(13),
    CONSUMABLE(14),
    BLOCKS(12),
    MISC(10),
    ;
    public final int glassDamageValue;

}
