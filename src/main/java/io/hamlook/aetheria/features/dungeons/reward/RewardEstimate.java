package io.hamlook.aetheria.features.dungeons.reward;

import java.util.Collections;
import java.util.List;

public class RewardEstimate {

    /**
     * The price of opening the chest. A negative value indicates price could not be determined.
     */
    public final long price;
    /**
     * List of rewards contained in the chest. The list is made immutable to prevent accidental modification.
     */
    public final List<DungeonReward> rewards;
    /**
     * Identifier of the chest type (e.g., "wood", "gold").
     */
    public final String chestID;
    /**
     * Cached profit value calculated from {@code price} and {@code rewards}.
     */
    public final double profit;

    /**
     * Constructs a new {@code RewardEstimate} and pre‑computes the profit.
     */
    public RewardEstimate(long price, List<DungeonReward> rewards, String chestID) {
        this.price = price;
        // Ensure the rewards list cannot be modified after construction.
        this.rewards = Collections.unmodifiableList(rewards);
        this.chestID = chestID;
        double p = - (double) price;
        for (DungeonReward reward : rewards) {
            p += reward.price;
        }
        this.profit = p;
    }

    /**
     * Returns the pre‑computed profit.
     */
    public double getProfit() {
        return profit;
    }

}
