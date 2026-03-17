package model;

public enum Category {
    GEOGRAPHY("Geography"),
    MATH("Math"),
    SCIENCE("Science"),
    HISTORY("History"),
    SPORTS("Sports"),
    TECHNOLOGY("Technology"),
    LITERATURE("Literature"),
    GENERAL_KNOWLEDGE("GeneralKnowledge");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Category fromValue(String value) {
        for (Category category : values()) {
            if (category.displayName.equalsIgnoreCase(value) || category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown category: " + value);
    }
}
