package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.ClassPassives;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import dev.ixpu.leaguemechanics.item.passives.epic.verdant_barrier;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
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
    private static final Particle.DustOptions WHITE = new Particle.DustOptions(Color.WHITE, 1.3f);

    private final LeagueMechanics plugin;

    public ClassPassiveListener(LeagueMechanics plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickSupportSpeed, SUPPORT_SPEED_CHECK_TICKS, SUPPORT_SPEED_CHECK_TICKS);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickMageRings, 2L, 2L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickShadowMarks, 4L, 4L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickAbsorptionRings, 2L, 2L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickVerdantOrbit, 2L, 2L);
    }

    private void tickVerdantOrbit() {
        verdant_barrier verdant = (verdant_barrier) ItemPassivesRegistry.getInstance().getPassive("verdant-barrier");
        if (verdant == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isDead() && verdant.isReady(player)) verdant_barrier.spawnOrbitPoint(player);
        }
    }

    private void tickShadowMarks() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ClassPassives.tickShadowMark(player);
        }
    }

    private void tickAbsorptionRings() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead() || player.getAbsorptionAmount() <= 0) continue;
            verdant_barrier.spawnOrbitPoint(player, WHITE);
        }
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
        verdant_barrier verdant = (verdant_barrier) ItemPassivesRegistry.getInstance().getPassive("verdant-barrier");
        if ((verdant != null && verdant.absorbDamage(player)) || ClassPassives.absorbMageDamage(player)) {
            event.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRagnarokKnockback(EntityKnockbackEvent event) {
        if (event.getEntity() instanceof Player player && AbilityEffectManager.getInstance().isKnockbackImmune(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ClassPassives.cleanup(event.getPlayer().getUniqueId());
    }
}