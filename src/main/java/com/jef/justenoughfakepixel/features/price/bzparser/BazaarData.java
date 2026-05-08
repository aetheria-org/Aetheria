package com.jef.justenoughfakepixel.features.price.bzparser;

import com.jef.justenoughfakepixel.features.price.bzparser.data.BazaarCategory;
import com.jef.justenoughfakepixel.features.price.bzparser.data.BazaarItem;
import lombok.AllArgsConstructor;

import java.util.EnumMap;
import java.util.List;

@AllArgsConstructor
public class BazaarData {

    public EnumMap<BazaarCategory, List<BazaarItem>> itemMap;

}
