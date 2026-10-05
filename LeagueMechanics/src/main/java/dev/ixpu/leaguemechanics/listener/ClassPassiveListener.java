package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.ClassPassives;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

public class ClassPassiveListener implements Listener {
    private static final long SUPPORT_SPEED_CHECK_TICKS = 10L;

    private final LeagueMechanics plugin;

    public ClassPassiveListener(LeagueMechanics plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickSupportSpeed, SUPPORT_SPEED_CHECK_TICKS, SUPPORT_SPEED_CHECK_TICKS);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickMageRings, 2L, 2L);
    }

    private void tickMageRings() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ClassPassives.tickMageRing(player);
        }
    }

    private void tickSupportSpeed() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (ClassPassives.refreshSupportSpeed(player)) {
                plugin.getPlayerEventListener().applyPlayerStats(player);
            }
        }
    }

    @EventHandler
    public void onSupportTransfer(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;
        if (!event.getPlayer().getInventory().getItemInMainHand().getType().isAir()) return;
        ClassPassives.transferHealth(event.getPlayer(), target);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMageDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getDamage() <= 0) return;

        switch (event.getCause()) {
            case VOID, SUICIDE, KILL -> {
                return;
            }
            default -> {
            }
        }

        if (ClassPassives.absorbMageDamage(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ClassPassives.cleanup(event.getPlayer().getUniqueId());
    }
}