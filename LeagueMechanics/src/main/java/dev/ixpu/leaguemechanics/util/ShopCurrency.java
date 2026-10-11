package dev.ixpu.leaguemechanics.util;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class ShopCurrency {
    private static Economy economy;

    private ShopCurrency() {}

    private static Economy economy() {
        if (economy == null && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (provider != null) {
                economy = provider.getProvider();
            }
        }
        return economy;
    }

    public static boolean isVault() {
        return economy() != null;
    }

    public static boolean has(Player player, int amount) {
        Economy econ = economy();
        if (econ != null) {
            return econ.has(player, amount);
        }
        return player.getLevel() >= amount;
    }

    public static boolean withdraw(Player player, int amount) {
        Economy econ = economy();
        if (econ != null) {
            return econ.withdrawPlayer(player, amount).transactionSuccess();
        }
        if (player.getLevel() < amount) return false;
        player.setLevel(player.getLevel() - amount);
        return true;
    }

    public static void deposit(Player player, int amount) {
        Economy econ = economy();
        if (econ != null) {
            econ.depositPlayer(player, amount);
            return;
        }
        player.setLevel(player.getLevel() + amount);
    }
}