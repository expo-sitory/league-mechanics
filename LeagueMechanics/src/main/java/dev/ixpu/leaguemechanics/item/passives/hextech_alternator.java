package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.ItemPassivesManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class hextech_alternator implements ItemPassive {
    private static final int REVVED_BONUS_MAGIC_DAMAGE = 45;
    private static final int REVVED_COOLDOWN_TICKS = 800;
    private static final ThreadLocal<Boolean> TRIGGERING_PASSIVES = ThreadLocal.withInitial(() -> false);

    @Override
    public String getId() {
        return "hextech-alternator";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ʀᴇᴠᴠᴇᴅ: §fDamaging a player deals §9" + REVVED_BONUS_MAGIC_DAMAGE + " bonus magic damage\n\n§740s Cooldown";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onDealDamage(Player attacker, LivingEntity target, double damage,
                             boolean isPhysical, boolean isMagic) {
        if (!(target instanceof Player targetPlayer)) return;

        ItemPassivesManager manager = ItemPassivesManager.getInstance();
        if (manager.isOnCooldown(attacker, getId())) return;

        double damageToApply = procDamage(attacker, target);
        double absorption = targetPlayer.getAbsorptionAmount();

        if (damageToApply > absorption) {
            damageToApply -= absorption;
            targetPlayer.setAbsorptionAmount(0);
        } else {
            targetPlayer.setAbsorptionAmount(absorption - damageToApply);
            damageToApply = 0;
        }

        double newHealth = Math.clamp(target.getHealth() - damageToApply, 0, target.getMaxHealth());

        KillSourceTracker.getInstance().setSource(targetPlayer, attacker);

        if (!TRIGGERING_PASSIVES.get()) {
            TRIGGERING_PASSIVES.set(true);
            for (ItemStack inv : attacker.getInventory().getContents()) {
                if (inv == null || inv.getType().isAir()) continue;
                String itemId = ItemModifier.getItemId(inv);
                ItemPassive passive = ItemPassivesRegistry.getInstance().getPassive(itemId);
                if (passive != null) {
                    passive.onDealDamage(attacker, target, damage, false, true);
                }
            }
            TRIGGERING_PASSIVES.set(false);
        }

        targetPlayer.setHealth(newHealth);
        manager.setCooldown(attacker, getId(), REVVED_COOLDOWN_TICKS);
    }

    private double procDamage(Player player, Entity target) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableOnlyAP();
        double baseDamage = damageManager.DamageCalculation(player, target, 0, 0, 0, REVVED_BONUS_MAGIC_DAMAGE);

        return baseDamage;
    }
}
