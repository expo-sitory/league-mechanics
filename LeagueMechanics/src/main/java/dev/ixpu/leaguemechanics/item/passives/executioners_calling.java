package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.rune.DebuffType;
import dev.ixpu.leaguemechanics.util.Icons;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class executioners_calling implements ItemPassive {
    private static final int GRIEVOUS_DURATION_TICKS = 60;

    @Override
    public String getId() {
        return "executioners-calling";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ɢʀɪᴇᴠᴏᴜs ᴡᴏᴜɴᴅs: §fDealing " + Icons.get("stats.physical-damage") + " §6physical damage §fto a\n§fplayer inflicts them with " + Icons.get("status.grievous-wounds") + " §4ɢʀɪᴇᴠᴏᴜs ᴡᴏᴜɴᴅs §ffor 3 seconds.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onDealDamage(Player attacker, LivingEntity target, double damage,
                             boolean isPhysical, boolean isMagic) {
        if (target instanceof Player targetPlayer && isPhysical) {
            DebuffManager.getInstance().applyDebuff(targetPlayer, DebuffType.GRIEVOUS_WOUNDS, GRIEVOUS_DURATION_TICKS);
        }
    }
}
