package dev.ixpu.leaguemechanics.gui;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.player.PlayerClass;
import dev.ixpu.leaguemechanics.player.PlayerClassType;
import dev.ixpu.leaguemechanics.player.PlayerRuneData;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.util.ItemModifier;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class InspectGUI {
    private static final String INVENTORY_TITLE = "§8§lɪɴsᴘᴇᴄᴛ";

    public static void openInspect(Player viewer, Player target) {
        new InspectGUI().openInspectInstance(viewer, target);
    }

    public static String getInventoryTitle() {
        return INVENTORY_TITLE;
    }

    private void openInspectInstance(Player viewer, Player target) {
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

        inventory.setItem(10, createPlayerHead(target));

        addLeagueItems(inventory, target);

        viewer.openInventory(inventory);
    }

    private void addLeagueItems(Inventory inventory, Player player) {
        int slot = 11;
        int count = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (slot > 16 || count >= 6) break;

            if (item != null && !item.getType().isAir()) {
                String itemId = ItemModifier.getItemId(item);
                if (itemId != null) {
                    inventory.setItem(slot, item.clone());
                    slot++;
                    count++;
                }
            }
        }
    }

    private ItemStack createPlayerHead(Player target) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.setDisplayName("§6" + target.getName());

            List<String> lore = new ArrayList<>();

            PlayerClassType classType = PlayerClass.getPlayerClass(target);
            if (classType != null) {
                lore.add("§7ᴄʟᴀss: §e" + classType.getDisplayName());
            } else {
                lore.add("§7ᴄʟᴀss: §cnone");
            }

            PlayerRuneData runeData = LeagueMechanics.getInstance().getRuneManager().getPlayerRuneData(target);
            if (runeData != null) {
                RunePath primaryPath = runeData.getPrimaryPath();
                if (primaryPath != null) {
                    lore.add("§7ᴘʀɪᴍᴀʀʏ: §e" + primaryPath.getId());

                    CooldownHandler keystone = runeData.getKeystoneRune();
                    if (keystone != null) {
                        lore.add("§7  ᴋᴇʏsᴛᴏɴᴇ: §e" + keystone.getId());
                    }

                    StringBuilder primarySlots = new StringBuilder();
                    CooldownHandler p1 = runeData.getPrimarySlot1Rune();
                    CooldownHandler p2 = runeData.getPrimarySlot2Rune();
                    CooldownHandler p3 = runeData.getPrimarySlot3Rune();
                    appendRune(primarySlots, p1);
                    appendRune(primarySlots, p2);
                    appendRune(primarySlots, p3);
                    if (primarySlots.length() > 0) {
                        lore.add("§7  sʟᴏᴛs: §e" + primarySlots.toString());
                    }
                } else {
                    lore.add("§7ᴘʀɪᴍᴀʀʏ: §cnone");
                }

                RunePath secondaryPath = runeData.getSecondaryPath();
                if (secondaryPath != null) {
                    lore.add("§7sᴇᴄᴏɴᴅᴀʀʏ: §e" + secondaryPath.getId());

                    StringBuilder secondarySlots = new StringBuilder();
                    CooldownHandler s1 = runeData.getSecondarySlot1Rune();
                    CooldownHandler s2 = runeData.getSecondarySlot2Rune();
                    appendRune(secondarySlots, s1);
                    appendRune(secondarySlots, s2);
                    if (secondarySlots.length() > 0) {
                        lore.add("§7  sʟᴏᴛs: §e" + secondarySlots.toString());
                    }
                } else {
                    lore.add("§7sᴇᴄᴏɴᴅᴀʀʏ: §cnone");
                }
            } else {
                lore.add("§7ʀᴜɴᴇs: §cnone");
            }

            meta.setLore(lore);
            playerHead.setItemMeta(meta);
        }
        return playerHead;
    }

    private void appendRune(StringBuilder sb, CooldownHandler rune) {
        if (rune == null) return;
        if (sb.length() > 0) sb.append("|");
        sb.append(rune.getId());
    }
}
