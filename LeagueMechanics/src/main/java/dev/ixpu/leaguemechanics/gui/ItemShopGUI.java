package dev.ixpu.leaguemechanics.gui;

import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import dev.ixpu.leaguemechanics.item.shop.ItemShopData;
import dev.ixpu.leaguemechanics.item.shop.ItemShopRegistry;
import dev.ixpu.leaguemechanics.util.Icons;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import dev.ixpu.leaguemechanics.util.ShopCurrency;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import dev.ixpu.leaguemechanics.item.ItemStatsData;
import dev.ixpu.leaguemechanics.item.ItemStatsRegistry;

public class ItemShopGUI {
    private static ItemShopGUI instance;
    private static final int INVENTORY_SIZE = 54;
    private static final String INVENTORY_TITLE = "§8§lɪᴛᴇᴍ ꜱʜᴏᴘ";

    private static final int COLUMNS = 9;
    private static final int GRID_FIRST_ROW = 2;
    private static final int GRID_FIRST_COL = 2;
    static final int ITEMS_PER_ROW = 7;
    static final int ITEMS_ROWS = 4;
    private static final int SCROLL_DOWN_SLOT = 7;
    private static final int SCROLL_UP_SLOT = 8;

    private static final String CATEGORY_ALL = "all";
    private static final String CATEGORY_MAIN = "main";
    private static final String CATEGORY_MAGE = "mage";
    private static final String CATEGORY_FIGHTER = "fighter";
    private static final String CATEGORY_ASSASSIN = "assassin";
    private static final String CATEGORY_TANK = "tank";
    private static final String CATEGORY_MARKSMAN = "marksman";
    private static final String CATEGORY_SUPPORT = "support";

    private static final Material BUTTON_MATERIAL = Material.NETHERITE_INGOT;

    private static final String[][] CATEGORY_BUTTONS = {
            {CATEGORY_ALL, "§7ᴀʟʟ", "§7View all items", "leaguemechanics:shop-all"},
            {CATEGORY_FIGHTER, "§7ғɪɢʜᴛᴇʀ", "§7Sustained Combat", "leaguemechanics:shop-fighter"},
            {CATEGORY_MAGE, "§7ᴍᴀɢᴇ", "§7Magic Ability", "leaguemechanics:shop-mage"},
            {CATEGORY_ASSASSIN, "§7ᴀꜱꜱᴀꜱꜱɪɴ", "§7Quick Attacks", "leaguemechanics:shop-assassin"},
            {CATEGORY_TANK, "§7ᴛᴀɴᴋ", "§7Defensive Frontline", "leaguemechanics:shop-tank"},
            {CATEGORY_SUPPORT, "§7ꜱᴜᴘᴘᴏʀᴛ", "§7Utilities", "leaguemechanics:shop-support"},
            {CATEGORY_MARKSMAN, "§7ᴍᴀʀᴋꜱᴍᴀɴ", "§7Ranged Physical", "leaguemechanics:shop-marksman"}
    };

    private static final String[][] STAT_BUTTONS = {
            {"ad", "§7ᴀᴛᴛᴀᴄᴋ ᴅᴀᴍᴀɢᴇ", "§7Items with Attack Damage", "leaguemechanics:shop-stat-ad"},
            {"as", "§7ᴀᴛᴛᴀᴄᴋ ꜱᴘᴇᴇᴅ", "§7Items with Attack Speed", "leaguemechanics:shop-stat-as"},
            {"cc", "§7ᴄʀɪᴛꜱ", "§7Items with Critical Chance", "leaguemechanics:shop-stat-cc"},
            {"ap", "§7ᴀʙɪʟɪᴛʏ ᴘᴏᴡᴇʀ", "§7Items with Ability Power", "leaguemechanics:shop-stat-ap"},
            {"ms", "§7ᴍᴏᴠᴇᴍᴇɴᴛ ꜱᴘᴇᴇᴅ", "§7Items with Movement Speed", "leaguemechanics:shop-stat-ms"}
    };

    private final Map<UUID, Integer> playerScrollRow = new HashMap<>();
    private final Map<UUID, String> playerCategory = new HashMap<>();
    private final Map<UUID, String> playerStatFilter = new HashMap<>();

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
        UUID id = player.getUniqueId();
        String statFilter = playerStatFilter.get(id);
        String activeCategory = statFilter != null ? null : playerCategory.getOrDefault(id, CATEGORY_ALL);
        int scrollRow = playerScrollRow.getOrDefault(id, 0);

