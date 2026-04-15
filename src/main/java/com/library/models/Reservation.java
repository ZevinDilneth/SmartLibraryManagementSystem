package com.library.models;

import java.util.Date;
import java.util.Calendar;

public class Reservation {
    private String reservationId;
    private Book book;
    private User user;
    private Date reservationDate;
    private boolean notified;
    private Date notificationDate;
    private boolean expired;
    private Date expiryDate;

    public Reservation(Book book, User user) {
        this.reservationId = generateReservationId();
        this.book = book;
        this.user = user;
        this.reservationDate = new Date();
        this.notified = false;
        this.notificationDate = null;
        this.expired = false;

        // Set default expiry date (30 days from reservation)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 30);
        this.expiryDate = calendar.getTime();
    }

    // Auto-generate reservation ID
    private String generateReservationId() {
        return "RSV" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    // Getters and Setters
    public String getReservationId() { return reservationId; }
    public Book getBook() { return book; }
    public User getUser() { return user; }
    public Date getReservationDate() { return reservationDate; }

    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) {
        this.notified = notified;
        this.notificationDate = new Date();
        if (notified) {
            // Set expiry date when notified (48 hours from notification)
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, 48);
            this.expiryDate = calendar.getTime();
            System.out.println("Reservation notified. Expiry set to: " + expiryDate);
        }
    }

    public Date getNotificationDate() { return notificationDate; }

    public boolean isExpired() {
        if (expired) return true;
        if (expiryDate != null) {
            boolean isExpired = new Date().after(expiryDate);
            if (isExpired && !expired) {
                System.out.println("Reservation " + reservationId + " has expired");
                expired = true;
            }
            return isExpired;
        }
        return false;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
        if (expired) {
            this.expiryDate = new Date(); // Set expiry date to now
        }
    }

    public void setNotificationDate(Date notificationDate) {
        this.notificationDate = notificationDate;
    }

    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "reservationId='" + reservationId + '\'' +
                ", book=" + (book != null ? book.getTitle() : "null") +
                ", user=" + (user != null ? user.getName() : "null") +
                ", reservationDate=" + reservationDate +
                ", notified=" + notified +
                ", notificationDate=" + notificationDate +
                ", expired=" + expired +
                '}';
    }
}