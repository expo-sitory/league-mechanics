package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.gui.ItemShopGUI;

import org.bukkit.entity.Player;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemShopListener {

    private final LeagueMechanics plugin;
    private final Map<UUID, List<Integer>> pendingTaskIds = new ConcurrentHashMap<>();

    public ItemShopListener(LeagueMechanics plugin) {
        this.plugin = plugin;
    }

    public boolean isShopOpen(Player player) {
        org.bukkit.inventory.InventoryView view = player.getOpenInventory();
        return ItemShopGUI.getInventoryTitle().equals(view.getTitle());
    }


    public void cancelPendingTasks(UUID playerId) {
        List<Integer> taskIds = pendingTaskIds.remove(playerId);
        if (taskIds != null) {
            for (int taskId : taskIds) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
        }
    }
}