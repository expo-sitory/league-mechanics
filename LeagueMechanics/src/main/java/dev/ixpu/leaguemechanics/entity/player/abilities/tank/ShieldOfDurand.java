package dev.ixpu.leaguemechanics.entity.player.abilities.tank;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.debuff.DebuffType;
import dev.ixpu.leaguemechanics.entity.player.ClassPassives;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import dev.ixpu.leaguemechanics.manager.ClassAbilitiesManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ShieldOfDurand extends ClassAbilities {
    private static final String CFG = "skills.shield-of-durand.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_RANGE = 10.0;
    private static final double DEFAULT_STUN_SECONDS = 2.0;
    private static final double DEFAULT_START_RANGE = 3.0;
    private static final double DEFAULT_RANGE_STEP = 1.0;
    private static final double DEFAULT_STEP_SECONDS = 0.5;
    private static final double DEFAULT_CHARGE_SLOW_PERCENT = 50.0;

    private static final int RELEASE_GRACE_TICKS = 6;
    private static final int REPEAT_CLICK_TICKS = 4;
    private static final int PREVIEW_INTERVAL_TICKS = 5;
    private static final int WAVE_TICKS = 8;

    private static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 221, 51), 1.4f);

    private final Map<UUID, Charge> charges = new HashMap<>();
    private final NamespacedKey slowKey;

    private static class Charge {
        final int startTick;
        int lastPressTick;
        AttributeModifier slow;
        BukkitTask task;

        Charge(int startTick) {
            this.startTick = startTick;
            this.lastPressTick = startTick;
        }
    }

    public ShieldOfDurand(LeagueMechanics plugin) {
        super(plugin, "shield-of-durand", "Shield of Durand", PlayerClassType.TANK,
                "passives.galio_durand",
                "leaguemechanics:shield-of-durand",
                "minecraft:shield");
        this.slowKey = new NamespacedKey(plugin, "durand_charge_slow");
    }

    @Override
    public double getMinCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "min-cooldown-seconds", DEFAULT_MIN_COOLDOWN_SECONDS);
    }

    @Override
    public double getCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "cooldown-seconds", DEFAULT_COOLDOWN_SECONDS);
    }

    private double getRange() {
        return plugin.getConfig().getDouble(CFG + "range", DEFAULT_RANGE);
    }

    private double getStunSeconds() {
        return plugin.getConfig().getDouble(CFG + "stun-seconds", DEFAULT_STUN_SECONDS);
    }

    private double getStartRange() {
        return plugin.getConfig().getDouble(CFG + "charge-start-range", DEFAULT_START_RANGE);
    }

    private double getRangeStep() {
        return plugin.getConfig().getDouble(CFG + "charge-range-step", DEFAULT_RANGE_STEP);
    }

    private double getStepSeconds() {
        return plugin.getConfig().getDouble(CFG + "charge-step-seconds", DEFAULT_STEP_SECONDS);
    }

    private double getChargeSlowPercent() {
        double percent = plugin.getConfig().getDouble(CFG + "charge-slow-percent", DEFAULT_CHARGE_SLOW_PERCENT);
        return Math.max(0.0, Math.min(100.0, percent));
    }

    private boolean requiresPvp() {
        return plugin.getConfig().getBoolean(CFG + "require-pvp", true);
    }

    @Override
    public double getEffectSeconds() {
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.SHIELD_OF_DURAND);
    }

    @Override
    public List<String> getDescription() {
        AbilityEffectManager effects = AbilityEffectManager.getInstance();
        double armor = effects.getConfiguredArmorPercent();
        double seconds = effects.getDurationSeconds(AbilityEffectType.SHIELD_OF_DURAND);
        return List.of(
                "§7Hold to charge from §f" + trim(getStartRange()) + " blocks§7, growing §f+" + trim(getRangeStep())
                        + " block§7 every §f" + trim(getStepSeconds()) + "s§7 (max §f" + trim(getRange()) + "§7),",
                "§7slowing you by §f" + trim(getChargeSlowPercent()) + "%§7. Release to gain §f+" + trim(armor)
                        + "% armor§7 for §f" + trim(seconds) + " seconds§7,",
                "§7taunt all mobs in range and stun non-partied",
                "§7players for §f" + trim(getStunSeconds()) + " seconds§7."
        );
    }

    @Override
    public boolean isChargeable() {
        return true;
    }

    @Override
    public boolean isCharging(Player player) {
        return charges.containsKey(player.getUniqueId());
    }

    @Override
    public void interruptCharge(Player player) {
        stopCharge(player);
    }

    @Override
    public boolean cast(Player caster) {
        Charge charge = charges.get(caster.getUniqueId());
        if (charge != null) {
            charge.lastPressTick = Bukkit.getCurrentTick();
            return false;
        }
        startCharge(caster);
        return false;
    }

    private void startCharge(Player caster) {
        Charge charge = new Charge(Bukkit.getCurrentTick());

        AttributeInstance speed = caster.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            for (AttributeModifier existing : speed.getModifiers()) {
                if (slowKey.equals(existing.getKey())) speed.removeModifier(existing);
            }
            charge.slow = new AttributeModifier(slowKey, -getChargeSlowPercent() / 100.0,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            speed.addTransientModifier(charge.slow);
        }

        charges.put(caster.getUniqueId(), charge);
        charge.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickCharge(caster, charge), 1L, 1L);
        spawnRing(caster.getLocation(), chargedRange(charge, charge.startTick), 0.2, 1.2f);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.6f, 0.5f);
    }

    private void tickCharge(Player player, Charge charge) {
        if (!player.isOnline() || player.isDead()) {
            stopCharge(player);
            return;
        }

        DebuffManager debuffs = DebuffManager.getInstance();
        String held = ClassAbilitiesManager.getInstance().getSkillId(player.getInventory().getItemInMainHand());
        if (!getId().equalsIgnoreCase(held)
                || debuffs.hasDebuff(player, DebuffType.STUN)
                || debuffs.hasDebuff(player, DebuffType.GROUNDED)) {
            stopCharge(player);
            return;
        }

        int now = Bukkit.getCurrentTick();
        double range = chargedRange(charge, now);

        if (now - charge.lastPressTick > RELEASE_GRACE_TICKS) {
            stopCharge(player);
            release(player, range);
            return;
        }

        if ((now - charge.startTick) % PREVIEW_INTERVAL_TICKS == 0) {
            spawnRing(player.getLocation(), range, 0.2, 1.2f);
        }
    }

    private double chargedRange(Charge charge, int now) {
        int heldUntil = Math.min(now, charge.lastPressTick + REPEAT_CLICK_TICKS);
        int stepTicks = Math.max(1, (int) Math.round(getStepSeconds() * 20.0));
        int steps = Math.max(0, heldUntil - charge.startTick) / stepTicks;
        return Math.min(getRange(), getStartRange() + steps * getRangeStep());
    }

    private void stopCharge(Player player) {
        Charge charge = charges.remove(player.getUniqueId());
        if (charge == null) return;
        if (charge.task != null) charge.task.cancel();
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null && charge.slow != null) speed.removeModifier(charge.slow);
    }

    private void release(Player caster, double range) {
        double rangeSq = range * range;
        int stunTicks = Math.max(1, (int) Math.round(getStunSeconds() * 20.0));
        boolean requirePvp = requiresPvp();

        PartyManager parties = PartyManager.getInstance();
        DebuffManager debuffs = DebuffManager.getInstance();
        Location origin = caster.getLocation();

        for (LivingEntity entity : origin.getNearbyLivingEntities(range)) {
            if (entity.equals(caster) || entity.isDead()) continue;
            if (entity.getLocation().distanceSquared(origin) > rangeSq) continue;

            if (entity instanceof Player other) {
                if (!other.isOnline()) continue;
                if (other.getGameMode() == GameMode.SPECTATOR || other.getGameMode() == GameMode.CREATIVE) continue;
                if (parties.isSameParty(caster, other)) continue;
                if (requirePvp && !isPvpEnabled(caster, other)) continue;

                debuffs.applyDebuff(other, DebuffType.STUN, stunTicks, caster);
                KillSourceTracker.getInstance().setSource(other, caster);

                Location eyes = other.getEyeLocation();
                other.getWorld().spawnParticle(Particle.DUST, eyes, 25, 0.3, 0.3, 0.3, 0, YELLOW);
                other.playSound(eyes, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.0f, 0.8f);
            } else if (entity instanceof Mob mob) {
                if (mob instanceof Tameable tameable && tameable.isTamed()) continue;
                ClassPassives.taunt(mob, caster);
            }
        }

        AbilityEffectManager.getInstance().activate(caster, AbilityEffectType.SHIELD_OF_DURAND);
        ClassAbilitiesManager.getInstance().startCooldown(caster, this);

        spawnShockwave(origin, range);
        caster.getWorld().spawnParticle(Particle.DUST, origin.clone().add(0, 1, 0), 60, 0.5, 0.9, 0.5, 0, YELLOW);
        caster.getWorld().playSound(origin, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.7f);
        caster.getWorld().playSound(origin, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 0.6f);
    }

    private void spawnShockwave(Location origin, double range) {
        Bukkit.getScheduler().runTaskTimer(plugin, new Consumer<BukkitTask>() {
            int step = 0;

            @Override
            public void accept(BukkitTask task) {
                step++;
                double radius = range * step / WAVE_TICKS;
                spawnRing(origin, radius, 0.2, 1.4f);
                spawnRing(origin, radius, 1.0, 1.0f);
                if (step >= WAVE_TICKS) task.cancel();
            }
        }, 0L, 1L);
    }

    private void spawnRing(Location center, double radius, double height, float size) {
        Particle.DustOptions yellow = new Particle.DustOptions(Color.fromRGB(255, 221, 51), size);
        int points = Math.max(24, (int) (radius * 6));
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            Location point = center.clone().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            center.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, yellow);
        }
    }

    private boolean isPvpEnabled(Player attacker, Player target) {
        var handler = plugin.getCommandHandler();
        return handler != null && handler.isPvpEnabled(attacker, target);
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}