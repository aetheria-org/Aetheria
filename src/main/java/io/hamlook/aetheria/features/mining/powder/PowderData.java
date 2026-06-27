package io.hamlook.aetheria.features.mining.powder;

import java.util.HashMap;
import java.util.Map;

public class PowderData {

    public int totalChestsPicked = 0;
    public long gemstonePowder = 0L;
    public long diamondEssence = 0L;
    public long goldEssence = 0L;
    public long oilBarrels = 0L;
    public long ascensionRopes = 0L;
    public long wishingCompasses = 0L;
    public long jungleHearts = 0L;
    public long hardStone = 0L;
    public long hardStoneCompacted = 0L;

    public long activeTimeMs = 0L;

    public Map<String, Long> gemstones = new HashMap<>();

    public long goblinEgg = 0L;
    public long greenGoblinEgg = 0L;
    public long redGoblinEgg = 0L;
    public long yellowGoblinEgg = 0L;
    public long blueGoblinEgg = 0L;

    public void reset() {
        totalChestsPicked = 0;
        gemstonePowder = 0L;
        diamondEssence = 0L;
        goldEssence = 0L;
        oilBarrels = 0L;
        ascensionRopes = 0L;
        wishingCompasses = 0L;
        jungleHearts = 0L;
        hardStone = 0L;
        hardStoneCompacted = 0L;
        activeTimeMs = 0L;
        gemstones.clear();
        goblinEgg = greenGoblinEgg = redGoblinEgg = yellowGoblinEgg = blueGoblinEgg = 0L;
    }
}