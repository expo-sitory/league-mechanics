package dev.ixpu.leaguemechanics.player;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RunePath;
import org.bukkit.entity.Player;

public class PlayerRuneData {
    private final Player player;
    private RunePath primaryPath;
    private RunePath secondaryPath;
    private CooldownHandler keystoneRune;
    private CooldownHandler primarySlot1Rune;
    private CooldownHandler primarySlot2Rune;
    private CooldownHandler primarySlot3Rune;
    private CooldownHandler secondarySlot1Rune;
    private CooldownHandler secondarySlot2Rune;
    private CooldownHandler shardSlot1Rune;
    private CooldownHandler shardSlot2Rune;
    private CooldownHandler shardSlot3Rune;

    public PlayerRuneData(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public RunePath getPrimaryPath() {
        return primaryPath;
    }

    public void setPrimaryPath(RunePath path) {
        this.primaryPath = path;
    }

    public RunePath getSecondaryPath() {
        return secondaryPath;
    }

    public void setSecondaryPath(RunePath path) {
        this.secondaryPath = path;
    }

    public CooldownHandler getKeystoneRune() {
        return keystoneRune;
    }

    public void setKeystoneRune(CooldownHandler rune) {
        this.keystoneRune = rune;
    }

    public CooldownHandler getPrimarySlot1Rune() {
        return primarySlot1Rune;
    }

    public void setPrimarySlot1Rune(CooldownHandler rune) {
        this.primarySlot1Rune = rune;
    }

    public CooldownHandler getPrimarySlot2Rune() {
        return primarySlot2Rune;
    }

    public void setPrimarySlot2Rune(CooldownHandler rune) {
        this.primarySlot2Rune = rune;
    }

    public CooldownHandler getPrimarySlot3Rune() {
        return primarySlot3Rune;
    }

    public void setPrimarySlot3Rune(CooldownHandler rune) {
        this.primarySlot3Rune = rune;
    }

    public CooldownHandler getSecondarySlot1Rune() {
        return secondarySlot1Rune;
    }

    public void setSecondarySlot1Rune(CooldownHandler rune) {
        this.secondarySlot1Rune = rune;
    }

    public CooldownHandler getSecondarySlot2Rune() {
        return secondarySlot2Rune;
    }

    public void setSecondarySlot2Rune(CooldownHandler rune) {
        this.secondarySlot2Rune = rune;
    }

    public CooldownHandler getShardSlot1Rune() {
        return shardSlot1Rune;
    }

    public void setShardSlot1Rune(CooldownHandler rune) {
        this.shardSlot1Rune = rune;
    }

    public CooldownHandler getShardSlot2Rune() {
        return shardSlot2Rune;
    }

    public void setShardSlot2Rune(CooldownHandler rune) {
        this.shardSlot2Rune = rune;
    }

    public CooldownHandler getShardSlot3Rune() {
        return shardSlot3Rune;
    }

    public void setShardSlot3Rune(CooldownHandler rune) {
        this.shardSlot3Rune = rune;
    }

    public CooldownHandler[] getAllRunes() {
        return new CooldownHandler[] {
            keystoneRune,
            primarySlot1Rune,
            primarySlot2Rune,
            primarySlot3Rune,
            secondarySlot1Rune,
            secondarySlot2Rune,
            shardSlot1Rune,
            shardSlot2Rune,
            shardSlot3Rune
        };
    }
}
