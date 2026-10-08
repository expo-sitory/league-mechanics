package dev.ixpu.leaguemechanics.entity.player.abilities.assassin;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.debuff.DebuffType;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Paranoia extends ClassAbilities {
    private static final String CFG = "skills.paranoia.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_RANGE = 25.0;
    private static final double DEFAULT_DURATION_SECONDS = 5.0;

    public Paranoia(LeagueMechanics plugin) {
        super(plugin, "paranoia", "Paranoia", PlayerClassType.ASSASSIN,
                "passives.nocturn_paranoia",
                "leaguemechanics:paranoia",
                "minecraft:ender_eye");
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

    private double getDurationSeconds() {
        return plugin.getConfig().getDouble(CFG + "duration-seconds", DEFAULT_DURATION_SECONDS);
    }

    private boolean requiresPvp() {
        return plugin.getConfig().getBoolean(CFG + "require-pvp", true);
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "§7Reduce the sight of every non-partied",
                "§7players within §f" + trim(getRange()) + " blocks§7 for §f" + trim(getDurationSeconds()) + " seconds§7."
        );
    }

    @Override
    public boolean cast(Player caster) {
        double range = getRange();
        double rangeSq = range * range;
        int durationTicks = Math.max(1, (int) Math.round(getDurationSeconds() * 20.0));
        boolean requirePvp = requiresPvp();

        PartyManager parties = PartyManager.getInstance();
        Location origin = caster.getLocation();

        List<Player> targets = new ArrayList<>();
        for (Player other : caster.getWorld().getPlayers()) {
            if (other.equals(caster)) continue;
            if (!other.isOnline() || other.isDead()) continue;
            if (other.getGameMode() == GameMode.SPECTATOR || other.getGameMode() == GameMode.CREATIVE) continue;
            if (parties.isSameParty(caster, other)) continue;
            if (other.getLocation().distanceSquared(origin) > rangeSq) continue;
            if (requirePvp && !isPvpEnabled(caster, other)) continue;
            targets.add(other);
        }

        if (targets.isEmpty()) {
            caster.sendMessage(Component.text("§cParanoia: no enemies within " + trim(range) + " blocks"));
            return false;
        }

        DebuffManager debuffs = DebuffManager.getInstance();
        for (Player target : targets) {
            debuffs.applyDebuff(target, DebuffType.NEARSIGHTED, durationTicks);
            Location eyes = target.getEyeLocation();
            target.getWorld().spawnParticle(Particle.SQUID_INK, eyes, 20, 0.3, 0.3, 0.3, 0.02);
            target.playSound(eyes, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.7f);
        }

        caster.getWorld().spawnParticle(Particle.SCULK_SOUL, origin.clone().add(0, 1, 0), 40, 0.6, 0.8, 0.6, 0.03);
        caster.getWorld().playSound(origin, Sound.ENTITY_WARDEN_NEARBY_CLOSER, 0.8f, 0.6f);
        return true;
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