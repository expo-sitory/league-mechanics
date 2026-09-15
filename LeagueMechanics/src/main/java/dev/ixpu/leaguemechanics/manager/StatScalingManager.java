package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.entity.player.PlayerStats;

import org.bukkit.entity.Player;

public class StatScalingManager {

    public double calculateScaledValue(Player player, double baseValue, double adPercentageMultiplier, double apPercentageMultiplier) {
        PlayerStats stats = PlayerStats.getOrCreate(player);

        double totalAD = stats.getPlayerAD(player);
        double totalAP = stats.getPlayerAP(player);

        double adBonus = totalAD * (adPercentageMultiplier / 100);
        double apBonus = totalAP * (apPercentageMultiplier / 100);

        return baseValue + adBonus + apBonus;
    }

    public double calculateReverseScaledValue(double baseValue, double reductionPercentage) {
        return baseValue * (1 - (reductionPercentage / 100));
    }
}