package com.library.gui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.List;
import com.library.services.LibraryService;
import com.library.services.NotificationService;
import com.library.models.User;
import com.library.models.Book;
import com.library.models.BorrowRecord;
import java.util.Date;

public class NotificationsPanel extends JPanel {
    private JTable notificationsTable;
    private DefaultTableModel tableModel;
    private JTextArea messageArea;
    private JComboBox<String> userComboBox;
    private JComboBox<String> typeComboBox;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private NotificationService notificationService;
    private LibraryService libraryService;

    private JLabel totalLabel;
    private JLabel dueDateReminderLabel;
    private JLabel overdueAlertLabel;
    private JLabel reservationAvailableLabel;
    private JLabel generalNotificationLabel;

    // Search components
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> tableSorter;

    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 150); // Light yellow

    public NotificationsPanel() {
        notificationService = NotificationService.getInstance();
        libraryService = LibraryService.getInstance();

        initializeUI();
        refreshLists();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        // Table - REMOVED "Status" column
        String[] columns = {"Date", "Type", "User", "Message"}; // Removed "Status"
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        notificationsTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                // Convert view row to model row
                int modelRow = convertRowIndexToModel(row);
                String searchText = searchField.getText().trim().toLowerCase();

                // Check if this row contains the search text in any column
                boolean rowHasMatch = false;
                if (!searchText.isEmpty()) {
                    for (int col = 0; col < getColumnCount(); col++) {
                        Object value = tableModel.getValueAt(modelRow, col);
                        if (value != null && value.toString().toLowerCase().contains(searchText)) {
                            rowHasMatch = true;
                            break;
                        }
                    }
                }

                // Highlight entire row if it contains the search text
                if (rowHasMatch) {
                    c.setBackground(HIGHLIGHT_COLOR);
                } else {
                    // Default background
                    if (!c.getBackground().equals(getSelectionBackground())) {
                        Color bg = (row % 2 == 0) ? Color.WHITE : new Color(245, 245, 245);
                        c.setBackground(bg);
                    }
                }
                return c;
            }
        };

        JScrollPane scrollPane = new JScrollPane(notificationsTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Notifications List"));

        // CONTROL PANEL WITH GRIDBAGLAYOUT
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Send Notification"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Create searchable user combo box
        userComboBox = createSearchableComboBox();
        typeComboBox = new JComboBox<>(new String[]{
                "Due Date Reminder",
                "Overdue Alert",
                "Reservation Available",
                "General Notification"
        });

        // Message area (3× taller)
        messageArea = new JTextArea(5, 30);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane messageScroll = new JScrollPane(messageArea);

        // ROW 1 - USER LABEL
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        controlPanel.add(new JLabel("User:"), gbc);

        // ROW 1 - USER COMBOBOX (with filler before it to push to middle)
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        controlPanel.add(Box.createHorizontalStrut(50), gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        controlPanel.add(userComboBox, gbc);

        // ROW 2 - TYPE LABEL
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        controlPanel.add(new JLabel("Type:"), gbc);

        // ROW 2 - TYPE COMBOBOX (with filler before it to push to middle)
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        controlPanel.add(Box.createHorizontalStrut(50), gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        controlPanel.add(typeComboBox, gbc);

        // ROW 3 - MESSAGE LABEL
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        controlPanel.add(new JLabel("Message:"), gbc);

        // ROW 3 - MESSAGE AREA (BIG!) (with filler before it to push to middle)
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        controlPanel.add(Box.createHorizontalStrut(50), gbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        controlPanel.add(messageScroll, gbc);

        // STATS PANEL
        JPanel statsPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Notification Statistics"));

        totalLabel = new JLabel("Total: 0", SwingConstants.CENTER);
        dueDateReminderLabel = new JLabel("Due Date Reminder: 0", SwingConstants.CENTER);
        overdueAlertLabel = new JLabel("Overdue Alert: 0", SwingConstants.CENTER);
        reservationAvailableLabel = new JLabel("Reservation Available: 0", SwingConstants.CENTER);
        generalNotificationLabel = new JLabel("General Notification: 0", SwingConstants.CENTER);

        statsPanel.add(totalLabel);
        statsPanel.add(dueDateReminderLabel);
        statsPanel.add(overdueAlertLabel);
        statsPanel.add(reservationAvailableLabel);
        statsPanel.add(generalNotificationLabel);

        //STRETCHED SEND NOTIFICATION BUTTON
        JButton addButton = new JButton("Send Notification");
        addButton.addActionListener(e -> sendNotification());
        addButton.setBorder(new LineBorder(new Color(76, 175, 80), 2)); // Green color

        // Make button stretch horizontally
        addButton.setPreferredSize(new Dimension(Integer.MAX_VALUE, addButton.getPreferredSize().height));

        //SEARCH PANEL
        JPanel searchPanel = new JPanel(new BorderLayout(10, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Notifications"));
        searchPanel.setPreferredSize(new Dimension(0, 60)); // Set preferred height

        // Inner panel for search components with GridBagLayout for better control
        JPanel searchComponentsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints searchGbc = new GridBagConstraints();
        searchGbc.fill = GridBagConstraints.HORIZONTAL;
        searchGbc.insets = new Insets(5, 5, 5, 5);

        // Search label
        searchGbc.gridx = 0;
        searchGbc.gridy = 0;
        searchGbc.weightx = 0;
        searchComponentsPanel.add(new JLabel("Search:"), searchGbc);

        // Search field - will stretch horizontally
        searchField = new JTextField();
        searchField.setToolTipText("Search by Date, Type, User, or Message...");
        searchGbc.gridx = 1;
        searchGbc.gridy = 0;
        searchGbc.weightx = 1.0; // Take all available horizontal space
        searchGbc.fill = GridBagConstraints.HORIZONTAL;
        searchComponentsPanel.add(searchField, searchGbc);

        // Search button
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());
        searchGbc.gridx = 2;
        searchGbc.gridy = 0;
        searchGbc.weightx = 0;
        searchGbc.fill = GridBagConstraints.NONE;
        searchComponentsPanel.add(searchButton, searchGbc);

        // Clear button
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearSearch());
        searchGbc.gridx = 3;
        searchGbc.gridy = 0;
        searchComponentsPanel.add(clearButton, searchGbc);

        // Add key listener for Enter key
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performSearch();
                }
            }
        });

        searchPanel.add(searchComponentsPanel, BorderLayout.CENTER);

        // TOP SECTION
        JPanel topSectionPanel = new JPanel(new BorderLayout());
        topSectionPanel.add(controlPanel, BorderLayout.NORTH);
        topSectionPanel.add(addButton, BorderLayout.CENTER);
        topSectionPanel.add(statsPanel, BorderLayout.SOUTH);

        // CENTER PANEL WITH SEARCH AND TABLE
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // BOTTOM PANEL WITH REFRESH BUTTON
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshLists());
        refreshButton.setBorder(new LineBorder(new Color(255, 193, 7), 2)); // Orange color

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomPanel.add(refreshButton);

        // MAIN LAYOUT
        add(topSectionPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Setup table sorter for search
        tableSorter = new TableRowSorter<>(tableModel);
        notificationsTable.setRowSorter(tableSorter);

        notificationsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && notificationsTable.getSelectedRow() != -1) {
                int row = notificationsTable.getSelectedRow();
                String message = tableModel.getValueAt(row, 3).toString();
                JOptionPane.showMessageDialog(this,
                        "Notification Details:\n\n" + message,
                        "Notification View",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private void performSearch() {
        String searchText = searchField.getText().trim().toLowerCase();

        if (searchText.isEmpty()) {
            clearSearch();
            return;
        }

        // Clear previous filter
        tableSorter.setRowFilter(null);

        // Create a filter that searches all columns
        RowFilter<DefaultTableModel, Integer> filter = new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                for (int i = 0; i < entry.getValueCount(); i++) {
                    String value = entry.getStringValue(i);
                    if (value != null && value.toLowerCase().contains(searchText)) {
                        return true;
                    }
                }
                return false;
            }
        };

        tableSorter.setRowFilter(filter);

        // Force table repaint to update highlights
        notificationsTable.repaint();

        // Show notification only if no results found
        int resultCount = notificationsTable.getRowCount();
        if (resultCount == 0) {
            JOptionPane.showMessageDialog(this,
                    "No notifications found for '" + searchText + "'",
                    "Search Results",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void clearSearch() {
        searchField.setText("");
        tableSorter.setRowFilter(null);
        notificationsTable.repaint();
        searchField.requestFocus();
    }

    private JComboBox<String> createSearchableComboBox() {
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.setEditable(true);

        // Add key listener to the editor component for filtering
        JTextField editor = (JTextField) comboBox.getEditor().getEditorComponent();
        editor.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    // Handle Enter key - try to find and select matching item
                    String searchText = editor.getText().trim();
                    if (!searchText.isEmpty()) {
                        ComboBoxModel<String> model = comboBox.getModel();
                        for (int i = 0; i < model.getSize(); i++) {
                            String item = model.getElementAt(i);
                            if (item.equalsIgnoreCase(searchText) ||
                                    item.toLowerCase().contains(searchText.toLowerCase())) {
                                comboBox.setSelectedItem(item);
                                editor.setText(item);
                                break;
                            }
                        }
                    }
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    comboBox.setPopupVisible(false);
                } else if (e.getKeyCode() != java.awt.event.KeyEvent.VK_UP &&
                        e.getKeyCode() != java.awt.event.KeyEvent.VK_DOWN) {
                    // Filter on typing (except arrow keys)
                    filterComboBox(comboBox, editor.getText());
                }
            }
        });

        return comboBox;
    }

    private void filterComboBox(JComboBox<String> comboBox, String filter) {
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) comboBox.getModel();
        DefaultComboBoxModel<String> filteredModel = new DefaultComboBoxModel<>();

        // Store original items if not already stored
        if (comboBox.getClientProperty("originalModel") == null) {
            comboBox.putClientProperty("originalModel", model);
        }

        DefaultComboBoxModel<String> originalModel =
                (DefaultComboBoxModel<String>) comboBox.getClientProperty("originalModel");

        if (filter.isEmpty()) {
            // Restore original model when filter is empty
            comboBox.setModel(originalModel);
        } else {
            // Filter items
            String filterLower = filter.toLowerCase();
            for (int i = 0; i < originalModel.getSize(); i++) {
                String item = originalModel.getElementAt(i);
                if (item.toLowerCase().contains(filterLower)) {
                    filteredModel.addElement(item);
                }
            }
            comboBox.setModel(filteredModel);
            comboBox.setPopupVisible(filteredModel.getSize() > 0);
        }

        // Make sure the editor text remains what the user typed
        JTextField editor = (JTextField) comboBox.getEditor().getEditorComponent();
        editor.setText(filter);
    }

    private void sendNotification() {
        String selectedUser = (String) userComboBox.getSelectedItem();
        String type = (String) typeComboBox.getSelectedItem();
        String message = messageArea.getText().trim();

        // Check if user is selected
        if (selectedUser == null || selectedUser.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a user",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Special rule: General Notification requires a message
        if ("General Notification".equals(type) && message.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "To send a General Notification you must write a message",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // For other types, empty message is allowed - will use default messages

        // Extract user ID from the combo box selection
        String userId = selectedUser.split(" - ")[0];

        // Get the user to verify
        User user = libraryService.getUser(userId);
        if (user == null) {
            JOptionPane.showMessageDialog(this,
                    "Selected user not found",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Send notification based on the selected type
        boolean notificationSent = false;

        switch (type) {
            case "General Notification":
                // General notification always requires a message (already validated above)
                notificationService.sendGeneralNotification(userId, message);
                notificationSent = true;
                break;

            case "Due Date Reminder":
                // For due date reminders, we need a BorrowRecord
                List<BorrowRecord> userRecords = libraryService.getAllBorrowRecords();
                BorrowRecord recordForReminder = null;

                for (BorrowRecord record : userRecords) {
                    if (record.getUser().getUserId().equals(userId) && record.getReturnDate() == null) {
                        recordForReminder = record;
                        break;
                    }
                }

                if (recordForReminder != null) {
                    notificationService.sendDueDateReminder(recordForReminder);
                    notificationSent = true;
                } else {
                    // For demo: Send as general notification but with the correct type
                    // Use provided message or default if empty
                    String reminderMessage = message.isEmpty() ?
                            "Book is due soon. Please return the book before the deadline to avoid late fines." : message;
                    notificationService.sendGeneralNotification(userId, "DUE DATE REMINDER: " + reminderMessage);
                    notificationSent = true;
                }
                break;

            case "Overdue Alert":
                // For overdue alerts, we need a BorrowRecord
                List<BorrowRecord> overdueRecords = libraryService.getOverdueBooks();
                BorrowRecord overdueRecord = null;

                for (BorrowRecord record : overdueRecords) {
                    if (record.getUser().getUserId().equals(userId)) {
                        overdueRecord = record;
                        break;
                    }
                }

                if (overdueRecord != null) {
                    notificationService.sendOverdueAlert(overdueRecord);
                    notificationSent = true;
                } else {
                    // For demo: Send as general notification but with the correct type
                    // Use provided message or default if empty
                    String alertMessage = message.isEmpty() ?
                            "You have overdue books. Please return the book immediately and pay any outstanding fines to avoid additional charges." : message;
                    notificationService.sendGeneralNotification(userId, "OVERDUE ALERT: " + alertMessage);
                    notificationSent = true;
                }
                break;

            case "Reservation Available":
                // For reservation available, we need a Book
                List<Book> allBooks = libraryService.getAllBooks();
                Book reservedBook = null;

                // Find a book that might be reserved
                for (Book book : allBooks) {
                    if (!book.getBorrowHistory().isEmpty()) {
                        reservedBook = book;
                        break;
                    }
                }

                if (reservedBook != null) {
                    notificationService.sendReservationAvailable(userId, reservedBook);
                    notificationSent = true;
                } else {
                    // For demo: Send as general notification but with the correct type
                    // Use provided message or default if empty
                    String reservationMessage = message.isEmpty() ?
                            "Your reserved book is now available for pickup. You have 48 hours to collect it from the library." : message;
                    notificationService.sendGeneralNotification(userId, "RESERVATION AVAILABLE: " + reservationMessage);
                    notificationSent = true;
                }
                break;
        }

        if (notificationSent) {
            refreshTable();
            messageArea.setText("");
            clearSearch(); // Clear search after sending notification

            JOptionPane.showMessageDialog(this,
                    type + " sent successfully to " + user.getName() + "!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Failed to send notification. Please try again.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshLists() {
        // Clear existing items
        userComboBox.removeAllItems();

        // Load users from LibraryService
        List<User> users = libraryService.getAllUsers();
        for (User user : users) {
            // Show name and membership type in combo box
            String membershipType = user.getMembershipType().toString().toLowerCase();
            userComboBox.addItem(user.getUserId() + " - " + user.getName() + " - " + membershipType);
        }

        // Refresh table and statistics
        refreshTable();
        clearSearch(); // Clear search when refreshing lists
    }

    public void refreshTable() {
        tableModel.setRowCount(0);

        // Get notifications from NotificationService
        List<NotificationService.Notification> notifications = notificationService.getAllNotifications();

        for (NotificationService.Notification notification : notifications) {
            // Get user for display
            User user = libraryService.getUser(notification.getUserId());

            // Format user display with name and membership type
            String userDisplay = "Unknown User";
            if (user != null) {
                String membershipType = user.getMembershipType().toString().toLowerCase();
                userDisplay = user.getName() + " - " + membershipType;
            }

            // Extract the actual type from the message if it's a prefixed general notification
            String displayType = notification.getType();
            String displayMessage = notification.getMessage();

            // Check if it's a general notification with a specific type prefix
            if ("General Notification".equals(displayType)) {
                if (displayMessage.startsWith("DUE DATE REMINDER: ")) {
                    displayType = "Due Date Reminder";
                    displayMessage = displayMessage.substring("DUE DATE REMINDER: ".length());
                } else if (displayMessage.startsWith("OVERDUE ALERT: ")) {
                    displayType = "Overdue Alert";
                    displayMessage = displayMessage.substring("OVERDUE ALERT: ".length());
                } else if (displayMessage.startsWith("RESERVATION AVAILABLE: ")) {
                    displayType = "Reservation Available";
                    displayMessage = displayMessage.substring("RESERVATION AVAILABLE: ".length());
                }
            }

            // REMOVED status column - only 4 columns now
            tableModel.addRow(new Object[]{
                    dateFormat.format(notification.getTimestamp()),
                    displayType,
                    userDisplay, // Now includes name and membership type
                    displayMessage
            });
        }
        updateStatistics();
    }

    private void updateStatistics() {
        // Get all notifications
        List<NotificationService.Notification> notifications = notificationService.getAllNotifications();
        int total = notifications.size();

        // Count notifications by type
        int dueDateReminderCount = 0;
        int overdueAlertCount = 0;
        int reservationAvailableCount = 0;
        int generalNotificationCount = 0;

        for (NotificationService.Notification notification : notifications) {
            String type = notification.getType();
            String message = notification.getMessage();

            // Check if it's a general notification with a specific type prefix
            if ("General Notification".equals(type)) {
                if (message.startsWith("DUE DATE REMINDER: ")) {
                    dueDateReminderCount++;
                } else if (message.startsWith("OVERDUE ALERT: ")) {
                    overdueAlertCount++;
                } else if (message.startsWith("RESERVATION AVAILABLE: ")) {
                    reservationAvailableCount++;
                } else {
                    generalNotificationCount++;
                }
            } else if ("Due Date Reminder".equals(type)) {
                dueDateReminderCount++;
            } else if ("Overdue Alert".equals(type)) {
                overdueAlertCount++;
            } else if ("Reservation Available".equals(type)) {
                reservationAvailableCount++;
            } else if ("General Notification".equals(type)) {
                generalNotificationCount++;
            }
        }

        totalLabel.setText("Total: " + total);
        dueDateReminderLabel.setText("Due Date Reminder: " + dueDateReminderCount);
        overdueAlertLabel.setText("Overdue Alert: " + overdueAlertCount);
        reservationAvailableLabel.setText("Reservation Available: " + reservationAvailableCount);
        generalNotificationLabel.setText("General Notification: " + generalNotificationCount);
    }

    private void testEmailSystem() {
        // Create a simple test dialog
        JDialog testDialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this),
                "Email System Test", true);
        testDialog.setLayout(new BorderLayout());
        testDialog.setSize(400, 200);
        testDialog.setLocationRelativeTo(this);

        JTextArea testArea = new JTextArea();
        testArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(testArea);

        JButton testButton = new JButton("Run Email Test");
        testButton.addActionListener(e -> {
            testArea.append("Testing email system...\n");
            boolean success = NotificationService.getInstance().testEmailConnection();
            if (success) {
                testArea.append("✅ Email system is working correctly!\n");
                testArea.append("SMTP: smtp.gmail.com:587\n");
                testArea.append("Sender: zevindilneth@gmail.com\n");
            } else {
                testArea.append("❌ Email system test failed.\n");
                testArea.append("Please check your internet connection and SMTP settings.\n");
            }
        });

        testDialog.add(scrollPane, BorderLayout.CENTER);
        testDialog.add(testButton, BorderLayout.SOUTH);
        testDialog.setVisible(true);
    }
}