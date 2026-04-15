package com.library.patterns.strategy;

public interface FineCalculationStrategy {
    double calculateFine(int overdueDays);
    double getDailyRate();
}