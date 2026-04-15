package com.library.patterns.strategy;

public class FacultyFineStrategy implements FineCalculationStrategy {
    private static final double DAILY_RATE = 20.0; // LKR 20 per day for faculty

    @Override
    public double calculateFine(int overdueDays) {
        return overdueDays * DAILY_RATE;
    }

    @Override
    public double getDailyRate() {
        return DAILY_RATE;
    }
}