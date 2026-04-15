package com.library.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;

public class MainFrame extends JFrame {

    private JTabbedPane tabbedPane;
    private BookManagementPanel bookPanel;
    private UserManagementPanel userPanel;
    private BorrowReturnPanel borrowPanel;
    private ReservationPanel reservationPanel;
    private ReportsPanel reportsPanel;
    private NotificationsPanel notificationsPanel;
    private boolean firstTimeNormalState = true;

    public MainFrame() {
        setTitle("Smart Library Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set minimum size to ensure usability when not maximized
        setMinimumSize(new Dimension(1250, 750));

        // Set window icon from resources
        setIconImage(loadIcon("/icon.png"));

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Create panels
        bookPanel = new BookManagementPanel();
        userPanel = new UserManagementPanel();
        borrowPanel = new BorrowReturnPanel();
        reservationPanel = new ReservationPanel();
        reportsPanel = new ReportsPanel();
        notificationsPanel = new NotificationsPanel();

        // Add panels to tabbed pane
        tabbedPane.addTab("📚 Book Management", bookPanel);
        tabbedPane.addTab("👥 User Management", userPanel);
        tabbedPane.addTab("📖 Borrow/Return", borrowPanel);
        tabbedPane.addTab("🔔 Reservations", reservationPanel);
        tabbedPane.addTab("📊 Reports", reportsPanel);
        tabbedPane.addTab("🔔 Notifications", notificationsPanel);

        // Add tab change listener to auto-refresh panels
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            refreshPanel(selectedIndex);
        });

        // Add component listener to detect when window is shown/resized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Check if window is in normal state (not maximized or minimized)
                if ((getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
                    // Only center on the first time it enters normal state
                    if (firstTimeNormalState) {
                        centerWindow();
                        firstTimeNormalState = false;
                    }
                } else {
                    // Reset flag when maximized
                    firstTimeNormalState = true;
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
                // Center if window is shown in normal state
                if ((getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
                    centerWindow();
                }
            }
        });

        // Add window state listener to handle minimize/restore
        addWindowStateListener(e -> {
            // When window is restored from minimized
            if ((e.getOldState() & Frame.ICONIFIED) != 0 &&
                    (e.getNewState() & Frame.ICONIFIED) == 0) {

                // If window is not maximized after restore, center it
                if ((e.getNewState() & Frame.MAXIMIZED_BOTH) == 0) {
                    SwingUtilities.invokeLater(() -> {
                        centerWindow();
                    });
                }
            }

            // When window changes from maximized to normal
            if ((e.getOldState() & Frame.MAXIMIZED_BOTH) != 0 &&
                    (e.getNewState() & Frame.MAXIMIZED_BOTH) == 0) {

                SwingUtilities.invokeLater(() -> {
                    centerWindow();
                });
            }
        });

        // Add tabbed pane to frame
        add(tabbedPane);

        // Start maximized
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    // Method to center the window on screen
    private void centerWindow() {
        SwingUtilities.invokeLater(() -> {
            // Ensure window is not minimized
            if ((getExtendedState() & Frame.ICONIFIED) == 0) {
                setLocationRelativeTo(null);
                // Force repaint to ensure window is properly positioned
                revalidate();
                repaint();
            }
        });
    }

    private void refreshPanel(int tabIndex) {
        switch (tabIndex) {
            case 0: // Book Management
                bookPanel.refreshTable();
                break;
            case 1: // User Management
                userPanel.refreshTable();
                break;
            case 2: // Borrow/Return
                borrowPanel.refreshLists();
                break;
            case 3: // Reservations
                reservationPanel.refreshLists();
                break;
            case 4: // Reports
                reportsPanel.generateAllReports(false);
                break;
            case 5: // Notifications
                notificationsPanel.refreshLists();
                break;
        }
    }

    // Loads icon safely from resources
    private Image loadIcon(String path) {
        try {
            URL iconURL = getClass().getResource(path);
            if (iconURL != null) {
                return new ImageIcon(iconURL).getImage();
            } else {
                System.err.println("Icon not found at: " + path);
            }
        } catch (Exception e) {
            System.err.println("Error loading icon: " + e);
        }
        return null;
    }

    // Main method to run the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}