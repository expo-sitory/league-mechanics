package dev.ixpu.leaguemechanics.rune.keystones.sorcery;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import dev.ixpu.leaguemechanics.util.DebugLogger;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;

import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.manager.StatScalingManager;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dev.ixpu.leaguemechanics.util.ItemModifier;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;


import net.kyori.adventure.text.Component;

public class DeathfireTorch extends CooldownHandler {

    private double BASE_MAGIC_DAMAGE;

    private double AD_PERCENTAGE_MULTIPLIER;
    private double AP_PERCENTAGE_MULTIPLIER;

    private PlayerEventListener listener;

    private static final double DAMAGE_TICKS = 20;

    private final Map<UUID, Map<UUID, Integer>> burnedPlayers = new HashMap<>();
    private final Map<UUID, Map<UUID, Double>> burnDamage = new HashMap<>();
    private final Map<UUID, Map<UUID, LivingEntity>> burnedTargets = new HashMap<>();
    private LeagueMechanics plugin;

    public DeathfireTorch(ConfigurationSection config, PlayerEventListener listener) {
        super("deathfire-torch", RunePath.SORCERY, RuneSlot.KEYSTONE);
        ConfigurationSection section = config.getConfigurationSection("runes.keystones.sorcery.deathfire-torch");
        this.listener = listener;
        if (section != null) {
            this.BASE_MAGIC_DAMAGE = section.getDouble("base-magic-damage", this.BASE_MAGIC_DAMAGE);
            this.AD_PERCENTAGE_MULTIPLIER = section.getDouble("ad-percentage-multiplier", this.AD_PERCENTAGE_MULTIPLIER);
            this.AP_PERCENTAGE_MULTIPLIER = section.getDouble("ap-percentage-multiplier", this.AP_PERCENTAGE_MULTIPLIER);
        }
    }

    @Override
    public void onEnable(Player player) {
        UUID uuid = player.getUniqueId();
        burnedPlayers.put(uuid, new HashMap<>());
        burnDamage.put(uuid, new HashMap<>());
        burnedTargets.put(uuid, new HashMap<>());
    }

    @Override
    public void onDisable(Player player) {
        UUID uuid = player.getUniqueId();
        burnedPlayers.remove(uuid);
        burnDamage.remove(uuid);
        burnedTargets.remove(uuid);
    }

    public void onProjectileHit(Player shooter, Entity target) {
        triggerDeathFireTorch(shooter, target);
    }

    public void onAttack(Player attacker, Entity target) {
        triggerDeathFireTorch(attacker, target);
    }

