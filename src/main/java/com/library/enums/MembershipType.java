package com.library.enums;

public enum MembershipType {
    STUDENT("Student", 14, 5, 50.0), // 14-day loan period, LKR 50/day fine
    FACULTY("Faculty", 30, 10, 20.0), // 30-day loan period, LKR 20/day fine
    GUEST("Guest", 7, 2, 100.0); // 7-day loan period, LKR 100/day fine

    private final String displayName;
    private final int loanPeriod;
    private final int borrowingLimit;
    private final double dailyFine;

    MembershipType(String displayName, int loanPeriod, int borrowingLimit, double dailyFine) {
        this.displayName = displayName;
        this.loanPeriod = loanPeriod;
        this.borrowingLimit = borrowingLimit;
        this.dailyFine = dailyFine;
    }

    public String getDisplayName() { return displayName; }
    public int getLoanPeriod() { return loanPeriod; }
    public int getBorrowingLimit() { return borrowingLimit; }
    public double getDailyFine() { return dailyFine; }
}