package com.library.models;

import java.util.Date;

public class Review {
    private String reviewId;
    private String userId;
    private String userName;
    private String comment;
    private int rating;
    private Date reviewDate;

    public Review(String userId, String userName, String comment, int rating) {
        this.reviewId = "RV" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        this.userId = userId;
        this.userName = userName;
        this.comment = comment;
        this.rating = rating;
        this.reviewDate = new Date();
    }

    // Getters and setters
    public String getReviewId() { return reviewId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public int getRating() { return rating; }
    public void setRating(int rating) {
        if (rating >= 1 && rating <= 5) {
            this.rating = rating;
        }
    }
    public Date getReviewDate() { return reviewDate; }
    public void setReviewDate(Date reviewDate) { this.reviewDate = reviewDate; }

    @Override
    public String toString() {
        return String.format("%s: %d/5 - %s", userName, rating, comment);
    }
}