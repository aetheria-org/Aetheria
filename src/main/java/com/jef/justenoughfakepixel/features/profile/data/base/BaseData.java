package com.jef.justenoughfakepixel.features.profile.data.base;

import com.jef.justenoughfakepixel.features.profile.vars.ProfileMode;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BaseData {

    public String playerName,playerProfile;
    public int currentLevel;
    public long profileAge;
    public ProfileMode currentMode;
    public int currentPurse;
    public int bankBalance;
    public int bitCount;

    public NetworthData networth;
    public Statistics stats;

}
