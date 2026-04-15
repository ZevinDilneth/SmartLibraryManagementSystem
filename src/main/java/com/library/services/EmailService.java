package com.library.services;

import com.library.config.EmailConfiguration;
import com.library.models.User;
import com.library.models.Book;
import com.library.models.BorrowRecord;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Date;
import java.util.Properties;

public class EmailService {
    private static EmailService instance;
    private Session session;

    private EmailService() {
        initializeSession();
    }

    public static EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    private void initializeSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", EmailConfiguration.SMTP_HOST);
        props.put("mail.smtp.port", EmailConfiguration.SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(EmailConfiguration.USE_TLS));
        props.put("mail.smtp.ssl.enable", String.valueOf(EmailConfiguration.USE_SSL));
        props.put("mail.smtp.ssl.trust", EmailConfiguration.SMTP_HOST);
        props.put("mail.debug", String.valueOf(EmailConfiguration.DEBUG));

        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        EmailConfiguration.SMTP_USERNAME,
                        EmailConfiguration.SMTP_PASSWORD
                );
            }
        };

        session = Session.getInstance(props, authenticator);
    }

    public boolean sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        try {
            MimeMessage message = new MimeMessage(session);

            // Set From
            message.setFrom(new InternetAddress(
                    EmailConfiguration.SENDER_EMAIL,
                    EmailConfiguration.SENDER_NAME
            ));

            // Set To
            message.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(toEmail, toName));

            // Set Subject
            message.setSubject(subject, "UTF-8");

            // Set HTML Content
            MimeMultipart multipart = new MimeMultipart("alternative");

            // HTML Part
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            message.setContent(multipart);
            message.setSentDate(new Date());

            // Send Email
            Transport.send(message);
            System.out.println("Email sent successfully to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Template-based email sending methods
    public boolean sendDueDateReminderEmail(User user, BorrowRecord record) {
        String htmlContent = EmailTemplateGenerator.generateDueDateReminderTemplate(user, record);
        return sendEmail(user.getEmail(), user.getName(),
                EmailConfiguration.SUBJECT_DUE_DATE, htmlContent);
    }

    public boolean sendOverdueAlertEmail(User user, BorrowRecord record, double fineAmount) {
        String htmlContent = EmailTemplateGenerator.generateOverdueAlertTemplate(user, record, fineAmount);
        return sendEmail(user.getEmail(), user.getName(),
                EmailConfiguration.SUBJECT_OVERDUE, htmlContent);
    }

    public boolean sendReservationAvailableEmail(User user, Book book) {
        String htmlContent = EmailTemplateGenerator.generateReservationAvailableTemplate(user, book);
        return sendEmail(user.getEmail(), user.getName(),
                EmailConfiguration.SUBJECT_RESERVATION, htmlContent);
    }

    public boolean sendGeneralNotificationEmail(User user, String message) {
        String htmlContent = EmailTemplateGenerator.generateGeneralNotificationTemplate(user, message);
        return sendEmail(user.getEmail(), user.getName(),
                EmailConfiguration.SUBJECT_GENERAL, htmlContent);
    }

    public boolean sendFineNotificationEmail(User user, double amount, String reason) {
        String htmlContent = EmailTemplateGenerator.generateFineNotificationTemplate(user, amount, reason);
        return sendEmail(user.getEmail(), user.getName(),
                EmailConfiguration.SUBJECT_FINE, htmlContent);
    }

    public boolean sendNewBookNotificationEmail(User user, Book book) {
        String htmlContent = EmailTemplateGenerator.generateNewBookNotificationTemplate(user, book);
        return sendEmail(user.getEmail(), user.getName(),
                EmailConfiguration.SUBJECT_BOOK_ADDED, htmlContent);
    }

    public boolean sendMembershipUpdateEmail(User user, String updateMessage) {
        String htmlContent = EmailTemplateGenerator.generateMembershipUpdateTemplate(user, updateMessage);
        return sendEmail(user.getEmail(), user.getName(),
                EmailConfiguration.SUBJECT_MEMBERSHIP, htmlContent);
    }

    // New method for book action notifications (borrowed/reserved)
    public boolean sendBookActionEmail(User user, Book book, String action, String additionalInfo) {
        String htmlContent = EmailTemplateGenerator.generateBookActionTemplate(user, book, action, additionalInfo);
        String subject = "";

        switch (action.toLowerCase()) {
            case "borrowed":
                subject = "📚 Book Borrowed - SLMS";
                break;
            case "reserved":
                subject = "✅ Book Reserved - SLMS";
                break;
            default:
                subject = "📚 Book Action - SLMS";
        }

        return sendEmail(user.getEmail(), user.getName(), subject, htmlContent);
    }

    // Test email connection
    public boolean testConnection() {
        try {
            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();
            return true;
        } catch (Exception e) {
            System.err.println("SMTP Connection Test Failed: " + e.getMessage());
            return false;
        }
    }
}