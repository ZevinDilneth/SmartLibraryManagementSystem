package com.library.services;

import com.library.models.BorrowRecord;
import com.library.models.Reservation;
import com.library.models.Book;
import com.library.enums.BookStatus;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.stream.Collectors;

public class EmailSchedulerService {
    private static EmailSchedulerService instance;
    private ScheduledExecutorService scheduler;
    private LibraryService libraryService;
    private NotificationService notificationService;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Email scheduling intervals (in SECONDS) - CHANGED FROM MINUTES TO SECONDS
    private static final int DUE_DATE_CHECK_INTERVAL = 10; // Check every 10 seconds
    private static final int OVERDUE_CHECK_INTERVAL = 10;  // Check every 10 seconds
    private static final int RESERVATION_CHECK_INTERVAL = 10; // Check every 10 seconds

    // Track last sent emails to avoid duplicates
    private Map<String, Date> lastDueDateReminders = new HashMap<>();
    private Map<String, Date> lastOverdueAlerts = new HashMap<>();
    private Map<String, Date> lastFineNotifications = new HashMap<>();

    private EmailSchedulerService() {
        scheduler = Executors.newScheduledThreadPool(3);
        libraryService = LibraryService.getInstance();
        notificationService = NotificationService.getInstance();
    }

    public static EmailSchedulerService getInstance() {
        if (instance == null) {
            instance = new EmailSchedulerService();
        }
        return instance;
    }

    /**
     * Start all email schedulers
     */
    public void startAllSchedulers() {
        System.out.println("🚀 Starting email schedulers...");
        System.out.println("Current time: " + DATETIME_FORMAT.format(new Date()));
        System.out.println("📢 ALL CHECKS WILL RUN EVERY 10 SECONDS");

        // Start due date reminder scheduler - CHANGED TO SECONDS
        scheduler.scheduleAtFixedRate(
                this::checkDueDateReminders,
                0, // Initial delay (start immediately)
                DUE_DATE_CHECK_INTERVAL,
                TimeUnit.SECONDS  // CHANGED FROM MINUTES TO SECONDS
        );

        // Start overdue alert scheduler - CHANGED TO SECONDS
        scheduler.scheduleAtFixedRate(
                this::checkOverdueBooks,
                2, // Initial delay (2 seconds)
                OVERDUE_CHECK_INTERVAL,
                TimeUnit.SECONDS  // CHANGED FROM MINUTES TO SECONDS
        );

        // Start reservation availability scheduler - CHANGED TO SECONDS
        scheduler.scheduleAtFixedRate(
                this::checkAvailableReservations,
                1, // Initial delay (1 second)
                RESERVATION_CHECK_INTERVAL,
                TimeUnit.SECONDS  // CHANGED FROM MINUTES TO SECONDS
        );

        System.out.println("✅ All email schedulers started successfully!");
        System.out.println("Due Date Check: Every " + DUE_DATE_CHECK_INTERVAL + " seconds");
        System.out.println("Overdue Check: Every " + OVERDUE_CHECK_INTERVAL + " seconds");
        System.out.println("Reservation Check: Every " + RESERVATION_CHECK_INTERVAL + " seconds");
    }

