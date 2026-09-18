package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.manager.ItemPassivesManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        return "§7ᴜɴɪQᴜᴇ – ʀᴇᴀᴘ: §fKilling an §eentity §fgrants an\n§fadditional §a" + XP_PER_KILL + " xp§f, up to a maximum of §a" + MAX_XP + "§f.\n§fAfter having killed §e" + UPGRADE_THRESHOLD + " entities§f, grants an\n§fadditional §a" + UPGRADED_XP + " xp§f and breaks this item.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {
        ItemPassivesManager manager = ItemPassivesManager.getInstance();

        if (manager.isPassiveDisabled(player, getId())) {
            return;
        }

        int killCount = manager.getKillCount(player, getId());
        manager.addKill(player, getId());

        if (killCount >= UPGRADE_THRESHOLD) {
            player.giveExp(UPGRADED_XP);
            item.setAmount(0);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        } else {
            int totalXpGained = killCount * XP_PER_KILL;
            if (totalXpGained < MAX_XP) {
                player.giveExp(XP_PER_KILL);
            }
        }
    }
}