package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.ItemStatsManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class bamis_cinder implements ItemPassive {
    private static final double IMMOLATE_DAMAGE_PER_TICK = 1.5;
    private static final double IMMOLATE_RADIUS = 5.0;
    private static final int IMMOLATE_DURATION_TICKS = 60;
    private static final int IMMOLATE_TICK_INTERVAL = 10;

    private static final Color IMMOLATE_COLOR = Color.fromRGB(255, 110, 0);

    @Override
    public String getId() {
        return "bamis-cinder";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ɪᴍᴍᴏʟᴀᴛᴇ: §fTaking or dealing damage activates this passive,\n§fdealing §915 magic damage §fover §e3 seconds §fto all entities within §65 blocks§f.\n\n§790s Cooldown";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    private static final long COOLDOWN_TICKS = 1800L;

    @Override
    public void onDealDamage(Player attacker, LivingEntity target, double damage,
                             boolean isPhysical, boolean isMagic) {
        if (attacker == null) return;
        ignite(attacker);
    }

    @Override
    public void onTakeDamage(Player victim, Player attacker, double damage, boolean isMagic) {
        if (victim == null) return;
        ignite(victim);
    }

    private boolean isOnCooldown(Player player) {
        dev.ixpu.leaguemechanics.manager.ItemPassivesManager mgr =
                dev.ixpu.leaguemechanics.manager.ItemPassivesManager.getInstance();
        return mgr != null && mgr.getRemainingCooldownSeconds(player, "bamis-cinder") > 0;
    }

    private void ignite(Player source) {
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return;
        if (isOnCooldown(source)) return;

        spawnImmolateBurst(source.getLocation());

        new BukkitRunnable() {
            int elapsed = 0;
            @Override
            public void run() {
                if (!source.isOnline() || elapsed >= IMMOLATE_DURATION_TICKS) {
                    cancel();
                    return;
                }

                Location sourceLoc = source.getLocation();
                spawnImmolateRing(sourceLoc, elapsed);

                for (Entity entity : source.getWorld().getNearbyEntities(
                        sourceLoc, IMMOLATE_RADIUS, IMMOLATE_RADIUS, IMMOLATE_RADIUS,
                        e -> e instanceof LivingEntity && !e.getUniqueId().equals(source.getUniqueId()))) {
                    if (!(entity instanceof LivingEntity nearby)) continue;
                    double damageToApply = immolateDamage(source, nearby);
                    applyMagicDamage(nearby, damageToApply, source);
                }

                elapsed += IMMOLATE_TICK_INTERVAL;
            }
        }.runTaskTimer(plugin, 0L, IMMOLATE_TICK_INTERVAL);

        new BukkitRunnable() {
            @Override
            public void run() {
                dev.ixpu.leaguemechanics.manager.ItemPassivesManager mgr =
                        dev.ixpu.leaguemechanics.manager.ItemPassivesManager.getInstance();
                if (mgr != null) {
                    mgr.setCooldown(source, "bamis-cinder", (int) COOLDOWN_TICKS);
                }
            }
        }.runTaskLater(plugin, IMMOLATE_DURATION_TICKS);
    }

    private double immolateDamage(Player source, Entity target) {
        ItemStatsManager statsManager = LeagueMechanics.getInstance().getStatsManager();
        DamageManager damageManager = new DamageManager(statsManager);
        damageManager.enableOnlyAP();
        return damageManager.DamageCalculation(source, target, 0, 0, 0);
    }

    private void applyMagicDamage(LivingEntity target, double damage, Player source) {
        if (target.isDead() || target.getHealth() <= 0) return;

        if (target instanceof Player targetPlayer) {
            double absorption = targetPlayer.getAbsorptionAmount();
            if (damage > absorption) {
                damage -= absorption;
                targetPlayer.setAbsorptionAmount(0);
            } else {
                targetPlayer.setAbsorptionAmount(absorption - damage);
                damage = 0;
            }
        }

        double newHealth = Math.clamp(target.getHealth() - damage, 0, target.getMaxHealth());

        if (target instanceof Player targetPlayer) {
            KillSourceTracker.getInstance().setSource(targetPlayer, source);
        }

        target.damage(0.00001);
        target.setHealth(newHealth);
    }

    private void spawnImmolateRing(Location center, int elapsed) {
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return;

        int[] taskId = { -1 };
        taskId[0] = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            private int tick = 0;
            private final int maxTicks = 12;

            @Override
            public void run() {
                if (tick >= maxTicks) {
                    plugin.getServer().getScheduler().cancelTask(taskId[0]);
                    return;
                }
                double progress = (double) tick / maxTicks;
                double radius = IMMOLATE_RADIUS * progress;

                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 6.0) {
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    Location particleLoc = new Location(center.getWorld(), x, center.getY() + 0.5, z);

                    center.getWorld().spawnParticle(
                            Particle.DUST,
                            particleLoc,
                            1,
                            0.1, 0.1, 0.1,
                            new Particle.DustOptions(IMMOLATE_COLOR, 1.0f)
                    );
                }

                center.getWorld().spawnParticle(
                        Particle.FLAME,
                        center.clone().add(0, 0.5, 0),
                        3,
                        0.2, 0.3, 0.2,
                        0.01
                );

                tick++;
            }
        }, 0L, 1L);
    }

    private void spawnImmolateBurst(Location center) {
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            center.getWorld().playEffect(center, org.bukkit.Effect.MOBSPAWNER_FLAMES, 1);
            center.getWorld().spawnParticle(
                    Particle.DUST,
                    center.clone().add(0, 0.3, 0),
                    30,
                    IMMOLATE_RADIUS * 0.4, 0.5, IMMOLATE_RADIUS * 0.4,
                    new Particle.DustOptions(IMMOLATE_COLOR, 1.5f)
            );
            center.getWorld().spawnParticle(
                    Particle.FLAME,
                    center.clone().add(0, 0.5, 0),
                    20,
                    IMMOLATE_RADIUS * 0.3, 1.0, IMMOLATE_RADIUS * 0.3,
                    0.02
            );
        }, 1L);
    }
}
