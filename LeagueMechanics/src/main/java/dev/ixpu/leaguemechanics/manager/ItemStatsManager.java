package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.util.ItemModifier;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemStatsManager {
    private static final long CACHE_TTL_MS = 100;
    private static final String[] STAT_TYPES = {
            "HP", "HR", "AD", "AP", "TD", "AS", "AR", "MR", "SR", "LS", "CC", "MS", "CH", "TN",
            "APEN", "APEN_PERCENT", "MPEN", "MPEN_PERCENT", "CSD", "HM"
    };

    private final Map<UUID, Map<String, Double>> statsCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cacheTimestamp = new ConcurrentHashMap<>();

    public int countLeagueItems(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                String itemId = ItemModifier.getItemId(item);
                if (itemId != null) {
                    count++;
                }
            }
        }
        return count;
    }

    public void enforceItemLimit(Player player) {
        int totalLimit = 6;
        ItemStack[] contents = player.getInventory().getContents();
        dev.ixpu.leaguemechanics.item.shop.ItemShopData shopData = dev.ixpu.leaguemechanics.item.shop.ItemShopData.getInstance();

        Map<String, Integer> owned = new HashMap<>();
        int removed = 0;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) {
                continue;
            }
            String itemId = ItemModifier.getItemId(item);
            if (itemId == null || !shopData.hasItem(itemId)) {
                continue;
            }
            int count = owned.merge(itemId, 1, Integer::sum);
            if (count > shopData.getLimit(itemId)) {
                player.getInventory().setItem(i, null);
                contents[i] = null;
                removed++;
            }
        }

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir() && ItemModifier.getItemId(item) != null) {
                slots.add(i);
            }
        }
        for (int i = slots.size() - 1; i >= totalLimit; i--) {
            player.getInventory().setItem(slots.get(i), null);
            removed++;
        }

        if (removed > 0) {
            invalidateCache(player.getUniqueId());
            player.sendMessage(net.kyori.adventure.text.Component.text("§cLeague item limit exceeded, removed " + removed + " extra item(s)"));
        }
    }

    public void invalidateCache(UUID uuid) {
        statsCache.remove(uuid);
        cacheTimestamp.remove(uuid);
    }

    private Map<String, Double> computeAllStats(Player player) {
        Map<String, Double> stats = new HashMap<>();
        for (String stat : STAT_TYPES) {
            stats.put(stat, 0.0);
        }

        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (count >= 6) break;

            if (item != null && !item.getType().isAir()) {
                String itemId = ItemModifier.getItemId(item);
                if (itemId != null) {
                    for (String stat : STAT_TYPES) {
                        double value = ItemModifier.getStat(item, stat);
                        stats.merge(stat, value, Double::sum);
                    }
                    count++;
                }
            }
        }

        return stats;
    }

    private double getStatWithLimit(Player player, String statType) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long lastUpdate = cacheTimestamp.get(uuid);
        if (lastUpdate != null && (now - lastUpdate) < CACHE_TTL_MS) {
            Map<String, Double> cached = statsCache.get(uuid);
            if (cached != null) {
                return cached.getOrDefault(statType, 0.0);
            }
        }

        Map<String, Double> stats = computeAllStats(player);
        statsCache.put(uuid, stats);
        cacheTimestamp.put(uuid, now);

        return stats.getOrDefault(statType, 0.0);
    }

    public double getItemHP(Player player) {
        return getStatWithLimit(player, "HP");
    }

    public double getItemHR(Player player) {
        return getStatWithLimit(player, "HR");
    }

    public double getItemAD(Player player) {
        return getStatWithLimit(player, "AD");
    }

    public double getItemAP(Player player) {
        return getStatWithLimit(player, "AP");
    }

    public double getItemTD(Player player) {
        return getStatWithLimit(player, "TD");
    }

    public double getItemAS(Player player) {
        return getStatWithLimit(player, "AS");
    }

    public double getItemAR(Player player) {
        return getStatWithLimit(player, "AR");
    }

    public double getItemMR(Player player) {
        return getStatWithLimit(player, "MR");
    }

    public double getItemSR(Player player) {
        return getStatWithLimit(player, "SR");
    }

    public double getItemLS(Player player) {
        return getStatWithLimit(player, "LS");
    }

    public double getItemCC(Player player) {
        return getStatWithLimit(player, "CC");
    }

    public double getItemMS(Player player) {
        return getStatWithLimit(player, "MS");
    }

    public double getItemAPen(Player player) {
        return getStatWithLimit(player, "APEN");
    }

    public double getItemAPenPercent(Player player) {
        return getStatWithLimit(player, "APEN_PERCENT");
    }

    public double getItemMPen(Player player) {
        return getStatWithLimit(player, "MPEN");
    }

    public double getItemMPenPercent(Player player) {
        return getStatWithLimit(player, "MPEN_PERCENT");
    }

    public double getItemCH(Player player) {
        return getStatWithLimit(player, "CH");
    }

    public double getItemTN(Player player) {
        return getStatWithLimit(player, "TN");
    }

    public double getItemCSD(Player player) {
        return getStatWithLimit(player, "CSD");
    }

    public double getItemHM(Player player) {
        return getStatWithLimit(player, "HM");
    }
}