package io.hamlook.aetheria.features.dungeons.reward;

import java.util.List;

import lombok.Getter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

@Getter
public final class RewardEstimate {

    private final long price;
    private final List<DungeonReward> rewards;
    private final String chestID;

    public RewardEstimate(long price, List<DungeonReward> rewards, String chestID) {
        this.price = price;
        this.rewards = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(rewards, "rewards cannot be null")));
        this.chestID = chestID != null ? chestID : "";
    }

    /**
     * Calculates the net profit of the chest after subtracting its price
     * from the total value of all rewards.
     */
    public double getProfit() {
        double totalRewardValue = rewards.stream().mapToDouble(DungeonReward::getPrice).sum();
        return totalRewardValue - price;
    }

}
