package dev.ixpu.leaguemechanics.entity.player.abilities.support;

import dev.ixpu.leaguemechanics.manager.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public final class SupportEffects {
    private SupportEffects() {}

    public static Location groundedCenter(Player caster) {
        Location loc = caster.getLocation().clone();
        RayTraceResult hit = loc.getWorld().rayTraceBlocks(loc.clone().add(0, 0.5, 0), new Vector(0, -1, 0),
                6.0, FluidCollisionMode.NEVER, true);
        if (hit != null) {
            loc.setY(hit.getHitPosition().getY());
        }
        return loc;
    }

    public static List<Player> partyInRange(Player caster, double radius) {
        PartyManager parties = PartyManager.getInstance();
        List<Player> targets = new ArrayList<>();
        targets.add(caster);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(caster) || other.isDead() || other.getGameMode() == GameMode.SPECTATOR) continue;
            if (!parties.isSameParty(caster, other)) continue;
            if (!other.getWorld().equals(caster.getWorld())
                    || other.getLocation().distanceSquared(caster.getLocation()) > radius * radius) continue;
            targets.add(other);
        }
        return targets;
    }


    static double absorptionPoints(double hearts) {
        if (hearts <= 0.0) return 0.0;
        int amplifier = Math.max(0, (int) Math.ceil(hearts / 2.0) - 1);
        return 4.0 * (amplifier + 1);
    }


    public static void grantAbsorption(Player player, NamespacedKey key, double hearts) {
        double points = absorptionPoints(hearts);
        if (points <= 0.0) return;
        AttributeInstance max = player.getAttribute(Attribute.MAX_ABSORPTION);
        if (max == null) return;

        AttributeModifier existing = findModifier(max, key);
        if (existing != null) max.removeModifier(existing);
        max.addTransientModifier(new AttributeModifier(key, points, AttributeModifier.Operation.ADD_NUMBER));
        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), points));
    }


    public static void revokeAbsorption(Player player, NamespacedKey key) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_ABSORPTION);
        if (max == null) return;
        AttributeModifier modifier = findModifier(max, key);
        if (modifier == null) return;

        player.setAbsorptionAmount(Math.max(0.0, player.getAbsorptionAmount() - modifier.getAmount()));
        max.removeModifier(modifier);
    }

    private static AttributeModifier findModifier(AttributeInstance attribute, NamespacedKey key) {
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (key.equals(modifier.getKey())) return modifier;
        }
        return null;
    }
}