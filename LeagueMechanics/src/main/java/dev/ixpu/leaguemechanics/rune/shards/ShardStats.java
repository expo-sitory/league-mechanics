package dev.ixpu.leaguemechanics.rune.shards;

import dev.ixpu.leaguemechanics.rune.RuneShard;
import org.bukkit.entity.Player;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;

public class ShardStats {
    private RuneShard row1;
    private RuneShard row2;
    private RuneShard row3;

    public ShardStats() {
        this.row1 = RuneShard.ROW1_ADAP;
        this.row2 = RuneShard.ROW2_ADAP;
        this.row3 = RuneShard.ROW3_HP;
    }

    public void selectShards(RuneShard r1, RuneShard r2, RuneShard r3) {
        this.row1 = r1;
        this.row2 = r2;
        this.row3 = r3;
    }

    public RuneShard getRow1() { return row1; }
    public RuneShard getRow2() { return row2; }
    public RuneShard getRow3() { return row3; }

    public double getAdOrAp(Player player) {
        int adaptiveCount = 0;
        if (row1 == RuneShard.ROW1_ADAP) adaptiveCount++;
        if (row2 == RuneShard.ROW2_ADAP) adaptiveCount++;

        if (adaptiveCount == 0) return 0;

        double af = PlayerStats.getOrCreate(player).getPlayerAF(player);
        double baseBonus = af <= 0.7 ? 15.4 : 19.0;
        return baseBonus * adaptiveCount;
    }

    public int getAttackSpeed() {
        return row1 == RuneShard.ROW1_AS ? 10 : 0;
    }

    public int getCooldownHaste() {
        return row1 == RuneShard.ROW1_CH ? 18 : 0;
    }

    public int getMovementSpeed() {
        return row2 == RuneShard.ROW2_MS ? 15 : 0;
    }

    public int getHealthRegen() {
        int haste = 0;
        if (row2 == RuneShard.ROW2_HR) haste += 2;
        if (row3 == RuneShard.ROW3_HR) haste += 2;
        return haste;
    }

    public int getHealth() {
        return row3 == RuneShard.ROW3_HP ? 20 : 0;
    }

    public int getTenacity() {
        return row3 == RuneShard.ROW3_TN ? 30 : 0;
    }
}