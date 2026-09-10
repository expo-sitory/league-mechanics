package dev.ixpu.leaguemechanics.rune;

public enum RuneShard {

    ROW1_ADAP("row1", "option-1", "Adaptive Force", 1),
    ROW1_AS("row1", "option-2", "Attack Speed", 1),
    ROW1_CH("row1", "option-3", "Cooldown Haste", 1),

    ROW2_ADAP("row2", "option-1", "Adaptive Force", 2),
    ROW2_MS("row2", "option-2", "Movement Speed", 2),
    ROW2_HR("row2", "option-3", "Heatlh Regen", 2),

    ROW3_HP("row3", "option-1", "Health", 3),
    ROW3_TN("row3", "option-2", "Tenacity", 3),
    ROW3_HR("row3", "option-3", "Health Regen", 3);

    private final String rowId;
    private final String optionId;
    private final String display;
    private final int rowNumber;

    RuneShard(String rowId, String optionId, String display, int rowNumber) {
        this.rowId = rowId;
        this.optionId = optionId;
        this.display = display;
        this.rowNumber = rowNumber;
    }

    public static RuneShard fromRowAndOption(int row, String option) {
        for (RuneShard shard : values()) {
            if (shard.rowNumber == row && shard.optionId.equals(option)) {
                return shard;
            }
        }
        return null;
    }

    public String getRowId() { return rowId; }
    public String getOptionId() { return optionId; }
    public String getDisplay() { return display; }
    public int getRowNumber() { return rowNumber; }
}