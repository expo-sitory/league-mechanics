package dev.ixpu.leaguemechanics.rune.paths;

public enum RunePath {
    PRECISION("precision"),
    DOMINATION("domination"),
    RESOLVE("resolve"),
    SORCERY("sorcery"),
    INSPIRATION("inspiration");

    private final String id;

    RunePath(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static RunePath fromId(String id) {
        for (RunePath path : values()) {
            if (path.id.equalsIgnoreCase(id)) {
                return path;
            }
        }
        return null;
    }
}
