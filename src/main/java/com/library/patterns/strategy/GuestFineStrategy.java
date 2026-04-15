package com.library.patterns.strategy;

public class GuestFineStrategy implements FineCalculationStrategy {
    private static final double DAILY_RATE = 100.0; // LKR 100 per day for guests

    @Override
    public double calculateFine(int overdueDays) {
        return overdueDays * DAILY_RATE;
    }

    @Override
    public double getDailyRate() {
        return DAILY_RATE;
    }
}