    public void triggerDeathFireTorch(Player player, Entity target) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget.getMaxHealth() < 20) {
            return;
        }
        if (!CheckEnchant(weapon)) {
            return;
        }
        if(!listener.letRunesThrough(player)) {
            return;
        }

        applyBurn(player, livingTarget, keystoneDamage(player, target));
    }

    private double keystoneDamage(Player player, Entity target) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableOnlyAP();
        double baseDamage = damageManager.DamageCalculation(player, target, 0, 0, 0, BASE_MAGIC_DAMAGE);
        double scaledBonus = getScaledBonusDamage(player);

        return baseDamage + scaledBonus;
    }

    private double getScaledBonusDamage(Player player) {
        StatScalingManager statScalingManager = new StatScalingManager();
        return statScalingManager.calculateScaledValue(
                player,
                0,
                AD_PERCENTAGE_MULTIPLIER,
                AP_PERCENTAGE_MULTIPLIER
        );
    }

    private void applyBurn(Player attacker, LivingEntity victim, double burnDamagePerTick) {
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        int enchantLevel = getFireEnchantLevel(weapon);
        int burnDuration = 50 * enchantLevel;

        UUID attackerUUID = attacker.getUniqueId();
        UUID victimUUID = victim.getUniqueId();

        Map<UUID, Integer> burned = burnedPlayers.get(attackerUUID);
        Map<UUID, Double> damages = burnDamage.get(attackerUUID);
        Map<UUID, LivingEntity> targets = burnedTargets.get(attackerUUID);

        if (burned == null || damages == null || targets == null) {
            return;
        }

        burned.put(victimUUID, burnDuration);
        damages.put(victimUUID, burnDamagePerTick);
        targets.put(victimUUID, victim);

        for (ItemStack inv : attacker.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) continue;
            String itemId = ItemModifier.getItemId(inv);
            ItemPassive passive = ItemPassivesRegistry.getInstance().getPassive(itemId);
            if (passive != null) {
                passive.onDealDamage(attacker, victim, burnDamagePerTick, false, true);
            }
        }

        attacker.playSound(victim.getLocation(), org.bukkit.Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.8f);
    }

    private int getFireEnchantLevel(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        return meta.getEnchants().entrySet().stream()
                .filter(e -> {
                    String enchantName = e.getKey().toString().toLowerCase();
                    return enchantName.contains("fire_aspect") || enchantName.contains("flame");
                })
                .mapToInt(Map.Entry::getValue)
                .findFirst()
                .orElse(0);
    }

    public void clearBurnForTarget(UUID victimUUID) {
        for (Map<UUID, Integer> burned : burnedPlayers.values()) {
            burned.remove(victimUUID);
        }
        for (Map<UUID, Double> damages : burnDamage.values()) {
            damages.remove(victimUUID);
        }
        for (Map<UUID, LivingEntity> targets : burnedTargets.values()) {
            targets.remove(victimUUID);
        }
    }

    private boolean CheckEnchant(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        return meta.getEnchants().keySet().stream()
                .anyMatch(e -> {
                    String enchantName = e.toString().toLowerCase();
                    return enchantName.contains("fire_aspect") || enchantName.contains("flame");
                });
    }

    private void spawnBurnParticles(LivingEntity victim) {
        Location loc = victim.getLocation().add(0, 1, 0);
        victim.getWorld().spawnParticle(
                Particle.DUST,
                loc,
                20,
                0.2, 0.2, 0.2,
                new Particle.DustOptions(Color.fromRGB(0, 150, 255), 1.0f)
        );
    }


    @Override
    public void tick(Player player) {
        UUID playerUUID = player.getUniqueId();

        Map<UUID, Integer> burned = burnedPlayers.get(playerUUID);
        Map<UUID, Double> damages = burnDamage.get(playerUUID);
        Map<UUID, LivingEntity> targets = burnedTargets.get(playerUUID);

        if (burned == null || damages == null || targets == null) {
            String runeDisplay = getRuneDisplay(RuneState.IDLE, 0);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        java.util.ArrayList<UUID> toRemove = new java.util.ArrayList<>();
        for (UUID targetUUID : new java.util.ArrayList<>(burned.keySet())) {
            int duration = burned.getOrDefault(targetUUID, 0);

            if (duration > 0) {
                duration--;
                burned.put(targetUUID, duration);

                if (duration % DAMAGE_TICKS == 0) {
                    LivingEntity target = targets.get(targetUUID);
                    if (target != null && target.isValid()) {
                        double damagePerTick = damages.getOrDefault(targetUUID, 0.0);

                        if (target instanceof Player targetPlayer) {
                            double absorption = targetPlayer.getAbsorptionAmount();
                            if (damagePerTick > absorption) {
                                damagePerTick -= absorption;
                                targetPlayer.setAbsorptionAmount(0);
                            } else {
                                targetPlayer.setAbsorptionAmount(absorption - damagePerTick);
                                damagePerTick = 0;
                            }
                        }

                        double newHealth = Math.max(0, target.getHealth() - damagePerTick);
                        DebugLogger.debug(player, "§f[§dSource§f] §f[§9Deathfire Torch§f] Keystone Damage = §d" + Math.ceil(damagePerTick * 100) / 100.0);
                        DebugLogger.debug(player, "§f[§dSource§f] Keystone Damage Type = §dMagic Damage");
                        DebugLogger.debug(player, "§f[§dTarget§f] Target New HP = §d" + Math.ceil(newHealth * 100) / 100.0);
                        if (target instanceof Player targetPlayer) {
                            KillSourceTracker.getInstance().setSource(targetPlayer, player);
                        }
                        target.setHealth(newHealth);
                        spawnBurnParticles(target);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 2.0f);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 0.7f);
                    }
                }

                if (duration <= 0) {
                    toRemove.add(targetUUID);
                }
            } else {
                toRemove.add(targetUUID);
            }
        }

        for (UUID targetUUID : toRemove) {
            burned.remove(targetUUID);
            damages.remove(targetUUID);
            targets.remove(targetUUID);
        }

        int burnCount = burned.size();
        RuneState state = burnCount > 0 ? RuneState.ACTIVE : RuneState.IDLE;
        String runeDisplay = getRuneDisplay(state, burnCount);
        setPlayerDisplay(player, runeDisplay);
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);


        String statsDisplay = playerStats.getActionBarSections(player);
        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        ACTIVE, IDLE
    }

    private String getRuneDisplay(RuneState state, int victimCount) {
        return switch (state) {
            case ACTIVE -> "§9🔥 (" + victimCount + ")";
            case IDLE -> "§1🔥";
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        UUID playerUUID = player.getUniqueId();
        Map<UUID, Integer> burned = burnedPlayers.get(playerUUID);
        int burnCount = (burned != null) ? burned.size() : 0;
        RuneState state = burnCount > 0 ? RuneState.ACTIVE : RuneState.IDLE;
        return getRuneDisplay(state, burnCount);
    }

}