    /**
     * Stop all email schedulers
     */
    public void stopAllSchedulers() {
        System.out.println("🛑 Stopping email schedulers...");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                System.out.println("✅ Email schedulers stopped successfully");
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
                System.err.println("❌ Error stopping schedulers: " + e.getMessage());
            }
        }
    }

    /**
     * Check for books that are due tomorrow and send reminders
     */
    private void checkDueDateReminders() {
        System.out.println("\n📅 [Due Date Check] Running at: " + DATETIME_FORMAT.format(new Date()));

        try {
            List<BorrowRecord> activeRecords = libraryService.getAllBorrowRecords()
                    .stream()
                    .filter(record -> record.getReturnDate() == null)
                    .collect(Collectors.toList());

            Date tomorrow = getTomorrowDate();
            Date today = new Date();
            int remindersSent = 0;

            System.out.println("Checking " + activeRecords.size() + " active borrow records...");

            for (BorrowRecord record : activeRecords) {
                if (record.getDueDate() != null) {
                    Date dueDate = record.getDueDate();
                    String recordKey = record.getRecordId();

                    // Check if due date is tomorrow
                    if (isSameDay(dueDate, tomorrow)) {
                        // Check if we already sent a reminder for this record today
                        if (shouldSendReminder(recordKey, lastDueDateReminders)) {
                            System.out.println("📨 Sending due date reminder for: " +
                                    record.getBook().getTitle() +
                                    " to user: " + record.getUser().getName());

                            // Send due date reminder
                            notificationService.sendDueDateReminder(record);

                            // Update last sent time
                            lastDueDateReminders.put(recordKey, new Date());
                            remindersSent++;

                            System.out.println("✓ Reminder sent: " +
                                    record.getUser().getName() +
                                    " - Book '" + record.getBook().getTitle() +
                                    "' due tomorrow!");
                        } else {
                            System.out.println("⏭️  Skipping - reminder already sent today for: " +
                                    record.getBook().getTitle());
                        }
                    }

                    // Check if due date is today (same day reminder)
                    if (isSameDay(dueDate, today)) {
                        String todayKey = recordKey + "_today";
                        if (shouldSendReminder(todayKey, lastDueDateReminders)) {
                            System.out.println("📨 Sending today's due date reminder for: " +
                                    record.getBook().getTitle());

                            notificationService.sendGeneralNotification(
                                    record.getUser().getUserId(),
                                    "⏰ FINAL REMINDER: Today is the due date for book: '" +
                                            record.getBook().getTitle() +
                                            "'. Please return it today to avoid fines."
                            );

                            lastDueDateReminders.put(todayKey, new Date());
                            remindersSent++;
                        } else {
                            System.out.println("⏭️  Skipping - today reminder already sent for: " +
                                    record.getBook().getTitle());
                        }
                    }
                }
            }

            if (remindersSent == 0) {
                System.out.println("✓ No due date reminders needed at this time");
            } else {
                System.out.println("✓ Sent " + remindersSent + " due date reminders");
            }

        } catch (Exception e) {
            System.err.println("❌ Error in due date check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check for overdue books and send daily alerts with updated fines
     */
    private void checkOverdueBooks() {
        System.out.println("\n⚠️ [Overdue Check] Running at: " + DATETIME_FORMAT.format(new Date()));

        try {
            List<BorrowRecord> overdueRecords = libraryService.getOverdueBooks();
            int alertsSent = 0;
            int fineNotificationsSent = 0;

            System.out.println("Found " + overdueRecords.size() + " overdue records");

            for (BorrowRecord record : overdueRecords) {
                String recordKey = record.getRecordId();

                // Check if we should send an alert (once per day)
                if (shouldSendReminder(recordKey, lastOverdueAlerts)) {
                    System.out.println("📨 Sending overdue alert for: " +
                            record.getBook().getTitle() +
                            " (Overdue by " + record.getOverdueDays() + " days) to user: " +
                            record.getUser().getName());

                    // Send overdue alert with current fine calculation
                    notificationService.sendOverdueAlert(record);

                    // Update last sent time
                    lastOverdueAlerts.put(recordKey, new Date());
                    alertsSent++;

                    // Send separate fine notification
                    double fine = libraryService.calculateFine(record);
                    if (fine > 0) {
                        String fineKey = recordKey + "_fine";
                        if (shouldSendReminder(fineKey, lastFineNotifications)) {
                            System.out.println("💰 Sending fine notification: LKR " + fine +
                                    " for " + record.getOverdueDays() + " days overdue");
                            notificationService.sendFineNotification(
                                    record.getUser().getUserId(),
                                    fine,
                                    "Daily overdue fine for book: " + record.getBook().getTitle() +
                                            " (Overdue by " + record.getOverdueDays() + " days)"
                            );
                            lastFineNotifications.put(fineKey, new Date());
                            fineNotificationsSent++;
                        } else {
                            System.out.println("⏭️  Skipping - fine already sent today for: " +
                                    record.getBook().getTitle());
                        }
                    }

                    System.out.println("✓ Overdue alert sent: " +
                            record.getUser().getName() +
                            " - Book '" + record.getBook().getTitle() +
                            "' is " + record.getOverdueDays() + " days overdue.");
                } else {
                    System.out.println("⏭️  Skipping - overdue alert already sent today for: " +
                            record.getBook().getTitle());
                }
            }

            if (alertsSent == 0 && fineNotificationsSent == 0) {
                System.out.println("✓ No overdue alerts needed at this time");
            } else {
                System.out.println("✓ Sent " + alertsSent + " overdue alerts and " +
                        fineNotificationsSent + " fine notifications");
            }

        } catch (Exception e) {
            System.err.println("❌ Error in overdue check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check for available reservations and notify users
     */
    private void checkAvailableReservations() {
        System.out.println("\n🔔 [Reservation Check] Running at: " + DATETIME_FORMAT.format(new Date()));

        try {
            // Get all reservations that haven't been notified and aren't expired
            List<Reservation> pendingReservations = libraryService.getAllReservations()
                    .stream()
                    .filter(reservation -> !reservation.isNotified() && !reservation.isExpired())
                    .collect(Collectors.toList());

            System.out.println("Checking " + pendingReservations.size() + " pending reservations...");
            int notificationsSent = 0;

            for (Reservation reservation : pendingReservations) {
                Book book = reservation.getBook();
                if (book != null && book.getStatus() == BookStatus.AVAILABLE) {
                    System.out.println("📨 Book available for reservation: " +
                            book.getTitle() +
                            " for user: " +
                            reservation.getUser().getName());

                    // Send reservation available notification
                    boolean emailSent = notificationService.sendReservationAvailable(
                            reservation.getUser().getUserId(),
                            book
                    );

                    if (emailSent) {
                        // Mark as notified
                        reservation.setNotified(true);
                        notificationsSent++;

                        System.out.println("✓ Reservation notification sent: " +
                                reservation.getUser().getName() +
                                " - Book '" + book.getTitle() + "' is now available!");

                        // Update book status based on remaining reservations
                        long remainingReservations = libraryService.getAllReservations()
                                .stream()
                                .filter(r -> r.getBook().getBookId().equals(book.getBookId()) &&
                                        !r.isNotified() && !r.isExpired())
                                .count();

                        if (remainingReservations > 0) {
                            book.setStatus(BookStatus.RESERVED);
                            System.out.println("Book status updated to RESERVED (more reservations pending)");
                        }
                    } else {
                        System.err.println("❌ Failed to send reservation notification to: " +
                                reservation.getUser().getName());
                    }
                }
            }

            if (notificationsSent == 0) {
                System.out.println("✓ No reservation notifications needed at this time");
            } else {
                System.out.println("✓ Sent " + notificationsSent + " reservation notifications");
            }

        } catch (Exception e) {
            System.err.println("❌ Error in reservation check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if we should send a reminder (prevents duplicate emails)
     * MODIFIED: For testing every 10 seconds, we'll allow more frequent checks
     * but still prevent spam by checking if sent within last 10 minutes
     */
    private boolean shouldSendReminder(String key, Map<String, Date> lastSentMap) {
        if (!lastSentMap.containsKey(key)) {
            return true;
        }

        Date lastSent = lastSentMap.get(key);
        Date now = new Date();
        long secondsSinceLastSent = (now.getTime() - lastSent.getTime()) / 1000;

        // For testing: Allow sending every 10 minutes instead of 24 hours
        // This prevents spam while still allowing frequent testing
        return secondsSinceLastSent >= 600; // 10 minutes
    }

    /**
     * Get tomorrow's date at midnight
     */
    private Date getTomorrowDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * Check if two dates are on the same day (ignoring time)
     */
    private boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Manually trigger all checks (for testing)
     */
    public void triggerManualCheck() {
        System.out.println("\n🔧 Triggering manual email checks...");
        System.out.println("Time: " + DATETIME_FORMAT.format(new Date()));

        checkDueDateReminders();
        checkOverdueBooks();
        checkAvailableReservations();

        System.out.println("✅ Manual checks completed!");
    }

    /**
     * Get scheduler status
     */
    public String getSchedulerStatus() {
        if (scheduler == null || scheduler.isShutdown()) {
            return "❌ Scheduler is not running";
        }

        return "✅ Scheduler is running.\n" +
                "📅 Due Date Check: Every " + DUE_DATE_CHECK_INTERVAL + " seconds\n" +
                "⚠️ Overdue Check: Every " + OVERDUE_CHECK_INTERVAL + " seconds\n" +
                "🔔 Reservation Check: Every " + RESERVATION_CHECK_INTERVAL + " seconds\n" +
                "📊 Statistics:\n" +
                "   - Due date reminders sent: " + lastDueDateReminders.size() + "\n" +
                "   - Overdue alerts sent: " + lastOverdueAlerts.size() + "\n" +
                "   - Fine notifications sent: " + lastFineNotifications.size() + "\n" +
                "⚠️ NOTE: Emails are prevented from being sent more than once every 10 minutes\n" +
                "   to avoid spam during testing.";
    }

    /**
     * Clear all sent reminders (for testing)
     */
    public void clearSentReminders() {
        lastDueDateReminders.clear();
        lastOverdueAlerts.clear();
        lastFineNotifications.clear();
        System.out.println("🧹 Cleared all sent reminder history");
    }
}