package com.library.patterns.strategy;

public class StudentFineStrategy implements FineCalculationStrategy {
    private static final double DAILY_RATE = 50.0; // LKR 50 per day for students

    @Override
    public double calculateFine(int overdueDays) {
        return overdueDays * DAILY_RATE;
    }

    @Override
    public double getDailyRate() {
        return DAILY_RATE;
    }
}