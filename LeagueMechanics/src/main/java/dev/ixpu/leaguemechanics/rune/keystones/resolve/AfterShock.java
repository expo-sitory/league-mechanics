package dev.ixpu.leaguemechanics.rune.keystones.resolve;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.util.DebugLogger;
import dev.ixpu.leaguemechanics.player.PlayerStats;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import dev.ixpu.leaguemechanics.rune.RuneCooldownGate;

import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.manager.ItemStatsManager;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;

import java.util.*;

public class AfterShock extends CooldownHandler {

    private RuneCooldownGate listener;

    private double baseArmor = 45.0;
    private double baseMagicResist = 45.0;
    private double bonusArmorPercent = 75.0;
    private double bonusMagicResistPercent = 75.0;
    private double baseShockwaveDamage = 3.5;
    private double shockwaveBonusHpPercent = 8.0;


    private int COOLDOWN_DURATION_SECONDS = 20;

    private static final double shockwaveRadius = 5.0;
    private static final double triggerThresholdPercent = 30.0;
    private static final int buffDurationTicks = 50;


    private final Map<UUID, Integer> effectTaskIds = new HashMap<>();
    private final Map<UUID, Integer> effectRemainingTicks = new HashMap<>();
    private final Map<UUID, Double> lastArmorBonus = new HashMap<>();
    private final Map<UUID, Double> lastMRBonus = new HashMap<>();

    public AfterShock(ConfigurationSection config, PlayerEventListener listener) {
        super("after-shock", RunePath.RESOLVE, RuneSlot.KEYSTONE);
        this.listener = listener;

        ConfigurationSection section = config.getConfigurationSection("runes.keystones.resolve.aftershock");
        if (section != null) {
            this.baseArmor = section.getDouble("base-armor", this.baseArmor);
            this.baseMagicResist = section.getDouble("base-magic-resist", this.baseMagicResist);
            this.bonusArmorPercent = section.getDouble("bonus-armor-percent", this.bonusArmorPercent);
            this.bonusMagicResistPercent = section.getDouble("bonus-magic-resist-percent", this.bonusMagicResistPercent);
            this.baseShockwaveDamage = section.getDouble("base-shockwave-damage", this.baseShockwaveDamage);
            this.shockwaveBonusHpPercent = section.getDouble("shockwave-bonus-hp-percent", this.shockwaveBonusHpPercent);
            this.COOLDOWN_DURATION_SECONDS = section.getInt("cooldown", this.COOLDOWN_DURATION_SECONDS);
        }

        this.setCooldownSeconds(COOLDOWN_DURATION_SECONDS);
    }

    @Override
    public void onEnable(Player player) {
        UUID uuid = player.getUniqueId();
        effectTaskIds.remove(uuid);
        effectRemainingTicks.remove(uuid);
    }

    @Override
    public void onDisable(Player player) {
        cancelEffect(player);
    }

    @Override
    public void onPlayerDamage(Player player, double damage) {
        activateAfterShock(player);
    }

    private void activateAfterShock(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (effectRemainingTicks.getOrDefault(playerUUID, 0) > 0) {
            return;
        }
        if (isOnCooldown(player)) {
            return;
        }
        if (isHotbarCooldownBlocking(listener, player)) {
            return;
        }
        doActivateAfterShock(player);
    }

    static boolean isHotbarCooldownBlocking(RuneCooldownGate gate, Player player) {
        return gate.isAnyHotbarOnCooldown(player) && !gate.letRunesThrough(player);
    }

