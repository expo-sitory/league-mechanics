package dev.ixpu.leaguemechanics.gui;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.player.PlayerClass;
import dev.ixpu.leaguemechanics.player.PlayerClassType;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ClassSelectionGUI {
    private static ClassSelectionGUI instance;
    private static final String INVENTORY_TITLE = "§8§lsᴇʟᴇᴄᴛ ᴀ ᴄʟᴀss";

    private static final int[] CLASS_SLOTS = {10, 11, 12, 14, 15, 16};

    public static ClassSelectionGUI getInstance() {
        if (instance == null) {
            instance = new ClassSelectionGUI();
        }
        return instance;
    }

    public static String getInventoryTitle() {
        return INVENTORY_TITLE;
    }

    public static void openForPlayer(Player player) {
        getInstance().openForPlayerInstance(player);
    }

    public static boolean hasPlayerSelectedClass(Player player) {
        return PlayerClass.getPlayerClass(player) != null;
    }

    private void openForPlayerInstance(Player player) {
        Inventory inventory = Bukkit.createInventory(null, InventoryType.CHEST, INVENTORY_TITLE);

        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = blackGlass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            blackGlass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, blackGlass);
        }

        PlayerClassType[] classes = PlayerClassType.values();
        for (int i = 0; i < classes.length && i < CLASS_SLOTS.length; i++) {
            inventory.setItem(CLASS_SLOTS[i], createClassItem(classes[i]));
        }

        player.openInventory(inventory);
    }

    public void handleClick(Player player, int slot) {
        PlayerClassType[] classes = PlayerClassType.values();

        for (int i = 0; i < classes.length && i < CLASS_SLOTS.length; i++) {
            if (slot == CLASS_SLOTS[i]) {
                PlayerClass.setPlayerClass(player, classes[i]);
                LeagueMechanics.getInstance().getPlayerEventListener().applyPlayerStats(player);
                player.closeInventory();
                player.sendMessage(net.kyori.adventure.text.Component.text("§a✓ Class set to §e" + classes[i].getDisplayName() + "§a!"));
                return;
            }
        }
    }

    private ItemStack createClassItem(PlayerClassType classType) {
        ItemStack item = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§e§l" + classType.getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add("§7This class grants the following base stats:");
            lore.add("");

            PlayerClass.ClassStats stats = PlayerClass.getClassStats(classType);
            if (stats != null) {
                if (stats.baseAD() > 0) {
                    lore.add("§6🗡 " + formatStat(stats.baseAD()) + " §fAttack Damage");
                }
                if (stats.baseAP() > 0) {
                    lore.add("§9☄ " + formatStat(stats.baseAP()) + " §fAbility Power");
                }
                if (stats.baseHP() > 0) {
                    lore.add("§a❤ " + formatStat(stats.baseHP()) + " §fHealth");
                }
                if (stats.baseAR() > 0) {
                    lore.add("§e🛡 " + formatStat(stats.baseAR()) + " §fArmor");
                }
                if (stats.baseMR() > 0) {
                    lore.add("§b⦿ " + formatStat(stats.baseMR()) + " §fMagic Resist");
                }
                if (stats.baseAS() > 0) {
                    lore.add("§c➺ " + formatStat(stats.baseAS() * 100) + "% §fAttack Speed");
                }
            }

            lore.add("");
            lore.add("§a§l[ ꜱᴇʟᴇᴄᴛ ᴛʜɪꜱ ᴄʟᴀꜱꜱ ]");

            meta.setLore(lore);
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);

            String model = dev.ixpu.leaguemechanics.item.shop.ItemShopData.getInstance().getClassModel(classType.getId());
            if (model != null) {
                ItemModifier.setItemModel(item, model);
            }
        }

        return item;
    }

    private String formatStat(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.format("%.1f", value);
        }
    }
}
