package dev.ixpu.leaguemechanics.gui;

import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import dev.ixpu.leaguemechanics.item.shop.ItemShopData;
import dev.ixpu.leaguemechanics.item.shop.ItemShopRegistry;
import dev.ixpu.leaguemechanics.util.ItemModifier;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import dev.ixpu.leaguemechanics.item.ItemStatsData;

public class ItemShopGUI {
    private static ItemShopGUI instance;
    private static final int INVENTORY_SIZE = 54;
    private static final String INVENTORY_TITLE = "§8§lɪᴛᴇᴍ ꜱʜᴏᴘ";

    private static final int CATEGORY_START = 0;
    private static final int CATEGORY_END = 8;
    private static final int FILLER_START = 9;
    private static final int FILLER_END = 17;
    static final int ITEMS_START = 18;
    private static final int ITEMS_PER_ROW = 9;
    private static final int ITEMS_ROWS = 4;

    private static final String CATEGORY_ALL = "all";
    private static final String CATEGORY_MAIN = "main";
    private static final String CATEGORY_MAGE = "mage";
    private static final String CATEGORY_FIGHTER = "fighter";
    private static final String CATEGORY_TANK = "tank";
    private static final String CATEGORY_MARKSMAN = "marksman";
    private static final String CATEGORY_SUPPORT = "support";

    private final Map<UUID, Integer> playerScrollRow = new HashMap<>();
    private final Map<UUID, String> playerCategory = new HashMap<>();

    public static ItemShopGUI getInstance() {
        if (instance == null) {
            instance = new ItemShopGUI();
        }
        return instance;
    }

    public static void openShop(Player player) {
        getInstance().openShopInstance(player);
    }

    public static void updateShopDisplay(Player player) {
        getInstance().updateShopDisplayInstance(player);
    }

    public static String getInventoryTitle() {
        return INVENTORY_TITLE;
    }

    public void openShopInstance(Player player) {
        Inventory shopInventory = Bukkit.createInventory(null, INVENTORY_SIZE, INVENTORY_TITLE);

        playerCategory.putIfAbsent(player.getUniqueId(), CATEGORY_ALL);
        playerScrollRow.putIfAbsent(player.getUniqueId(), 0);

        buildGUI(shopInventory, player);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        player.openInventory(shopInventory);
    }

    private void buildGUI(Inventory inventory, Player player) {
        String category = playerCategory.getOrDefault(player.getUniqueId(), CATEGORY_ALL);
        int scrollRow = playerScrollRow.getOrDefault(player.getUniqueId(), 0);

        setCategoryButtons(inventory, category);
        setFillers(inventory);
        setItems(inventory, player, category, scrollRow);
    }

    private void setCategoryButtons(Inventory inventory, String activeCategory) {
        int[] categorySlots = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        String[][] categories = {
            {CATEGORY_ALL, "§f§lᴀʟʟ", "§7View all items", "COPPER_CHEST", "WAXED_OXIDIZED_COPPER_CHEST"},
            {CATEGORY_MAIN, "§7★ §lꜱᴛᴀʀᴛᴇʀ", "§7Early Scaling", "COPPER_CHEST", "WAXED_OXIDIZED_COPPER_CHEST"},
            {CATEGORY_FIGHTER, "§6🗡 §lғɪɢʜᴛᴇʀ", "§7Sustained Combat", "COPPER_CHEST", "WAXED_OXIDIZED_COPPER_CHEST"},
            {CATEGORY_MAGE, "§9☄ §lᴍᴀɢᴇ", "§7Magic Ability", "COPPER_CHEST", "WAXED_OXIDIZED_COPPER_CHEST"},
            {CATEGORY_TANK, "§e🛡 §lᴛᴀɴᴋ", "§7Defensive Frontline", "COPPER_CHEST", "WAXED_OXIDIZED_COPPER_CHEST"},
            {CATEGORY_MARKSMAN, "§c🏹 §lᴍᴀʀᴋꜱᴍᴀɴ", "§7Ranged Physical", "COPPER_CHEST", "WAXED_OXIDIZED_COPPER_CHEST"},
            {CATEGORY_SUPPORT, "§a❤ §lꜱᴜᴘᴘᴏʀᴛ", "§7Utilities", "COPPER_CHEST", "WAXED_OXIDIZED_COPPER_CHEST"},
            {"page_prev", "§c§l◀", "§7Previous page", "ARROW", "ARROW"},
            {"page_next", "§a§l▶", "§7Next page", "ARROW", "ARROW"}
        };

        for (int i = 0; i < categories.length; i++) {
            String[] cat = categories[i];
            String categoryId = cat[0];
            String displayName = cat[1];
            String description = cat[2];
            String defaultMaterialName = cat[3];
            String activeMaterialName = cat[4];

            if (categoryId.isEmpty() && displayName.isEmpty()) {
                continue;
            }

            boolean isActive = categoryId.equals(activeCategory);
            boolean isPageButton = categoryId.equals("page_prev") || categoryId.equals("page_next");

            String materialName = (isActive && !isPageButton && !activeMaterialName.isEmpty())
                    ? activeMaterialName
                    : defaultMaterialName;

            Material material;
            try {
                material = Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                material = Material.PAPER;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                if (isActive && !isPageButton) {
                    meta.setDisplayName(" §a▶ " + displayName);
                } else {
                    meta.setDisplayName(displayName);
                }
                List<String> lore = new ArrayList<>();
                lore.add(description);
                meta.setLore(lore);
                item.setItemMeta(meta);
                inventory.setItem(categorySlots[i], item);
            }
        }
    }

