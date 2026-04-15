package com.library.models;

import java.util.Date;

public class BorrowRecord {
    private String recordId;
    private Book book;
    private User user;
    private Date borrowDate;
    private Date dueDate;
    private Date returnDate;
    private double fineAmount;
    private boolean finePaid;

    public BorrowRecord(Book book, User user, Date borrowDate, Date dueDate) {
        this.recordId = generateRecordId();
        this.book = book;
        this.user = user;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.fineAmount = 0.0;
        this.finePaid = false;
    }

    // Auto-generate record ID
    private String generateRecordId() {
        return "BR" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    // Getters and Setters
    public String getRecordId() { return recordId; }
    public Book getBook() { return book; }
    public User getUser() { return user; }
    public Date getBorrowDate() { return borrowDate; }
    public Date getDueDate() { return dueDate; }
    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }
    public double getFineAmount() { return fineAmount; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }
    public boolean isFinePaid() { return finePaid; }
    public void setFinePaid(boolean finePaid) { this.finePaid = finePaid; }

    public boolean isOverdue() {
        if (returnDate != null) return false;
        Date now = new Date();
        return now.after(dueDate);
    }

    public int getOverdueDays() {
        if (!isOverdue()) return 0;
        Date now = new Date();
        long diff = now.getTime() - dueDate.getTime();
        return (int) (diff / (1000 * 60 * 60 * 24));
    }
}