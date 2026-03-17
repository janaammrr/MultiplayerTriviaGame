package model;

public enum DifficultyLevel {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    private final String displayName;

    DifficultyLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DifficultyLevel fromValue(String value) {
        for (DifficultyLevel level : values()) {
            if (level.displayName.equalsIgnoreCase(value) || level.name().equalsIgnoreCase(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown difficulty: " + value);
    }
}
