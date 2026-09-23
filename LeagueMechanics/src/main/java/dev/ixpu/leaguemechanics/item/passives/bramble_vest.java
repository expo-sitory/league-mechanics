package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.rune.DebuffType;
import dev.ixpu.leaguemechanics.util.DebugLogger;
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
        return "§7ᴜɴɪQᴜᴇ – ᴛʜᴏʀɴs: §fWhen struck by an attack, deal §9" + THORNS_MAGIC_DAMAGE + "\n§9magic damage §fto the source. If they are a player,\n§finflict them with §cɢʀɪᴇᴠᴏᴜs ᴡᴏᴜɴᴅs §ffor 3 seconds.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onTakeDamage(Player target, Player source, double damage, boolean isMagic) {
        if (source == null || !source.isOnline()) return;

        double damageToApply = procDamage(source, target);

        KillSourceTracker.getInstance().setSource(source, target);
        target.damage(damageToApply);
        DebugLogger.debug(target, "§f[§dSource§f] §f[§6Bramble Vest§f] Thorns Damage = §d" + Math.ceil(damageToApply * 100) / 100.0 + "§f | Type = §dMagic Damage");
        DebuffManager.getInstance().applyDebuff(source, DebuffType.GRIEVOUS_WOUNDS, GRIEVOUS_DURATION_TICKS);
    }

    private double procDamage(Player player, Entity target) {
        DamageManager damageManager = new DamageManager(LeagueMechanics.getInstance().getStatsManager());
        damageManager.enableOnlyAP();
        damageManager.enableItemProc();
        double baseDamage = damageManager.DamageCalculation(player, target, 0, 0, 0, THORNS_MAGIC_DAMAGE);

        return baseDamage;
    }
}
