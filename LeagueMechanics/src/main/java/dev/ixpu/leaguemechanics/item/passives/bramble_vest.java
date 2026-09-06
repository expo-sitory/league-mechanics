package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.rune.DebuffType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class bramble_vest implements ItemPassive {
    private static final int THORNS_MAGIC_DAMAGE = 10;
    private static final int GRIEVOUS_DURATION_TICKS = 60;

    @Override
    public String getId() {
        return "bramble-vest";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ᴛʜᴏʀɴs: §fWhen struck by an attack, deal §910\n§9magic damage §fto the attacker. If they are a player,\n§finflict them with §cɢʀɪᴇᴠᴏᴜs ᴡᴏᴜɴᴅs §ffor 3 seconds.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onTakeDamage(Player victim, Player attacker, double damage, boolean isMagic) {
        if (attacker == null || !attacker.isOnline()) return;

        DamageManager dm = new DamageManager(
            dev.ixpu.leaguemechanics.LeagueMechanics.getInstance().getStatsManager()
        );
        dm.enableOnlyAP();
        dm.DamageCalculation(victim, attacker, 0, THORNS_MAGIC_DAMAGE, 0);

        double attackerHealth = Math.max(0, attacker.getHealth() - THORNS_MAGIC_DAMAGE);
        KillSourceTracker.getInstance().setSource(attacker, victim);
        attacker.damage(0.00001);
        attacker.setHealth(attackerHealth);

        DebuffManager.getInstance().applyDebuff(attacker, DebuffType.GRIEVOUS_WOUNDS, GRIEVOUS_DURATION_TICKS);
    }
}
