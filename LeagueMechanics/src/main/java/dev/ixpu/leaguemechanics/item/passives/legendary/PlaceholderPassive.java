package dev.ixpu.leaguemechanics.item.passives.legendary;

import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlaceholderPassive implements ItemPassive {
    private final String id;

    public PlaceholderPassive(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return "§7§oPassive coming soon.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}
}