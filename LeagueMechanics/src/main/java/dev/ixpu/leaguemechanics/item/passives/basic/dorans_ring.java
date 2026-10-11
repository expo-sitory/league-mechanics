package dev.ixpu.leaguemechanics.item.passives.basic;

import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.util.Icons;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class dorans_ring implements ItemPassive {
    private static final int ONHIT_AD_BONUS = 10;

    @Override
    public String getId() {
        return "dorans-ring";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ʜᴇʟᴘɪɴɢ ʜᴀɴᴅ: §fAttacks deal " + Icons.get("stats.physical-damage") + " §6" + ONHIT_AD_BONUS + " bonus physical damage\n§fon-hit against targets§r.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {
    }

    public int getOnHitAdBonus() {
        return ONHIT_AD_BONUS;
    }
}