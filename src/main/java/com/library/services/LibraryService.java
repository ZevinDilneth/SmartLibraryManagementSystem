package com.library.services;

import com.library.models.*;
import com.library.enums.BookStatus;
import com.library.enums.MembershipType;
import com.library.enums.FineStatus;
import com.library.patterns.observer.BookObserver;
import com.library.patterns.observer.BookSubject;
import com.library.patterns.strategy.*;
import java.util.*;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;

public class LibraryService {
    private static LibraryService instance;

    private Map<String, Book> books = new HashMap<>();
    private Map<String, User> users = new HashMap<>();
    private Map<String, BorrowRecord> borrowRecords = new HashMap<>();
    private Map<String, Reservation> reservations = new HashMap<>();
    private Map<String, FineRecord> fineRecords = new HashMap<>();
    private Map<MembershipType, FineCalculationStrategy> fineStrategies = new HashMap<>();
    private BookSubject bookSubject = new BookSubject();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Private constructor for Singleton
    private LibraryService() {
        // Initialize fine strategies
        fineStrategies.put(MembershipType.STUDENT, new StudentFineStrategy());
        fineStrategies.put(MembershipType.FACULTY, new FacultyFineStrategy());
        fineStrategies.put(MembershipType.GUEST, new GuestFineStrategy());
    }

    // Singleton instance getter
    public static LibraryService getInstance() {
        if (instance == null) {
            instance = new LibraryService();
        }
        return instance;
    }

    // Email Scheduler methods - Fixed to avoid circular dependency
    public void startEmailScheduler() {
        EmailSchedulerService.getInstance().startAllSchedulers();
    }

    public void stopEmailScheduler() {
        EmailSchedulerService.getInstance().stopAllSchedulers();
    }

    public void triggerEmailCheck() {
        EmailSchedulerService.getInstance().triggerManualCheck();
    }

    public String getEmailSchedulerStatus() {
        return EmailSchedulerService.getInstance().getSchedulerStatus();
    }

    // Book Management
    public Book addBook(String title, String author, String category, String isbn) {
        Book book = new Book(title, author, category, isbn);
        books.put(book.getBookId(), book);
        bookSubject.notifyObservers("New book added: " + book.getTitle());
        return book;
    }

    // Enhanced add book with optional features
    public Book addBook(String title, String author, String category, String isbn,
                        boolean featured, boolean recommended, boolean specialEdition,
                        int edition, String publisher, Date publicationDate) {
        Book book = new Book(title, author, category, isbn);
        book.setFeatured(featured);
        book.setRecommended(recommended);
        book.setSpecialEdition(specialEdition);
        book.setEdition(edition);
        book.setPublisher(publisher);
        book.setPublicationDate(publicationDate);

        books.put(book.getBookId(), book);

        // Create appropriate notification based on features
        String notification = "New book added: " + book.getTitle();
        if (specialEdition) notification = "🌟 New Special Edition: " + book.getTitle();
        else if (featured) notification = "⭐ New Featured Book: " + book.getTitle();
        else if (recommended) notification = "👍 New Recommended Book: " + book.getTitle();

        bookSubject.notifyObservers(notification);
        return book;
    }

    // Method to add book with extended metadata
    public Book addBookWithMetadata(String title, String author, String category, String isbn,
                                    boolean featured, boolean recommended, boolean specialEdition,
                                    int edition, String publisher, Date publicationDate,
                                    String coverImagePath, String description, String language,
                                    int pageCount, double rating, List<String> tags,
                                    List<String> additionalAuthors) {
        Book book = new Book(title, author, category, isbn);
        book.setFeatured(featured);
        book.setRecommended(recommended);
        book.setSpecialEdition(specialEdition);
        book.setEdition(edition);
        book.setPublisher(publisher);
        book.setPublicationDate(publicationDate);
        book.setCoverImagePath(coverImagePath);
        book.setDescription(description);
        book.setLanguage(language);
        book.setPageCount(pageCount);
        book.setRating(rating);

        // Add tags
        if (tags != null) {
            for (String tag : tags) {
                book.addTag(tag);
            }
        }

        // Add additional authors
        if (additionalAuthors != null) {
            for (String additionalAuthor : additionalAuthors) {
                book.addAdditionalAuthor(additionalAuthor);
            }
        }

        books.put(book.getBookId(), book);

        // Create appropriate notification based on features
        String notification = "New book added: " + book.getTitle();
        if (specialEdition) notification = "🌟 New Special Edition: " + book.getTitle();
        else if (featured) notification = "⭐ New Featured Book: " + book.getTitle();
        else if (recommended) notification = "👍 New Recommended Book: " + book.getTitle();

        bookSubject.notifyObservers(notification);
        return book;
    }

