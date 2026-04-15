package com.library.models;

import com.library.enums.BookStatus;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Book {
    private String bookId;
    private String title;
    private String author;
    private String category;
    private String isbn;
    private BookStatus status;
    private List<BorrowRecord> borrowHistory;
    private int edition;
    private String publisher;
    private Date publicationDate;
    private List<String> tags;
    private boolean featured;
    private boolean recommended;
    private boolean specialEdition;
    private List<Review> reviews;
    private List<String> additionalAuthors;
    private double rating;
    private String coverImagePath; 
    private String description;
    private String language;
    private int pageCount;
    private List<Reservation> reservations; 

    public Book(String title, String author, String category, String isbn) {
        this.bookId = generateBookId();
        this.title = title;
        this.author = author;
        this.category = category;
        this.isbn = isbn;
        this.status = BookStatus.AVAILABLE;
        this.borrowHistory = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.featured = false;
        this.recommended = false;
        this.specialEdition = false;
        this.reviews = new ArrayList<>();
        this.additionalAuthors = new ArrayList<>();
        this.rating = 0.0;
        this.coverImagePath = "";
        this.description = "";
        this.language = "English";
        this.pageCount = 0;
        this.reservations = new ArrayList<>(); // Initialize reservations list
    }

    // Auto-generate book ID
    private String generateBookId() {
        return "BK" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    // Getters and Setters
    public String getBookId() { return bookId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public BookStatus getStatus() { return status; }
    public void setStatus(BookStatus status) { this.status = status; }
    public List<BorrowRecord> getBorrowHistory() { return borrowHistory; }
    public void addBorrowRecord(BorrowRecord record) { this.borrowHistory.add(record); }
    public int getEdition() { return edition; }
    public void setEdition(int edition) { this.edition = edition; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public Date getPublicationDate() { return publicationDate; }
    public void setPublicationDate(Date publicationDate) { this.publicationDate = publicationDate; }
    public List<String> getTags() { return tags; }
    public void addTag(String tag) { this.tags.add(tag); }
    public void removeTag(String tag) { this.tags.remove(tag); }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public boolean isRecommended() { return recommended; }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }
    public boolean isSpecialEdition() { return specialEdition; }
    public void setSpecialEdition(boolean specialEdition) { this.specialEdition = specialEdition; }

    // Extended metadata getters and setters
    public List<Review> getReviews() { return reviews; }
    public void addReview(Review review) { this.reviews.add(review); }
    public void removeReview(Review review) { this.reviews.remove(review); }
    public List<String> getAdditionalAuthors() { return additionalAuthors; }
    public void addAdditionalAuthor(String author) { this.additionalAuthors.add(author); }
    public void removeAdditionalAuthor(String author) { this.additionalAuthors.remove(author); }
    public double getRating() { return rating; }
    public void setRating(double rating) {
        if (rating >= 0 && rating <= 5) {
            this.rating = rating;
        }
    }
    public String getCoverImagePath() { return coverImagePath; }
    public void setCoverImagePath(String coverImagePath) { this.coverImagePath = coverImagePath; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) {
        if (pageCount >= 0) {
            this.pageCount = pageCount;
        }
    }

    // Reservations management
    public List<Reservation> getReservations() {
        return new ArrayList<>(reservations);
    }

    public void addReservation(Reservation reservation) {
        this.reservations.add(reservation);
    }

    public void removeReservation(Reservation reservation) {
        this.reservations.remove(reservation);
    }

    public boolean hasActiveReservations() {
        return reservations.stream().anyMatch(r -> !r.isNotified() && !r.isExpired());
    }

    // Helper method to calculate average rating from reviews
    public double calculateAverageRating() {
        if (reviews.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (Review review : reviews) {
            total += review.getRating();
        }
        return total / reviews.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Add feature indicators
        if (specialEdition) {
            sb.append("🌟 SPECIAL EDITION 🌟 ");
        }
        if (featured) {
            sb.append("⭐ FEATURED ⭐ ");
        }
        if (recommended) {
            sb.append("👍 RECOMMENDED 👍 ");
        }

        sb.append(title).append(" by ").append(author);

        if (edition > 0) {
            sb.append(" (Edition ").append(edition).append(")");
        }

        sb.append(" [").append(isbn).append("]");

        // Add reservation info if any
        if (!reservations.isEmpty()) {
            long activeReservations = reservations.stream()
                    .filter(r -> !r.isNotified() && !r.isExpired())
                    .count();
            if (activeReservations > 0) {
                sb.append(" [").append(activeReservations).append(" reservation(s) pending]");
            }
        }

        return sb.toString();
    }
}