package com.library.services;

import com.library.models.User;
import com.library.models.Book;
import com.library.models.BorrowRecord;
import com.library.enums.MembershipType;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.List;

public class EmailTemplateGenerator {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd MMMM yyyy 'at' hh:mm a");

    // Helper method to calculate days until due
    private static int calculateDaysUntilDue(Date dueDate) {
        Date now = new Date();
        long diff = dueDate.getTime() - now.getTime();
        return (int) (diff / (1000 * 60 * 60 * 24));
    }

    // Helper method to get max books for membership type
    private static int getMaxBooksForMembership(MembershipType type) {
        switch (type) {
            case STUDENT:
                return 5;
            case FACULTY:
                return 10;
            case GUEST:
                return 3;
            default:
                return 3;
        }
    }

    // Helper method to get daily fine rate for membership type
    private static double getDailyFineRateForMembership(MembershipType type) {
        switch (type) {
            case STUDENT:
                return 10.0;
            case FACULTY:
                return 5.0;
            case GUEST:
                return 15.0;
            default:
                return 10.0;
        }
    }

    // Helper method to generate book info section with cover image above details
    private static String generateBookInfoSection(Book book) {
        String coverImage = "";
        if (book.getCoverImagePath() != null && !book.getCoverImagePath().isEmpty()) {
            coverImage = """
                <div class="book-cover-container">
                    <img src="%s" alt="%s Cover" class="book-cover-img" />
                </div>
                """.formatted(book.getCoverImagePath(), book.getTitle());
        } else {
            coverImage = """
                <div class="book-cover-container">
                    <div class="cover-placeholder">
                        📚<br>
                        <span class="cover-text">%s</span>
                    </div>
                </div>
                """.formatted(book.getTitle());
        }

        // Get additional details
        String editionInfo = book.getEdition() > 0 ? "Edition " + book.getEdition() : "First Edition";
        String publisherInfo = book.getPublisher() != null && !book.getPublisher().isEmpty() ?
                book.getPublisher() : "Not specified";
        String publicationDate = book.getPublicationDate() != null ?
                new SimpleDateFormat("yyyy").format(book.getPublicationDate()) : "N/A";
        String pageCount = book.getPageCount() > 0 ? book.getPageCount() + " pages" : "Page count not available";
        String language = book.getLanguage() != null && !book.getLanguage().isEmpty() ?
                book.getLanguage() : "English";
        String rating = book.getRating() > 0 ? String.format("%.1f/5.0", book.getRating()) : "No ratings yet";

        // Get tags
        StringBuilder tagsHtml = new StringBuilder();
        List<String> tags = book.getTags();
        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                tagsHtml.append("<span class='tag'>").append(tag).append("</span> ");
            }
        } else {
            tagsHtml.append("No tags");
        }

        // Get additional authors
        StringBuilder additionalAuthorsHtml = new StringBuilder();
        List<String> additionalAuthors = book.getAdditionalAuthors();
        if (additionalAuthors != null && !additionalAuthors.isEmpty()) {
            for (String author : additionalAuthors) {
                additionalAuthorsHtml.append(author).append("<br>");
            }
        }

        // Get description
        String description = book.getDescription() != null && !book.getDescription().isEmpty() ?
                book.getDescription() : "No description available.";

        // Check book features
        StringBuilder featuresHtml = new StringBuilder();
        if (book.isSpecialEdition()) {
            featuresHtml.append("<span class='feature-badge special-edition'>🌟 Special Edition</span> ");
        }
        if (book.isFeatured()) {
            featuresHtml.append("<span class='feature-badge featured'>⭐ Featured</span> ");
        }
        if (book.isRecommended()) {
            featuresHtml.append("<span class='feature-badge recommended'>👍 Recommended</span> ");
        }

        return """
            %s
            
            <div class="book-header">
                <h3 class="book-title-email">%s</h3>
                <div class="book-features">
                    %s
                </div>
            </div>
            
            <div class="book-detail-list">
                <div class="book-detail-item">
                    <div class="book-detail-label">✍️ Author:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                %s
                <div class="book-detail-item">
                    <div class="book-detail-label">📄 ISBN:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">🏷️ Book ID:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📂 Category:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">🏢 Publisher:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📅 Published:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📖 Edition:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📄 Pages:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">🗣️ Language:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">⭐ Rating:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">🏷️ Tags:</div>
                    <div class="book-detail-value tags-container">%s</div>
                </div>
            </div>
            
            <div class="book-description">
                <h4>📖 Description:</h4>
                <p>%s</p>
            </div>
            """.formatted(
                coverImage,
                book.getTitle(),
                featuresHtml.toString(),
                book.getAuthor(),
                additionalAuthorsHtml.length() > 0 ?
                        """
                        <div class="book-detail-item">
                            <div class="book-detail-label">👥 Co-authors:</div>
                            <div class="book-detail-value">%s</div>
                        </div>
                        """.formatted(additionalAuthorsHtml.toString()) : "",
                book.getIsbn(),
                book.getBookId(),
                book.getCategory(),
                publisherInfo,
                publicationDate,
                editionInfo,
                pageCount,
                language,
                rating,
                tagsHtml.toString(),
                description
        );
    }

    // Base HTML template with styling
    private static String getBaseTemplate(String title, String content) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f5f5f5;
                        margin: 0;
                        padding: 20px;
                    }
                    .email-container {
                        max-width: 700px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border-radius: 10px;
                        overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 40px 20px;
                        text-align: center;
                    }
                    .logo {
                        font-size: 32px;
                        font-weight: bold;
                        margin-bottom: 10px;
                        letter-spacing: 1px;
                    }
                    .subtitle {
                        font-size: 18px;
                        opacity: 0.9;
                        font-weight: 300;
                    }
                    .content {
                        padding: 40px;
                    }
                    .notification-box {
                        background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%);
                        color: white;
                        padding: 25px;
                        border-radius: 10px;
                        margin: 25px 0;
                        text-align: center;
                    }
                    .info-box {
                        background-color: #f8f9fa;
                        border-left: 4px solid #007bff;
                        padding: 20px;
                        margin: 25px 0;
                        border-radius: 8px;
                        font-size: 15px;
                    }
                    .book-details {
                        background-color: #e8f4fd;
                        border: 1px solid #b8daff;
                        border-radius: 10px;
                        padding: 25px;
                        margin: 25px 0;
                    }
                    .book-title-email {
                        font-size: 24px;
                        font-weight: bold;
                        color: #0056b3;
                        margin: 0 0 15px 0;
                        padding-bottom: 10px;
                        border-bottom: 2px solid #eee;
                        text-align: center;
                    }
                    .action-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 15px 40px;
                        text-decoration: none;
                        border-radius: 30px;
                        font-weight: bold;
                        margin: 25px 0;
                        font-size: 16px;
                        transition: transform 0.3s ease;
                    }
                    .action-button:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
                    }
                    .footer {
                        text-align: center;
                        padding: 30px 20px;
                        background-color: #f8f9fa;
                        color: #6c757d;
                        font-size: 14px;
                        border-top: 1px solid #dee2e6;
                    }
                    .highlight {
                        color: #dc3545;
                        font-weight: bold;
                        background-color: #ffeaea;
                        padding: 2px 6px;
                        border-radius: 4px;
                    }
                    .success {
                        color: #28a745;
                        font-weight: bold;
                        background-color: #e8f5e9;
                        padding: 2px 6px;
                        border-radius: 4px;
                    }
                    .warning {
                        color: #ffc107;
                        font-weight: bold;
                        background-color: #fff8e1;
                        padding: 2px 6px;
                        border-radius: 4px;
                    }
                    
                    /* Book cover on top */
                    .book-cover-container {
                        text-align: center;
                        margin: 20px 0 30px 0;
                    }
                    
                    .book-cover-img {
                        width: 220px;
                        height: 300px;
                        object-fit: cover;
                        border-radius: 12px;
                        box-shadow: 0 6px 15px rgba(0,0,0,0.2);
                        border: 1px solid #ddd;
                        display: block;
                        margin: 0 auto;
                    }
                    
                    .cover-placeholder {
                        width: 220px;
                        height: 300px;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        border-radius: 12px;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        color: white;
                        font-size: 48px;
                        box-shadow: 0 6px 15px rgba(0,0,0,0.2);
                        border: 1px solid #ddd;
                        margin: 0 auto;
                        text-align: center;
                        padding: 20px;
                    }
                    
                    .cover-text {
                        font-size: 14px;
                        margin-top: 10px;
                        opacity: 0.9;
                        line-height: 1.4;
                    }
                    
                    .book-detail-item {
                        margin: 10px 0;
                        display: flex;
                        padding: 8px 0;
                        border-bottom: 1px dashed #eee;
                    }
                    .book-detail-item:last-child {
                        border-bottom: none;
                    }
                    .book-detail-label {
                        font-weight: bold;
                        min-width: 140px;
                        color: #555;
                        font-size: 14px;
                    }
                    .book-detail-value {
                        color: #333;
                        flex: 1;
                        font-size: 14px;
                    }
                    .book-detail-list {
                        background-color: #fafafa;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 10px 0;
                        border: 1px solid #eee;
                    }
                    .book-header {
                        margin-bottom: 20px;
                    }
                    .book-features {
                        text-align: center;
                        margin-bottom: 20px;
                    }
                    .feature-badge {
                        display: inline-block;
                        padding: 6px 15px;
                        border-radius: 20px;
                        font-size: 13px;
                        font-weight: bold;
                        margin-right: 10px;
                        margin-bottom: 10px;
                    }
                    .special-edition {
                        background-color: #fff3cd;
                        color: #856404;
                        border: 1px solid #ffeaa7;
                    }
                    .featured {
                        background-color: #d4edda;
                        color: #155724;
                        border: 1px solid #c3e6cb;
                    }
                    .recommended {
                        background-color: #d1ecf1;
                        color: #0c5460;
                        border: 1px solid #bee5eb;
                    }
                    .tags-container {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 8px;
                    }
                    .tag {
                        background-color: #e9ecef;
                        color: #495057;
                        padding: 4px 10px;
                        border-radius: 15px;
                        font-size: 12px;
                        border: 1px solid #dee2e6;
                    }
                    .book-description {
                        background-color: #f9f9f9;
                        border-radius: 10px;
                        padding: 20px;
                        margin: 25px 0;
                        border-left: 4px solid #6c757d;
                    }
                    .book-description h4 {
                        color: #495057;
                        margin-top: 0;
                        margin-bottom: 15px;
                        font-size: 18px;
                    }
                    .book-description p {
                        color: #666;
                        line-height: 1.8;
                        margin: 0;
                    }
                    .user-info {
                        background-color: #f0f8ff;
                        border-radius: 10px;
                        padding: 20px;
                        margin: 20px 0;
                        border: 1px solid #d1e7ff;
                    }
                    .user-info h4 {
                        color: #0056b3;
                        margin-top: 0;
                    }
                    .due-date-info {
                        background: linear-gradient(135deg, #ffecd2 0%%, #fcb69f 100%%);
                        border-radius: 10px;
                        padding: 20px;
                        margin: 20px 0;
                        border: 1px solid #ffd8b1;
                    }
                    .library-info {
                        text-align: center;
                        margin: 25px 0;
                        padding: 20px;
                        background-color: #f8f9fa;
                        border-radius: 10px;
                        border: 1px dashed #6c757d;
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <div class="logo">📚 SMART LIBRARY</div>
                        <div class="subtitle">Digital Library Management System</div>
                    </div>
                    <div class="content">
                        %s
                    </div>
                    <div class="footer">
                        <p>This is an automated message from Smart Library Management System.</p>
                        <p>📍 Library Address: 123 Knowledge Street, Colombo 07, Sri Lanka</p>
                        <p>📞 Contact: +94 11 234 5678 | 📧 Email: library@smartlib.lk</p>
                        <p>⏰ Hours: Mon-Fri 8:00 AM - 8:00 PM, Sat-Sun 9:00 AM - 6:00 PM</p>
                        <p>&copy; 2024 Smart Library. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(title, content);
    }

    public static String generateDueDateReminderTemplate(User user, BorrowRecord record) {
        int daysUntilDue = calculateDaysUntilDue(record.getDueDate());
        Book book = record.getBook();

        String content = """
            <h2>Dear %s,</h2>
            <p>This is a friendly reminder about your borrowed book.</p>
            
            <div class="notification-box">
                <h3>📅 Due Date Reminder</h3>
                <p>The due date for your borrowed book is approaching. Please return it on time.</p>
            </div>
            
            <h3>📚 Book Details:</h3>
            %s
            
            <div class="due-date-info">
                <h4>📅 Borrowing Information:</h4>
                <div class="book-detail-item">
                    <div class="book-detail-label">📖 Borrowed On:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">⏰ Due Date:</div>
                    <div class="book-detail-value"><span class="warning">%s</span></div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">⏳ Days Remaining:</div>
                    <div class="book-detail-value"><strong>%d days</strong></div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">💰 Daily Fine Rate:</div>
                    <div class="book-detail-value">LKR %.2f per day</div>
                </div>
            </div>
            
            <div class="user-info">
                <h4>👤 Your Information:</h4>
                <div class="book-detail-item">
                    <div class="book-detail-label">Member ID:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Membership Type:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Currently Borrowed:</div>
                    <div class="book-detail-value">%d/%d books</div>
                </div>
            </div>
            
            <div class="info-box">
                <p>💡 <strong>Important Notice:</strong></p>
                <p>• Please return or renew the book before the due date to avoid late fees</p>
                <p>• You can renew books online through your library account</p>
                <p>• Late returns affect your borrowing privileges</p>
                <p>• Books can be returned at any library counter or drop box</p>
            </div>
            
            <div class="library-info">
                <p>Need help? Visit our website or contact library staff.</p>
                <center>
                    <a href="https://slms.lk/my-books" class="action-button">View My Borrowed Books</a>
                </center>
            </div>
            
            <p>Thank you for using Smart Library Management System!</p>
            <p>Best regards,<br><strong>The Smart Library Team</strong></p>
            """.formatted(
                user.getName(),
                generateBookInfoSection(book),
                DATE_FORMAT.format(record.getBorrowDate()),
                DATE_FORMAT.format(record.getDueDate()),
                daysUntilDue,
                getDailyFineRateForMembership(user.getMembershipType()),
                user.getUserId(),
                user.getMembershipType().toString(),
                user.getBorrowedBooks().size(),
                getMaxBooksForMembership(user.getMembershipType())
        );

        return getBaseTemplate("Due Date Reminder - Smart Library", content);
    }

    public static String generateOverdueAlertTemplate(User user, BorrowRecord record, double fineAmount) {
        // Calculate overdue days
        Date now = new Date();
        long overdueMillis = now.getTime() - record.getDueDate().getTime();
        int overdueDays = (int) (overdueMillis / (1000 * 60 * 60 * 24));
        Book book = record.getBook();

        String content = """
            <h2>Dear %s,</h2>
            <p>We noticed that you have an overdue book in your account.</p>
            
            <div class="notification-box" style="background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%);">
                <h3>⚠️ URGENT: Overdue Book Alert</h3>
                <p>Your borrowed book is overdue. Immediate action is required.</p>
            </div>
            
            <h3>📚 Overdue Book Details:</h3>
            %s
            
            <div class="book-details" style="background: linear-gradient(135deg, #ffd6d6 0%%, #ff9a9a 100%%);">
                <h4>⏰ Overdue Information:</h4>
                <div class="book-detail-item">
                    <div class="book-detail-label">📅 Due Date:</div>
                    <div class="book-detail-value"><span class="highlight">%s (OVERDUE)</span></div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📅 Borrowed On:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">⏳ Days Overdue:</div>
                    <div class="book-detail-value"><span class="highlight">%d days</span></div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">💰 Accumulated Fine:</div>
                    <div class="book-detail-value"><span class="highlight">LKR %.2f</span></div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📝 Record ID:</div>
                    <div class="book-detail-value">%s</div>
                </div>
            </div>
            
            <div class="user-info">
                <h4>👤 Account Information:</h4>
                <div class="book-detail-item">
                    <div class="book-detail-label">Member ID:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Membership Type:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Daily Fine Rate:</div>
                    <div class="book-detail-value">LKR %.2f per day</div>
                </div>
            </div>
            
            <div class="info-box" style="border-left-color: #dc3545;">
                <p>🚨 <strong>Immediate Action Required:</strong></p>
                <p>1. Please return the book as soon as possible</p>
                <p>2. Pay the overdue fine at the library counter</p>
                <p>3. Your borrowing privileges may be suspended if not resolved within 7 days</p>
                <p>4. Unpaid fines will be added to your library account</p>
            </div>
            
            <div class="library-info">
                <p>💳 <strong>Payment Methods Available:</strong></p>
                <p>• Library Counter (Cash/Card) • Online Payment Portal • Bank Transfer</p>
                <center>
                    <a href="https://slms.lk/pay-fines" class="action-button">Pay Fine Online</a>
                </center>
            </div>
            
            <p>If you have already returned the book, please contact library staff immediately.</p>
            <p>Best regards,<br><strong>Library Management Team</strong></p>
            """.formatted(
                user.getName(),
                generateBookInfoSection(book),
                DATE_FORMAT.format(record.getDueDate()),
                DATE_FORMAT.format(record.getBorrowDate()),
                overdueDays,
                fineAmount,
                record.getRecordId(),
                user.getUserId(),
                user.getMembershipType().toString(),
                getDailyFineRateForMembership(user.getMembershipType())
        );

        return getBaseTemplate("Overdue Alert - Smart Library", content);
    }

    public static String generateReservationAvailableTemplate(User user, Book book) {
        // Generate random shelf number for demo
        int shelfNumber = 100 + (int)(Math.random() * 20);
        String reservationExpiry = new SimpleDateFormat("dd MMMM yyyy 'at' hh:mm a")
                .format(new Date(System.currentTimeMillis() + 48 * 60 * 60 * 1000));

        String content = """
            <h2>Dear %s,</h2>
            <p>Great news! The book you reserved is now available for pickup.</p>
            
            <div class="notification-box" style="background: linear-gradient(135deg, #4facfe 0%%, #00f2fe 100%%);">
                <h3>✅ Reservation Available</h3>
                <p>The book you reserved is now ready for collection. Don't miss it!</p>
            </div>
            
            <h3>📚 Reserved Book Details:</h3>
            %s
            
            <div class="info-box" style="border-left-color: #17a2b8;">
                <p>⏰ <strong>Important Collection Information:</strong></p>
                <div class="book-detail-item">
                    <div class="book-detail-label">📍 Available at:</div>
                    <div class="book-detail-value"><strong>Main Library, Shelf: A%d</strong></div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">⏳ Collection Deadline:</div>
                    <div class="book-detail-value"><span class="warning">%s</span></div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📝 Reservation ID:</div>
                    <div class="book-detail-value"><strong>%s</strong></div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">👤 Reserved by:</div>
                    <div class="book-detail-value">%s</div>
                </div>
            </div>
            
            <div class="user-info">
                <h4>👤 Your Information:</h4>
                <div class="book-detail-item">
                    <div class="book-detail-label">Member ID:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Membership Type:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Email:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Notification Date:</div>
                    <div class="book-detail-value">%s</div>
                </div>
            </div>
            
            <div class="info-box">
                <p>📋 <strong>Collection Instructions:</strong></p>
                <p>1. Bring your library card for identification</p>
                <p>2. Visit the main library circulation desk</p>
                <p>3. Provide your name and reservation ID</p>
                <p>4. The book will be held for 48 hours from this notification</p>
                <p>5. After 48 hours, the reservation will be automatically cancelled</p>
            </div>
            
            <div class="library-info">
                <p>We look forward to seeing you at the library!</p>
                <center>
                    <a href="https://slms.lk/my-reservations" class="action-button">View My Reservations</a>
                </center>
            </div>
            
            <p>Best regards,<br><strong>Smart Library Team</strong></p>
            """.formatted(
                user.getName(),
                generateBookInfoSection(book),
                shelfNumber,
                reservationExpiry,
                "RES-" + System.currentTimeMillis() % 100000,
                user.getName(),
                user.getUserId(),
                user.getMembershipType().toString(),
                user.getEmail(),
                DATETIME_FORMAT.format(new Date())
        );

        return getBaseTemplate("Reservation Available - Smart Library", content);
    }

    public static String generateGeneralNotificationTemplate(User user, String message) {
        String content = """
            <h2>Dear %s,</h2>
            <p>You have a new notification from Smart Library Management System.</p>
            
            <div class="notification-box">
                <h3>📢 Important Notification</h3>
                <p>%s</p>
            </div>
            
            <div class="user-info">
                <h4>👤 Account Details:</h4>
                <div class="book-detail-item">
                    <div class="book-detail-label">Member Name:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Member ID:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Membership Type:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Email:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Contact:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Notification Date:</div>
                    <div class="book-detail-value">%s</div>
                </div>
            </div>
            
            <div class="info-box">
                <p>ℹ️ <strong>Current Account Status:</strong></p>
                <p>• Max Books Allowed: %d</p>
                <p>• Loan Period: %d days</p>
                <p>• Fine Rate: LKR %.2f per day (if applicable)</p>
                <p>• Currently Borrowed: %d books</p>
            </div>
                        
            <div class="library-info">
                <p>Need assistance? Contact our support team.</p>
                <center>
                    <a href="https://slms.lk/support" class="action-button">Contact Support</a>
                </center>
            </div>
            
            <p>Thank you for being a valued member of our library community.</p>
            <p>Best regards,<br><strong>Smart Library Team</strong></p>
            """.formatted(
                user.getName(),
                message.replace("\n", "<br>"),
                user.getName(),
                user.getUserId(),
                user.getMembershipType().toString(),
                user.getEmail(),
                user.getContactNumber(),
                DATETIME_FORMAT.format(new Date()),
                getMaxBooksForMembership(user.getMembershipType()),
                getLoanPeriodForMembership(user.getMembershipType()),
                getDailyFineRateForMembership(user.getMembershipType()),
                user.getBorrowedBooks().size()
        );

        return getBaseTemplate("Library Notification - Smart Library", content);
    }

    public static String generateFineNotificationTemplate(User user, double amount, String reason) {
        // Generate a future due date (7 days from now)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        Date dueDate = calendar.getTime();

        // Generate a random fine ID
        String fineId = "FINE-" + System.currentTimeMillis() % 100000;
        String paymentLink = "https://slms.lk/pay-fine/" + fineId;

        String content = """
            <h2>Dear %s,</h2>
            <p>This is regarding your library account fine.</p>
            
            <div class="notification-box" style="background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%);">
                <h3>💰 Fine Notification</h3>
                <p>You have been charged a fine on your library account.</p>
            </div>
            
            <div class="book-details">
                <h4>📋 Fine Details:</h4>
                <div class="book-detail-item">
                    <div class="book-detail-label">💰 Fine Amount:</div>
                    <div class="book-detail-value"><span class="highlight">LKR %.2f</span></div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📝 Reason:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📅 Issue Date:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">⏰ Due Date for Payment:</div>
                    <div class="book-detail-value"><span class="warning">%s</span></div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📄 Fine ID:</div>
                    <div class="book-detail-value"><strong>%s</strong></div>
                </div>
            </div>
            
            <div class="user-info">
                <h4>👤 Account Information:</h4>
                <div class="book-detail-item">
                    <div class="book-detail-label">Member ID:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Name:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">Membership Type:</div>
                    <div class="book-detail-value">%s</div>
                </div>
            </div>
            
            <div class="info-box" style="border-left-color: #ffc107;">
                <p>💳 <strong>Payment Methods:</strong></p>
                <p>1. <strong>Online Payment:</strong> Click the button below to pay online</p>
                <p>2. <strong>Library Counter:</strong> Cash or card payment at any library branch</p>
                <p>3. <strong>Bank Transfer:</strong> Account details available on our website</p>
                <p>4. <strong>Mobile Payment:</strong> Available through Dialog eZ Cash, Mobitel mCash</p>
                <p><br>Please clear your fine to restore full library privileges.</p>
            </div>
            
            <div class="library-info">
                <p>Pay your fine now to avoid any inconvenience.</p>
                <center>
                    <a href="%s" class="action-button">Pay Fine Now Online</a>
                </center>
                <p style="margin-top: 15px; font-size: 13px; color: #666;">
                    Payment Reference: %s | Member ID: %s
                </p>
            </div>
            
            <p>If you believe this is an error, please contact library staff within 3 days.</p>
            <p>Best regards,<br><strong>Library Finance Department</strong></p>
            """.formatted(
                user.getName(),
                amount,
                reason,
                DATE_FORMAT.format(new Date()),
                DATE_FORMAT.format(dueDate),
                fineId,
                user.getUserId(),
                user.getName(),
                user.getMembershipType().toString(),
                paymentLink,
                fineId,
                user.getUserId()
        );

        return getBaseTemplate("Fine Notification - Smart Library", content);
    }

    public static String generateNewBookNotificationTemplate(User user, Book book) {
        // Check book features
        String featuredBadge = "";
        String recommendedBadge = "";
        String specialEditionBadge = "";

        // Use the actual Book class methods
        if (book.isFeatured()) {
            featuredBadge = "<span class='feature-badge featured'>⭐ Featured Book</span>";
        }
        if (book.isRecommended()) {
            recommendedBadge = "<span class='feature-badge recommended'>👍 Recommended</span>";
        }
        if (book.isSpecialEdition()) {
            specialEditionBadge = "<span class='feature-badge special-edition'>🌟 Special Edition</span>";
        }

        // Get description
        String description = book.getDescription();
        if (description == null || description.isEmpty()) {
            description = "A wonderful addition to our library collection. This book has been highly anticipated and is now available for borrowing.";
        }

        String content = """
            <h2>Dear %s,</h2>
            <p>Exciting news! A new book has been added to our library collection.</p>
            
            <div class="notification-box" style="background: linear-gradient(135deg, #43e97b 0%%, #38f9d7 100%%);">
                <h3>📖 New Book Added to Library</h3>
                <p>Check out our latest addition to the library collection!</p>
            </div>
            
            <h3>📚 New Book Details:</h3>
            %s
            
            <div class="info-box" style="border-left-color: #28a745;">
                <p>🎯 <strong>Why you might like this book:</strong></p>
                <p>• %s</p>
                <p>• Added to our %s collection</p>
                <p>• Available for borrowing immediately</p>
                <p>• Multiple copies available</p>
            </div>
            
            <div class="user-info">
                <h4>📊 Book Statistics:</h4>
                <div class="book-detail-item">
                    <div class="book-detail-label">📅 Added to Library:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📍 Available at:</div>
                    <div class="book-detail-value">All library branches</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📚 Copies Available:</div>
                    <div class="book-detail-value">3 copies (including 1 reserved copy)</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">⏰ Estimated Read Time:</div>
                    <div class="book-detail-value">%d-%.0f hours</div>
                </div>
            </div>
            
            <div class="library-info">
                <p>Visit the library or our online catalog to borrow this book!</p>
                <center>
                    <a href="https://slms.lk/catalog/%s" class="action-button">View Book in Catalog</a>
                </center>
            </div>
            
            <p>Happy reading!</p>
            <p>Best regards,<br><strong>Smart Library Collection Team</strong></p>
            """.formatted(
                user.getName(),
                generateBookInfoSection(book),
                book.isSpecialEdition() ? "Special edition with exclusive content" :
                        book.isFeatured() ? "Featured selection for this month" :
                                book.isRecommended() ? "Recommended by our librarians" : "Popular in its category",
                book.getCategory(),
                DATE_FORMAT.format(new Date()),
                book.getPageCount() / 50,
                Math.ceil(book.getPageCount() / 30),
                book.getBookId()
        );

        return getBaseTemplate("New Book Notification - Smart Library", content);
    }

    public static String generateMembershipUpdateTemplate(User user, String updateMessage) {
        String content = """
            <h2>Dear %s,</h2>
            <p>Important update regarding your library membership.</p>
            
            <div class="notification-box">
                <h3>👤 Membership Update</h3>
                <p>%s</p>
            </div>
            
            <div class="book-details">
                <h4>📋 Membership Details:</h4>
                <div class="book-detail-item">
                    <div class="book-detail-label">👤 Member Name:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">🏷️ Member ID:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">🎫 Membership Type:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📧 Email:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📞 Contact:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📅 Update Date:</div>
                    <div class="book-detail-value">%s</div>
                </div>
            </div>
            
            <div class="info-box">
                <p>ℹ️ <strong>Updated Privileges:</strong></p>
                <p>• <strong>Max Books:</strong> %d books at a time</p>
                <p>• <strong>Loan Period:</strong> %d days per book</p>
                <p>• <strong>Fine Rate:</strong> LKR %.2f per day (if overdue)</p>
                <p>• <strong>Reservation Limit:</strong> %d books can be reserved</p>
                <p>• <strong>Renewals Allowed:</strong> %d times per book</p>
            </div>
            
            <div class="user-info">
                <h4>📊 Your Current Status:</h4>
                <div class="book-detail-item">
                    <div class="book-detail-label">📚 Currently Borrowed:</div>
                    <div class="book-detail-value">%d books</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">⏰ Active Reservations:</div>
                    <div class="book-detail-value">%d books</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">💰 Outstanding Fines:</div>
                    <div class="book-detail-value">LKR %.2f</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📅 Membership Since:</div>
                    <div class="book-detail-value">%s</div>
                </div>
            </div>
            
            <div class="library-info">
                <p>View your updated membership details online.</p>
                <center>
                    <a href="https://slms.lk/my-account" class="action-button">View Membership Details</a>
                </center>
            </div>
            
            <p>If you have any questions about your membership, please contact us.</p>
            <p>Best regards,<br><strong>Smart Library Membership Team</strong></p>
            """.formatted(
                user.getName(),
                updateMessage,
                user.getName(),
                user.getUserId(),
                user.getMembershipType().toString(),
                user.getEmail(),
                user.getContactNumber(),
                DATETIME_FORMAT.format(new Date()),
                getMaxBooksForMembership(user.getMembershipType()),
                getLoanPeriodForMembership(user.getMembershipType()),
                getDailyFineRateForMembership(user.getMembershipType()),
                getMaxBooksForMembership(user.getMembershipType()) / 2,
                user.getMembershipType() == MembershipType.FACULTY ? 3 : 1,
                user.getBorrowedBooks().size(),
                user.getReservations().size(),
                LibraryService.getInstance().getUserTotalFines(user.getUserId()),
                DATE_FORMAT.format(new Date()) // Simplified for demo
        );

        return getBaseTemplate("Membership Update - Smart Library", content);
    }

    // Helper method to get loan period for membership type
    private static int getLoanPeriodForMembership(MembershipType type) {
        switch (type) {
            case STUDENT:
                return 14;
            case FACULTY:
                return 30;
            case GUEST:
                return 7;
            default:
                return 14;
        }
    }

    // New method for book action notifications (borrowed/reserved)
    public static String generateBookActionTemplate(User user, Book book, String action, String additionalInfo) {
        String notificationType = "";
        String notificationStyle = "";
        String actionButtonText = "";
        String actionIcon = "";

        switch (action.toLowerCase()) {
            case "borrowed":
                notificationType = "📚 Book Borrowed Successfully";
                notificationStyle = "background: linear-gradient(135deg, #43e97b 0%%, #38f9d7 100%%);";
                actionButtonText = "View Borrowed Books";
                actionIcon = "📚";
                break;
            case "reserved":
                notificationType = "✅ Book Reserved Successfully";
                notificationStyle = "background: linear-gradient(135deg, #4facfe 0%%, #00f2fe 100%%);";
                actionButtonText = "View Reservations";
                actionIcon = "✅";
                break;
            default:
                notificationType = "📚 Book Action";
                notificationStyle = "background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);";
                actionButtonText = "View Library";
                actionIcon = "📚";
        }

        String content = """
            <h2>Dear %s,</h2>
            <p>This is a confirmation of your recent book action.</p>
            
            <div class="notification-box" style="%s">
                <h3>%s</h3>
                <p>Your book action has been confirmed by the library system.</p>
            </div>
            
            <h3>%s Book Details:</h3>
            %s
            
            <div class="book-details">
                %s
                <div class="book-detail-item">
                    <div class="book-detail-label">⏰ Action Date:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">🎫 Membership Type:</div>
                    <div class="book-detail-value">%s</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">🏷️ Transaction ID:</div>
                    <div class="book-detail-value">TXN-%s</div>
                </div>
            </div>
            
            <div class="user-info">
                <h4>👤 Action Summary:</h4>
                <div class="book-detail-item">
                    <div class="book-detail-label">%s Action:</div>
                    <div class="book-detail-value"><strong>%s</strong></div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">👤 Processed by:</div>
                    <div class="book-detail-value">Smart Library System</div>
                </div>
                <div class="book-detail-item">
                    <div class="book-detail-label">📍 Location:</div>
                    <div class="book-detail-value">Main Library - Digital System</div>
                </div>
            </div>
            
            <div class="library-info">
                <p>View your updated account information.</p>
                <center>
                    <a href="https://slms.lk/my-%s" class="action-button">%s</a>
                </center>
            </div>
            
            <p>Thank you for using Smart Library Management System!</p>
            <p>Best regards,<br><strong>Smart Library Team</strong></p>
            """.formatted(
                user.getName(),
                notificationStyle,
                notificationType,
                actionIcon,
                generateBookInfoSection(book),
                additionalInfo,
                DATETIME_FORMAT.format(new Date()),
                user.getMembershipType().toString(),
                System.currentTimeMillis() % 1000000,
                action.substring(0, 1).toUpperCase() + action.substring(1),
                book.getTitle(),
                action.equals("borrowed") ? "books" : "reservations",
                actionButtonText
        );

        return getBaseTemplate(notificationType + " - Smart Library", content);
    }
}