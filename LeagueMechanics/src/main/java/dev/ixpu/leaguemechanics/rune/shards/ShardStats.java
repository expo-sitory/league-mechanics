package dev.ixpu.leaguemechanics.rune.shards;

import dev.ixpu.leaguemechanics.rune.RuneShard;
import org.bukkit.entity.Player;
import dev.ixpu.leaguemechanics.player.PlayerStats;

public class ShardStats {
    private RuneShard row1;
    private RuneShard row2;
    private RuneShard row3;

    public ShardStats() {
    }

    public void selectShards(RuneShard r1, RuneShard r2, RuneShard r3) {
        this.row1 = r1;
        this.row2 = r2;
        this.row3 = r3;
    }

    public RuneShard getRow(int rowNumber) {
        if (rowNumber == 1) return row1;
        if (rowNumber == 2) return row2;
        if (rowNumber == 3) return row3;
        return null;
    }

    public RuneShard getRow1() { return row1; }
    public RuneShard getRow2() { return row2; }
    public RuneShard getRow3() { return row3; }

    public double getAdOrAp(Player player) {
        if (row1 == RuneShard.ROW1_ADAP || row2 == RuneShard.ROW2_ADAP) {
            double af = PlayerStats.getOrCreate(player).getPlayerAF(player);
            return af <= 0.7 ? 15.4 : 19.0;
        }
        return 0;
    }

    public int getAttackSpeed() {
        return row1 == RuneShard.ROW1_AS ? 10 : 0;
    }

    public int getCooldownHaste() {
        return row1 == RuneShard.ROW1_CH ? 8 : 0;
    }

    public int getMovementSpeed() {
        return row2 == RuneShard.ROW2_MS ? 5 : 0;
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