    public void updateBook(String bookId, Book updatedBook) {
        if (books.containsKey(bookId)) {
            books.put(bookId, updatedBook);

            // Check if any special feature was added
            String notification = "Book updated: " + updatedBook.getTitle();
            if (updatedBook.isSpecialEdition()) {
                notification = "🌟 Book marked as Special Edition: " + updatedBook.getTitle();
            } else if (updatedBook.isFeatured()) {
                notification = "⭐ Book marked as Featured: " + updatedBook.getTitle();
            } else if (updatedBook.isRecommended()) {
                notification = "👍 Book marked as Recommended: " + updatedBook.getTitle();
            }

            bookSubject.notifyObservers(notification);
        }
    }

    public void removeBook(String bookId) {
        Book book = books.remove(bookId);
        if (book != null) {
            bookSubject.notifyObservers("Book removed: " + book.getTitle());
        }
    }

    public Book getBook(String bookId) {
        return books.get(bookId);
    }

    // Method to mark book as featured
    public void markAsFeatured(String bookId, boolean featured) {
        Book book = books.get(bookId);
        if (book != null) {
            book.setFeatured(featured);
            bookSubject.notifyObservers(featured ?
                    "⭐ Book marked as Featured: " + book.getTitle() :
                    "Book removed from Featured: " + book.getTitle());
        }
    }

    // Method to mark book as recommended
    public void markAsRecommended(String bookId, boolean recommended) {
        Book book = books.get(bookId);
        if (book != null) {
            book.setRecommended(recommended);
            bookSubject.notifyObservers(recommended ?
                    "👍 Book marked as Recommended: " + book.getTitle() :
                    "Book removed from Recommended: " + book.getTitle());
        }
    }

    // Method to mark book as special edition
    public void markAsSpecialEdition(String bookId, boolean specialEdition, int edition) {
        Book book = books.get(bookId);
        if (book != null) {
            book.setSpecialEdition(specialEdition);
            book.setEdition(edition);
            bookSubject.notifyObservers(specialEdition ?
                    "🌟 Book marked as Special Edition (Edition " + edition + "): " + book.getTitle() :
                    "Book removed from Special Edition: " + book.getTitle());
        }
    }

    // Method to update book cover image
    public void updateBookCover(String bookId, String coverImagePath) {
        Book book = books.get(bookId);
        if (book != null) {
            book.setCoverImagePath(coverImagePath);
            bookSubject.notifyObservers("Book cover updated: " + book.getTitle());
        }
    }

    // Method to add review to book
    public void addBookReview(String bookId, Review review) {
        Book book = books.get(bookId);
        if (book != null) {
            book.addReview(review);
            // Update average rating
            book.setRating(book.calculateAverageRating());
            bookSubject.notifyObservers("New review added to: " + book.getTitle());
        }
    }

    // Method to remove review from book
    public void removeBookReview(String bookId, String reviewId) {
        Book book = books.get(bookId);
        if (book != null) {
            Review reviewToRemove = null;
            for (Review review : book.getReviews()) {
                if (review.getReviewId().equals(reviewId)) {
                    reviewToRemove = review;
                    break;
                }
            }

            if (reviewToRemove != null) {
                book.removeReview(reviewToRemove);
                // Update average rating
                book.setRating(book.calculateAverageRating());
                bookSubject.notifyObservers("Review removed from: " + book.getTitle());
            }
        }
    }

    // Get featured books
    public List<Book> getFeaturedBooks() {
        return books.values().stream()
                .filter(Book::isFeatured)
                .collect(Collectors.toList());
    }

    // Get recommended books
    public List<Book> getRecommendedBooks() {
        return books.values().stream()
                .filter(Book::isRecommended)
                .collect(Collectors.toList());
    }