        setCategoryButtons(inventory, activeCategory);
        setStatButtons(inventory, statFilter);
        setFillers(inventory);
        setItems(inventory, player, scrollRow);
    }

    private ItemStack createFilterButton(String displayName, String description, String model, boolean active) {
        ItemStack item = new ItemStack(BUTTON_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(active ? " §a▶ " + displayName : displayName);
            List<String> lore = new ArrayList<>();
            lore.add(description);
            meta.setLore(lore);
            if (active) {
                meta.setEnchantmentGlintOverride(true);
            }
            item.setItemMeta(meta);
            ItemModifier.setItemModel(item, model);
        }
        return item;
    }

    private void setCategoryButtons(Inventory inventory, String activeCategory) {
        for (int slot = 0; slot < CATEGORY_BUTTONS.length; slot++) {
            String[] cat = CATEGORY_BUTTONS[slot];
            boolean active = cat[0].equals(activeCategory);
            inventory.setItem(slot, createFilterButton(cat[1], cat[2], cat[3], active));
        }
    }

    private void setStatButtons(Inventory inventory, String activeStat) {
        for (int i = 0; i < STAT_BUTTONS.length; i++) {
            String[] stat = STAT_BUTTONS[i];
            boolean active = stat[0].equals(activeStat);
            inventory.setItem(statSlot(i), createFilterButton(stat[1], stat[2], stat[3], active));
        }
    }

    private static int statSlot(int statIndex) {
        return (statIndex + 1) * COLUMNS;
    }

    private static int statIndexForSlot(int slot) {
        if (slot % COLUMNS != 0) return -1;
        int index = slot / COLUMNS - 1;
        return index >= 0 && index < STAT_BUTTONS.length ? index : -1;
    }

    private static int gridSlot(int row, int col) {
        return (GRID_FIRST_ROW + row) * COLUMNS + GRID_FIRST_COL + col;
    }

    private static int gridIndexForSlot(int slot) {
        int row = slot / COLUMNS - GRID_FIRST_ROW;
        int col = slot % COLUMNS - GRID_FIRST_COL;
        if (row < 0 || row >= ITEMS_ROWS || col < 0 || col >= ITEMS_PER_ROW) return -1;
        return row * ITEMS_PER_ROW + col;
    }

    private void setFillers(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§r");
            filler.setItemMeta(meta);
        }

        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            int row = slot / COLUMNS;
            int col = slot % COLUMNS;
            boolean isFiller = (row == 1 && col >= 1) || (col == 1 && row >= GRID_FIRST_ROW);
            if (isFiller) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private void setItems(Inventory inventory, Player player, int scrollRow) {
        List<ItemShopRegistry.ShopItem> filteredItems = getVisibleItems(player.getUniqueId());

        int totalItems = filteredItems.size();
        int maxScrollRow = getMaxScrollRow(totalItems);
        int safeScrollRow = Math.min(scrollRow, maxScrollRow);
        int startIndex = safeScrollRow * ITEMS_PER_ROW;

        for (int row = 0; row < ITEMS_ROWS; row++) {
            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                int absoluteIndex = startIndex + (row * ITEMS_PER_ROW) + col;
                int slot = gridSlot(row, col);
                if (absoluteIndex < totalItems) {
                    ItemShopRegistry.ShopItem shopItem = filteredItems.get(absoluteIndex);
                    inventory.setItem(slot, createItemDisplay(shopItem, player));
                } else {
                    inventory.setItem(slot, null);
                }
            }
        }
        updateScrollIndicators(inventory, safeScrollRow, maxScrollRow);
    }

    private int getMaxScrollRow(int totalItems) {
        return Math.max(0, (int) Math.ceil((double) totalItems / ITEMS_PER_ROW) - ITEMS_ROWS);
    }

    private void updateScrollIndicators(Inventory inventory, int currentRow, int maxRow) {
        boolean canScrollDown = currentRow < maxRow;
        inventory.setItem(SCROLL_DOWN_SLOT, createScrollButton(
                canScrollDown ? "§a§l▼ §fScroll Down" : "§7§l▼ §8Scroll Down",
                canScrollDown ? "leaguemechanics:shop-scroll-down" : "leaguemechanics:shop-scroll-down-off"));

        boolean canScrollUp = currentRow > 0;
        inventory.setItem(SCROLL_UP_SLOT, createScrollButton(
                canScrollUp ? "§a§l▲ §fScroll Up" : "§7§l▲ §8Scroll Up",
                canScrollUp ? "leaguemechanics:shop-scroll-up" : "leaguemechanics:shop-scroll-up-off"));
    }

    private ItemStack createScrollButton(String displayName, String model) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
            ItemModifier.setItemModel(item, model);
        }
        return item;
    }

    private List<ItemShopRegistry.ShopItem> getVisibleItems(UUID playerId) {
        String category = playerCategory.getOrDefault(playerId, CATEGORY_ALL);
        String statFilter = playerStatFilter.get(playerId);
        return getSortedItems(ItemShopRegistry.getInstance().getAllShopItems(), category, statFilter,
                ItemShopData.getInstance());
    }

    private List<ItemShopRegistry.ShopItem> getSortedItems(List<ItemShopRegistry.ShopItem> allItems,
                                                           String category, String statFilter,
                                                           ItemShopData shopData) {
        boolean hasStat = statFilter != null;
        boolean allCategories = category == null || category.equals(CATEGORY_ALL);

        List<ItemShopRegistry.ShopItem> filtered = new ArrayList<>();
        for (ItemShopRegistry.ShopItem item : allItems) {
            if (hasStat) {
                if (getStatValue(item.getStats(), statFilter) <= 0) continue;
            } else if (!allCategories && !category.equals(shopData.getCategory(item.getId()))) {
                continue;
            }
            filtered.add(item);
        }

        boolean groupByCategory = hasStat || allCategories;
        filtered.sort((a, b) -> {
            if (groupByCategory) {
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

    private double getStatValue(ItemStatsRegistry stats, String statFilter) {
        if (stats == null) return 0.0;
        return switch (statFilter) {
            case "ad" -> stats.getAd();
            case "as" -> stats.getAs();
            case "cc" -> stats.getCc();
            case "ap" -> stats.getAp();
            case "ms" -> stats.getMs();
            default -> 0.0;
        };
    }

    private int getCategoryDisplayOrder(String category) {
        if (category == null) return 99;
        return switch (category) {
            case CATEGORY_MAIN -> 0;
            case CATEGORY_FIGHTER -> 1;
            case CATEGORY_MAGE -> 2;
            case CATEGORY_ASSASSIN -> 3;
            case CATEGORY_TANK -> 4;
            case CATEGORY_SUPPORT -> 5;
            case CATEGORY_MARKSMAN -> 6;
            default -> 99;
        };
    }

    private ItemStack createItemDisplay(ItemShopRegistry.ShopItem shopItem, Player player) {
        ItemStack item = new ItemStack(Material.NETHERITE_INGOT);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String itemName = shopItem.getDisplayName();
            int price = shopItem.getEffectivePrice(player);

            String displayName = "§f" + itemName + " §f" + Icons.get("stats.gold") + " §e" + price;
            meta.setDisplayName(displayName);

            boolean canBuy = canPlayerBuyItem(player, shopItem);
            String itemGroup = ItemShopData.getInstance().getGroup(shopItem.getId());
            boolean isGroupedItem = itemGroup != null;
            List<String> ownedGroupIds = isGroupedItem ? getOwnedGroupIds(player, itemGroup) : List.of();
            List<String> thisRequired = ItemShopData.getInstance().getRequiredItems(shopItem.getId());
            boolean ownsThisItem = ownedGroupIds.contains(shopItem.getId());
            boolean ownsUpgradeBase = ownedGroupIds.stream().anyMatch(thisRequired::contains);
            boolean isComponentOfOwned = ownedGroupIds.stream()
                    .anyMatch(id -> ItemShopData.getInstance().getRequiredItems(id).contains(shopItem.getId()));

            if (canBuy) {
                meta.setEnchantmentGlintOverride(true);
            }

            List<String> lore = new ArrayList<>();

            String shortDesc = ItemShopData.getInstance().getShortDesc(shopItem.getId());
            if (shortDesc != null && !shortDesc.isEmpty()) {
                lore.add("§7" + shortDesc);
                lore.add("");
            }

            if (shopItem.getStats().getAd() > 0) {
                lore.add("§f" + Icons.get("stats.ad") + " " + formatStat(shopItem.getStats().getAd()) + " §7Attack Damage");
            }
            if (shopItem.getStats().getAp() > 0) {
                lore.add("§f" + Icons.get("stats.ap") + " " + formatStat(shopItem.getStats().getAp()) + " §7Ability Power");
            }
            if (shopItem.getStats().getAr() > 0) {
                lore.add("§f" + Icons.get("stats.ar") + " " + formatStat(shopItem.getStats().getAr()) + " §7Armor");
            }
            if (shopItem.getStats().getMr() > 0) {
                lore.add("§f" + Icons.get("stats.mr") + " " + formatStat(shopItem.getStats().getMr()) + " §7Magic Resist");
            }
            if (shopItem.getStats().getHp() > 0) {
                lore.add("§f" + Icons.get("stats.hp") + " " + formatStat(shopItem.getStats().getHp()) + " §7Health");
            }
            if (shopItem.getStats().getHr() > 0) {
                lore.add("§f" + Icons.get("stats.hr") + " " + formatStat(shopItem.getStats().getHr()) + " §7Health Regen per 5 sec.");
            }
            if (shopItem.getStats().getSr() > 0) {
                lore.add("§6" + Icons.get("stats.sr") + " §f" + formatStat(shopItem.getStats().getSr()) + " §7Saturation Regen per 5 sec.");
            }
            if (shopItem.getStats().getAs() > 0) {
                lore.add("§f" + Icons.get("stats.as") + " " + formatStat(shopItem.getStats().getAs()) + "% §7Attack Speed");
            }
            if (shopItem.getStats().getLs() > 0) {
                lore.add("§f" + Icons.get("stats.ls") + " " + formatStat(shopItem.getStats().getLs()) + "% §7Life Steal");
            }
            if (shopItem.getStats().getCc() > 0) {
                lore.add("§f" + Icons.get("stats.cc") + " " + formatStat(shopItem.getStats().getCc()) + "% §7Critical Chance");
            }
            if (shopItem.getStats().getApenFlat() > 0) {
                lore.add("§f" + Icons.get("stats.apen-flat") + " " + formatStat(shopItem.getStats().getApenFlat()) + " §7Lethality");
            }
            if (shopItem.getStats().getApenPercent() > 0) {
                lore.add("§f" + Icons.get("stats.apen-percent") + " " + formatStat(shopItem.getStats().getApenPercent()) + "% §7Armor Penetration");
            }
            if (shopItem.getStats().getMpenFlat() > 0) {
                lore.add("§f" + Icons.get("stats.mpen") + " " + formatStat(shopItem.getStats().getMpenFlat()) + " §7Magic Penetration");
            }
            if (shopItem.getStats().getMpenPercent() > 0) {
                lore.add("§f" + Icons.get("stats.mpen") + " " + formatStat(shopItem.getStats().getMpenPercent()) + "% §7Magic Penetration");
            }
            if (shopItem.getStats().getCsd() > 0) {
                lore.add("§f" + Icons.get("stats.enhanced-critical-damage") + " " + formatStat(shopItem.getStats().getCsd()) + "% §7Critical Damage");
            }
            if (shopItem.getStats().getHm() > 0) {
                lore.add("§f" + Icons.get("stats.heal-and-shield-power") + " " + formatStat(shopItem.getStats().getHm()) + "% §7Healing Power");
            }
            if (shopItem.getStats().getCh() > 0) {
                lore.add("§f" + Icons.get("stats.ch") + " " + formatStat(shopItem.getStats().getCh()) + " §7Ability Haste");
            }
            if (shopItem.getStats().getTn() > 0) {
                lore.add("§f" + Icons.get("stats.tn") + " " + formatStat(shopItem.getStats().getTn()) + " §7Tenacity");
            }
            if (shopItem.getStats().getMs() > 0) {
                lore.add("§f" + Icons.get("stats.ms") + " " + formatStat(shopItem.getStats().getMs()) + "% §7Movement Speed");
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

            int requiredOwned = shopItem.countOwnedComponents(player);
            int effectiveNetChange = 1 - requiredOwned;
            int currentCount = dev.ixpu.leaguemechanics.LeagueMechanics.getInstance().getStatsManager().countLeagueItems(player);

            String status;
            String statusColor;
            boolean canAfford = ShopCurrency.has(player, price);

            if (isGroupedItem && (ownsThisItem || isComponentOfOwned)) {
                status = "ᴏᴡɴᴇᴅ";
                statusColor = "§7";
            } else if (isGroupedItem && ownsUpgradeBase) {
                status = "ᴘᴜʀᴄʜᴀꜱᴇ";
                statusColor = canAfford ? "§a" : "§7";
            } else if (isGroupedItem && !ownedGroupIds.isEmpty()) {
                status = "ᴜɴᴀᴠᴀɪʟᴀʙʟᴇ";
                statusColor = "§7";
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
                String model = shopData.getModel(shopItem.getId());
                boolean grayedOut = status.equals("ʟᴏᴄᴋᴇᴅ") || status.equals("ᴜɴᴀᴠᴀɪʟᴀʙʟᴇ") || status.equals("ᴏᴡɴᴇᴅ");
                ItemModifier.setItemModel(item, grayedOut ? model + "_locked" : model);
            }
        }
        return item;
    }

    public void handleClick(Player player, int slot) {
        UUID playerId = player.getUniqueId();

        if (slot >= 0 && slot < CATEGORY_BUTTONS.length) {
            playerStatFilter.remove(playerId);
            playerCategory.put(playerId, CATEGORY_BUTTONS[slot][0]);
            playerScrollRow.put(playerId, 0);
            refreshGUI(player);
            return;
        }

        if (slot == SCROLL_UP_SLOT || slot == SCROLL_DOWN_SLOT) {
            int currentRow = playerScrollRow.getOrDefault(playerId, 0);
            int maxRow = getMaxScrollRow(getVisibleItems(playerId).size());

            if (slot == SCROLL_UP_SLOT) {
                playerScrollRow.put(playerId, Math.max(0, currentRow - 1));
            } else {
                playerScrollRow.put(playerId, Math.min(maxRow, currentRow + 1));
            }
            refreshGUI(player);
            return;
        }

        int statIndex = statIndexForSlot(slot);
        if (statIndex >= 0) {
            String statId = STAT_BUTTONS[statIndex][0];
            if (statId.equals(playerStatFilter.get(playerId))) {
                playerStatFilter.remove(playerId);
            } else {
                playerStatFilter.put(playerId, statId);
            }
            playerCategory.put(playerId, CATEGORY_ALL);
            playerScrollRow.put(playerId, 0);
            refreshGUI(player);
            return;
        }

        if (gridIndexForSlot(slot) >= 0) {
            handleItemClick(player, slot);
        }
    }

    private void handleItemClick(Player player, int slot) {
        UUID playerId = player.getUniqueId();
        int scrollRow = playerScrollRow.getOrDefault(playerId, 0);
        List<ItemShopRegistry.ShopItem> visibleItems = getVisibleItems(playerId);

        int gridIndex = gridIndexForSlot(slot);
        if (gridIndex < 0) return;

        int absoluteIndex = scrollRow * ITEMS_PER_ROW + gridIndex;

        if (absoluteIndex < visibleItems.size()) {
            ItemShopRegistry.ShopItem clickedItem = visibleItems.get(absoluteIndex);
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
        int netChange = 1 - shopItem.countOwnedComponents(player);
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
        for (String requiredId : requiredIds) {
            int required = countRequiredOccurrences(requiredIds, requiredId, requiredIds.lastIndexOf(requiredId));
            int owned = countOwnedLeagueItems(player, requiredId);
            if (owned >= required) continue;
            if (ShopCurrency.has(player, shopData.getPrice(requiredId))) continue;
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
            } else if (ShopCurrency.has(player, shopData.getPrice(id))) {
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

    private List<String> getOwnedGroupIds(Player player, String group) {
        ItemShopData shopData = ItemShopData.getInstance();
        List<String> ids = new ArrayList<>();
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) continue;
            String ownedId = ItemModifier.getItemId(inv);
            if (ownedId != null && group.equals(shopData.getGroup(ownedId))) {
                ids.add(ownedId);
            }
        }
        return ids;
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
        playerStatFilter.remove(player.getUniqueId());
    }

    public String getPlayerCategory(Player player) {
        return playerCategory.getOrDefault(player.getUniqueId(), CATEGORY_ALL);
    }

}