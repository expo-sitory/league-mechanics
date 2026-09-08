package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.item.shop.ItemShopData;
import dev.ixpu.leaguemechanics.gui.ItemShopGUI;
import dev.ixpu.leaguemechanics.item.shop.ItemShopRegistry;
import dev.ixpu.leaguemechanics.item.ItemStatsData;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemShopManager implements Listener {
    private static ItemShopManager instance;
    public static final String SHOP_INVENTORY_TITLE = "§8§lɪᴛᴇᴍ ꜱʜᴏᴘ";

    private final Set<UUID> processingPurchases = ConcurrentHashMap.newKeySet();

    private ItemShopManager() {
    }

    public static ItemShopManager getInstance() {
        if (instance == null) {
            instance = new ItemShopManager();
        }
        return instance;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShopInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!event.getView().getTitle().equals(SHOP_INVENTORY_TITLE)) {
            return;
        }
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (event.getClickedInventory() == player.getInventory() && event.isShiftClick()
                && currentItem != null && !currentItem.getType().isAir()
                && ItemModifier.getItemId(currentItem) != null) {
            event.setCancelled(true);
            sellItem(player, currentItem, event.getSlot());
            ItemShopGUI.updateShopDisplay(player);
            return;
        }

        if (event.getClickedInventory() == player.getInventory() && currentItem != null && !currentItem.getType().isAir() && ItemModifier.getItemId(currentItem) != null && (event.getClick().isLeftClick() || event.getClick().isRightClick()) && cursor.getType().isAir()) {
            event.setCancelled(true);
            sellItem(player, currentItem, event.getSlot());
            ItemShopGUI.updateShopDisplay(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onShopInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!event.getView().getTitle().equals(SHOP_INVENTORY_TITLE)) {
            return;
        }

        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir() || ItemModifier.getItemId(cursor) == null) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < 54) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public void purchaseFromGUI(Player player, ItemShopRegistry.ShopItem shopItem) {
        UUID playerId = player.getUniqueId();
        if (!processingPurchases.add(playerId)) {
            player.sendMessage(Component.text("§cPurchase already in progress..."));
            return;
        }
        try {
            if (purchaseItem(player, shopItem)) {
                ItemShopGUI.updateShopDisplay(player);
            }
        } finally {
            processingPurchases.remove(playerId);
        }
    }

    private boolean purchaseItem(Player player, ItemShopRegistry.ShopItem shopItem) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        List<String> required = shopItem.getRequiredItems();

        int price = shopItem.getEffectivePrice(player);
        int playerLevel = player.getLevel();
        int currentCount = itemStatsManager.countLeagueItems(player);

        int requiredOwned = 0;
        for (String reqId : required) {
            if (hasLeagueItemInInventory(player, reqId)) {
                requiredOwned++;
            }
        }
        int netChange = 1 - requiredOwned;
        if (currentCount + netChange > 6) {
            player.sendMessage(Component.text("§cLeague Items Count: " + currentCount + "/6"));
            return false;
        }

        if (!hasInventorySpace(player, required)) {
            player.sendMessage(Component.text("§cInventory full. Need space in main inventory to purchase."));
            return false;
        }

        if (playerLevel < price) {
            player.sendMessage(Component.text("§cInsufficient levels."));
            return false;
        }
        if (playerOwnsConflictingItem(player, shopItem)) {
            ItemShopData shopData = ItemShopData.getInstance();
            String group = shopData.getGroup(shopItem.getId());
            player.sendMessage(Component.text("§cYou can only apply one (1) " + group + " item to your build"));
            return false;
        }
        if (playerOwnsItem(player, shopItem)) {
            return false;
        }
        if (!required.isEmpty()) {
            if (!playerHasRequiredItemsOrCanAfford(player, required)) {
                String names = formatRequiredItemNames(player, required);
                player.sendMessage(Component.text("§cRequires: §f" + names));
                return false;
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        player.setLevel(playerLevel - price);
        ItemStack purchasedItem = createPurchaseItem(shopItem);
        consumeRequiredItems(player, required);
        addItemToInventory(player, purchasedItem);

        LeagueMechanics.getInstance().getPlayerEventListener().applyPlayerStats(player);
        return true;
    }

    private boolean playerHasRequiredItemsOrCanAfford(Player player, List<String> requiredIds) {
        ItemShopData shopData = ItemShopData.getInstance();
        int playerLevel = player.getLevel();
        java.util.Map<String, Integer> requiredCounts = new java.util.LinkedHashMap<>();
        for (String id : requiredIds) requiredCounts.merge(id, 1, Integer::sum);
        for (var e : requiredCounts.entrySet()) {
            int owned = 0;
            for (ItemStack inv : player.getInventory().getContents()) {
                if (inv != null && !inv.getType().isAir() && e.getKey().equals(ItemModifier.getItemId(inv))) {
                    owned++;
                }
            }
            if (owned >= e.getValue()) continue;
            if (playerLevel >= shopData.getPrice(e.getKey())) continue;
            return false;
        }
        return true;
    }

    private boolean hasLeagueItemInInventory(Player player, String itemId) {
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv != null && !inv.getType().isAir() && itemId.equals(ItemModifier.getItemId(inv))) {
                return true;
            }
        }
        return false;
    }

    private void consumeRequiredItems(Player player, List<String> requiredIds) {
        java.util.Map<String, Integer> requiredCounts = new java.util.LinkedHashMap<>();
        for (String id : requiredIds) requiredCounts.merge(id, 1, Integer::sum);
        for (var e : requiredCounts.entrySet()) {
            int toConsume = e.getValue();
            for (int i = 0; i < player.getInventory().getSize() && toConsume > 0; i++) {
                ItemStack inv = player.getInventory().getItem(i);
                if (inv == null || inv.getType().isAir()) continue;
                if (!e.getKey().equals(ItemModifier.getItemId(inv))) continue;
                if (inv.getAmount() > toConsume) {
                    inv.setAmount(inv.getAmount() - toConsume);
                    toConsume = 0;
                } else {
                    toConsume -= inv.getAmount();
                    player.getInventory().clear(i);
                }
            }
        }
    }

    private String formatRequiredItemNames(Player player, List<String> requiredIds) {
        ItemStatsData statsData = ItemStatsData.getInstance();
        ItemShopData shopData = ItemShopData.getInstance();
        int playerLevel = player.getLevel();
        java.util.LinkedHashMap<String, Integer> requiredCounts = new java.util.LinkedHashMap<>();
        for (String id : requiredIds) requiredCounts.merge(id, 1, Integer::sum);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var e : requiredCounts.entrySet()) {
            if (!first) sb.append(" §7+ ");
            first = false;
            String id = e.getKey();
            int total = e.getValue();
            int owned = 0;
            for (ItemStack inv : player.getInventory().getContents()) {
                if (inv != null && !inv.getType().isAir() && id.equals(ItemModifier.getItemId(inv))) {
                    owned++;
                }
            }
            String name = statsData != null && statsData.getItem(id) != null
                    ? statsData.getItem(id).getName()
                    : id;

            String color;
            if (owned >= total) {
                color = "§a";
            } else if (playerLevel >= shopData.getPrice(id)) {
                color = "§e";
            } else {
                color = "§c";
            }

            if (total > 1) {
                sb.append(color).append(name).append(" §7(").append(owned).append("/").append(total).append(")");
            } else {
                sb.append(color).append(name);
            }
        }
        return sb.toString();
    }

    private ItemStack createPurchaseItem(ItemShopRegistry.ShopItem shopItem) {
        ItemStack item = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§f" + shopItem.getDisplayName());
            meta.setMaxStackSize(1);
            ItemShopData shopData = ItemShopData.getInstance();
            meta.setRarity(shopData.getRarity(shopItem.getId()));

            item.setItemMeta(meta);
        }
        ItemModifier.setItemId(item, shopItem.getId());
        ItemModifier.syncItemStats(item);

        ItemShopData shopData = ItemShopData.getInstance();
        if (shopData.hasCustomModel(shopItem.getId())) {
            ItemModifier.setItemModel(item, shopData.getModel(shopItem.getId()));
        }

        return item;
    }

    private boolean hasInventorySpace(Player player, List<String> requiredIds) {
        int emptySlots = 0;
        int freedSlots = 0;
        Set<String> requiredSet = new java.util.HashSet<>(requiredIds);

        for (int i = 9; i < 36; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || slot.getType().isAir()) {
                emptySlots++;
            } else if (requiredSet.contains(ItemModifier.getItemId(slot))) {
                freedSlots++;
            }
        }
        return emptySlots + freedSlots >= 1;
    }

    private void addItemToInventory(Player player, ItemStack item) {
        for (int i = 9; i < 36; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || slot.getType().isAir()) {
                player.getInventory().setItem(i, item);
                return;
            }
        }
    }

    private boolean playerOwnsItem(Player player, ItemShopRegistry.ShopItem shopItem) {
        int ownedCount = 0;
        int limit = shopItem.getLimit();

        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv != null && inv.hasItemMeta() && inv.getItemMeta().hasDisplayName()) {
                if (inv.getItemMeta().getDisplayName().contains(shopItem.getDisplayName())) {
                    ownedCount++;
                    if (ownedCount >= limit) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean playerOwnsConflictingItem(Player player, ItemShopRegistry.ShopItem shopItem) {
        ItemShopData shopData = ItemShopData.getInstance();
        String itemGroup = shopData.getGroup(shopItem.getId());

        if (itemGroup == null) {
            return false;
        }

        java.util.Set<String> requiredIds = new java.util.HashSet<>(shopItem.getRequiredItems());

        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv != null && !inv.getType().isAir()) {
                String itemId = ItemModifier.getItemId(inv);
                if (itemId != null
                        && !itemId.equals(shopItem.getId())
                        && !requiredIds.contains(itemId)) {
                    String ownerGroup = shopData.getGroup(itemId);
                    if (ownerGroup != null && ownerGroup.equals(itemGroup)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void processItemSale(Player player, ItemStack item, int slotToRemove) {
        String itemId = ItemModifier.getItemId(item);
        if (itemId == null) return;

        ItemShopRegistry registry = ItemShopRegistry.getInstance();
        ItemShopRegistry.ShopItem shopItem = registry.getShopItem(itemId);
        if (shopItem == null) return;

        int refundAmount = (int) Math.floor(shopItem.getEffectivePrice(player) * 0.70);

        if (slotToRemove >= 0 && slotToRemove < 36) {
            ItemStack inInv = player.getInventory().getItem(slotToRemove);
            if (inInv != null && !inInv.getType().isAir()
                    && itemId.equals(ItemModifier.getItemId(inInv))) {
                if (inInv.getAmount() > 1) {
                    inInv.setAmount(inInv.getAmount() - 1);
                } else {
                    player.getInventory().clear(slotToRemove);
                }
            }
        }

        player.setLevel(player.getLevel() + refundAmount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendMessage(Component.text("§fSold §e" + shopItem.getDisplayName() + " §ffor §a◎" + refundAmount + " §flevels (70%)"));

        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                UUID uuid = player.getUniqueId();
                dev.ixpu.leaguemechanics.player.PlayerStats.invalidateCache(uuid);
                ItemStatsManager manager = plugin.getStatsManager();
                if (manager != null) manager.invalidateCache(uuid);
                plugin.getPlayerEventListener().applyPlayerStats(player);
            }, 1L);
        }
    }

    public void consumeSellXp(Player player, ItemStack item) {
        processItemSale(player, item, -1);
    }

    public void sellItem(Player player, ItemStack item, int knownSlot) {
        processItemSale(player, item, knownSlot);
    }
}