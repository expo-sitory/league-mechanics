package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.ItemPassivesManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class cull implements ItemPassive {
    private static final int XP_PER_KILL = 10;
    private static final int MAX_XP = 500;
    private static final int UPGRADE_THRESHOLD = 100;
    private static final int UPGRADED_XP = 200;

    @Override
    public String getId() {
        return "cull";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ʀᴇᴀᴘ: §fKilling an entity §fgrants an additional\n§a" + XP_PER_KILL + " xp§f, up to a maximum of §a" + MAX_XP + " §fAfter having killed " + UPGRADE_THRESHOLD + "\n§fentities§f, grants an additional §a" + UPGRADED_XP + " xp§f and breaks this item.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {
        if (ItemPassivesManager.getInstance().isPassiveDisabled(player, getId())) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        NamespacedKey killsKey = new NamespacedKey(LeagueMechanics.getInstance(), "cull_kills");
        PersistentDataContainer data = meta.getPersistentDataContainer();
        int kills = data.getOrDefault(killsKey, PersistentDataType.INTEGER, 0) + 1;

        if (kills >= UPGRADE_THRESHOLD) {
            player.giveExp(UPGRADED_XP);
            breakItem(player, item);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return;
        }

        data.set(killsKey, PersistentDataType.INTEGER, kills);
        item.setItemMeta(meta);

        if (kills * XP_PER_KILL <= MAX_XP) {
            player.giveExp(XP_PER_KILL);
        }
    }

    private void breakItem(Player player, ItemStack item) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].equals(item)) {
                player.getInventory().setItem(i, null);
                return;
            }
        }
        item.setAmount(0);
    }
}