    private void doActivateAfterShock(Player player) {
        UUID playerUUID = player.getUniqueId();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();
        double healthPercent = (currentHealth / maxHealth) * 100.0;

        if (healthPercent > triggerThresholdPercent) {
            return;
        }
        applyResistances(player);
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        effectRemainingTicks.put(playerUUID, buffDurationTicks);

        int[] taskId = { -1 };
        taskId[0] = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            int tick = buffDurationTicks;

            @Override
            public void run() {
                tick--;
                effectRemainingTicks.put(playerUUID, tick);

                if (tick <= 0) {
                    releaseShockwave(player);
                    plugin.getServer().getScheduler().cancelTask(taskId[0]);
                    effectTaskIds.remove(playerUUID);
                    effectRemainingTicks.remove(playerUUID);
                    resetCooldown(player);
                }
            }
        }, 0L, 1L);

        effectTaskIds.put(playerUUID, taskId[0]);

        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.5f);
    }

    private void applyResistances(Player player) {
        UUID playerUUID = player.getUniqueId();
        clearResistances(player);
        double attributeArmor = player.getAttribute(Attribute.ARMOR).getValue();
        double bonusArmorBonus = baseArmor + (bonusArmorPercent / 100.0 * Math.max(0, attributeArmor));

        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        double itemMR = (itemStatsManager != null) ? itemStatsManager.getItemMR(player) : 0.0;
        double bonusMRBonus = baseMagicResist + (bonusMagicResistPercent / 100.0 * itemMR);
        lastArmorBonus.put(playerUUID, bonusArmorBonus);
        lastMRBonus.put(playerUUID, bonusMRBonus);
        PlayerStats stats = PlayerStats.getOrCreate(player);
        stats.modifyAR(bonusArmorBonus);
        stats.modifyMR(bonusMRBonus);
    }

    private void clearResistances(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerStats stats = PlayerStats.getOrCreate(player);
        Double armor = lastArmorBonus.remove(playerUUID);
        Double mr = lastMRBonus.remove(playerUUID);
        if (armor != null) {
            stats.modifyAR(-armor);
        }
        if (mr != null) {
            stats.modifyMR(-mr);
        }
    }

    private void releaseShockwave(Player player) {
        clearResistances(player);

        Location playerLoc = player.getLocation();
        Collection<Entity> nearby = playerLoc.getWorld().getNearbyEntities(
                playerLoc,
                shockwaveRadius,
                shockwaveRadius,
                shockwaveRadius
        );

        int hitCount = 0;
        for (Entity entity : nearby) {
            if (entity.equals(player)) continue;
            if (!(entity instanceof LivingEntity living)) continue;

            double damageToApply = keystoneDamage(player, living);

            applyMagicDamage(living, damageToApply, player);

            DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] §f[§aAfter Shock§f] Keystone Damage = §d" + String.format("%.1f", damageToApply));

            hitCount++;
        }

        spawnShockwaveEffect(playerLoc);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_WARDEN_ROAR, 1.0f, 1.0f);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
    }

    private double keystoneDamage(Player player, Entity target) {
        double totalHP = PlayerStats.getOrCreate(player).getPlayerHP(player);
        double bonusHP = Math.max(0, totalHP - 20.0);
        double baseComponent = baseShockwaveDamage + (shockwaveBonusHpPercent / 100.0 * bonusHP);

        double targetMR = getTargetMR(target);
        double mitigatedBase = baseComponent / (1.0 + (targetMR / 100.0));

        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableOnlyAP();
        double apComponent = damageManager.DamageCalculation(player, target, 0, 0, 0);

        return mitigatedBase + apComponent;
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

    private double getTargetMR(Entity target) {
        if (!(target instanceof Player targetPlayer)) {
            return 0;
        }
        return PlayerStats.getOrCreate(targetPlayer).getPlayerMR(targetPlayer);
    }

    private void spawnShockwaveEffect(Location center) {
        int[] taskId = { -1 };
        taskId[0] = LeagueMechanics.getInstance().getServer().getScheduler().scheduleSyncRepeatingTask(
                LeagueMechanics.getInstance(),
                new Runnable() {
                    int tick = 0;
                    final int maxTicks = 15;

                    @Override
                    public void run() {
                        if (tick >= maxTicks) {
                            LeagueMechanics.getInstance().getServer().getScheduler().cancelTask(taskId[0]);
                            return;
                        }

                        double progress = (double) tick / maxTicks;
                        double radius = shockwaveRadius * progress;

                        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8.0) {
                            double x = center.getX() + radius * Math.cos(angle);
                            double z = center.getZ() + radius * Math.sin(angle);
                            Location particleLoc = new Location(center.getWorld(), x, center.getY() + 0.5, z);

                            center.getWorld().spawnParticle(
                                    Particle.DUST,
                                    particleLoc,
                                    1,
                                    0.1, 0.1, 0.1,
                                    new Particle.DustOptions(Color.LIME, 1.0f)
                            );
                        }

                        tick++;
                    }
                },
                0L, 1L
        );
    }

    private void cancelEffect(Player player) {
        UUID uuid = player.getUniqueId();
        Integer taskId = effectTaskIds.remove(uuid);
        if (taskId != null && taskId != -1) {
            LeagueMechanics.getInstance().getServer().getScheduler().cancelTask(taskId);
        }
        effectRemainingTicks.remove(uuid);
        clearResistances(player);
    }

    @Override
    public void tick(Player player) {
        UUID playerUUID = player.getUniqueId();
        int remainingTicks = effectRemainingTicks.getOrDefault(playerUUID, 0);

        if (remainingTicks > 0) {
            String runeDisplay = getRuneDisplay(RuneState.ACTIVE, player, remainingTicks);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        if (isOnCooldown(player)) {
            String runeDisplay = getRuneDisplay(RuneState.COOLDOWN, player, 0);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        String runeDisplay = getRuneDisplay(RuneState.IDLE, player, 0);
        setPlayerDisplay(player, runeDisplay);
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);

        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        ACTIVE, COOLDOWN, IDLE
    }

    private String getRuneDisplay(RuneState state, Player player, int remainingTicks) {
        return switch (state) {
            case ACTIVE -> {
                double remainingSeconds = remainingTicks / 20.0;
                yield String.format("§a🌀 (%.1fs)", remainingSeconds);
            }
            case COOLDOWN -> "§7🌀 " + getCooldownDisplay(player);
            case IDLE -> "§2🌀 ";
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        UUID playerUUID = player.getUniqueId();
        int remainingTicks = effectRemainingTicks.getOrDefault(playerUUID, 0);
        if (remainingTicks > 0) {
            return getRuneDisplay(RuneState.ACTIVE, player, remainingTicks);
        }
        if (isOnCooldown(player)) {
            return getRuneDisplay(RuneState.COOLDOWN, player, 0);
        }
        return getRuneDisplay(RuneState.IDLE, player, 0);
    }
}
