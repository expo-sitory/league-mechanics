package dev.ixpu.leaguemechanics.util;

import dev.ixpu.leaguemechanics.LeagueMechanics;

public final class Icons {
    private Icons() {}

    public static String get(String key) {
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        if (plugin == null) return "";
        return plugin.getConfig().getString("icons." + key, "");
    }
}