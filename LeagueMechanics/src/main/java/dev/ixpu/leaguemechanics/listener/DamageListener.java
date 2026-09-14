package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;

import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.util.DebugLogger;
import dev.ixpu.leaguemechanics.player.PlayerRuneData;
import dev.ixpu.leaguemechanics.player.PlayerStats;

import dev.ixpu.leaguemechanics.manager.*;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RuneCooldownGate;
import dev.ixpu.leaguemechanics.rune.keystones.domination.HailOfBlades;
import dev.ixpu.leaguemechanics.rune.keystones.resolve.GraspOfTheUndying;
import dev.ixpu.leaguemechanics.rune.keystones.resolve.Guardian;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.bukkit.entity.*;
import org.bukkit.Sound;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DamageListener implements Listener, RuneCooldownGate {

    private final LeagueMechanics plugin;
    private final RuneManager runeManager;
    private final ItemStatsManager itemStatsManager;
    private final PlayerStatsListener playerStatsListener;
    private final CombatStateManager combatState = CombatStateManager.getInstance();

    private final Map<UUID, Long> attackCooldown = new ConcurrentHashMap<>();

    public DamageListener(LeagueMechanics plugin, PlayerStatsListener playerStatsListener) {
        this.plugin = plugin;
        this.runeManager = plugin.getRuneManager();
        this.itemStatsManager = plugin.getStatsManager();
        this.playerStatsListener = playerStatsListener;
    }

    public ItemPassive getEquippedPassive(ItemStack item) {
        return playerStatsListener.getEquippedPassive(item);
    }

    @EventHandler
    public void onProjectileDraw(PlayerLaunchProjectileEvent event) {
        Player shooter = event.getPlayer();
        if (event.getProjectile() instanceof ThrownPotion || event.getProjectile() instanceof ThrownExpBottle) {
            return;
        }
        if (isAnyHotbarOnCooldown(shooter)) {
            event.setCancelled(true);
        }
        if (isPlayerOnAttackCooldown(shooter)) {
            event.setCancelled(true);
            return;
        }
        setAttackCooldown(shooter);
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) {
            return;
        }
        if (isPlayerOnAttackCooldown(shooter)) {
            event.setCancelled(true);
            return;
        }
        if (isAnyHotbarOnCooldown(shooter)) {
            event.setCancelled(true);
        }
        combatState.removeLetRunesThrough(shooter.getUniqueId());
        setAttackCooldown(shooter);
    }

    @EventHandler
    public void onProjectileDamage(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onAttackSwing(PlayerArmSwingEvent event) {
        Player attacker = event.getPlayer();
        if (isPlayerOnAttackCooldown(attacker)) {
            event.setCancelled(true);
            return;
        }
        if (isAnyHotbarOnCooldown(attacker)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAttackDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        double damage = event.getDamage();
        event.setDamage(damage / 0.5);
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }
        if (event.getEntity() instanceof ThrownPotion || event.getEntity() instanceof ThrownExpBottle) {
            return;
        }
        if (event.getHitEntity() == null || !(event.getHitEntity() instanceof LivingEntity target)) {
            return;
        }

        if (target instanceof Player targetPlayer) {
            if (!plugin.getCommandHandler().isPvpEnabled(shooter, targetPlayer)) {
                return;
            }
        }

        combatState.addLetRunesThrough(shooter.getUniqueId());

        if (event.getEntity() instanceof Arrow) {
            ItemStack bow = shooter.getInventory().getItemInMainHand();

            if (bow.containsEnchantment(Enchantment.FLAME)) {
                target.setFireTicks(8 * 20);
            }
            if (!target.getUniqueId().equals(shooter.getUniqueId())) {
                Vector direction = target.getLocation().toVector().subtract(shooter.getLocation().toVector()).normalize();
                double knockbackPower = 0.5;

                if (bow.containsEnchantment(Enchantment.KNOCKBACK)) {
                    int knockbackLevel = bow.getEnchantmentLevel(Enchantment.KNOCKBACK);
                    knockbackPower = knockbackLevel * 0.5;
                }
                target.setVelocity(direction.multiply(knockbackPower));
            }
        }
        if (target instanceof Creature creature) {
            creature.setTarget(shooter);
        }
        shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);

        if (target instanceof Player targetPlayer) {
            recordHit(shooter, targetPlayer);
        }
        PlayerRuneData runeData = runeManager.getPlayerRuneData(shooter);

        if (runeData != null) {
            for (CooldownHandler rune : runeData.getAllRunes()) {
                if (rune == null) {
                    continue;
                }
                rune.onProjectileHit(shooter, target);
            }
        }
        event.getEntity().remove();
        damageEvent(shooter, target, "Projectile Hit Event");
        combatState.removeLetRunesThrough(shooter.getUniqueId());
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (target instanceof Player targetPlayer) {
            if (!plugin.getCommandHandler().isPvpEnabled(attacker, targetPlayer)) {
                event.setCancelled(true);
                return;
            }
        }

        if (isPlayerOnAttackCooldown(attacker)) {
            event.setCancelled(true);
            return;
        }
        if (isAnyHotbarOnCooldown(attacker)) {
            event.setCancelled(true);
            return;
        }
        if (target instanceof Creature creature) {
            creature.setTarget(attacker);
        }
        combatState.addLetRunesThrough(attacker.getUniqueId());

        PlayerRuneData runeData = runeManager.getPlayerRuneData(attacker);
        if (runeData != null) {
            for (CooldownHandler rune : runeData.getAllRunes()) {
                if (rune == null) {
                    continue;
                }
                rune.onAttack(attacker, target);
            }
        }
        if (target instanceof Player targetPlayer) {
            PlayerRuneData targetRuneData = runeManager.getPlayerRuneData(targetPlayer);
            if (targetRuneData != null) {
                CooldownHandler targetKeystoneRune = targetRuneData.getKeystoneRune();
                if (targetKeystoneRune instanceof Guardian guardian) {
                    guardian.onTakeDamage(targetPlayer);
                }
            }
            recordHit(attacker, targetPlayer);
        }
        damageEvent(attacker, target, "Melee Hit Event");
        setAttackCooldown(attacker);
        combatState.removeLetRunesThrough(attacker.getUniqueId());
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PlayerRuneData runeData = runeManager.getPlayerRuneData(player);
        if (runeData == null) {
            return;
        }

        double damage = event.getDamage();
        boolean blockedByShield = player.isBlocking();

        for (CooldownHandler rune : runeData.getAllRunes()) {
            if (rune == null) {
                continue;
            }
            rune.onPlayerDamage(player, damage);
            if (blockedByShield) {
                rune.onShieldBlock(player);
            }
            if (rune instanceof GraspOfTheUndying grasp) {
                grasp.activateGraspOfTheUndying(player, null);
            }
            if (rune instanceof HailOfBlades hailOfBlades) {
                hailOfBlades.activateHailofBlades(player, null);
            }
        }
        if (!(event.getDamager() instanceof Player)) {
            Player attacker = null;
            LivingEntity sourceMob = null;
            if (event.getDamager() instanceof Projectile projectile
                    && projectile.getShooter() instanceof Player shooter) {
                attacker = shooter;
            } else if (event.getDamager() instanceof Projectile projectile
                    && projectile.getShooter() instanceof LivingEntity mob) {
                sourceMob = mob;
            } else if (event.getDamager() instanceof LivingEntity mob) {
                sourceMob = mob;
            }
            if (sourceMob != null && !(sourceMob instanceof Player)) {
                combatState.setLastMobDamager(player, sourceMob);
            }
            boolean isMagic = event.getCause() == EntityDamageEvent.DamageCause.MAGIC
                    || event.getCause() == EntityDamageEvent.DamageCause.POISON
                    || event.getCause() == EntityDamageEvent.DamageCause.WITHER;
            for (ItemStack inv : player.getInventory().getContents()) {
                if (inv == null || inv.getType().isAir()) continue;
                ItemPassive passive = getEquippedPassive(inv);
                if (passive != null) {
                    passive.onTakeDamage(player, attacker, damage, isMagic);
                }
            }
        }
    }

    public void recordHit(Player attacker, Player victim) {
        combatState.recordHit(victim, attacker);
    }

    public boolean letRunesThrough(Player player) {
        return combatState.hasLetRunesThrough(player.getUniqueId());
    }

    public void damageEvent(Player player, LivingEntity target, String type) {
        if (target instanceof Player targetPlayer) {
            KillSourceTracker.getInstance().setSource(targetPlayer, player);
        }
        DamageManager damage = new DamageManager(itemStatsManager);
        PlayerStats stats = PlayerStats.getOrCreate(player);

        double attackerAD = stats.getPlayerAD(player);
        double attackerAP = stats.getPlayerAP(player);
        double targetAR = damage.getTargetAR(target);
        double targetMR = damage.getTargetMR(target);

        double statsDamage = damage.DamageCalculation(player, target, 0, 0, 0);

        boolean didCrit = false;
        if (statsDamage > 0) {
            double critChance = Math.round(damage.getPlayerCritChance(player) * 10.0) / 10.0;
            if (critChance > 0 && DamageManager.criticalChance(player, critChance)) {
                statsDamage *= DamageManager.getCritDamageMultiplier(player);
                didCrit = true;
            }
        }
        if (didCrit) {
            PlayerRuneData critRuneData = runeManager.getPlayerRuneData(player);
            if (critRuneData != null) {
                for (CooldownHandler rune : critRuneData.getAllRunes()) {
                    if (rune != null) {
                        rune.onCrit(player);
                    }
                }
            }
        }

        boolean isMagic = damage.isMagicDamage();
        boolean isPhysical = !isMagic;
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) continue;
            ItemPassive passive = getEquippedPassive(inv);
            if (passive != null) {
                passive.onDealDamage(player, target, statsDamage, isPhysical, isMagic);
            }
        }

        if (target instanceof Player targetPlayer) {
            for (ItemStack inv : targetPlayer.getInventory().getContents()) {
                if (inv == null || inv.getType().isAir()) continue;
                ItemPassive passive = getEquippedPassive(inv);
                if (passive != null) {
                    passive.onTakeDamage(targetPlayer, player, statsDamage, isMagic);
                }
            }
        }

        double baseLifeStealPercent = itemStatsManager.getItemLS(player);
        double effectiveLifeSteal = stats.getEffectiveLifeSteal(player, baseLifeStealPercent);
        if (effectiveLifeSteal > 0 && statsDamage > 0) {
            double healingMultiplier = stats.getEffectiveHealingMultiplier(player);
            double healthRestored = statsDamage * (effectiveLifeSteal / 100.0) * healingMultiplier;
            if (healthRestored > 0) {
                double playerHealth = player.getHealth() + healthRestored;
                playerHealth = Math.min(playerHealth, player.getMaxHealth());
                player.setHealth(playerHealth);
            }
        }

        double newHealth = target.getHealth();
        if (target instanceof Player targetPlayer) {
            double absorption = targetPlayer.getAbsorptionAmount();
            if (statsDamage > absorption) {
                statsDamage -= absorption;
                targetPlayer.setAbsorptionAmount(0);
            } else {
                targetPlayer.setAbsorptionAmount(absorption - statsDamage);
                statsDamage = 0;
            }
        }

        newHealth = Math.clamp(newHealth - statsDamage, 0, target.getMaxHealth());

        for (ItemStack armor : target.getEquipment().getArmorContents()) {
            if (armor != null && !armor.getType().isAir()) {
                armor.damage((short) 1, target);
            }
        }

        DebugLogger.debug(player, "§7----------- §f[ §dDEBUG MODE §f] §7-----------");
        DebugLogger.debug(player, "§aTrigger Type: " + type);
        DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] Total AD = §d" + Math.ceil(attackerAD * 100) / 100.0);
        DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] Total AP = §d" + Math.ceil(attackerAP * 100) / 100.0);
        DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] Crit Chance = §d" + Math.ceil(damage.getPlayerCritChance(player) * 100) / 100.0 + "%");
        DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] Crit Streak = §d" + CritManager.getInstance().getCritStreak(player));
        DebugLogger.debug(player, "§7[Debug] §f[§dTarget§f] Total AR = §d" + Math.ceil(targetAR * 100) / 100.0);
        DebugLogger.debug(player, "§7[Debug] §f[§dTarget§f] Total MR = §d" + Math.ceil(targetMR * 100) / 100.0);

        DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] Stats Damage = §d" + Math.ceil(statsDamage * 100) / 100.0);
        DebugLogger.debug(player, "§7[Debug] §f[§dTarget§f] Target New HP = §d" + Math.ceil(newHealth * 100) / 100.0);

        target.damage(0.001);
        if (newHealth <= 0) {
            target.setHealth(0);
            target.damage(1000, player);
        } else {
            target.setHealth(newHealth);
        }
    }

    public void setAttackCooldown(Player player) {
        PlayerStats stats = PlayerStats.getOrCreate(player);

        double playerAS = stats.getPlayerAS(player);

        double cooldownTicks = 20.0 / playerAS;
        int cooldownInt = (int) Math.max(1, Math.ceil(cooldownTicks));

        UUID uuid = player.getUniqueId();
        long cooldownMs = (long) (cooldownTicks * 50);
        attackCooldown.put(uuid, System.currentTimeMillis() + cooldownMs);

        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && !item.getType().isAir()) {
                player.setCooldown(item.getType(), cooldownInt);
            }
        }
    }

    public boolean isAnyHotbarOnCooldown(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && !item.getType().isAir()) {
                if (player.getCooldown(item.getType()) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isPlayerOnAttackCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        Long cooldownEnd = attackCooldown.get(uuid);
        if (cooldownEnd == null) {
            return false;
        }
        if (System.currentTimeMillis() >= cooldownEnd) {
            attackCooldown.remove(uuid);
            return false;
        }
        return true;
    }

    public long getAttackCooldownRemaining(Player player) {
        UUID uuid = player.getUniqueId();
        if (!attackCooldown.containsKey(uuid)) {
            return 0;
        }

        long cooldownEnd = attackCooldown.get(uuid);
        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }
}