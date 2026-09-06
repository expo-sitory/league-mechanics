package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.player.PlayerStats;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class recurve_bow implements ItemPassive {
    private static final double STING_BONUS_AD = 15.0;
    private static final long STING_DURATION_TICKS = 30L; 

    private static final Map<UUID, BukkitTask> activeRecurveTasks = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "recurve-bow";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – sᴛɪɴɢ: §fAttacks grant §6+15 bonus attack\n§6damage §ffor §e1.5 seconds§f, refreshed on each hit.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onDealDamage(Player attacker, LivingEntity target, double damage,
                             boolean isPhysical, boolean isMagic) {
        if (attacker == null || !(attacker instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        PlayerStats stats = PlayerStats.getOrCreate(player);

        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return;

        BukkitTask existingTask = activeRecurveTasks.remove(uuid);
        if (existingTask != null) {
            existingTask.cancel();
        } else {
            stats.modifyAD(STING_BONUS_AD);
        }

        BukkitTask newTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    activeRecurveTasks.remove(uuid);
                    return;
                }
                PlayerStats s = PlayerStats.getOrCreate(player);
                s.modifyAD(-STING_BONUS_AD);
                plugin.getPlayerEventListener().applyPlayerStats(player);
                activeRecurveTasks.remove(uuid);
            }
        }.runTaskLater(plugin, STING_DURATION_TICKS);

        activeRecurveTasks.put(uuid, newTask);
    }
}