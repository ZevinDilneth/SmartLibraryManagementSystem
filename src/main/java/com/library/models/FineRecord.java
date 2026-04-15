package com.library.models;

import com.library.enums.FineStatus;
import java.util.Date;

public class FineRecord {
    private String fineId;
    private String borrowRecordId;
    private String userId;
    private double amount;
    private Date issueDate;
    private Date dueDate;
    private FineStatus status;
    private String reason;

    public FineRecord(BorrowRecord borrowRecord) {
        this.fineId = generateFineId();
        this.borrowRecordId = borrowRecord.getRecordId();
        this.userId = borrowRecord.getUser().getUserId();
        this.amount = borrowRecord.getFineAmount();
        this.issueDate = new Date();
        this.status = FineStatus.PENDING;
        System.out.println("💰 Fine Record created: ID=" + fineId +
                ", Amount=LKR " + amount +
                ", User=" + userId);
    }

    // Auto-generate fine ID
    private String generateFineId() {
        return "FINE" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    // Getters and Setters
    public String getFineId() { return fineId; }
    public String getBorrowRecordId() { return borrowRecordId; }
    public String getUserId() { return userId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) {
        this.amount = amount;
        System.out.println("💰 Fine amount updated: LKR " + amount);
    }
    public Date getIssueDate() { return issueDate; }
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
        System.out.println("📅 Fine due date set: " + dueDate);
    }
    public FineStatus getStatus() { return status; }
    public void setStatus(FineStatus status) {
        this.status = status;
        System.out.println("📊 Fine status updated: " + status.getDisplayName());
    }
    public String getReason() { return reason; }
    public void setReason(String reason) {
        this.reason = reason;
        System.out.println("📝 Fine reason set: " + reason);
    }

    public void markAsPaid() {
        this.status = FineStatus.PAID;
        System.out.println("✅ Fine marked as PAID: " + fineId);
    }

    public boolean isOverdue() {
        Date now = new Date();
        boolean overdue = status == FineStatus.PENDING && dueDate != null && now.after(dueDate);
        if (overdue) {
            System.out.println("⚠️ Fine is OVERDUE: " + fineId);
        }
        return overdue;
    }
}