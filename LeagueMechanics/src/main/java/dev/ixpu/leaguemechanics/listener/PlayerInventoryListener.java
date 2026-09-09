package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.item.shop.ItemShopData;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import dev.ixpu.leaguemechanics.player.PlayerStats;

import dev.ixpu.leaguemechanics.gui.ClassSelectionGUI;
import dev.ixpu.leaguemechanics.gui.InspectGUI;
import dev.ixpu.leaguemechanics.gui.ItemShopGUI;

import dev.ixpu.leaguemechanics.manager.ItemShopManager;
import dev.ixpu.leaguemechanics.manager.ItemStatsManager;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.kyori.adventure.text.Component;

public class PlayerInventoryListener implements Listener {

    private final LeagueMechanics plugin;
    private final ItemStatsManager itemStatsManager;
    private final PlayerStatsListener playerStatsListener;
    private final ItemShopListener itemShopListener;

    private final Map<UUID, List<Integer>> pendingTaskIds = new ConcurrentHashMap<>();
    private final Set<UUID> pendingStatUpdates = ConcurrentHashMap.newKeySet();

    public PlayerInventoryListener(LeagueMechanics plugin, PlayerStatsListener playerStatsListener) {
        this.plugin = plugin;
        this.itemStatsManager = plugin.getStatsManager();
        this.playerStatsListener = playerStatsListener;
        this.itemShopListener = new ItemShopListener(plugin);
    }

    public void applyPlayerStats(Player player) {
        playerStatsListener.applyPlayerStats(player);
    }

    public void syncItemStatsOnMove(ItemStack cursor, ItemStack currentItem) {
        playerStatsListener.syncItemStatsOnMove(cursor, currentItem);
    }

    public boolean isShopOpen(Player player) {
        return itemShopListener.isShopOpen(player);
    }

    public void cancelPendingTasks(UUID playerId) {
        itemShopListener.cancelPendingTasks(playerId);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (handleGUIClick(event, player)) {
            return;
        }

        if (isLeagueItemTransfer(event)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("§cLeague items cannot be transferred to another inventory"));
            return;
        }

