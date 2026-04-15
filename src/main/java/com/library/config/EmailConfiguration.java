package com.library.config;

public class EmailConfiguration {
    // SMTP Configuration
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final int SMTP_PORT = 587;
    public static final String SMTP_USERNAME = "zevindilneth@gmail.com";
    public static final String SMTP_PASSWORD = "tpta ahrv cptc tohi"; // Use app password without spaces
    public static final String SENDER_EMAIL = "zevindilneth@gmail.com";
    public static final String SENDER_NAME = "Smart Library Management System";

    // Email Settings
    public static final boolean USE_SSL = false;
    public static final boolean USE_TLS = true;
    public static final boolean DEBUG = true; // Set to true for debugging

    // Email Templates
    public static final String SUBJECT_DUE_DATE = "📚 Due Date Reminder - SLMS";
    public static final String SUBJECT_OVERDUE = "⚠️ Overdue Alert - SLMS";
    public static final String SUBJECT_RESERVATION = "✅ Reservation Available - SLMS";
    public static final String SUBJECT_GENERAL = "ℹ️ Notification - SLMS";
    public static final String SUBJECT_FINE = "💰 Fine Notification - SLMS";
    public static final String SUBJECT_BOOK_ADDED = "📖 New Book Added - SLMS";
    public static final String SUBJECT_MEMBERSHIP = "👤 Membership Update - SLMS";

    // Email scheduling intervals (in minutes)
    public static final int DUE_DATE_CHECK_INTERVAL = 1;      // Check every minute for testing
    public static final int OVERDUE_CHECK_INTERVAL = 1;       // Check every minute for testing
    public static final int RESERVATION_CHECK_INTERVAL = 1;   // Check every minute for testing

    // For production, use these values:
    // public static final int DUE_DATE_CHECK_INTERVAL = 60;      // Check every hour
    // public static final int OVERDUE_CHECK_INTERVAL = 1440;     // Check every day
    // public static final int RESERVATION_CHECK_INTERVAL = 30;   // Check every 30 minutes

    private EmailConfiguration() {
        // Private constructor to prevent instantiation
    }
}