    private void setFillers(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§r");
            filler.setItemMeta(meta);
        }

        for (int slot = FILLER_START; slot <= FILLER_END; slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private void setItems(Inventory inventory, Player player, String category, int scrollRow) {
        ItemShopRegistry registry = ItemShopRegistry.getInstance();
        ItemShopData shopData = ItemShopData.getInstance();

        List<ItemShopRegistry.ShopItem> filteredItems = getSortedItems(registry.getAllShopItems(), category, shopData);

        int totalItems = filteredItems.size();
        int maxScrollRow = Math.max(0, (int) Math.ceil((double) totalItems / ITEMS_PER_ROW) - ITEMS_ROWS);
        int safeScrollRow = Math.min(scrollRow, maxScrollRow);
        int startIndex = safeScrollRow * ITEMS_PER_ROW;

        for (int slot = ITEMS_START; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, null);
        }

        for (int row = 0; row < ITEMS_ROWS; row++) {
            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                int absoluteIndex = startIndex + (row * ITEMS_PER_ROW) + col;
                int slot = ITEMS_START + (row * ITEMS_PER_ROW) + col;
                if (absoluteIndex < totalItems) {
                    ItemShopRegistry.ShopItem shopItem = filteredItems.get(absoluteIndex);
                    ItemStack displayItem = createItemDisplay(shopItem, player);
                    inventory.setItem(slot, displayItem);
                }
            }
        }
        updateScrollIndicators(inventory, safeScrollRow, maxScrollRow);
    }

    private void updateScrollIndicators(Inventory inventory, int currentRow, int maxRow) {
        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta meta = prev.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(currentRow > 0 ? "§a§l▲ §fScroll Up" : "§7§l▲ §8Scroll Up");
            prev.setItemMeta(meta);
        }
        inventory.setItem(CATEGORY_START + 7, prev);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        if (nextMeta != null) {
            nextMeta.setDisplayName(currentRow < maxRow ? "§a§l▼ §fScroll Down" : "§7§l▼ §8Scroll Down");
            next.setItemMeta(nextMeta);
        }
        inventory.setItem(CATEGORY_START + 8, next);
    }

    private List<ItemShopRegistry.ShopItem> getSortedItems(List<ItemShopRegistry.ShopItem> allItems,
                                                          String category, ItemShopData shopData) {
        List<ItemShopRegistry.ShopItem> filtered;

        if (category == null || category.equals(CATEGORY_ALL)) {
            filtered = new ArrayList<>(allItems);
        } else {
            filtered = new ArrayList<>();
            for (ItemShopRegistry.ShopItem item : allItems) {
                String itemCategory = shopData.getCategory(item.getId());
                if (category.equals(itemCategory)) {
                    filtered.add(item);
                }
            }
        }

        filtered.sort((a, b) -> {
            if (category == null || category.equals(CATEGORY_ALL)) {
                int catOrderA = getCategoryDisplayOrder(shopData.getCategory(a.getId()));
                int catOrderB = getCategoryDisplayOrder(shopData.getCategory(b.getId()));
                if (catOrderA != catOrderB) {
                    return Integer.compare(catOrderA, catOrderB);
                }
            }
            int orderA = shopData.getOrder(a.getId());
            int orderB = shopData.getOrder(b.getId());
            return Integer.compare(orderA, orderB);
        });

        return filtered;
    }

    private int getCategoryDisplayOrder(String category) {
        if (category == null) return 99;
        return switch (category) {
            case "main" -> 0;
            case "fighter" -> 1;
            case "mage" -> 2;
            case "tank" -> 3;
            case "marksman" -> 4;
            case "support" -> 5;
            default -> 99;
        };
    }

    private ItemStack createItemDisplay(ItemShopRegistry.ShopItem shopItem, Player player) {
        ItemStack item = new ItemStack(Material.NETHERITE_INGOT);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String itemName = shopItem.getDisplayName();
            int price = shopItem.getEffectivePrice(player);

            String displayName = "§f" + itemName + " §a◎ " + price;
            meta.setDisplayName(displayName);

            boolean canBuy = canPlayerBuyItem(player, shopItem);
            String itemGroup = ItemShopData.getInstance().getGroup(shopItem.getId());
            boolean isBootsItem = "boots".equals(itemGroup);
            boolean ownsAdvancedBoots = isBootsItem && playerOwnsAdvancedBoots(player);
            boolean ownsNormalBoots = isBootsItem && playerOwnsNormalBoots(player);
            boolean ownsThisBoots = isBootsItem && playerOwnsItemById(player, shopItem.getId());

            if (canBuy && !(isBootsItem && ownsAdvancedBoots)) {
                meta.setEnchantmentGlintOverride(true);
            }

            List<String> lore = new ArrayList<>();

            String shortDesc = ItemShopData.getInstance().getShortDesc(shopItem.getId());
            if (shortDesc != null && !shortDesc.isEmpty()) {
                lore.add("§7" + shortDesc);
                lore.add("");
            }

            if (shopItem.getStats().getAd() > 0) {
                lore.add("§6🗡 §f" + formatStat(shopItem.getStats().getAd()) + " §7Attack Damage");
            }
            if (shopItem.getStats().getAp() > 0) {
                lore.add("§9☄ §f" + formatStat(shopItem.getStats().getAp()) + " §7Ability Power");
            }
            if (shopItem.getStats().getAr() > 0) {
                lore.add("§e🛡 §f" + formatStat(shopItem.getStats().getAr()) + " §7Armor");
            }
            if (shopItem.getStats().getMr() > 0) {
                lore.add("§b⦿ §f" + formatStat(shopItem.getStats().getMr()) + " §7Magic Resist");
            }
            if (shopItem.getStats().getHp() > 0) {
                lore.add("§a❤ §f" + formatStat(shopItem.getStats().getHp()) + " §7Health");
            }
            if (shopItem.getStats().getHr() > 0) {
                lore.add("§2❣ §f" + formatStat(shopItem.getStats().getHr()) + " §7Health Regen per 5 sec.");
            }
            if (shopItem.getStats().getSr() > 0) {
                lore.add("§6🍖 §f" + formatStat(shopItem.getStats().getSr()) + " §7Saturation Regen per 5 sec.");
            }
            if (shopItem.getStats().getAs() > 0) {
                lore.add("§c➺ §f" + formatStat(shopItem.getStats().getAs()) + "% §7Attack Speed");
            }
            if (shopItem.getStats().getLs() > 0) {
                lore.add("§4✚ §f" + formatStat(shopItem.getStats().getLs()) + "% §7Life Steal");
            }
            if (shopItem.getStats().getCc() > 0) {
                lore.add("§4➷ §f" + formatStat(shopItem.getStats().getCc()) + "% §7Critical Chance");
            }
            if (shopItem.getStats().getApenFlat() > 0) {
                lore.add("§6🔰 §f" + formatStat(shopItem.getStats().getApenFlat()) + " §7Lethality");
            }
            if (shopItem.getStats().getApenPercent() > 0) {
                lore.add("§6⛨ §f" + formatStat(shopItem.getStats().getApenPercent()) + "% §7Armor Penetration");
            }
            if (shopItem.getStats().getMpenFlat() > 0) {
                lore.add("§d🔘 §f" + formatStat(shopItem.getStats().getMpenFlat()) + " §7Magic Penetration");
            }
            if (shopItem.getStats().getMpenPercent() > 0) {
                lore.add("§d🔘 §f" + formatStat(shopItem.getStats().getMpenPercent()) + "% §7Magic Penetration");
            }
            if (shopItem.getStats().getCh() > 0) {
                lore.add("§7⌛ §f" + formatStat(shopItem.getStats().getCh()) + " §7Cooldown Haste");
            }
            if (shopItem.getStats().getTn() > 0) {
                lore.add("§3⏩ §f" + formatStat(shopItem.getStats().getTn()) + " §7Tenacity");
            }
            if (shopItem.getStats().getMs() > 0) {
                lore.add("§f👣 §f" + formatStat(shopItem.getStats().getMs()) + "% §7Movement Speed");
            }

            if (shopItem.getStats().hasPassive()) {
                ItemPassive passive = ItemPassivesRegistry.getInstance().getPassive(shopItem.getStats().getPassiveId());
                if (passive != null) {
                    lore.add("");
                    String[] passiveLines = passive.getDescription().split("\n");
                    for (String line : passiveLines) {
                        lore.add(line);
                    }
                }
            }

            lore.add("");

            List<String> required = shopItem.getRequiredItems();
            String names = formatRequiredItemNamesWithCount(player, required);

            int requiredOwned = 0;
            for (String reqId : required) {
                if (countOwnedLeagueItems(player, reqId) > 0) {
                    requiredOwned++;
                }
            }
            int effectiveNetChange = 1 - requiredOwned;
            int currentCount = dev.ixpu.leaguemechanics.LeagueMechanics.getInstance().getStatsManager().countLeagueItems(player);

            String status;
            String statusColor;
            boolean canAfford = player.getLevel() >= price;

            if (isBootsItem) {
                if (ownsNormalBoots) {
                    if (ownsThisBoots) {
                        status = "ᴏᴡɴᴇᴅ";
                        statusColor = "§7";
                    } else {
                        status = "ᴘᴜʀᴄʜᴀꜱᴇ";
                        statusColor = canAfford ? "§a" : "§7";
                    }
                } else if (ownsAdvancedBoots) {
                    if (ownsThisBoots || "boots".equals(shopItem.getId())) {
                        status = "ᴏᴡɴᴇᴅ";
                        statusColor = "§7";
                    } else {
                        status = "ᴜɴᴀᴠᴀɪʟᴀʙʟᴇ";
                        statusColor = "§7";
                    }
                } else {
                    if (canBuy) {
                        if (currentCount >= 6 && effectiveNetChange > 0) {
                            status = "ʟᴏᴄᴋᴇᴅ";
                            statusColor = "§7";
                        } else {
                            status = "ᴘᴜʀᴄʜᴀꜱᴇ";
                            statusColor = canAfford ? "§a" : "§7";
                        }
                    } else {
                        status = "ʟᴏᴄᴋᴇᴅ";
                        statusColor = "§7";
                    }
                }
            } else {
                if (playerOwnsItem(player, shopItem)) {
                    status = "ᴏᴡɴᴇᴅ";
                    statusColor = "§7";
                } else if (canBuy) {
                    if (currentCount >= 6 && effectiveNetChange > 0) {
                        status = "ʟᴏᴄᴋᴇᴅ";
                        statusColor = "§7";
                    } else {
                        status = "ᴘᴜʀᴄʜᴀꜱᴇ";
                        statusColor = canAfford ? "§a" : "§7";
                    }
                } else {
                    status = "ʟᴏᴄᴋᴇᴅ";
                    statusColor = "§7";
                }
            }

            lore.add(statusColor + "§l" + status);

            if (status.equals("ʟᴏᴄᴋᴇᴅ") && currentCount >= 6 && effectiveNetChange > 0) {
                lore.add("");
                lore.add("§7ʟᴇᴀɢᴜᴇ ɪᴛᴇᴍꜱ: §c" + currentCount + "/6");
            }

            if ((status.equals("ᴘᴜʀᴄʜᴀꜱᴇ") || status.equals("ʟᴏᴄᴋᴇᴅ")) && !required.isEmpty()) {
                lore.add("");
                lore.add("§7ᴄᴏᴍᴘᴏɴᴇɴᴛꜱ: §7" + names);
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            ItemShopData shopData = ItemShopData.getInstance();
            meta.setRarity(shopData.getRarity(shopItem.getId()));
            item.setItemMeta(meta);

            if (shopData.hasCustomModel(shopItem.getId())) {
                ItemModifier.setItemModel(item, shopData.getModel(shopItem.getId()));
            }
        }
        return item;
    }

    public void handleClick(Player player, int slot) {
        UUID playerId = player.getUniqueId();

        if (slot >= CATEGORY_START && slot <= CATEGORY_END) {
            int buttonIndex = slot - CATEGORY_START;

            String[] categoryIds = {CATEGORY_ALL, CATEGORY_MAIN, CATEGORY_FIGHTER, CATEGORY_MAGE, CATEGORY_TANK,
                                   CATEGORY_MARKSMAN, CATEGORY_SUPPORT, "scroll_up", "scroll_down"};

            String clicked = categoryIds[buttonIndex];

            if (clicked.equals("scroll_up") || clicked.equals("scroll_down")) {
                int currentRow = playerScrollRow.getOrDefault(playerId, 0);
                String category = playerCategory.getOrDefault(playerId, CATEGORY_ALL);

                ItemShopRegistry registry = ItemShopRegistry.getInstance();
                ItemShopData shopData = ItemShopData.getInstance();
                List<ItemShopRegistry.ShopItem> filtered = getSortedItems(registry.getAllShopItems(), category, shopData);
                int totalItems = filtered.size();
                int maxRow = Math.max(0, (int) Math.ceil((double) totalItems / ITEMS_PER_ROW) - ITEMS_ROWS);

                if (clicked.equals("scroll_up")) {
                    playerScrollRow.put(playerId, Math.max(0, currentRow - 1));
                } else {
                    playerScrollRow.put(playerId, Math.min(maxRow, currentRow + 1));
                }
                refreshGUI(player);
            } else {
                playerCategory.put(playerId, clicked);
                playerScrollRow.put(playerId, 0);
                refreshGUI(player);
            }
            return;
        }

        if (slot >= ITEMS_START && slot < INVENTORY_SIZE) {
            handleItemClick(player, slot);
        }

    }

    private void handleItemClick(Player player, int slot) {
        int scrollRow = playerScrollRow.getOrDefault(player.getUniqueId(), 0);
        String category = playerCategory.getOrDefault(player.getUniqueId(), CATEGORY_ALL);

        ItemShopRegistry registry = ItemShopRegistry.getInstance();
        ItemShopData shopData = ItemShopData.getInstance();
        List<ItemShopRegistry.ShopItem> sortedItems = getSortedItems(registry.getAllShopItems(), category, shopData);

        int localIndex = slot - ITEMS_START;
        int row = localIndex / ITEMS_PER_ROW;
        int col = localIndex % ITEMS_PER_ROW;
        int absoluteIndex = (scrollRow + row) * ITEMS_PER_ROW + col;

        if (absoluteIndex < sortedItems.size()) {
            ItemShopRegistry.ShopItem clickedItem = sortedItems.get(absoluteIndex);
            dev.ixpu.leaguemechanics.manager.ItemShopManager.getInstance()
                    .purchaseFromGUI(player, clickedItem);
        }
    }

    private void refreshGUI(Player player) {
        org.bukkit.inventory.InventoryView view = player.getOpenInventory();

        if (view.getTitle().equals(INVENTORY_TITLE)) {
            Inventory inventory = view.getTopInventory();
            buildGUI(inventory, player);
        }
    }

    public void updateShopDisplayInstance(Player player) {
        refreshGUI(player);
    }

    private boolean canPlayerBuyItem(Player player, ItemShopRegistry.ShopItem shopItem) {
        if (playerOwnsItem(player, shopItem)) {
            return false;
        }
        if (playerOwnsGroupSibling(player, shopItem)) {
            return false;
        }
        List<String> required = shopItem.getRequiredItems();
        if (!required.isEmpty() && !playerHasRequiredItemsOrCanAfford(player, required)) {
            return false;
        }
        int currentCount = dev.ixpu.leaguemechanics.LeagueMechanics.getInstance().getStatsManager().countLeagueItems(player);
        int netChange = 1 - required.size();
        return netChange <= 0 || currentCount < 6;
    }


    private boolean playerOwnsGroupSibling(Player player, ItemShopRegistry.ShopItem shopItem) {
        ItemShopData shopData = ItemShopData.getInstance();
        String itemGroup = shopData.getGroup(shopItem.getId());
        if (itemGroup == null) {
            return false;
        }
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) continue;
            String ownedId = ItemModifier.getItemId(inv);
            if (ownedId == null || ownedId.equals(shopItem.getId())) continue;
            String ownedGroup = shopData.getGroup(ownedId);
            if (ownedGroup != null && ownedGroup.equals(itemGroup)) {
                return true;
            }
        }
        return false;
    }

    private boolean playerHasRequiredItemsCount(Player player, List<String> requiredIds) {
        for (String requiredId : requiredIds) {
            int required = countRequiredOccurrences(requiredIds, requiredId, requiredIds.lastIndexOf(requiredId));
            int owned = countOwnedLeagueItems(player, requiredId);
            if (owned < required) return false;
        }
        return true;
    }

    private boolean playerHasRequiredItemsOrCanAfford(Player player, List<String> requiredIds) {
        ItemShopData shopData = ItemShopData.getInstance();
        int playerLevel = player.getLevel();
        for (String requiredId : requiredIds) {
            int required = countRequiredOccurrences(requiredIds, requiredId, requiredIds.lastIndexOf(requiredId));
            int owned = countOwnedLeagueItems(player, requiredId);
            if (owned >= required) continue;
            if (playerLevel >= shopData.getPrice(requiredId)) continue;
            return false;
        }
        return true;
    }

    private boolean playerOwnsLeagueItemId(Player player, String itemId) {
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv != null && !inv.getType().isAir() && itemId.equals(ItemModifier.getItemId(inv))) {
                return true;
            }
        }
        return false;
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
        return playerOwnsGroupSibling(player, shopItem);
    }

    private String formatRequiredItemNamesWithCount(Player player, List<String> requiredIds) {
        ItemStatsData statsData = ItemStatsData.getInstance();
        ItemShopData shopData = ItemShopData.getInstance();
        int playerLevel = player.getLevel();
        java.util.LinkedHashMap<String, Integer> requiredById = new java.util.LinkedHashMap<>();
        for (String id : requiredIds) requiredById.merge(id, 1, Integer::sum);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var e : requiredById.entrySet()) {
            if (!first) sb.append(" §7+ ");
            first = false;
            String id = e.getKey();
            int total = e.getValue();
            int owned = countOwnedLeagueItems(player, id);
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

    private int countRequiredOccurrences(List<String> requiredIds, String targetId, int pos) {
        int count = 0;
        for (int i = 0; i <= pos; i++) {
            if (targetId.equals(requiredIds.get(i))) count++;
        }
        return count;
    }

    private int countOwnedLeagueItems(Player player, String itemId) {
        int count = 0;
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv != null && !inv.getType().isAir() && itemId.equals(ItemModifier.getItemId(inv))) {
                count++;
            }
        }
        return count;
    }

    private boolean playerOwnsAdvancedBoots(Player player) {
        ItemShopData shopData = ItemShopData.getInstance();
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) continue;
            String ownedId = ItemModifier.getItemId(inv);
            if (ownedId == null) continue;
            String ownedGroup = shopData.getGroup(ownedId);
            if ("boots".equals(ownedGroup) && !shopData.getRequiredItems(ownedId).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean playerOwnsAnyBoots(Player player) {
        ItemShopData shopData = ItemShopData.getInstance();
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) continue;
            String ownedId = ItemModifier.getItemId(inv);
            if (ownedId == null) continue;
            if ("boots".equals(shopData.getGroup(ownedId))) {
                return true;
            }
        }
        return false;
    }

    private boolean playerOwnsNormalBoots(Player player) {
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) continue;
            if ("boots".equals(ItemModifier.getItemId(inv))) {
                return true;
            }
        }
        return false;
    }

    private boolean playerOwnsItemById(Player player, String itemId) {
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) continue;
            if (itemId.equals(ItemModifier.getItemId(inv))) {
                return true;
            }
        }
        return false;
    }

    private String formatStat(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.format("%.1f", value);
        }
    }

    public void cleanup(Player player) {
        playerScrollRow.remove(player.getUniqueId());
        playerCategory.remove(player.getUniqueId());
    }

    public String getPlayerCategory(Player player) {
        return playerCategory.getOrDefault(player.getUniqueId(), CATEGORY_ALL);
    }

}
