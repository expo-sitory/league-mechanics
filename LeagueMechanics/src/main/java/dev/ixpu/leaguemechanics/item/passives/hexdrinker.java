package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.ItemPassivesManager;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.util.Icons;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class hexdrinker implements ItemPassive {
    private static final double LIFELINE_THRESHOLD_PERCENT = 0.30;
    private static final double LIFELINE_MR_BONUS_PERCENT = 0.60;
    private static final long LIFELINE_DURATION_TICKS = 200L;
    private static final int LIFELINE_COOLDOWN_TICKS = 1800;

    @Override
    public String getId() {
        return "hexdrinker";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ʟɪғᴇʟɪɴᴇ: §fTaking damage that would reduce you below 30%\n§fof your maximum health temporarily increases your " + Icons.get("stats.mr") + " §bMagic Resistance\n§fby 60% for 10 seconds.\n\n§790s Cooldown";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onTakeDamage(Player victim, Player attacker, double damage, boolean isMagic) {
        if (victim == null || damage <= 0) return;

        ItemPassivesManager manager = ItemPassivesManager.getInstance();
        if (manager.isOnCooldown(victim, getId())) return;

        double maxHp = victim.getMaxHealth();
        double currentHp = victim.getHealth();
        double threshold = maxHp * LIFELINE_THRESHOLD_PERCENT;

        if (currentHp - damage < threshold) {
            PlayerStats stats = PlayerStats.getOrCreate(victim);
            double currentMR = stats.getPlayerMR(victim);
            double mrBonus = currentMR * LIFELINE_MR_BONUS_PERCENT;
            stats.modifyMR(mrBonus);

            manager.setCooldown(victim, getId(), LIFELINE_COOLDOWN_TICKS);

            LeagueMechanics plugin = LeagueMechanics.getInstance();
            if (plugin == null) return;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!victim.isOnline()) {
                        cancel();
                        return;
                    }
                    PlayerStats s = PlayerStats.getOrCreate(victim);
                    s.modifyMR(-mrBonus);
                    plugin.getPlayerEventListener().applyPlayerStats(victim);
                }
            }.runTaskLater(plugin, LIFELINE_DURATION_TICKS);
        }
    }
}
