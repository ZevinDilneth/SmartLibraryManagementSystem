package com.library.services;

import com.library.models.User;
import com.library.models.Book;
import com.library.models.BorrowRecord;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationService {
    private static NotificationService instance;
    private List<Notification> notifications = new ArrayList<>();
    private EmailService emailService;

    private NotificationService() {
        emailService = EmailService.getInstance();
    }

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    public void sendDueDateReminder(BorrowRecord record) {
        String message = String.format("Book '%s' is due on %s",
                record.getBook().getTitle(),
                record.getDueDate().toString());

        Notification notification = new Notification(
                generateId(),
                record.getUser().getUserId(),
                "Due Date Reminder",
                message,
                new Date()
        );
        notifications.add(notification);

        // Send email notification
        try {
            boolean emailSent = emailService.sendDueDateReminderEmail(record.getUser(), record);
            if (emailSent) {
                System.out.println("✅ Due date reminder email sent to: " + record.getUser().getEmail());
            } else {
                System.err.println("❌ Failed to send due date reminder email to: " + record.getUser().getEmail());
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to send email notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendOverdueAlert(BorrowRecord record) {
        double fineAmount = LibraryService.getInstance().calculateFine(record);
        String message = String.format("Book '%s' is overdue by %d days. Fine: LKR %.2f",
                record.getBook().getTitle(),
                record.getOverdueDays(),
                fineAmount);

        Notification notification = new Notification(
                generateId(),
                record.getUser().getUserId(),
                "Overdue Alert",
                message,
                new Date()
        );
        notifications.add(notification);

        // Send email notification
        try {
            boolean emailSent = emailService.sendOverdueAlertEmail(record.getUser(), record, fineAmount);
            if (emailSent) {
                System.out.println("✅ Overdue alert email sent to: " + record.getUser().getEmail());
            } else {
                System.err.println("❌ Failed to send overdue alert email to: " + record.getUser().getEmail());
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to send email notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean sendReservationAvailable(String userId, Book book) {
        // Get user from LibraryService
        LibraryService libraryService = LibraryService.getInstance();
        User user = libraryService.getUser(userId);

        if (user == null) {
            System.err.println("❌ Cannot send reservation notification: User not found - " + userId);
            System.err.println("📋 Available users: " + libraryService.getAllUsers().stream()
                    .map(User::getUserId)
                    .collect(Collectors.toList()));
            return false;
        }

        if (book == null) {
            System.err.println("❌ Cannot send reservation notification: Book is null");
            return false;
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            System.err.println("❌ Cannot send reservation notification: User has no email - " + user.getName());
            return false;
        }

        String message = String.format("Reserved book '%s' is now available for pickup",
                book.getTitle());

        Notification notification = new Notification(
                generateId(),
                userId,
                "Reservation Available",
                message,
                new Date()
        );
        notifications.add(notification);

        // Send email notification
        try {
            System.out.println("📧 Sending reservation notification to: " + user.getEmail());
            System.out.println("📚 Book: " + book.getTitle());
            System.out.println("👤 User: " + user.getName());

            boolean emailSent = emailService.sendReservationAvailableEmail(user, book);
            if (emailSent) {
                System.out.println("✅ Reservation available email sent to: " + user.getEmail());
                return true;
            } else {
                System.err.println("❌ Failed to send reservation email to: " + user.getEmail());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Exception sending reservation notification to " + user.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void sendGeneralNotification(String userId, String message) {
        // Get user from LibraryService
        LibraryService libraryService = LibraryService.getInstance();
        User user = libraryService.getUser(userId);

        if (user == null) {
            System.err.println("❌ Cannot send general notification: User not found - " + userId);
            System.err.println("📋 Available users: " + libraryService.getAllUsers().stream()
                    .map(User::getUserId)
                    .collect(Collectors.toList()));
            return;
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            System.err.println("❌ Cannot send general notification: User has no email - " + user.getName());
            return;
        }

        Notification notification = new Notification(
                generateId(),
                userId,
                "General Notification",
                message,
                new Date()
        );
        notifications.add(notification);

        // Send email notification
        try {
            System.out.println("📧 Attempting to send general notification to: " + user.getEmail());
            boolean emailSent = emailService.sendGeneralNotificationEmail(user, message);
            if (emailSent) {
                System.out.println("✅ General notification email sent to: " + user.getEmail());
            } else {
                System.err.println("❌ Failed to send general notification email to: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to send email notification to " + user.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendBookActionNotification(String userId, Book book, String action, String additionalInfo) {
        LibraryService libraryService = LibraryService.getInstance();
        User user = libraryService.getUser(userId);

        if (user == null) {
            System.err.println("❌ Cannot send book action notification: User not found - " + userId);
            System.err.println("📋 Available users: " + libraryService.getAllUsers().stream()
                    .map(User::getUserId)
                    .collect(Collectors.toList()));
            return;
        }

        if (book == null) {
            System.err.println("❌ Cannot send book action notification: Book is null");
            return;
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            System.err.println("❌ Cannot send book action notification: User has no email - " + user.getName());
            return;
        }

        String message = String.format("Book '%s' %s successfully", book.getTitle(), action);

        Notification notification = new Notification(
                generateId(),
                userId,
                "Book " + action.substring(0, 1).toUpperCase() + action.substring(1),
                message,
                new Date()
        );
        notifications.add(notification);

        // Send enhanced email notification
        try {
            System.out.println("📧 Attempting to send book action notification to: " + user.getEmail());
            boolean emailSent = emailService.sendBookActionEmail(user, book, action, additionalInfo);
            if (emailSent) {
                System.out.println("✅ Book " + action + " notification email sent to: " + user.getEmail());
            } else {
                System.err.println("❌ Failed to send book " + action + " notification email to: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to send book action notification to " + user.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendFineNotification(String userId, double amount, String reason) {
        LibraryService libraryService = LibraryService.getInstance();
        User user = libraryService.getUser(userId);

        if (user == null) {
            System.err.println("❌ Cannot send fine notification: User not found - " + userId);
            System.err.println("📋 Available users: " + libraryService.getAllUsers().stream()
                    .map(User::getUserId)
                    .collect(Collectors.toList()));
            return;
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            System.err.println("❌ Cannot send fine notification: User has no email - " + user.getName());
            return;
        }

        String message = String.format("Fine of LKR %.2f applied. Reason: %s", amount, reason);

        Notification notification = new Notification(
                generateId(),
                userId,
                "Fine Notification",
                message,
                new Date()
        );
        notifications.add(notification);

        // Send email notification
        try {
            System.out.println("💰 Attempting to send fine notification to: " + user.getEmail());
            System.out.println("💸 Amount: LKR " + amount);
            System.out.println("📝 Reason: " + reason);

            boolean emailSent = emailService.sendFineNotificationEmail(user, amount, reason);
            if (emailSent) {
                System.out.println("✅ Fine notification email sent to: " + user.getEmail());
                System.out.println("📬 Email sent successfully for fine: LKR " + amount);
            } else {
                System.err.println("❌ Failed to send fine notification email to: " + user.getEmail());
                System.err.println("⚠️ Email service returned false for: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("❌ Exception sending fine notification to " + user.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendNewBookNotification(Book book) {
        // Get all users or specific users who opted for notifications
        LibraryService libraryService = LibraryService.getInstance();
        List<User> users = libraryService.getAllUsers();

        String message = String.format("New book added: '%s' by %s",
                book.getTitle(), book.getAuthor());

        int emailsSent = 0;
        int emailsFailed = 0;

        System.out.println("📚 Sending new book notification to " + users.size() + " users...");

        for (User user : users) {
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                emailsFailed++;
                System.err.println("❌ Skipping user " + user.getName() + " - no email");
                continue;
            }

            Notification notification = new Notification(
                    generateId(),
                    user.getUserId(),
                    "Book Added",
                    message,
                    new Date()
            );
            notifications.add(notification);

            // Send email notification
            try {
                boolean emailSent = emailService.sendNewBookNotificationEmail(user, book);
                if (emailSent) {
                    emailsSent++;
                } else {
                    emailsFailed++;
                    System.err.println("❌ Failed to send email to " + user.getEmail());
                }
            } catch (Exception e) {
                emailsFailed++;
                System.err.println("❌ Failed to send email to " + user.getEmail() + ": " + e.getMessage());
            }
        }

        System.out.println("📊 New book notifications: " + emailsSent + " sent, " + emailsFailed + " failed");
    }

    public void sendMembershipUpdate(String userId, String updateMessage) {
        LibraryService libraryService = LibraryService.getInstance();
        User user = libraryService.getUser(userId);

        if (user == null) {
            System.err.println("❌ Cannot send membership update: User not found - " + userId);
            System.err.println("📋 Available users: " + libraryService.getAllUsers().stream()
                    .map(User::getUserId)
                    .collect(Collectors.toList()));
            return;
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            System.err.println("❌ Cannot send membership update: User has no email - " + user.getName());
            return;
        }

        Notification notification = new Notification(
                generateId(),
                userId,
                "Membership Update",
                updateMessage,
                new Date()
        );
        notifications.add(notification);

        // Send email notification
        try {
            System.out.println("📧 Attempting to send membership update to: " + user.getEmail());
            boolean emailSent = emailService.sendMembershipUpdateEmail(user, updateMessage);
            if (emailSent) {
                System.out.println("✅ Membership update email sent to: " + user.getEmail());
            } else {
                System.err.println("❌ Failed to send membership update email to: " + user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to send email notification to " + user.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Enhanced test method for fine notifications
    public boolean testFineNotification(String userId, double testAmount, String testReason) {
        LibraryService libraryService = LibraryService.getInstance();
        User user = libraryService.getUser(userId);

        if (user == null) {
            System.err.println("❌ Test failed: User not found - " + userId);
            return false;
        }

        System.out.println("🧪 Testing fine notification system...");
        System.out.println("👤 User: " + user.getName());
        System.out.println("📧 Email: " + user.getEmail());
        System.out.println("💰 Test amount: LKR " + testAmount);
        System.out.println("📝 Test reason: " + testReason);

        // Test email connection first
        boolean connectionTest = emailService.testConnection();
        if (!connectionTest) {
            System.err.println("❌ Email connection test failed!");
            return false;
        }
        System.out.println("✅ Email connection test passed");

        // Send test fine notification
        sendFineNotification(userId, testAmount, testReason);

        System.out.println("✅ Fine notification test completed");
        return true;
    }

    // Test email connection
    public boolean testEmailConnection() {
        System.out.println("🔌 Testing email connection...");
        boolean connectionTest = emailService.testConnection();
        System.out.println("📧 Email connection test: " + (connectionTest ? "✅ SUCCESS" : "❌ FAILED"));
        return connectionTest;
    }

    // Rest of the existing methods remain the same...
    public List<Notification> getUserNotifications(String userId) {
        List<Notification> userNotifications = new ArrayList<>();
        for (Notification notification : notifications) {
            if (notification.getUserId().equals(userId)) {
                userNotifications.add(notification);
            }
        }
        return userNotifications;
    }

    public List<Notification> getAllNotifications() {
        return new ArrayList<>(notifications);
    }

    public String generateId() {
        return "NT" + System.currentTimeMillis() + "_" + notifications.size();
    }

    public void addNotification(Notification notification) {
        notifications.add(notification);
    }

    // Notification inner class
    public class Notification {
        private String id;
        private String userId;
        private String type;
        private String message;
        private Date timestamp;
        private boolean read;

        public Notification(String id, String userId, String type,
                            String message, Date timestamp) {
            this.id = id;
            this.userId = userId;
            this.type = type;
            this.message = message;
            this.timestamp = timestamp;
            this.read = false;
        }

        // Getters and setters
        public String getId() { return id; }
        public String getUserId() { return userId; }
        public String getType() { return type; }
        public String getMessage() { return message; }
        public Date getTimestamp() { return timestamp; }
        public boolean isRead() { return read; }
        public void markAsRead() { this.read = true; }
    }
}