package com.library;

import com.library.services.LibraryService;
import com.library.services.EmailSchedulerService;
import com.library.services.NotificationService;
import com.library.enums.MembershipType;
import com.library.models.Book;
import com.library.models.User;
import com.library.models.BorrowRecord;
import com.library.models.Reservation;
import java.util.Scanner;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class LibraryApplication {

    public static void main(String[] args) {
        System.out.println("📚 SMART LIBRARY MANAGEMENT SYSTEM 📚");
        System.out.println("========================================\n");

        // Initialize services
        LibraryService libraryService = LibraryService.getInstance();
        NotificationService notificationService = NotificationService.getInstance();
        EmailSchedulerService emailScheduler = EmailSchedulerService.getInstance();

        // Test email connection
        System.out.println("Testing email connection...");
        boolean emailConnected = notificationService.testEmailConnection();

        if (!emailConnected) {
            System.out.println("⚠️ WARNING: Email service is not connected.");
            System.out.println("Please check your email configuration in EmailConfiguration.java");
            System.out.println("Continuing without email notifications...\n");
        } else {
            System.out.println("✅ Email service connected successfully!\n");
        }

        // Create sample data for testing
        createSampleData(libraryService);

        // Start the email scheduler
        System.out.println("Starting automated email scheduler...");
        emailScheduler.startAllSchedulers();

        System.out.println("\n" + emailScheduler.getSchedulerStatus());
        System.out.println("\n🎯 Automated Email System:");
        System.out.println("1. Due Date Reminders → 1 day before due date");
        System.out.println("2. Overdue Alerts → Daily with updated fines");
        System.out.println("3. Reservation Available → Immediate notification\n");

        // Display initial statistics
        displayLibraryStatistics(libraryService);

        // Interactive menu
        runInteractiveMenu(libraryService, emailScheduler, notificationService);
    }

    private static void createSampleData(LibraryService libraryService) {
        System.out.println("Creating sample data for testing...");

        // Create sample users
        User user1 = libraryService.addUser("John Doe", "john.doe@example.com", "0771234567", MembershipType.STUDENT);
        User user2 = libraryService.addUser("Jane Smith", "jane.smith@example.com", "0777654321", MembershipType.FACULTY);
        User user3 = libraryService.addUser("Bob Wilson", "bob.wilson@example.com", "0775555555", MembershipType.GUEST);

        // Create sample books
        Book book1 = libraryService.addBook("The Great Gatsby", "F. Scott Fitzgerald", "Fiction", "9780743273565");
        Book book2 = libraryService.addBook("To Kill a Mockingbird", "Harper Lee", "Fiction", "9780446310789");
        Book book3 = libraryService.addBook("1984", "George Orwell", "Science Fiction", "9780451524935");
        Book book4 = libraryService.addBook("Pride and Prejudice", "Jane Austen", "Romance", "9780141439518");

        // Make some books featured/recommended
        libraryService.markAsFeatured(book1.getBookId(), true);
        libraryService.markAsRecommended(book2.getBookId(), true);
        libraryService.markAsSpecialEdition(book3.getBookId(), true, 2);

        // Borrow some books with different due dates for testing
        Calendar calendar = Calendar.getInstance();

        // Book due tomorrow
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Book book5 = libraryService.addBook("The Hobbit", "J.R.R. Tolkien", "Fantasy", "9780547928227");
        BorrowRecord record1 = libraryService.borrowBook(user1.getUserId(), book5.getBookId());

        // Book overdue by 3 days
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_YEAR, -3);
        Book book6 = libraryService.addBook("The Catcher in the Rye", "J.D. Salinger", "Fiction", "9780316769488");
        BorrowRecord record2 = libraryService.borrowBook(user2.getUserId(), book6.getBookId());

        System.out.println("✅ Sample data created:");
        System.out.println("   - Users: 3");
        System.out.println("   - Books: 6");
        System.out.println("   - Active borrowings: 2");
        System.out.println("   - 1 book due tomorrow");
        System.out.println("   - 1 book overdue by 3 days");
    }

    private static void displayLibraryStatistics(LibraryService libraryService) {
        System.out.println("\n📊 LIBRARY STATISTICS:");
        System.out.println("=====================");
        System.out.println("Total Books: " + libraryService.getAllBooks().size());
        System.out.println("Total Users: " + libraryService.getAllUsers().size());

        long activeBorrowings = libraryService.getAllBorrowRecords().stream()
                .filter(r -> r.getReturnDate() == null)
                .count();
        System.out.println("Active Borrowings: " + activeBorrowings);

        List<BorrowRecord> overdueBooks = libraryService.getOverdueBooks();
        System.out.println("Overdue Books: " + overdueBooks.size());

        if (!overdueBooks.isEmpty()) {
            System.out.println("\nOverdue Books List:");
            for (BorrowRecord record : overdueBooks) {
                System.out.println("  - " + record.getBook().getTitle() +
                        " (User: " + record.getUser().getName() +
                        ", Overdue: " + record.getOverdueDays() + " days)");
            }
        }

        long pendingReservations = libraryService.getAllReservations().stream()
                .filter(r -> !r.isNotified())
                .count();
        System.out.println("Pending Reservations: " + pendingReservations);
        System.out.println("=====================\n");
    }

    private static void runInteractiveMenu(LibraryService libraryService,
                                           EmailSchedulerService emailScheduler,
                                           NotificationService notificationService) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n📋 MAIN MENU:");
            System.out.println("1. View library statistics");
            System.out.println("2. View scheduler status");
            System.out.println("3. Trigger manual email check");
            System.out.println("4. Test email notifications");
            System.out.println("5. Create new borrowing for testing");
            System.out.println("6. Return a book");
            System.out.println("7. Make a reservation");
            System.out.println("8. Exit system");
            System.out.print("Select option (1-8): ");

            try {
                int option = scanner.nextInt();
                scanner.nextLine(); // Clear buffer

                switch (option) {
                    case 1:
                        displayLibraryStatistics(libraryService);
                        break;

                    case 2:
                        System.out.println("\n⏰ SCHEDULER STATUS:");
                        System.out.println(emailScheduler.getSchedulerStatus());
                        break;

                    case 3:
                        System.out.println("\n🔧 Triggering manual email check...");
                        emailScheduler.triggerManualCheck();
                        break;

                    case 4:
                        testEmailNotifications(libraryService, notificationService);
                        break;

                    case 5:
                        createTestBorrowing(libraryService, scanner);
                        break;

                    case 6:
                        returnBook(libraryService, scanner);
                        break;

                    case 7:
                        createReservation(libraryService, scanner);
                        break;

                    case 8:
                        System.out.println("\n🛑 Shutting down system...");
                        emailScheduler.stopAllSchedulers();
                        scanner.close();
                        System.out.println("✅ System shutdown complete. Goodbye!");
                        System.exit(0);
                        break;

                    default:
                        System.out.println("❌ Invalid option. Please enter 1-8.");
                }
            } catch (Exception e) {
                System.out.println("❌ Invalid input. Please enter a number 1-8.");
                scanner.nextLine(); // Clear invalid input
            }
        }
    }

    private static void testEmailNotifications(LibraryService libraryService, NotificationService notificationService) {
        System.out.println("\n📧 TESTING EMAIL NOTIFICATIONS:");

        // Get first user
        if (libraryService.getAllUsers().isEmpty()) {
            System.out.println("No users found. Please create sample data first.");
            return;
        }

        User testUser = libraryService.getAllUsers().get(0);
        System.out.println("Testing with user: " + testUser.getName() + " (" + testUser.getEmail() + ")");

        // Test different notification types

        // Test general notification
        notificationService.sendGeneralNotification(
                testUser.getUserId(),
                "This is a test notification from Smart Library Management System."
        );

        // Test fine notification
        notificationService.sendFineNotification(
                testUser.getUserId(),
                500.00,
                "Test fine notification"
        );

        System.out.println("✅ Test notifications sent. Check email: " + testUser.getEmail());
    }

    private static void createTestBorrowing(LibraryService libraryService, Scanner scanner) {
        System.out.println("\n📖 CREATE TEST BORROWING:");

        if (libraryService.getAllUsers().isEmpty() || libraryService.getAllBooks().isEmpty()) {
            System.out.println("Need at least 1 user and 1 book. Creating sample data...");
            createSampleData(libraryService);
        }

        // List users
        System.out.println("\nAvailable Users:");
        libraryService.getAllUsers().forEach(user ->
                System.out.println("  " + user.getUserId() + ": " + user.getName() +
                        " (" + user.getMembershipType() + ")")
        );

        System.out.print("Enter User ID: ");
        String userId = scanner.nextLine();

        // List available books
        System.out.println("\nAvailable Books:");
        libraryService.getAllBooks().stream()
                .filter(book -> book.getStatus().toString().equals("AVAILABLE"))
                .forEach(book ->
                        System.out.println("  " + book.getBookId() + ": " + book.getTitle())
                );

        System.out.print("Enter Book ID: ");
        String bookId = scanner.nextLine();

        BorrowRecord record = libraryService.borrowBook(userId, bookId);
        if (record != null) {
            System.out.println("✅ Book borrowed successfully!");
            System.out.println("Due date: " + record.getDueDate());
            System.out.println("Reminder emails will be sent 1 day before due date.");
        } else {
            System.out.println("❌ Failed to borrow book. Check if book is available or user can borrow more.");
        }
    }

    private static void returnBook(LibraryService libraryService, Scanner scanner) {
        System.out.println("\n📚 RETURN A BOOK:");

        // List borrowed books
        System.out.println("\nCurrently Borrowed Books:");
        List<BorrowRecord> borrowedBooks = libraryService.getAllBorrowRecords().stream()
                .filter(record -> record.getReturnDate() == null)
                .toList();

        if (borrowedBooks.isEmpty()) {
            System.out.println("No books are currently borrowed.");
            return;
        }

        for (BorrowRecord record : borrowedBooks) {
            System.out.println("  Book: " + record.getBook().getTitle() +
                    " | User: " + record.getUser().getName() +
                    " | Due: " + record.getDueDate() +
                    (record.isOverdue() ? " (OVERDUE)" : ""));
        }

        System.out.print("Enter Book ID to return: ");
        String bookId = scanner.nextLine();

        double fine = libraryService.returnBook(bookId);
        if (fine > 0) {
            System.out.println("✅ Book returned successfully!");
            System.out.println("Fine applied: LKR " + fine);
        } else {
            System.out.println("✅ Book returned successfully!");
        }
    }

    private static void createReservation(LibraryService libraryService, Scanner scanner) {
        System.out.println("\n🔖 MAKE A RESERVATION:");

        // List users
        System.out.println("\nAvailable Users:");
        List<User> users = libraryService.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("No users available.");
            return;
        }

        users.forEach(user ->
                System.out.println("  " + user.getUserId() + ": " + user.getName())
        );

        System.out.print("Enter User ID: ");
        String userId = scanner.nextLine();

        // List borrowed books (can only reserve borrowed books)
        System.out.println("\nCurrently Borrowed Books (Available for reservation):");
        List<BorrowRecord> borrowedBooks = libraryService.getAllBorrowRecords().stream()
                .filter(record -> record.getReturnDate() == null)
                .toList();

        if (borrowedBooks.isEmpty()) {
            System.out.println("No books are currently borrowed.");
            return;
        }

        for (BorrowRecord record : borrowedBooks) {
            System.out.println("  " + record.getBook().getBookId() + ": " +
                    record.getBook().getTitle() + " (Borrowed by: " +
                    record.getUser().getName() + ")");
        }

        System.out.print("Enter Book ID to reserve: ");
        String bookId = scanner.nextLine();

        Reservation reservation = libraryService.reserveBook(userId, bookId);
        if (reservation != null) {
            System.out.println("✅ Reservation created successfully!");
            System.out.println("You will be notified by email when the book becomes available.");
        } else {
            System.out.println("❌ Failed to create reservation. Book may already be reserved or not borrowed.");
        }
    }
}