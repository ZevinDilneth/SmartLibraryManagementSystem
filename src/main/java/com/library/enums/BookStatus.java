package com.library.enums;

public enum BookStatus {
    AVAILABLE("Available"),
    BORROWED("Borrowed"),
    RESERVED("Reserved"),
    BORROWED_RESERVED("Borrowed/Reserved");  // New status

    private final String displayName;

    BookStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}