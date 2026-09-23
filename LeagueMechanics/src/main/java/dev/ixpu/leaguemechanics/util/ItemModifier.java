package dev.ixpu.leaguemechanics.util;

import dev.ixpu.leaguemechanics.item.ItemStatsData;
import dev.ixpu.leaguemechanics.item.ItemStatsRegistry;
import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import dev.ixpu.leaguemechanics.item.shop.ItemShopData;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;

public class ItemModifier {
    private static NamespacedKey ITEM_ID_KEY;
    private static NamespacedKey CUSTOM_MODEL_DATA_KEY;
    private static NamespacedKey ITEM_MODEL_KEY;

    public static void initialize(Plugin plugin) {
        ITEM_ID_KEY = new NamespacedKey(plugin, "league_item_id");
        CUSTOM_MODEL_DATA_KEY = new NamespacedKey(plugin, "league_model_data");
        ITEM_MODEL_KEY = new NamespacedKey(plugin, "league_item_model");
    }

    public static void setItemId(ItemStack item, String itemId) {
        if (item == null || item.getType().isAir() || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.setMaxStackSize(1);
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(ITEM_ID_KEY, PersistentDataType.STRING, itemId);
        item.setItemMeta(meta);
    }

    public static String getItemId(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getItemMeta() == null) return null;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (data.has(ITEM_ID_KEY, PersistentDataType.STRING)) {
            return data.get(ITEM_ID_KEY, PersistentDataType.STRING);
        }
        return null;
    }

    public static void setCustomModelData(ItemStack item, int modelData) {
        if (item == null || item.getType().isAir() || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(modelData);
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(CUSTOM_MODEL_DATA_KEY, PersistentDataType.INTEGER, modelData);
        item.setItemMeta(meta);
    }

    public static void setItemModel(ItemStack item, String model) {
        if (item == null || item.getType().isAir() || item.getItemMeta() == null || model == null) return;

        ItemMeta meta = item.getItemMeta();

        try {
            String[] sep = model.split(":");
            if (sep.length != 2) return;

            NamespacedKey modelKey = new NamespacedKey(sep[0], sep[1]);

            try {
                Method setItemModelMethod = ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
                setItemModelMethod.invoke(meta, modelKey);
                item.setItemMeta(meta);
                return;
            } catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                //
            }
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(ITEM_MODEL_KEY, PersistentDataType.STRING, model);
            item.setItemMeta(meta);
        } catch (Exception e) {
            //
        }
    }

    public static void syncItemStats(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getItemMeta() == null) return;

        String itemId = getItemId(item);
        if (itemId == null) return;

        ItemStatsRegistry statData = ItemStatsData.getInstance().getItem(itemId);
        if (statData == null) return;

        updateItemLore(item, statData);
        updateItemModel(item, statData);
    }

    private static void updateItemLore(ItemStack item, ItemStatsRegistry statData) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = new ArrayList<>();

        String shortDesc = ItemShopData.getInstance().getShortDesc(statData.getId());
        if (shortDesc != null && !shortDesc.isEmpty()) {
            lore.add("§7" + shortDesc);
            lore.add("");
        }

        if (statData.getAd() > 0) {
            lore.add("§6🗡 §f" + formatStat(statData.getAd()) + " §7Attack Damage");
        }
        if (statData.getAp() > 0) {
            lore.add("§9☄ §f" + formatStat(statData.getAp()) + " §7Ability Power");
        }
        if (statData.getAr() > 0) {
            lore.add("§e🛡 §f" + formatStat(statData.getAr()) + " §7Armor");
        }
        if (statData.getMr() > 0) {
            lore.add("§b⦿ §f" + formatStat(statData.getMr()) + " §7Magic Resist");
        }
        if (statData.getHp() > 0) {
            lore.add("§a❤ §f" + formatStat(statData.getHp()) + " §7Health");
        }
        if (statData.getHr() > 0) {
            lore.add("§2❣ §f" + formatStat(statData.getHr()) + " §7Health Regen per 5 sec.");
        }
        if (statData.getSr() > 0) {
            lore.add("§6🍖 §f" + formatStat(statData.getSr()) + " §7Saturation Regen per 5 sec.");
        }
        if (statData.getAs() > 0) {
            lore.add("§c➺ §f" + formatStat(statData.getAs()) + "% §7Attack Speed");
        }
        if (statData.getLs() > 0) {
            lore.add("§4✚ §f" + formatStat(statData.getLs()) + "% §7Life Steal");
        }
        if (statData.getCc() > 0) {
            lore.add("§4➷ §f" + formatStat(statData.getCc()) + "% §7Critical Chance");
        }
        if (statData.getApenFlat() > 0) {
            lore.add("§6🔰 §f" + formatStat(statData.getApenFlat()) + " §7Lethality");
        }
        if (statData.getApenPercent() > 0) {
            lore.add("§6⛨ §f" + formatStat(statData.getApenPercent()) + "% §7Armor Penetration");
        }
        if (statData.getMpenFlat() > 0) {
            lore.add("§d🔘 §f" + formatStat(statData.getMpenFlat()) + " §7Magic Penetration");
        }
        if (statData.getMpenPercent() > 0) {
            lore.add("§d🔘 §f" + formatStat(statData.getMpenPercent()) + "% §7Magic Penetration");
        }
        if (statData.getCh() > 0) {
            lore.add("§7⌛ §f" + formatStat(statData.getCh()) + " §7Cooldown Haste");
        }
        if (statData.getTn() > 0) {
            lore.add("§3⏩ §f" + formatStat(statData.getTn()) + " §7Tenacity");
        }
        if (statData.getMs() > 0) {
            lore.add("§f👣 §f" + formatStat(statData.getMs()) + "% §7Movement Speed");
        }

        if (statData.hasPassive()) {
            ItemPassive passive = ItemPassivesRegistry.getInstance().getPassive(statData.getPassiveId());
            if (passive != null) {
                lore.add("");
                for (String line : passive.getDescription().split("\n")) {
                    lore.add(line);
                }
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private static void updateItemModel(ItemStack item, ItemStatsRegistry statData) {
        int modelData = (int) (statData.getAd() + statData.getAp() + statData.getHp());
        setCustomModelData(item, modelData);
    }

    private static String formatStat(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.format("%.1f", value);
        }
    }

    public static double getStat(ItemStack item, String statType) {
        if (item == null || item.getType().isAir()) return 0;

        String itemId = getItemId(item);
        if (itemId == null) return 0;

        ItemStatsRegistry statData = ItemStatsData.getInstance().getItem(itemId);
        if (statData == null) return 0;

        return switch (statType.toUpperCase()) {
            case "HP" -> statData.getHp();
            case "HR" -> statData.getHr();

            case "AD" -> statData.getAd();
            case "AP" -> statData.getAp();

            case "TD" -> statData.getTd();
            case "AS" -> statData.getAs();
            case "AR" -> statData.getAr();
            case "MR" -> statData.getMr();
            case "LS" -> statData.getLs();
            case "CC" -> statData.getCc();
            case "SR" -> statData.getSr();
            case "MS" -> statData.getMs();
            case "APEN" -> statData.getApenFlat();
            case "APEN_PERCENT" -> statData.getApenPercent();
            case "MPEN" -> statData.getMpenFlat();
            case "MPEN_PERCENT" -> statData.getMpenPercent();
            case "CH" -> statData.getCh();
            case "TN" -> statData.getTn();

            default -> 0;
        };
    }
}