package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.ItemPassivesManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.util.DebugLogger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class hextech_alternator implements ItemPassive {
    private static final int REVVED_BONUS_MAGIC_DAMAGE = 45;
    private static final int REVVED_COOLDOWN_TICKS = 800;
    @Override
    public String getId() {
        return "hextech-alternator";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ʀᴇᴠᴠᴇᴅ: §fDamaging a target deals §9" + REVVED_BONUS_MAGIC_DAMAGE + " bonus magic damage\n\n§740s Cooldown";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onDealDamage(Player source, LivingEntity target, double damage,
                             boolean isPhysical, boolean isMagic) {
        ItemPassivesManager manager = ItemPassivesManager.getInstance();

        double damageToApply = procDamage(source, target);

        if (manager.isOnCooldown(source, getId())) return;
        if (target instanceof Player targetPlayer) {
            KillSourceTracker.getInstance().setSource(targetPlayer, source);
        }
        target.damage(damageToApply);

        DebugLogger.debug(source, "§f[§dSource§f] §f[§9Hextech Alternator§f] Proc Damage = §d" + Math.ceil(damageToApply * 100) / 100.0 + "§f | Type = §dMagic Damage");
        manager.setCooldown(source, getId(), REVVED_COOLDOWN_TICKS);
    }

    private double procDamage(Player player, LivingEntity target) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableOnlyAP();
        damageManager.enableItemProc();
        double baseDamage = damageManager.DamageCalculation(player, target, 0, 0, 0, REVVED_BONUS_MAGIC_DAMAGE);
        return baseDamage;
    }
}
