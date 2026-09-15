package dev.ixpu.leaguemechanics.entity.player;

public enum PlayerClassType {
    FIGHTER("fighter", "Fighter"),
    SUPPORT("support", "Support"),
    ASSASSIN("assassin", "Assassin"),
    MAGE("mage", "Mage"),
    TANK("tank", "Tank"),
    MARKSMAN("marksman", "Marksman");

    private final String id;
    private final String displayName;

    PlayerClassType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PlayerClassType fromId(String id) {
        for (PlayerClassType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    public static String[] getAllIds() {
        PlayerClassType[] types = values();
        String[] ids = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            ids[i] = types[i].id;
        }
        return ids;
    }
}
