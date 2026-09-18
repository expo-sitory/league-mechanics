package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.rune.DebuffType;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class bramble_vest implements ItemPassive {
    private static final int THORNS_MAGIC_DAMAGE = 10;
    private static final int GRIEVOUS_DURATION_TICKS = 60;
    private static final ThreadLocal<Boolean> TRIGGERING_PASSIVES = ThreadLocal.withInitial(() -> false);

    @Override
    public String getId() {
        return "bramble-vest";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ᴛʜᴏʀɴs: §fWhen struck by an attack, deal §9" + THORNS_MAGIC_DAMAGE + "\n§9magic damage §fto the attacker. If they are a player,\n§finflict them with §cɢʀɪᴇᴠᴏᴜs ᴡᴏᴜɴᴅs §ffor 3 seconds.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onTakeDamage(Player victim, Player attacker, double damage, boolean isMagic) {
        if (attacker == null || !attacker.isOnline()) return;


        double damageToApply = procDamage(attacker, victim);
        double absorption = attacker.getAbsorptionAmount();

        if (damageToApply > absorption) {
            damageToApply -= absorption;
            attacker.setAbsorptionAmount(0);
        } else {
            attacker.setAbsorptionAmount(absorption - damageToApply);
            damageToApply = 0;
        }

        double newHealth = Math.clamp(attacker.getHealth() - damageToApply, 0, attacker.getMaxHealth());

        KillSourceTracker.getInstance().setSource(attacker, victim);

        if (!TRIGGERING_PASSIVES.get()) {
            TRIGGERING_PASSIVES.set(true);
            for (ItemStack inv : attacker.getInventory().getContents()) {
                if (inv == null || inv.getType().isAir()) continue;
                String itemId = ItemModifier.getItemId(inv);
                ItemPassive passive = ItemPassivesRegistry.getInstance().getPassive(itemId);
                if (passive != null) {
                    passive.onDealDamage(victim, attacker, damage, false, true);
                }
            }
            TRIGGERING_PASSIVES.set(false);
        }

        attacker.damage(0.00001);
        attacker.setHealth(newHealth);

        DebuffManager.getInstance().applyDebuff(attacker, DebuffType.GRIEVOUS_WOUNDS, GRIEVOUS_DURATION_TICKS);
    }

    private double procDamage(Player player, Entity target) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableOnlyAP();
        double baseDamage = damageManager.DamageCalculation(player, target, 0, 0, 0, THORNS_MAGIC_DAMAGE);

        return baseDamage;
    }
}
