package dev.ixpu.leaguemechanics.rune;

import dev.ixpu.leaguemechanics.debuff.DebuffType;
import dev.ixpu.leaguemechanics.rune.paths.RunePath;
import dev.ixpu.leaguemechanics.rune.slots.RuneSlot;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class CooldownHandler {
    protected final String id;
    protected final RunePath path;
    protected final RuneSlot slot;
    protected boolean hasStacking;
    protected int maxStacks;
    protected double cooldownSeconds = 0.0;

    protected final Map<UUID, Long> playerCooldowns = new HashMap<>();

    public CooldownHandler(String id, RunePath path, RuneSlot slot) {
        this.id = id;
        this.path = path;
        this.slot = slot;
        this.hasStacking = false;
        this.maxStacks = 0;
    }

    public String getId() {
        return id;
    }

    public RunePath getPath() {
        return path;
    }

    public RuneSlot getSlot() {
        return slot;
    }

    public void setCooldownSeconds(double seconds) {
        this.cooldownSeconds = seconds;
    }

    public boolean isOnCooldown(Player player) {
        if (cooldownSeconds <= 0) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Long lastActivation = playerCooldowns.get(uuid);

        if (lastActivation == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownMs = (long) (cooldownSeconds * 1000);
        return (currentTime - lastActivation) < cooldownMs;
    }

    public double getRemainingCooldown(Player player) {
        if (cooldownSeconds <= 0) {
            return 0;
        }

        UUID uuid = player.getUniqueId();
        Long lastActivation = playerCooldowns.get(uuid);

        if (lastActivation == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownMs = (long) (cooldownSeconds * 1000);
        long elapsed = currentTime - lastActivation;
        long remaining = cooldownMs - elapsed;

        return Math.max(0, remaining / 1000.0);
    }

    public void resetCooldown(Player player) {
        playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void clearPlayerCooldown(Player player) {
        playerCooldowns.remove(player.getUniqueId());
    }

    public String getCooldownDisplay(Player player) {
        double remaining = getRemainingCooldown(player);
        if (remaining <= 0) {
            return "";
        }
        return String.format("§7%.1fs", remaining);
    }

    public abstract void onEnable(Player player);

    public abstract void onDisable(Player player);

    public abstract void tick(Player player);

    public String getDisplaySection(Player player) {
        return "";
    }

    public void onPlayerDamage(Player player, double damage) {}

    public void onAttack(Player attacker, Entity target) {}

    public void onProjectileHit(Player shooter, Entity hitEntity) {}

    public void onTakedown(Player attacker, Player victim, boolean isKill) {}

    public void onCrowdControl(Player source, Player target, DebuffType type) {}

    public void onPotionEffectGain(Player player, org.bukkit.potion.PotionEffect effect) {}

    public void onCrit(Player player) {}

    public void onBlockBreak(Player player, int blocksBroken) {}

    public void onShieldBlock(Player player) {}

}