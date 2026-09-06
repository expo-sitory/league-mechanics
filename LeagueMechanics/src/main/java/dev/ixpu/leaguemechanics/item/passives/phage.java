package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class phage implements ItemPassive {
    private static final double RAGE_MS_BONUS = 0.15;
    private static final long RAGE_DURATION_TICKS = 40L; 

    private static final Map<UUID, List<AttributeModifier>> activeModifiers = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "phage";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ʀᴀɢᴇ: §fAttacking a target grants §7+15% movement\n§7speed §ffor §e2 seconds§f, §fwhich refreshes on each hit.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onDealDamage(Player attacker, LivingEntity target, double damage,
                             boolean isPhysical, boolean isMagic) {
        if (attacker == null) return;
        UUID uuid = attacker.getUniqueId();
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return;

        BukkitTask existingTask = activeTasks.remove(uuid);
        if (existingTask != null) {
            existingTask.cancel();
        }
        removeModifier(attacker);

        AttributeModifier modifier = new AttributeModifier(
                UUID.randomUUID(),
                "phage-rage-speed",
                RAGE_MS_BONUS,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
        );

        var movementAttr = attacker.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementAttr != null) {
            movementAttr.addModifier(modifier);
            activeModifiers.computeIfAbsent(uuid, k -> new ArrayList<>()).add(modifier);
        }

        BukkitTask newTask = new BukkitRunnable() {
            @Override
            public void run() {
                removeModifier(attacker);
                activeTasks.remove(uuid);
            }
        }.runTaskLater(plugin, RAGE_DURATION_TICKS);

        activeTasks.put(uuid, newTask);
    }

    @SuppressWarnings("removal")
    private void removeModifier(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        List<AttributeModifier> mods = activeModifiers.remove(uuid);
        if (mods == null) return;

        var movementAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementAttr != null) {
            for (AttributeModifier mod : mods) {
                try {
                    movementAttr.removeModifier(mod);
                } catch (Exception e) {
                    //
                }
            }
        }
    }
}