        ItemShopGUI.updateShopDisplay(player);

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (preventBundleInsert(currentItem, cursor)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("§cLeague items cannot be inserted into bundles"));
            return;
        }

        if (!checkItemLimit(event, player, currentItem)) {
            return;
        }

        if (!checkItemGroupRestriction(event, player, cursor)) {
            return;
        }

        syncItemStatsOnMove(cursor, currentItem);

        UUID uuid = player.getUniqueId();
        PlayerStats.invalidateCache(uuid);
        itemStatsManager.invalidateCache(uuid);
        applyPlayerStatsIfNotPending(player);
    }

    public boolean handleGUIClick(InventoryClickEvent event, Player player) {
        String title = event.getView().getTitle();

        if (title.equals(ItemShopGUI.getInventoryTitle())) {
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                event.setCancelled(true);
                int slot = event.getRawSlot();
                if (slot >= 0 && slot < event.getInventory().getSize()) {
                    ItemShopGUI.getInstance().handleClick(player, slot);
                }
                return true;
            }
        }

        if (title.equals(InspectGUI.getInventoryTitle())) {
            event.setCancelled(true);
            return true;
        }

        if (title.equals(ClassSelectionGUI.getInventoryTitle())) {
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                event.setCancelled(true);
                int slot = event.getRawSlot();
                if (slot >= 0 && slot < event.getInventory().getSize()) {
                    ClassSelectionGUI.getInstance().handleClick(player, slot);
                }
                return true;
            }
        }

        return false;
    }

    public void applyPlayerStatsIfNotPending(Player player) {
        UUID uuid = player.getUniqueId();
        if (pendingStatUpdates.add(uuid)) {
            scheduleTask(uuid, () -> {
                try {
                    applyPlayerStats(player);
                } finally {
                    pendingStatUpdates.remove(uuid);
                }
            }, 1L);
        }
    }

    public boolean checkItemLimit(InventoryClickEvent event, Player player, ItemStack currentItem) {
        if (currentItem == null || currentItem.getType().isAir() || event.getClickedInventory() == player.getInventory()) {
            return true;
        }

        String itemId = ItemModifier.getItemId(currentItem);
        if (itemId != null) {
            ItemStatsManager statsManager = plugin.getStatsManager();
            if (statsManager.countLeagueItems(player) > 5) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cLeague Items Count: 6/6"));
                return false;
            }
        }
        return true;
    }

    public boolean checkItemGroupRestriction(InventoryClickEvent event, Player player, ItemStack cursor) {
        if (cursor.getType().isAir() || event.getClickedInventory() == player.getInventory()) {
            return true;
        }

        String cursorItemId = ItemModifier.getItemId(cursor);
        if (cursorItemId == null) {
            return true;
        }

        ItemShopData shopData = ItemShopData.getInstance();
        String itemGroup = shopData.getGroup(cursorItemId);
        if (itemGroup == null) {
            return true;
        }

        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) {
                continue;
            }
            String invItemId = ItemModifier.getItemId(inv);
            if (invItemId == null || invItemId.equals(cursorItemId)) {
                continue;
            }
            String ownerGroup = shopData.getGroup(invItemId);
            if (itemGroup.equals(ownerGroup)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cYou can only apply one (1) " + itemGroup + " item to your build."));
                return false;
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (event.getView().getTitle().equals(ItemShopGUI.getInventoryTitle())) {
                ItemShopGUI.getInstance().cleanup(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (ItemModifier.getItemId(event.getMainHandItem()) != null
                || ItemModifier.getItemId(event.getOffHandItem()) != null) {
            event.setCancelled(true);
            player.sendMessage(Component.text("§cLeague items cannot be swapped between hands"));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getView().getTitle().equals(ItemShopGUI.getInventoryTitle())) {
            return;
        }
        if (event.getView().getTitle().equals(InspectGUI.getInventoryTitle())) {
            event.setCancelled(true);
            return;
        }
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir() || ItemModifier.getItemId(cursor) == null) {
            return;
        }
        org.bukkit.inventory.InventoryView view = event.getView();
        org.bukkit.inventory.Inventory top = view.getTopInventory();
        int topSize = top.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot >= topSize) {
                continue;
            }
            event.setCancelled(true);
            player.sendMessage(Component.text("§cLeague items cannot be transferred to another inventory"));
            return;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryMoveItem(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
        if (ItemModifier.getItemId(event.getItem()) == null) {
            return;
        }
        if (event.getSource() instanceof org.bukkit.inventory.PlayerInventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.BUNDLE) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (ItemModifier.getItemId(mainHand) != null || ItemModifier.getItemId(offHand) != null) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cLeague items cannot be inserted into bundles"));
            }
        }
    }

    @EventHandler
    public void onLightningDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack drop = event.getItemDrop().getItemStack();

        if (!isLeagueItem(drop)) {
            scheduleTask(player.getUniqueId(), () -> applyPlayerStats(player), 1L);
            return;
        }

        boolean shopOpen = isShopOpen(player);

        if (shopOpen) {
            org.bukkit.entity.Item itemEntity = event.getItemDrop();
            UUID playerId = player.getUniqueId();
            scheduleTask(playerId, () -> {
                if (itemEntity.isValid() && !itemEntity.isDead()) {
                    itemEntity.remove();
                }
            });
            ItemShopManager.getInstance().consumeSellXp(player, drop);
            ItemShopGUI.updateShopDisplay(player);
        } else {
            event.setCancelled(true);
            player.sendMessage(Component.text("§cYou cannot sell League items outside the shop"));
        }
    }

    public boolean isLeagueItemTransfer(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return false;
        }
        if (event.getClickedInventory() == null) {
            return false;
        }
        boolean topIsPlayer = event.getView().getTopInventory() == player.getInventory();
        boolean clickedIsPlayer = event.getClickedInventory() == player.getInventory();
        if (!clickedIsPlayer) {
            ItemStack cursor = event.getCursor();
            if (!cursor.getType().isAir() && ItemModifier.getItemId(cursor) != null) {
                return true;
            }
            if (event.getClick().isKeyboardClick() && !topIsPlayer) {
                int hotbar = event.getHotbarButton();
                if (hotbar >= 0) {
                    ItemStack held = player.getInventory().getItem(hotbar);
                    if (held != null && !held.getType().isAir() && ItemModifier.getItemId(held) != null) {
                        return true;
                    }
                }
            }
        }
        if (clickedIsPlayer && !topIsPlayer && event.isShiftClick()) {
            ItemStack current = event.getCurrentItem();
            return current != null && !current.getType().isAir() && ItemModifier.getItemId(current) != null;
        }
        return false;
    }

    public int findItemSlot(Player player, ItemStack target) {
        if (target == null || target.getType().isAir()) {
            return -1;
        }
        String targetId = ItemModifier.getItemId(target);
        if (targetId == null) {
            return -1;
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack inv = contents[i];
            if (inv != null && !inv.getType().isAir() && targetId.equals(ItemModifier.getItemId(inv))) {
                return i;
            }
        }
        return -1;
    }

    public boolean preventBundleInsert(ItemStack currentItem, ItemStack cursor) {
        if (currentItem != null && !currentItem.getType().isAir()) {
            if (currentItem.getType() == Material.BUNDLE) {
                if (cursor != null && !cursor.getType().isAir() && ItemModifier.getItemId(cursor) != null) {
                    return true;
                }
            }
        }
        if (cursor != null && !cursor.getType().isAir() && cursor.getType() == Material.BUNDLE) {
            return currentItem != null && !currentItem.getType().isAir() && ItemModifier.getItemId(currentItem) != null;
        }

        return false;
    }

    public void removeFromHotbar(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        for (int i = 9; i <= 35; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || slot.getType().isAir()) {
                player.getInventory().setItem(i, item);
                return;
            }
        }
    }

    public void scheduleTask(UUID playerId, Runnable task, long delay) {
        int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, task, delay).getTaskId();
        pendingTaskIds.computeIfAbsent(playerId, k -> new ArrayList<>()).add(taskId);
    }

    public void scheduleTask(UUID playerId, Runnable task) {
        int taskId = plugin.getServer().getScheduler().runTask(plugin, task).getTaskId();
        pendingTaskIds.computeIfAbsent(playerId, k -> new ArrayList<>()).add(taskId);
    }

    public boolean isLeagueItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return ItemModifier.getItemId(item) != null;
    }
}