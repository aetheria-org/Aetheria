package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import io.hamlook.aetheria.features.mining.OreBlock;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class OreMinedEvent extends AetheriaEvent {

    @Nullable
    public final OreBlock originalOre;
    public final Map<OreBlock, Integer> extraBlocks;

    public OreMinedEvent(@Nullable OreBlock originalOre, Map<OreBlock, Integer> extraBlocks) {
        this.originalOre = originalOre;
        this.extraBlocks = Collections.unmodifiableMap(extraBlocks);
    }
}
