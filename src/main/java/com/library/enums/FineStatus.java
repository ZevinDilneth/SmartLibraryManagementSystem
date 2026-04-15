package com.library.enums;

public enum FineStatus {
    PENDING("Pending"),
    PAID("Paid"),
    WAIVED("Waived"),
    OVERDUE("Overdue");

    private final String displayName;

    FineStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}