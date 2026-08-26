package io.hamlook.aetheria.events;

import io.hamlook.aetheria.features.mining.OreBlock;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class OreMinedEvent extends Event {

    @Nullable
    public final OreBlock originalOre;
    public final Map<OreBlock, Integer> extraBlocks;

    public OreMinedEvent(@Nullable OreBlock originalOre, Map<OreBlock, Integer> extraBlocks) {
        this.originalOre = originalOre;
        this.extraBlocks = Collections.unmodifiableMap(extraBlocks);
    }
}
