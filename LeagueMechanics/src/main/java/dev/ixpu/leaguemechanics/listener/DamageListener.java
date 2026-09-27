package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;

import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.rune.keystones.sorcery.StormRaiderSurge;
import dev.ixpu.leaguemechanics.util.DebugLogger;
import dev.ixpu.leaguemechanics.entity.player.PlayerRuneData;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;

import dev.ixpu.leaguemechanics.manager.*;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.entity.player.skills.Parry;
import dev.ixpu.leaguemechanics.rune.RuneCooldownGate;
import dev.ixpu.leaguemechanics.rune.keystones.domination.HailOfBlades;
import dev.ixpu.leaguemechanics.rune.keystones.resolve.GraspOfTheUndying;
import dev.ixpu.leaguemechanics.rune.keystones.resolve.Guardian;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.*;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DamageListener implements Listener, RuneCooldownGate {

    private final LeagueMechanics plugin;
    private final RuneManager runeManager;
    private final ItemStatsManager itemStatsManager;
    private final PlayerStatsListener playerStatsListener;
    private final CombatStateManager combatState = CombatStateManager.getInstance();

    private final Map<UUID, Long> attackCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> parryWindow = new HashMap<>();
    private static final Map<UUID, Integer> parryWindowTasks = new HashMap<>();
    private final Set<Entity> parriedProjectiles = ConcurrentHashMap.newKeySet();
    private static final long PARRY_WINDOW_MS = 700L;

    public static final Parry parryCooldown = new Parry();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            if (item.getType().name().contains("SWORD")) {
                Player player = event.getPlayer();
                UUID uuid = player.getUniqueId();

                if (parryCooldown.isOnCooldown(player) || parryWindow.containsKey(uuid)) {
                    return;
                }

                parryWindow.put(uuid, System.currentTimeMillis());
                parryCooldown.showParryWindowBar(player);

                Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                    if (!parryWindow.containsKey(uuid)) {
                        task.cancel();
                        return;
                    }

                    if (!player.isBlocking() && !isHandRaised(player)) {
                        parryWindow.remove(uuid);
                        parryCooldown.hideParryWindowBar(player);
                        parryWindowTasks.remove(uuid);
                        task.cancel();
                    }
                }, 0L, 1L);

                int timeoutTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    if (parryWindow.containsKey(uuid)) {
                        parryWindow.remove(uuid);
                        parryCooldown.hideParryWindowBar(player);
                        parryCooldown.startMissedCooldown(player);
                        parryWindowTasks.remove(uuid);
                    }
                }, 4L);
                parryWindowTasks.put(uuid, timeoutTaskId);
            }
        }
    }

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
        combatState.removeLetRunesThrough(shooter.getUniqueId());
        setAttackCooldown(shooter);
    }

    @EventHandler
    public void onAttackSwing(PlayerArmSwingEvent event) {
        Player attacker = event.getPlayer();
        if (isPlayerOnAttackCooldown(attacker)) {
            event.setCancelled(true);
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {

        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            if (event.getEntity().getShooter() instanceof LivingEntity shooterEntity &&
                    event.getHitEntity() instanceof LivingEntity targetEntity) {
                entityDamageEvent(shooterEntity, targetEntity, "Projectile", 1);
            }
            return;
        }
        if (event.getEntity() instanceof ThrownPotion
                || event.getEntity() instanceof ThrownExpBottle
                || event.getEntity() instanceof Egg
                || event.getEntity() instanceof Snowball
                || event.getEntity() instanceof EnderPearl
                || event.getEntity() instanceof EnderSignal
                || event.getEntity() instanceof WindCharge) {
            return;
        }
        if (event.getEntity() instanceof org.bukkit.entity.FishHook) {
            return;
        }
        if (event.getHitEntity() == null || !(event.getHitEntity() instanceof LivingEntity target)) {
            return;
        }

        if (target instanceof Player targetPlayer) {
            Long parryTime = parryWindow.get(targetPlayer.getUniqueId());
            if (parryTime != null && (System.currentTimeMillis() - parryTime) <= PARRY_WINDOW_MS) {
                targetPlayer.sendTitlePart(TitlePart.TITLE, Component.text("ᴘᴀʀʀʏ!!", NamedTextColor.GREEN));
                shooter.sendTitlePart(TitlePart.TITLE, Component.text("ᴇɴᴇᴍʏ ᴘᴀʀʀɪᴇᴅ!!", NamedTextColor.RED));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "attack-parried-sound " + targetPlayer.getName());

                parriedProjectiles.add(event.getEntity());
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        parriedProjectiles.remove(event.getEntity()), 1L);

                parryWindow.remove(targetPlayer.getUniqueId());
                Integer taskId = parryWindowTasks.remove(targetPlayer.getUniqueId());
                if (taskId != null) {
                    Bukkit.getScheduler().cancelTask(taskId);
                }
                parryCooldown.hideParryWindowBar(targetPlayer);
                parryCooldown.resetCooldown(targetPlayer);

                event.setCancelled(true);
                return;
            }
        }

        if (target instanceof Player targetPlayer) {
            if (shooter == targetPlayer) {
                event.setCancelled(true);
                return;
            }
            if (!plugin.getCommandHandler().isPvpEnabled(shooter, targetPlayer)) {
                return;
            }
            recordHit(shooter, targetPlayer);
        }

        shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
        event.getEntity().remove();

        damageEvent(shooter, target, "Projectile", 1);
        combatState.removeLetRunesThrough(shooter.getUniqueId());

        combatState.addLetRunesThrough(shooter.getUniqueId());
        PlayerRuneData runeData = runeManager.getPlayerRuneData(shooter);

        if (runeData != null) {
            for (CooldownHandler rune : runeData.getAllRunes()) {
                if (rune == null) {
                    continue;
                }
                rune.onProjectileHit(shooter, target);
            }
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onAttack(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (target instanceof Player targetPlayer) {
            Long parryTime = parryWindow.get(targetPlayer.getUniqueId());
            if (parryTime != null && (System.currentTimeMillis() - parryTime) <= PARRY_WINDOW_MS) {
                targetPlayer.sendTitlePart(TitlePart.TITLE, Component.text("ᴘᴀʀʀʏ!!", NamedTextColor.GREEN));
                targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 50, 1, false, false));

                if (event.getDamager() instanceof Player attacker) {
                    attacker.sendTitlePart(TitlePart.TITLE, Component.text("ᴇɴᴇᴍʏ ᴘᴀʀʀɪᴇᴅ!!", NamedTextColor.RED));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "enemy-parried-sound " + attacker.getName());
                }

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "attack-parried-sound " + targetPlayer.getName());

                Integer taskId = parryWindowTasks.remove(targetPlayer.getUniqueId());
                if (taskId != null) {
                    Bukkit.getScheduler().cancelTask(taskId);
                }
                parryWindow.remove(targetPlayer.getUniqueId());
                parryCooldown.resetCooldown(targetPlayer);

                if (event.getDamager() instanceof LivingEntity damager) {
                    damager.knockback(0.5, Math.cos(damager.getLocation().getYaw() * Math.PI / 180), Math.sin(damager.getLocation().getYaw() * Math.PI / 180));
                    damager.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 3, false, false));
                    damager.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,30, 3, false, false));
                }

                event.setCancelled(true);
                return;
            }
        }

        if (!(event.getDamager() instanceof Player attacker)) {
            if (event.getDamager() instanceof LivingEntity livingAttacker && event.getEntity() instanceof LivingEntity) {
                entityDamageEvent(livingAttacker, target, "Melee", event.getDamage());
            }
            return;
        }

        if (target instanceof Player targetPlayer) {
            if (attacker == targetPlayer) {
                event.setCancelled(true);
                return;
            }
            if (!plugin.getCommandHandler().isPvpEnabled(attacker, targetPlayer)) {
                event.setCancelled(true);
                return;
            }
            recordHit(attacker, targetPlayer);
        }

        if (isPlayerOnAttackCooldown(attacker)) {
            event.setCancelled(true);
            return;
        }

        setAttackCooldown(attacker);
        event.setDamage(1);
        damageEvent(attacker, target, "Melee", event.getDamage());
        combatState.removeLetRunesThrough(attacker.getUniqueId());

        combatState.addLetRunesThrough(attacker.getUniqueId());
        PlayerRuneData runeData = runeManager.getPlayerRuneData(attacker);

        if (runeData != null) {
            for (CooldownHandler rune : runeData.getAllRunes()) {
                if (rune != null) {
                    rune.onAttack(attacker, target);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getDamage() == 0) {
            event.setCancelled(true);
            return;
        }

        if (event.getDamager() instanceof Projectile projectile && parriedProjectiles.contains(projectile)) {
            event.setCancelled(true);
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
            if (rune instanceof Guardian guardian) {
                guardian.onTakeDamage(player);
            }
        }
        if (!(event.getDamager() instanceof Player)) {
            Player attacker = null;
            LivingEntity sourceMob = null;
            switch (event.getDamager()) {
                case Projectile projectile when projectile.getShooter() instanceof Player shooter -> attacker = shooter;
                case Projectile projectile when projectile.getShooter() instanceof LivingEntity mob -> sourceMob = mob;
                case LivingEntity mob -> sourceMob = mob;
                default -> {
                }
            }
            if (sourceMob != null && !(sourceMob instanceof Player)) {
                combatState.setLastMobDamager(player, sourceMob);
            }
        }
    }

    public void recordHit(Player attacker, Player victim) {
        combatState.recordHit(victim, attacker);
    }

    public boolean letRunesThrough(Player player) {
        return combatState.hasLetRunesThrough(player.getUniqueId());
    }

    public boolean isBlocking(Player target, double damage) {
        if (!(target instanceof Player targetPlayer)) {
            return false;
        }

        ItemStack offhand = targetPlayer.getInventory().getItemInOffHand();
        ItemStack mainhand = targetPlayer.getInventory().getItemInMainHand();

        boolean hasShield = offhand != null && offhand.getType() == Material.SHIELD || mainhand != null && mainhand.getType() == Material.SHIELD;
        boolean hasSword = mainhand != null && mainhand.getType().name().contains("SWORD");

        if (!hasShield && !hasSword) {
            return false;
        }

        if (!targetPlayer.isBlocking() && !isHandRaised(targetPlayer)) {
            return false;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sword-blocking-sound " + targetPlayer.getName());
        BlockData blockData = Material.IRON_BLOCK.createBlockData();
        targetPlayer.getWorld().spawnParticle(Particle.BLOCK, targetPlayer.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0, blockData);

        int toolDamage = Math.max(1, (int) Math.ceil(Math.sqrt(damage)));

        if (hasSword && mainhand != null) {
            mainhand.damage((short) toolDamage, targetPlayer);
            if (mainhand.getDurability() <= 0) {
                targetPlayer.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }
        }

        if (hasShield && offhand != null) {
            offhand.damage((short) toolDamage, targetPlayer);
            if (offhand.getDurability() <= 0) {
                targetPlayer.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
        }

        targetPlayer.updateInventory();
        return true;
    }

    private boolean isHandRaised(Player player) {
        if (player == null) return false;
        try {
            return player.isHandRaised();
        } catch (NoSuchMethodError | AbstractMethodError ignored) {
            return false;
        }
    }

    private double getMitigationPercent(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() == Material.SHIELD) {
            return getMitigationPercent(offhand.getType());
        }
        ItemStack mainhand = player.getInventory().getItemInMainHand();
        return getMitigationPercent(mainhand.getType());
    }

    private double getMitigationPercent(Material weapon) {
        switch(weapon) {
            case WOODEN_SWORD -> {return 10.0;}
            case STONE_SWORD -> {return 20.0;}
            case IRON_SWORD, GOLDEN_SWORD -> {return 28.0;}
            case DIAMOND_SWORD, NETHERITE_SWORD -> {return 40.0;}
            case SHIELD -> {return 50.0;}
            default -> {return 0.0;}
        }
    }

    public void entityDamageEvent(LivingEntity source, LivingEntity target, String type, double vanillaDamage) {
        DamageManager damage = new DamageManager(itemStatsManager);

        if (type.equals("Projectile")) {
            damage.enableProjectileDamage();
        }

        double totalDamage = damage.DamageCalculation(source, target, 0, 0, 0, 0);

        for (ItemStack armor : target.getEquipment().getArmorContents()) {
            if (armor != null && !armor.getType().isAir()) {
                armor.damage((short) 1, target);
            }
        }

        if (target instanceof Player targetPlayer && isBlocking(targetPlayer, vanillaDamage)) {
            double mitigationPercent = getMitigationPercent(targetPlayer);
            totalDamage *= (1.0 - (mitigationPercent / 100.0));
        }
        target.damage(totalDamage);

        if (target instanceof Player targetPlayer) {
            for (ItemStack inv : targetPlayer.getInventory().getContents()) {
                if (inv == null || inv.getType().isAir()) continue;
                ItemPassive passive = getEquippedPassive(inv);
                if (passive != null) {
                    boolean isMagic = damage.isMagicDamage();
                    passive.onTakeDamage(targetPlayer, source instanceof Player ? (Player) source : null, totalDamage, isMagic);
                }
            }
        }
    }

    public void damageEvent(Player player, LivingEntity target, String type, double vanillaDamage) {
        DamageManager damage = new DamageManager(itemStatsManager);
        PlayerRuneData runeData = runeManager.getPlayerRuneData(player);
        PlayerRuneData critRuneData = runeManager.getPlayerRuneData(player);
        PlayerStats stats = PlayerStats.getOrCreate(player);

        double attackerAD = (stats.getPlayerAD(player) * stats.getDamageBalancer());
        double attackerAP = stats.getPlayerAP(player) * stats.getDamageBalancer();
        double targetAR = damage.getTargetAR(target);
        double targetMR = damage.getTargetMR(target);

        double baseLifeStealPercent = itemStatsManager.getItemLS(player);
        double effectiveLifeSteal = stats.getEffectiveLifeSteal(player, baseLifeStealPercent);

        double totalDamage = damage.DamageCalculation(player, target, 0, 0, 0, 0);
        double magicDamage = damage.getLastBonusMagicDamage();
        double physicalDamage = totalDamage - magicDamage;

        boolean didCrit = false;
        boolean isMagic = damage.isMagicDamage();
        boolean isPhysical = !isMagic;
        String crit = "False";

        if (type.equals("Projectile")) {
            damage.enableProjectileDamage();
        }
        if (runeData != null) {
            for (CooldownHandler rune : runeData.getAllRunes()) {
                if (rune instanceof StormRaiderSurge stormRaider) {
                    stormRaider.onAttack(player, target, totalDamage);
                }
            }
        }
        if (totalDamage > 0) {
            double critChance = Math.round(damage.getPlayerCritChance(player) * 10.0) / 10.0;
            if (critChance > 0 && DamageManager.criticalChance(player, critChance)) {
                totalDamage *= DamageManager.getCritDamageMultiplier(player);
                crit = "True";
                didCrit = true;
            }
        }
        if (didCrit) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.0f, 1.2f);
            if (critRuneData != null) {
                for (CooldownHandler rune : critRuneData.getAllRunes()) {
                    if (rune != null) {
                        rune.onCrit(player);
                    }
                }
            }
        }
        if (effectiveLifeSteal > 0 && totalDamage > 0) {
            double healingMultiplier = stats.getEffectiveHealingMultiplier(player);
            double healthRestored = totalDamage * (effectiveLifeSteal / 100.0) * healingMultiplier;
            if (healthRestored > 0) {
                player.heal(healthRestored);
            }
        }
        for (ItemStack armor : target.getEquipment().getArmorContents()) {
            if (armor != null && !armor.getType().isAir()) {
                armor.damage((short) 1, target);
            }
        }
        if (target instanceof Player targetPlayer) {
            KillSourceTracker.getInstance().setSource(targetPlayer, player);
            targetAR /= stats.getResistanceBalancer();
            targetMR /= stats.getResistanceBalancer();
        }

        if (target instanceof Player targetPlayer && isBlocking(targetPlayer, vanillaDamage) && vanillaDamage > 0 && !didCrit) {
            double mitigationPercent = getMitigationPercent(targetPlayer);
            totalDamage *= (1.0 - (mitigationPercent / 100.0));
        }

        target.damage(totalDamage);

        DebugLogger.debug(player, "§7----------- §f[ §dDEBUG MODE §f] §7-----------");
        DebugLogger.debug(player, "§aTrigger Type: " + type + " Hit Event");
        DebugLogger.debug(player, "§f[§dSource§f] Total AD = §d" + Math.ceil(attackerAD * 100) / 100.0 + "§f | Total AP = §d" + Math.ceil(attackerAP * 100) / 100.0);
        DebugLogger.debug(player, "§f[§dSource§f] Is Critical? = §d" + crit + "§f | Chance: §d" + Math.ceil(damage.getPlayerCritChance(player) * 100) / 100.0 + "% §f| Streak = §d" + CritManager.getInstance().getCritStreak(player));
        DebugLogger.debug(player, "§f[§dSource§f] Physical Damage = §d" + Math.ceil((physicalDamage) * 100) / 100.0 + "§f | Magic Damage = §d" + Math.ceil((magicDamage) * 100) / 100.0);
        DebugLogger.debug(player, "§f[§dTarget§f] Magic Resist = §d" + Math.ceil(targetMR * 100) / 100.0 + "§f | Armor = §d" + Math.ceil(targetAR * 100) / 100.0);
        DebugLogger.debug(player, "§f[§dTarget§f] Damage Received = §d" + Math.ceil(totalDamage * 100) / 100.0);

        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) continue;
            ItemPassive passive = getEquippedPassive(inv);
            if (passive != null) {
                passive.onDealDamage(player, target, totalDamage, isPhysical, isMagic);
            }
        }
        if (target instanceof Player targetPlayer) {
            for (ItemStack inv : targetPlayer.getInventory().getContents()) {
                if (inv == null || inv.getType().isAir()) continue;
                ItemPassive passive = getEquippedPassive(inv);
                if (passive != null) {
                    passive.onTakeDamage(targetPlayer, player, totalDamage, isMagic);
                }
            }
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
            if (item != null && !item.getType().isAir() && isWeapon(item)) {
                if (player.getCooldown(item.getType()) == 0) {
                    player.setCooldown(item.getType(), cooldownInt);
                }
            }
        }
    }

    private boolean isWeapon(ItemStack item) {
        String name = item.getType().name();
        return name.contains("SWORD") || name.contains("AXE") || name.contains("BOW") ||
                name.contains("TRIDENT") || name.contains("MACE") || name.contains("WIND_CHARGE");
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