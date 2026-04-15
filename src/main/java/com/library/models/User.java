package com.library.models;

import com.library.enums.MembershipType;
import com.library.patterns.observer.BookObserver;
import java.util.ArrayList;
import java.util.List;

public class User implements BookObserver {
    private String userId;
    private String name;
    private String email;
    private String contactNumber;
    private MembershipType membershipType;
    private List<Book> borrowedBooks;
    private List<BorrowRecord> borrowHistory;
    private List<Reservation> reservations;

    public User(String name, String email, String contactNumber, MembershipType membershipType) {
        this.userId = generateUserId();
        this.name = name;
        this.email = email;
        this.contactNumber = contactNumber;
        this.membershipType = membershipType;
        this.borrowedBooks = new ArrayList<>();
        this.borrowHistory = new ArrayList<>();
        this.reservations = new ArrayList<>();
    }

    // Auto-generate user ID
    private String generateUserId() {
        return "USR" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public MembershipType getMembershipType() { return membershipType; }
    public void setMembershipType(MembershipType membershipType) { this.membershipType = membershipType; }
    public List<Book> getBorrowedBooks() { return borrowedBooks; }
    public void borrowBook(Book book) { this.borrowedBooks.add(book); }
    public void returnBook(Book book) { this.borrowedBooks.remove(book); }
    public List<BorrowRecord> getBorrowHistory() { return borrowHistory; }
    public void addBorrowRecord(BorrowRecord record) { this.borrowHistory.add(record); }
    public List<Reservation> getReservations() { return reservations; }
    public void addReservation(Reservation reservation) { this.reservations.add(reservation); }
    public void removeReservation(Reservation reservation) { this.reservations.remove(reservation); }

    public boolean canBorrowMoreBooks() {
        return borrowedBooks.size() < membershipType.getBorrowingLimit();
    }

    // Implement BookObserver interface
    @Override
    public void update(String message) {
        System.out.println("🔔 Notification for " + name + " (" + email + "): " + message);
    }

    @Override
    public String getObserverId() {
        return userId;
    }

    @Override
    public String toString() {
        return name + " (" + membershipType.getDisplayName() + ")";
    }
}