    // Get special edition books
    public List<Book> getSpecialEditionBooks() {
        return books.values().stream()
                .filter(Book::isSpecialEdition)
                .collect(Collectors.toList());
    }

    // Get books by feature
    public List<Book> getBooksByFeature(String feature) {
        switch (feature.toLowerCase()) {
            case "featured":
                return getFeaturedBooks();
            case "recommended":
                return getRecommendedBooks();
            case "special":
                return getSpecialEditionBooks();
            default:
                return new ArrayList<>();
        }
    }

    // Get books by rating (minimum rating)
    public List<Book> getBooksByRating(double minRating) {
        return books.values().stream()
                .filter(book -> book.getRating() >= minRating)
                .sorted((b1, b2) -> Double.compare(b2.getRating(), b1.getRating()))
                .collect(Collectors.toList());
    }

    // Get books by tag
    public List<Book> getBooksByTag(String tag) {
        return books.values().stream()
                .filter(book -> book.getTags().contains(tag))
                .collect(Collectors.toList());
    }

    // User Management
    public User addUser(String name, String email, String contactNumber, MembershipType membershipType) {
        User user = new User(name, email, contactNumber, membershipType);
        users.put(user.getUserId(), user);
        bookSubject.registerObserver(user);

        // Send welcome notification
        String welcomeMessage = String.format(
                "Welcome to Smart Library Management System! You have been registered as a %s member.",
                membershipType.getDisplayName()
        );
        NotificationService.getInstance().sendGeneralNotification(user.getUserId(), welcomeMessage);

        return user;
    }

    public void updateUser(String userId, User updatedUser) {
        if (updatedUser == null) {
            // Remove user
            users.remove(userId);
            bookSubject.removeObserverById(userId);
        } else {
            // Update user
            users.put(userId, updatedUser);
        }
    }

    public User getUser(String userId) {
        return users.get(userId);
    }

    // Borrowing
    public BorrowRecord borrowBook(String userId, String bookId) {
        User user = users.get(userId);
        Book book = books.get(bookId);

        if (user == null || book == null) {
            System.err.println("❌ User or Book not found");
            return null;
        }

        if (!user.canBorrowMoreBooks()) {
            System.err.println("❌ User cannot borrow more books. Current: " +
                    user.getBorrowedBooks().size() +
                    ", Limit: " + user.getMembershipType().getBorrowingLimit());
            return null;
        }

        if (book.getStatus() != BookStatus.AVAILABLE) {
            System.err.println("❌ Book is not available. Status: " + book.getStatus());
            return null;
        }

        // Calculate due date based on membership
        Calendar calendar = Calendar.getInstance();
        Date borrowDate = new Date();
        calendar.setTime(borrowDate);
        calendar.add(Calendar.DAY_OF_YEAR, user.getMembershipType().getLoanPeriod());
        Date dueDate = calendar.getTime();

        // Create borrow record
        BorrowRecord record = new BorrowRecord(book, user, borrowDate, dueDate);
        borrowRecords.put(record.getRecordId(), record);

        // Update book and user
        book.setStatus(BookStatus.BORROWED);
        user.borrowBook(book);
        book.addBorrowRecord(record);
        user.addBorrowRecord(record);

        // Send enhanced book borrowed notification
        String additionalInfo = String.format(
                "<p><strong>Due Date:</strong> %s</p>",
                new SimpleDateFormat("yyyy-MM-dd").format(dueDate)
        );

        NotificationService.getInstance().sendBookActionNotification(
                userId,
                book,
                "borrowed",
                additionalInfo
        );

        return record;
    }

