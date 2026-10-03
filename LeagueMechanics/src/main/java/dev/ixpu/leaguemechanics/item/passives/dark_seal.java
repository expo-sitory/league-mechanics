package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.manager.ItemPassivesManager;
import dev.ixpu.leaguemechanics.util.Icons;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class dark_seal implements ItemPassive {
    private static final int STACKS_PER_KILL = 2;
    private static final int MAX_STACKS = 10;
    private static final int AP_PER_STACK = 15;

    @Override
    public String getId() {
        return "dark-seal";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ɢʟᴏʀʏ: §fGain " + STACKS_PER_KILL + " stacks §ffor each player kill,\n§fup to a maximum of " + MAX_STACKS + " stacks§f. §fFor every stack, gain\n§f" +
        Icons.get("stats.ap") + " §9" + AP_PER_STACK + " bonus ability power§f, up to §930 §fat maximum stacks.\n§fLose 2 stacks on death, Lose all stacks on disconnect";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {
        ItemPassivesManager manager = ItemPassivesManager.getInstance();

        int currentStacks = manager.getKillCount(player, getId());
        int newStacks = Math.min(currentStacks + STACKS_PER_KILL, MAX_STACKS);

        manager.setKillCount(player, getId(), newStacks);
    }

    public int getStacks(Player player) {
        ItemPassivesManager manager = ItemPassivesManager.getInstance();
        return Math.min(manager.getKillCount(player, getId()), MAX_STACKS);
    }

    public double getAbilityPower(Player player) {
        return getStacks(player) * AP_PER_STACK;
    }

    public void clearStacks(Player player) {
        ItemPassivesManager manager = ItemPassivesManager.getInstance();
        manager.setKillCount(player, getId(), 0);
    }
}