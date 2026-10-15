package dev.ixpu.leaguemechanics.util;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RuneRegistry {
    private static final RuneRegistry instance = new RuneRegistry();
    private final Map<String, CooldownHandler> runes = new HashMap<>();

    private RuneRegistry() {}

    public static RuneRegistry getInstance() {
        return instance;
    }

    public void registerRune(CooldownHandler rune) {
        runes.put(rune.getId(), rune);
    }

    public CooldownHandler getRune(String id) {
        return runes.get(id);
    }

    public boolean containsRune(String id) {
        return runes.containsKey(id);
    }

    public Map<String, CooldownHandler> getAllRunes() {
        return Collections.unmodifiableMap(runes);
    }

    public void clearRegistry() {
        runes.clear();
    }
}
