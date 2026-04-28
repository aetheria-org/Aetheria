package com.jef.justenoughfakepixel.features.profile.data.storage;

import com.jef.justenoughfakepixel.features.profile.data.ItemData;
import lombok.AllArgsConstructor;

import java.util.HashMap;

@AllArgsConstructor
public class ContainerData {

    public String containerID;
    public HashMap<Integer, ItemData> data;

}
