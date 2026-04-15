package com.library.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class IDGenerator {
    private static final AtomicInteger bookCounter = new AtomicInteger(1000);
    private static final AtomicInteger userCounter = new AtomicInteger(1000);
    private static final AtomicInteger borrowCounter = new AtomicInteger(1000);
    private static final AtomicInteger reservationCounter = new AtomicInteger(1000);
    private static final AtomicInteger fineCounter = new AtomicInteger(1000);

    // Book IDs: B1001, B1002, etc.
    public static String generateBookId() {
        return "B" + bookCounter.incrementAndGet();
    }

    // User IDs: U1001, U1002, etc.
    public static String generateUserId() {
        return "U" + userCounter.incrementAndGet();
    }

    // Borrow Record IDs: BR1001, BR1002, etc.
    public static String generateBorrowRecordId() {
        return "BR" + borrowCounter.incrementAndGet();
    }

    // Reservation IDs: RS1001, RS1002, etc.
    public static String generateReservationId() {
        return "RS" + reservationCounter.incrementAndGet();
    }

    // Fine IDs: FN1001, FN1002, etc.
    public static String generateFineId() {
        return "FN" + fineCounter.incrementAndGet();
    }

    // Reset counters (for testing)
    public static void resetCounters() {
        bookCounter.set(1000);
        userCounter.set(1000);
        borrowCounter.set(1000);
        reservationCounter.set(1000);
        fineCounter.set(1000);
    }
}