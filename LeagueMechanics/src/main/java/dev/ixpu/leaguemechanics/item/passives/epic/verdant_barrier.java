package dev.ixpu.leaguemechanics.item.passives.epic;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.manager.ItemPassivesManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class verdant_barrier implements ItemPassive {
    private static final int COOLDOWN_TICKS = 90_000 / 50;
    private static final String PASSIVE_ID = "verdant-barrier";
    private final Map<UUID, Integer> lastAbsorbTick = new HashMap<>();
    private static final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(160, 60, 255), 1.3f);

    @Override
    public String getId() {
        return PASSIVE_ID;
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ᴀɴɴᴜʟ: §fGrants a spell shield that blocks the next hostile attack\n\n§790s Cooldown";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    public boolean isReady(Player player) {
        return player != null
                && LeagueMechanics.playerHasVerdantBarrier(player)
                && !ItemPassivesManager.getInstance().isOnCooldown(player, PASSIVE_ID);
    }

    public boolean absorbDamage(Player player) {
        int now = Bukkit.getCurrentTick();
        Integer last = lastAbsorbTick.get(player.getUniqueId());
        if (last != null && last == now) return true;

        if (!isReady(player)) return false;

        lastAbsorbTick.put(player.getUniqueId(), now);
        ItemPassivesManager.getInstance().setCooldown(player, PASSIVE_ID, COOLDOWN_TICKS);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 1.2f);
        spawnCrossedRings(player.getLocation().add(0, 1.0, 0), PURPLE);
        return true;
    }

    public static void spawnOrbitPoint(Player player) {
        spawnOrbitPoint(player, PURPLE);
    }

    public static void spawnOrbitPoint(Player player, Particle.DustOptions dust) {
        double angle = (Bukkit.getCurrentTick() % 30) / 30.0 * 2 * Math.PI;
        Location loc = player.getLocation().add(Math.cos(angle) * 0.9, 1.0, Math.sin(angle) * 0.9);
        player.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
    }

    public static void spawnCrossedRings(Location center, Particle.DustOptions dust) {
        int points = 24;
        double radius = 1.1;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double a = Math.cos(angle) * radius;
            double b = Math.sin(angle) * radius;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(a, b, 0), 1, 0, 0, 0, 0, dust);
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, b, a), 1, 0, 0, 0, 0, dust);
        }
    }
}