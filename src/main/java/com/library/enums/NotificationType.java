package com.library.enums;

public enum NotificationType {
    DUE_DATE_REMINDER("Due Date Reminder"),
    OVERDUE_ALERT("Overdue Alert"),
    FINE_NOTIFICATION("Fine Notification"),
    RESERVATION_AVAILABLE("Reservation Available"),
    BOOK_ADDED("New Book Added"),
    BOOK_UPDATED("Book Updated"),
    GENERAL("General Notification"),
    FINE_PAID("Fine Paid"),
    MEMBERSHIP_UPDATE("Membership Update");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}