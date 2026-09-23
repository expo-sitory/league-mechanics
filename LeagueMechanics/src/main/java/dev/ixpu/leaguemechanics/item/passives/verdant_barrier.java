package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.manager.ItemPassivesManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class verdant_barrier implements ItemPassive {
    private static final int MAX_ABSORPTION_HEARTS = 10;
    private static final int HEARTS_PER_TICK = 1;
    private static final int ABSORPTION_DURATION_TICKS = Integer.MAX_VALUE;
    private static final int DEPLETED_COOLDOWN_TICKS = 90_000 / 50;
    private static final String PASSIVE_ID = "verdant-barrier";

    private final Map<UUID, Integer> currentHearts = new HashMap<>();

    @Override
    public String getId() {
        return PASSIVE_ID;
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ᴀɴɴᴜʟ: §fOnce equipped, grants §e+2 absorption hearts\n§fevery 5 seconds, up to a maximum of §e20 absorption hearts§f.\n\n§790s Cooldown (If all absorption hearts are lost.)";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onDealDamage(Player attacker, LivingEntity target, double damage,
                             boolean isPhysical, boolean isMagic) {}

    @Override
    public void onTakeDamage(Player victim, Player attacker, double damage, boolean isMagic) {
        if (victim == null) return;
        UUID uuid = victim.getUniqueId();
        int tracked = currentHearts.getOrDefault(uuid, 0);
        if (tracked <= 0) return;

        double actualAbsorption = victim.getAbsorptionAmount();
        int actualHearts = (int) Math.round(actualAbsorption / 2.0);

        if (actualHearts < tracked) {
            int newHearts = Math.max(0, actualHearts);
            currentHearts.put(uuid, newHearts);
            reapplyAbsorption(victim, newHearts);

            if (newHearts == 0) {
                ItemPassivesManager mgr = ItemPassivesManager.getInstance();
                if (mgr != null) {
                    mgr.setCooldown(victim, PASSIVE_ID, DEPLETED_COOLDOWN_TICKS);
                }
            }
        }
    }

    public void grantAbsorptionHeart(Player player) {
        if (player == null || player.isDead()) return;
        ItemPassivesManager mgr = ItemPassivesManager.getInstance();
        if (mgr != null && mgr.isOnCooldown(player, PASSIVE_ID)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        int hearts = currentHearts.getOrDefault(uuid, 0);
        if (hearts < MAX_ABSORPTION_HEARTS) {
            hearts = Math.min(MAX_ABSORPTION_HEARTS, hearts + HEARTS_PER_TICK);
            currentHearts.put(uuid, hearts);
        }
        reapplyAbsorption(player, hearts);
    }

    public boolean isOnCooldown(Player player) {
        ItemPassivesManager mgr = ItemPassivesManager.getInstance();
        if (mgr == null) return false;
        return mgr.isOnCooldown(player, PASSIVE_ID);
    }

    public void resetHearts(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        currentHearts.remove(uuid);
        ItemPassivesManager mgr = ItemPassivesManager.getInstance();
        if (mgr != null) {
            mgr.setCooldown(player, PASSIVE_ID, 0);
        }
        player.removePotionEffect(PotionEffectType.ABSORPTION);
    }

    private void reapplyAbsorption(Player player, int hearts) {
        if (hearts <= 0) {
            player.removePotionEffect(PotionEffectType.ABSORPTION);
            return;
        }
        player.removePotionEffect(PotionEffectType.ABSORPTION);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,
                ABSORPTION_DURATION_TICKS,
                hearts - 1,
                false,
                false
        ));
    }
}