    // New method to create and notify fines
    private void createAndNotifyFine(BorrowRecord record) {
        if (record.isOverdue()) {
            FineCalculationStrategy strategy = fineStrategies.get(record.getUser().getMembershipType());
            double fine = strategy.calculateFine(record.getOverdueDays());
            record.setFineAmount(fine);

            // Create fine record
            FineRecord fineRecord = new FineRecord(record);
            fineRecord.setReason("Overdue book: " + record.getBook().getTitle());

            // Set due date for fine (7 days from now)
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 7);
            fineRecord.setDueDate(calendar.getTime());

            fineRecords.put(fineRecord.getFineId(), fineRecord);

            // Send fine notification
            System.out.println("💰 Creating fine for user: " + record.getUser().getName() +
                    ", Amount: LKR " + fine +
                    ", Overdue days: " + record.getOverdueDays());

            NotificationService.getInstance().sendFineNotification(
                    record.getUser().getUserId(),
                    fine,
                    "Overdue book: '" + record.getBook().getTitle() +
                            "' (Overdue by " + record.getOverdueDays() + " days)"
            );
        }
    }

    // Returning - UPDATED: Now accepts both BORROWED and RESERVED books
    public double returnBook(String bookId) {
        Book book = books.get(bookId);
        // UPDATED: Allow return for both BORROWED and RESERVED books
        if (book == null || (book.getStatus() != BookStatus.BORROWED && book.getStatus() != BookStatus.RESERVED)) {
            System.err.println("❌ Book not found or not in a returnable state. Status: " +
                    (book != null ? book.getStatus() : "null"));
            return 0.0;
        }

        // Find active borrow record
        BorrowRecord record = borrowRecords.values().stream()
                .filter(r -> r.getBook().getBookId().equals(bookId) && r.getReturnDate() == null)
                .findFirst()
                .orElse(null);

        if (record == null) {
            System.err.println("❌ No active borrow record found for book: " + bookId);
            return 0.0;
        }

        // Update record
        record.setReturnDate(new Date());

        double fine = 0.0;
        if (record.isOverdue()) {
            createAndNotifyFine(record);
            fine = record.getFineAmount();
            System.out.println("💸 Fine applied: LKR " + fine + " for user: " + record.getUser().getName());
        } else {
            System.out.println("✅ Book returned on time, no fine applied");
        }

        // Update book and user
        book.setStatus(BookStatus.AVAILABLE); // Always set to AVAILABLE when returned
        record.getUser().returnBook(book);

        // Send return confirmation
        String returnMessage = "Book returned successfully: " + book.getTitle() +
                (fine > 0 ? ". Fine amount: LKR " + fine + " has been applied to your account." :
                        " - Thank you for returning on time!");
        NotificationService.getInstance().sendGeneralNotification(
                record.getUser().getUserId(),
                returnMessage
        );

        System.out.println("📚 Book '" + book.getTitle() + "' returned. Checking for reservations...");

        // Check for reservations IMMEDIATELY
        checkReservations(book);

        return fine;
    }

    // Reservations
    public Reservation reserveBook(String userId, String bookId) {
        User user = users.get(userId);
        Book book = books.get(bookId);

        if (user == null || book == null) {
            System.err.println("❌ User or Book not found");
            return null;
        }

        // UPDATED: Check if the book is in a state that can be reserved (BORROWED or RESERVED)
        if (book.getStatus() != BookStatus.BORROWED && book.getStatus() != BookStatus.RESERVED) {
            System.err.println("❌ Book is not currently borrowed. Status: " + book.getStatus());
            return null;
        }

        // Check if user is trying to reserve their own borrowed book
        boolean isUserBorrowingBook = borrowRecords.values().stream()
                .anyMatch(r -> r.getBook().getBookId().equals(bookId) &&
                        r.getUser().getUserId().equals(userId) && r.getReturnDate() == null);

        if (isUserBorrowingBook) {
            System.err.println("❌ User cannot reserve a book they are currently borrowing");
            return null;
        }

        // Check if already reserved
        boolean alreadyReserved = reservations.values().stream()
                .anyMatch(r -> r.getBook().getBookId().equals(bookId) &&
                        r.getUser().getUserId().equals(userId) && !r.isNotified() && !r.isExpired());

        if (alreadyReserved) {
            System.err.println("❌ Book already reserved by this user");
            return null;
        }

        Reservation reservation = new Reservation(book, user);
        reservations.put(reservation.getReservationId(), reservation);
        user.addReservation(reservation);
        book.addReservation(reservation);

        // Update book status if first reservation
        long activeReservations = reservations.values().stream()
                .filter(r -> r.getBook().getBookId().equals(bookId) && !r.isNotified() && !r.isExpired())
                .count();

        if (activeReservations == 1) {
            book.setStatus(BookStatus.RESERVED);
            System.out.println("📚 Book '" + book.getTitle() + "' status changed to RESERVED");
        }

        // Send enhanced book reserved notification
        String additionalInfo = String.format(
                "<p><strong>Queue Position:</strong> #%d</p>",
                activeReservations
        );

        NotificationService.getInstance().sendBookActionNotification(
                userId,
                book,
                "reserved",
                additionalInfo
        );

        System.out.println("✅ Reservation created for user '" + user.getName() +
                "' for book '" + book.getTitle() + "'");

        return reservation;
    }

    // NEW: Cancel reservation method
    public boolean cancelReservation(String reservationId) {
        Reservation reservation = reservations.get(reservationId);

        if (reservation == null) {
            System.err.println("❌ Reservation not found: " + reservationId);
            return false;
        }

        // Check if reservation is already notified
        if (reservation.isNotified()) {
            System.err.println("❌ Cannot cancel a reservation that has been notified");
            return false;
        }

        // Check if reservation is expired
        if (reservation.isExpired()) {
            System.err.println("❌ Cannot cancel an expired reservation");
            return false;
        }

        // Get book and user
        Book book = reservation.getBook();
        User user = reservation.getUser();

        // Remove from user's reservations
        user.removeReservation(reservation);

        // Remove from book's reservations
        book.removeReservation(reservation);

        // Remove from main reservations map
        reservations.remove(reservationId);

        // Check if there are other active reservations for this book
        long remainingActiveReservations = reservations.values().stream()
                .filter(r -> r.getBook().getBookId().equals(book.getBookId()) && !r.isNotified() && !r.isExpired())
                .count();

        // Update book status
        if (remainingActiveReservations == 0 && book.getStatus() == BookStatus.RESERVED) {
            // If no more reservations, but book is still borrowed, set to BORROWED
            // Otherwise set to AVAILABLE
            boolean isCurrentlyBorrowed = borrowRecords.values().stream()
                    .anyMatch(r -> r.getBook().getBookId().equals(book.getBookId()) && r.getReturnDate() == null);

            if (isCurrentlyBorrowed) {
                book.setStatus(BookStatus.BORROWED);
                System.out.println("📚 Book '" + book.getTitle() + "' status changed to BORROWED (no reservations left)");
            } else {
                book.setStatus(BookStatus.AVAILABLE);
                System.out.println("📚 Book '" + book.getTitle() + "' status changed to AVAILABLE (no reservations left)");
            }
        }

        // Send cancellation notification
        String message = String.format(
                "Your reservation has been cancelled.\n" +
                        "📚 Book: %s\n" +
                        "📝 Reservation ID: %s\n" +
                        "🗓️ Cancellation Date: %s\n\n" +
                        "You can make a new reservation when the book is available again.",
                book.getTitle(),
                reservationId,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );

        NotificationService.getInstance().sendGeneralNotification(
                user.getUserId(),
                message
        );

        System.out.println("✅ Reservation cancelled: " + reservationId +
                " for user: " + user.getName() +
                ", book: " + book.getTitle());
        return true;
    }

    // UPDATED: Better reservation checking with immediate email notification
    private void checkReservations(Book book) {
        System.out.println("🔍 Checking reservations for book: " + book.getTitle());

        List<Reservation> bookReservations = reservations.values().stream()
                .filter(r -> r.getBook().getBookId().equals(book.getBookId()) &&
                        !r.isNotified() && !r.isExpired())
                .sorted(Comparator.comparing(Reservation::getReservationDate))
                .collect(Collectors.toList());

        System.out.println("📊 Found " + bookReservations.size() + " pending reservations");

        if (!bookReservations.isEmpty()) {
            // Notify first reservation IMMEDIATELY
            Reservation firstReservation = bookReservations.get(0);

            // Send reservation available email IMMEDIATELY
            System.out.println("📧 Sending immediate reservation notification for: " +
                    book.getTitle() + " to user: " + firstReservation.getUser().getName());

            boolean emailSent = NotificationService.getInstance().sendReservationAvailable(
                    firstReservation.getUser().getUserId(),
                    book
            );

            if (emailSent) {
                System.out.println("✅ Email sent successfully to: " + firstReservation.getUser().getEmail());
                firstReservation.setNotified(true);
                System.out.println("✅ Marked reservation as notified for user: " + firstReservation.getUser().getName());
            } else {
                System.err.println("❌ Failed to send email to: " + firstReservation.getUser().getEmail());
                // Don't mark as notified if email failed
                return;
            }

            // Update book status based on remaining reservations
            long remainingReservations = bookReservations.stream()
                    .skip(1) // Skip the first one we just notified
                    .filter(r -> !r.isNotified())
                    .count();

            if (remainingReservations > 0) {
                book.setStatus(BookStatus.RESERVED);
                System.out.println("📚 Book '" + book.getTitle() + "' status set to RESERVED (" +
                        remainingReservations + " more reservations pending)");
            } else {
                book.setStatus(BookStatus.AVAILABLE);
                System.out.println("📚 Book '" + book.getTitle() + "' status set to AVAILABLE (no more reservations)");
            }
        } else {
            // No reservations, book is fully available
            book.setStatus(BookStatus.AVAILABLE);
            System.out.println("📚 No active reservations for '" + book.getTitle() + "', status set to AVAILABLE");
        }
    }

    // Fines
    public double calculateFine(BorrowRecord record) {
        if (!record.isOverdue()) {
            return 0.0;
        }

        FineCalculationStrategy strategy = fineStrategies.get(record.getUser().getMembershipType());
        double fine = strategy.calculateFine(record.getOverdueDays());
        System.out.println("📊 Fine calculated: LKR " + fine + " for " + record.getOverdueDays() + " days overdue");
        return fine;
    }

    // Method to calculate total fines for a user
    public double getUserTotalFines(String userId) {
        User user = users.get(userId);
        if (user == null) return 0.0;

        double totalFine = 0.0;

        // Get all borrow records for the user
        List<BorrowRecord> userBorrowRecords = borrowRecords.values().stream()
                .filter(record -> record.getUser() != null &&
                        record.getUser().getUserId().equals(userId))
                .collect(Collectors.toList());

        // Calculate fine for each overdue book
        for (BorrowRecord record : userBorrowRecords) {
            if (record.isOverdue() && !record.isFinePaid()) {
                // Get the fine calculation strategy based on membership type
                FineCalculationStrategy strategy = fineStrategies.get(record.getUser().getMembershipType());
                totalFine += strategy.calculateFine(record.getOverdueDays());
            }
        }

        return totalFine;
    }

    // Method to check if user has active reservations
    public boolean userHasActiveReservations(String userId) {
        User user = users.get(userId);
        if (user == null) return false;

        // Check user's reservations
        return user.getReservations().stream()
                .anyMatch(reservation -> !reservation.isExpired());
    }

    // NEW: Get reservation by ID
    public Reservation getReservation(String reservationId) {
        return reservations.get(reservationId);
    }

    // Test method for fine notifications
    public void testFineNotification(String userId) {
        User user = getUser(userId);
        if (user == null) {
            System.err.println("❌ User not found: " + userId);
            System.out.println("📋 Available users:");
            getAllUsers().forEach(u -> System.out.println("  - " + u.getUserId() + ": " + u.getName()));
            return;
        }

        System.out.println("🧪 Testing fine notification for user: " + user.getName());
        System.out.println("📧 User email: " + user.getEmail());
        System.out.println("👤 Membership type: " + user.getMembershipType());

        // Test email connection
        boolean connectionOk = NotificationService.getInstance().testEmailConnection();
        if (!connectionOk) {
            System.err.println("❌ Email connection test failed!");
            return;
        }

        // Test fine notification
        NotificationService.getInstance().testFineNotification(
                userId,
                150.0,
                "Test fine notification from library system"
        );
    }

    // Reports - Enhanced to include featured books and metadata
    public List<Book> getMostBorrowedBooks(int limit) {
        return books.values().stream()
                .sorted((b1, b2) -> Integer.compare(b2.getBorrowHistory().size(), b1.getBorrowHistory().size()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Get most borrowed featured books
    public List<Book> getMostBorrowedFeaturedBooks(int limit) {
        return books.values().stream()
                .filter(Book::isFeatured)
                .sorted((b1, b2) -> Integer.compare(b2.getBorrowHistory().size(), b1.getBorrowHistory().size()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Get most borrowed recommended books
    public List<Book> getMostBorrowedRecommendedBooks(int limit) {
        return books.values().stream()
                .filter(Book::isRecommended)
                .sorted((b1, b2) -> Integer.compare(b2.getBorrowHistory().size(), b1.getBorrowHistory().size()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Get highest rated books
    public List<Book> getHighestRatedBooks(int limit) {
        return books.values().stream()
                .filter(book -> book.getRating() > 0)
                .sorted((b1, b2) -> Double.compare(b2.getRating(), b1.getRating()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Get newest books (by publication date if available, otherwise by addition date)
    public List<Book> getNewestBooks(int limit) {
        return books.values().stream()
                .sorted((b1, b2) -> {
                    // Try to sort by publication date
                    if (b1.getPublicationDate() != null && b2.getPublicationDate() != null) {
                        return b2.getPublicationDate().compareTo(b1.getPublicationDate());
                    }
                    // Fall back to book ID (which contains timestamp)
                    return b2.getBookId().compareTo(b1.getBookId());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<User> getActiveBorrowers(int limit) {
        return users.values().stream()
                .sorted((u1, u2) -> Integer.compare(u2.getBorrowedBooks().size(), u1.getBorrowedBooks().size()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<BorrowRecord> getOverdueBooks() {
        List<BorrowRecord> overdueRecords = borrowRecords.values().stream()
                .filter(BorrowRecord::isOverdue)
                .collect(Collectors.toList());

        System.out.println("🔍 Found " + overdueRecords.size() + " overdue records");
        for (BorrowRecord record : overdueRecords) {
            double fine = calculateFine(record);
            System.out.println("  - " + record.getBook().getTitle() +
                    " (Overdue: " + record.getOverdueDays() + " days, Fine: LKR " + fine + ")");
        }
        return overdueRecords;
    }

    // Search books by multiple criteria
    public List<Book> searchBooks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllBooks();
        }

        String lowerQuery = query.toLowerCase().trim();
        return books.values().stream()
                .filter(book ->
                        book.getTitle().toLowerCase().contains(lowerQuery) ||
                                book.getAuthor().toLowerCase().contains(lowerQuery) ||
                                book.getCategory().toLowerCase().contains(lowerQuery) ||
                                book.getIsbn().toLowerCase().contains(lowerQuery) ||
                                book.getDescription().toLowerCase().contains(lowerQuery) ||
                                book.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(lowerQuery)) ||
                                book.getAdditionalAuthors().stream().anyMatch(author -> author.toLowerCase().contains(lowerQuery)) ||
                                book.getBookId().toLowerCase().contains(lowerQuery)
                )
                .collect(Collectors.toList());
    }

    // Getters
    public List<Book> getAllBooks() {
        return new ArrayList<>(books.values());
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public List<BorrowRecord> getAllBorrowRecords() {
        return new ArrayList<>(borrowRecords.values());
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }

    public List<FineRecord> getAllFines() {
        return new ArrayList<>(fineRecords.values());
    }

    // Statistics
    public Map<String, Object> getLibraryStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Basic counts
        stats.put("totalBooks", books.size());
        stats.put("totalUsers", users.size());
        stats.put("activeBorrowings", borrowRecords.values().stream()
                .filter(r -> r.getReturnDate() == null)
                .count());
        stats.put("activeReservations", reservations.values().stream()
                .filter(r -> !r.isNotified() && !r.isExpired())
                .count());

        // Book status breakdown
        Map<BookStatus, Long> statusCount = books.values().stream()
                .collect(Collectors.groupingBy(Book::getStatus, Collectors.counting()));
        stats.put("bookStatus", statusCount);

        // Feature counts
        stats.put("featuredBooks", books.values().stream().filter(Book::isFeatured).count());
        stats.put("recommendedBooks", books.values().stream().filter(Book::isRecommended).count());
        stats.put("specialEditionBooks", books.values().stream().filter(Book::isSpecialEdition).count());

        // Category distribution
        Map<String, Long> categoryCount = books.values().stream()
                .collect(Collectors.groupingBy(Book::getCategory, Collectors.counting()));
        stats.put("categories", categoryCount);

        // Most popular tags
        Map<String, Long> tagCount = books.values().stream()
                .flatMap(book -> book.getTags().stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        stats.put("popularTags", tagCount);

        return stats;
    }
}