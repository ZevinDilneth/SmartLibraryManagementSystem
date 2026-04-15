package com.library;

import com.library.gui.MainFrame;
import com.library.services.EmailSchedulerService;
import javax.swing.*;

public class MainApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                // Start the email scheduler (checks every 10 seconds)
                startEmailScheduler();

                // Create and show the main GUI
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Error starting application: " + e.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void startEmailScheduler() {
        try {
            System.out.println("🚀 Starting email scheduler...");

            // Get the email scheduler instance
            EmailSchedulerService scheduler = EmailSchedulerService.getInstance();

            // Start all schedulers (checks every 10 seconds)
            scheduler.startAllSchedulers();

            System.out.println("✅ Email scheduler started successfully!");
            System.out.println("📅 All checks will run every 10 seconds");
            System.out.println("📧 Emails will be sent for:");
            System.out.println("   - Due Date Reminders (1 day before deadline)");
            System.out.println("   - Overdue Alerts (daily after deadline)");
            System.out.println("   - Reservation Available (when book is returned)");

        } catch (Exception e) {
            System.err.println("❌ Failed to start email scheduler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}