package dev.ixpu.leaguemechanics.rune;

public enum RuneSlot {
    KEYSTONE(0, "keystone", false),
    PRIMARY_SLOT_1(1, "primary-slot-1", false),
    PRIMARY_SLOT_2(2, "primary-slot-2", false),
    PRIMARY_SLOT_3(3, "primary-slot-3", false),
    SECONDARY_SLOT_1(4, "secondary-slot-1", false),
    SECONDARY_SLOT_2(5, "secondary-slot-2", false),
    SHARD_SLOT_1(6, "shard-slot-1", false),
    SHARD_SLOT_2(7, "shard-slot-2", false),
    SHARD_SLOT_3(8, "shard-slot-3", false);

    private final int position;
    private final String id;
    private final boolean isSecondary;

    RuneSlot(int position, String id, boolean isSecondary) {
        this.position = position;
        this.id = id;
        this.isSecondary = isSecondary;
    }

    public int getPosition() {
        return position;
    }

    public String getId() {
        return id;
    }

    public boolean isSecondary() {
        return isSecondary;
    }

    public static RuneSlot fromId(String id) {
        for (RuneSlot slot : values()) {
            if (slot.id.equalsIgnoreCase(id)) {
                return slot;
            }
        }
        return null;
    }
}
