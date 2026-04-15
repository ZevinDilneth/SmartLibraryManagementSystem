package com.library.gui;

import com.library.services.LibraryService;
import com.library.models.Book;
import com.library.models.User;
import com.library.models.BorrowRecord;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

public class BorrowReturnPanel extends JPanel {
    private LibraryService libraryService;
    private JComboBox<String> userComboBox;
    private JComboBox<String> bookComboBox;
    private JTextArea statusArea;  
    private JTextArea operationLogArea;  
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private JLabel userInfoLabel;
    private JLabel bookInfoLabel;

    // Stacks for undo operations
    private Stack<BorrowRecord> recentBorrows = new Stack<>();
    private Stack<BorrowRecord> recentReturns = new Stack<>();

    public BorrowReturnPanel() {
        libraryService = LibraryService.getInstance(); 
        initializeUI();
        refreshLists();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        // Info panel for selected items
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Selected Information"));

        userInfoLabel = new JLabel("No user selected");
        userInfoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        bookInfoLabel = new JLabel("No book selected");
        bookInfoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        infoPanel.add(userInfoLabel);
        infoPanel.add(bookInfoLabel);

        // Input panel 
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Borrow/Return Operations"));

        // Create searchable combo boxes
        userComboBox = createSearchableComboBox();
        bookComboBox = createSearchableComboBox();

        // Add combo box listeners
        userComboBox.addActionListener(e -> updateUserInfo());
        bookComboBox.addActionListener(e -> updateBookInfo());

        inputPanel.add(new JLabel("Select User:"));
        inputPanel.add(userComboBox);
        inputPanel.add(new JLabel("Select Book:"));
        inputPanel.add(bookComboBox);

        JButton borrowButton = new JButton("Borrow Book");
        JButton returnButton = new JButton("Return Book");
        JButton undoBorrowButton = new JButton("Undo Borrow");
        JButton undoReturnButton = new JButton("Undo Return");

        // Set colored borders only (no background colors)
        borrowButton.setBorder(new LineBorder(new Color(76, 175, 80), 2));
        returnButton.setBorder(new LineBorder(new Color(33, 150, 243), 2));
        undoBorrowButton.setBorder(new LineBorder(new Color(244, 67, 54), 2));
        undoReturnButton.setBorder(new LineBorder(new Color(255, 152, 0), 2));

        borrowButton.addActionListener(e -> borrowBook());
        returnButton.addActionListener(e -> returnBook());
        undoBorrowButton.addActionListener(e -> undoBorrow());
        undoReturnButton.addActionListener(e -> undoReturn());

        // Add buttons to input panel
        inputPanel.add(borrowButton);
        inputPanel.add(returnButton);
        inputPanel.add(undoBorrowButton);
        inputPanel.add(undoReturnButton);

        // Operation panel
        JPanel operationPanel = new JPanel(new BorderLayout(10, 10));
        operationPanel.add(infoPanel, BorderLayout.NORTH);
        operationPanel.add(inputPanel, BorderLayout.CENTER);

        // Results panel - Split into two sections with different weights
        JSplitPane resultsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        resultsSplitPane.setResizeWeight(0.3); // 30% for status, 70% for log
        resultsSplitPane.setDividerSize(5);
        resultsSplitPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        // Current Status Panel (Top - Smaller)
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("Current Status"));

        statusArea = new JTextArea(3, 50);  // Reduced height for status
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane statusScrollPane = new JScrollPane(statusArea);

        statusPanel.add(statusScrollPane, BorderLayout.CENTER);

        // Operation Log Panel (Bottom - Larger)
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Operation Log"));

        operationLogArea = new JTextArea(12, 50);  // Increased height for logs
        operationLogArea.setEditable(false);
        operationLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(operationLogArea);

        logPanel.add(logScrollPane, BorderLayout.CENTER);

        // Add both panels to split pane
        resultsSplitPane.setTopComponent(statusPanel);
        resultsSplitPane.setBottomComponent(logPanel);

        // Bottom panel for refresh button - CENTERED
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton refreshButton = new JButton("Refresh Lists");
        refreshButton.setBorder(new LineBorder(new Color(255, 193, 7), 2));
        refreshButton.addActionListener(e -> refreshLists());

        bottomPanel.add(refreshButton);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Add components
        mainPanel.add(operationPanel, BorderLayout.NORTH);
        mainPanel.add(resultsSplitPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
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

    private void updateUserInfo() {
        String userSelection = (String) userComboBox.getSelectedItem();
        if (userSelection != null && !userSelection.isEmpty()) {
            String userId = userSelection.split(" - ")[0].trim();
            User user = libraryService.getUser(userId);
            if (user != null) {
                userInfoLabel.setText(String.format(
                        "👤 User: %s | ID: %s | Type: %s | Borrowed: %d/%d books",
                        user.getName(),
                        user.getUserId(),
                        user.getMembershipType().getDisplayName(),
                        user.getBorrowedBooks().size(),
                        user.getMembershipType().getBorrowingLimit()
                ));
                return;
            }
        }
        userInfoLabel.setText("No user selected");
    }

    private void updateBookInfo() {
        String bookSelection = (String) bookComboBox.getSelectedItem();
        if (bookSelection != null && !bookSelection.isEmpty()) {
            String bookId = bookSelection.split(" - ")[0].trim();
            Book book = libraryService.getBook(bookId);
            if (book != null) {
                // Find who is currently borrowing this book (if any)
                String borrowerInfo = "";
                for (BorrowRecord record : libraryService.getAllBorrowRecords()) {
                    if (record.getBook().getBookId().equals(bookId) && record.getReturnDate() == null) {
                        borrowerInfo = String.format(" | Borrowed by: %s (ID: %s)",
                                record.getUser().getName(),
                                record.getUser().getUserId());
                        break;
                    }
                }

                bookInfoLabel.setText(String.format(
                        "📚 Book: %s | ID: %s | Author: %s | Status: %s | Borrowed: %d times%s",
                        book.getTitle(),
                        book.getBookId(),
                        book.getAuthor(),
                        book.getStatus().getDisplayName(),
                        book.getBorrowHistory().size(),
                        borrowerInfo
                ));
                return;
            }
        }
        bookInfoLabel.setText("No book selected");
    }

    private void borrowBook() {
        String userSelection = (String) userComboBox.getSelectedItem();
        String bookSelection = (String) bookComboBox.getSelectedItem();

        if (userSelection == null || userSelection.isEmpty() || bookSelection == null || bookSelection.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select both user and book",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = userSelection.split(" - ")[0].trim();
        String bookId = bookSelection.split(" - ")[0].trim();

        BorrowRecord record = libraryService.borrowBook(userId, bookId);
        if (record != null) {
            // Store for possible undo
            recentBorrows.push(record);

            String message = String.format(
                    "✅ BOOK BORROWED SUCCESSFULLY!\n" +
                            "══════════════════════════════════════════════════\n" +
                            "📚 Book Details:\n" +
                            "   • Title: %s\n" +
                            "   • Book ID: %s\n" +
                            "   • Author: %s\n" +
                            "   • ISBN: %s\n" +
                            "\n" +
                            "👤 User Details:\n" +
                            "   • Name: %s\n" +
                            "   • User ID: %s\n" +
                            "   • Membership: %s\n" +
                            "\n" +
                            "📅 Transaction Details:\n" +
                            "   • Borrow Date: %s\n" +
                            "   • Due Date: %s\n" +
                            "   • Record ID: %s\n" +
                            "\n" +
                            "⚠️  IMPORTANT REMINDERS:\n" +
                            "   • Please return by the due date to avoid fines\n" +
                            "   • Late returns incur daily fines\n" +
                            "   • Current borrowed books: %d/%d\n" +
                            "   • You can undo this action using 'Undo Borrow' button\n" +
                            "══════════════════════════════════════════════════",
                    record.getBook().getTitle(),
                    record.getBook().getBookId(),
                    record.getBook().getAuthor(),
                    record.getBook().getIsbn(),
                    record.getUser().getName(),
                    record.getUser().getUserId(),
                    record.getUser().getMembershipType().getDisplayName(),
                    dateFormat.format(record.getBorrowDate()),
                    dateFormat.format(record.getDueDate()),
                    record.getRecordId(),
                    record.getUser().getBorrowedBooks().size(),
                    record.getUser().getMembershipType().getBorrowingLimit()
            );
            operationLogArea.append(message + "\n\n");
            refreshLists();

            // Show success popup
            JOptionPane.showMessageDialog(this,
                    String.format(
                            "✅ Book borrowed successfully!\n\n" +
                                    "📚 Book: %s\n" +
                                    "👤 User: %s\n" +
                                    "📅 Due Date: %s\n" +
                                    "📝 Record ID: %s\n\n" +
                                    "The transaction has been recorded in the system.\n" +
                                    "You can undo this action if needed.",
                            record.getBook().getTitle(),
                            record.getUser().getName(),
                            dateFormat.format(record.getDueDate()),
                            record.getRecordId()
                    ),
                    "Borrow Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            String errorMessage = "❌ FAILED TO BORROW BOOK\n" +
                    "══════════════════════════════════════════════════\n" +
                    "Possible reasons:\n" +
                    "1. ❗ User has reached borrowing limit\n" +
                    "2. ❗ Book is not available (Status: Borrowed/Reserved)\n" +
                    "3. ❗ Invalid user or book selection\n" +
                    "4. ❗ System error\n" +
                    "══════════════════════════════════════════════════";
            operationLogArea.append(errorMessage + "\n\n");
        }
    }

    private void returnBook() {
        String userSelection = (String) userComboBox.getSelectedItem();
        String bookSelection = (String) bookComboBox.getSelectedItem();

        if (bookSelection == null || bookSelection.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a book to return",
                    "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String bookId = bookSelection.split(" - ")[0].trim();

        // Check if a user is selected
        if (userSelection == null || userSelection.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select the user who is returning the book",
                    "User Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = userSelection.split(" - ")[0].trim();

        // Check if the selected user is the one who borrowed the book
        boolean isBorrowedByUser = false;
        String actualBorrowerId = null;
        String actualBorrowerName = null;

        // Find the active borrow record for this book
        for (BorrowRecord record : libraryService.getAllBorrowRecords()) {
            if (record.getBook().getBookId().equals(bookId) && record.getReturnDate() == null) {
                actualBorrowerId = record.getUser().getUserId();
                actualBorrowerName = record.getUser().getName();

                if (record.getUser().getUserId().equals(userId)) {
                    isBorrowedByUser = true;
                }
                break; // Found the active record
            }
        }

        if (!isBorrowedByUser) {
            if (actualBorrowerId != null) {
                // Book is borrowed by someone else
                JOptionPane.showMessageDialog(this,
                        String.format(
                                "❌ Cannot return this book!\n\n" +
                                        "This book is currently borrowed by:\n" +
                                        "👤 %s (ID: %s)\n\n" +
                                        "Only the user who borrowed the book can return it.",
                                actualBorrowerName,
                                actualBorrowerId
                        ),
                        "Return Permission Denied",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                // No active borrow record found
                JOptionPane.showMessageDialog(this,
                        "❌ This book is not currently borrowed.\n" +
                                "Please check the book status.",
                        "Book Not Borrowed",
                        JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        // Proceed with return since the user is the borrower
        double fine = libraryService.returnBook(bookId);
        String message;

        // Find the borrow record that was just returned
        BorrowRecord returnedRecord = null;
        for (BorrowRecord record : libraryService.getAllBorrowRecords()) {
            if (record.getBook().getBookId().equals(bookId) && record.getReturnDate() != null) {
                // Check if this is the most recent return (within last 5 seconds)
                long now = System.currentTimeMillis();
                long returnTime = record.getReturnDate().getTime();
                if (now - returnTime < 5000) { // Within 5 seconds
                    returnedRecord = record;
                    break;
                }
            }
        }

        if (returnedRecord != null) {
            recentReturns.push(returnedRecord);
        }

        if (fine > 0) {
            message = String.format(
                    "✅ BOOK RETURNED SUCCESSFULLY!\n" +
                            "══════════════════════════════════════════════════\n" +
                            "💰 FINE APPLIED: LKR %.2f\n" +
                            "\n" +
                            "📋 IMPORTANT INFORMATION:\n" +
                            "   • The book has been returned to the library\n" +
                            "   • An overdue fine has been calculated\n" +
                            "   • Please pay the fine at the library counter\n" +
                            "   • Receipt will be issued upon payment\n" +
                            "   • You can undo this action using 'Undo Return' button\n" +
                            "══════════════════════════════════════════════════",
                    fine
            );

            // Show fine popup
            JOptionPane.showMessageDialog(this,
                    String.format(
                            "✅ Book returned successfully!\n\n" +
                                    "💰 Overdue Fine: LKR %.2f\n\n" +
                                    "Please proceed to the library counter to pay the fine.\n" +
                                    "A receipt will be issued upon payment.\n" +
                                    "You can undo this action if needed.",
                            fine
                    ),
                    "Return with Fine",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            message = "✅ BOOK RETURNED SUCCESSFULLY!\n" +
                    "══════════════════════════════════════════════════\n" +
                    "🎉 NO FINES INCURRED\n" +
                    "\n" +
                    "📋 TRANSACTION COMPLETED:\n" +
                    "   • Book returned on time\n" +
                    "   • No overdue charges\n" +
                    "   • Book is now available for others\n" +
                    "   • Thank you for returning on time!\n" +
                    "   • You can undo this action using 'Undo Return' button\n" +
                    "══════════════════════════════════════════════════";

            // Show success popup
            JOptionPane.showMessageDialog(this,
                    "✅ Book returned successfully!\n\n" +
                            "🎉 No fines incurred.\n" +
                            "📚 The book is now available for other users.\n" +
                            "👍 Thank you for returning on time!\n" +
                            "You can undo this action if needed.",
                    "Return Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        operationLogArea.append(message + "\n\n");
        refreshLists();
    }

    private void undoBorrow() {
        if (recentBorrows.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No recent borrow operations to undo",
                    "Undo Borrow",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        BorrowRecord record = recentBorrows.pop();
        String bookId = record.getBook().getBookId();

        // Return the book immediately (undo the borrow)
        double fine = libraryService.returnBook(bookId);

        String message = String.format(
                "↩️  BORROW UNDO SUCCESSFUL!\n" +
                        "══════════════════════════════════════════════════\n" +
                        "📚 Book: %s\n" +
                        "👤 User: %s\n" +
                        "📅 Original Borrow Date: %s\n" +
                        "📝 Record ID: %s\n" +
                        "\n" +
                        "📋 ACTION DETAILS:\n" +
                        "   • Borrow transaction has been reversed\n" +
                        "   • Book status changed back to AVAILABLE\n" +
                        "   • User's borrowed count updated\n" +
                        "   • Fine waived for immediate return\n" +
                        "══════════════════════════════════════════════════",
                record.getBook().getTitle(),
                record.getUser().getName(),
                dateFormat.format(record.getBorrowDate()),
                record.getRecordId()
        );

        operationLogArea.append(message + "\n\n");
        refreshLists();

        JOptionPane.showMessageDialog(this,
                String.format(
                        "↩️  Borrow operation undone successfully!\n\n" +
                                "📚 Book: %s\n" +
                                "👤 User: %s\n" +
                                "📅 Original Borrow Date: %s\n" +
                                "📝 Record ID: %s\n\n" +
                                "The book has been returned and is now available.",
                        record.getBook().getTitle(),
                        record.getUser().getName(),
                        dateFormat.format(record.getBorrowDate()),
                        record.getRecordId()
                ),
                "Undo Borrow Successful",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void undoReturn() {
        if (recentReturns.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No recent return operations to undo",
                    "Undo Return",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        BorrowRecord record = recentReturns.pop();
        String userId = record.getUser().getUserId();
        String bookId = record.getBook().getBookId();

        // Clear the return date to effectively "re-borrow" the book
        record.setReturnDate(null);

        // Update book status back to BORROWED
        Book book = record.getBook();
        book.setStatus(com.library.enums.BookStatus.BORROWED);

        // Add the book back to user's borrowed list
        User user = record.getUser();
        if (!user.getBorrowedBooks().contains(book)) {
            user.getBorrowedBooks().add(book);
        }

        String message = String.format(
                "↩️  RETURN UNDO SUCCESSFUL!\n" +
                        "══════════════════════════════════════════════════\n" +
                        "📚 Book: %s\n" +
                        "👤 User: %s\n" +
                        "📅 Original Borrow Date: %s\n" +
                        "📅 Due Date: %s\n" +
                        "📝 Record ID: %s\n" +
                        "\n" +
                        "📋 ACTION DETAILS:\n" +
                        "   • Return transaction has been reversed\n" +
                        "   • Book status changed back to BORROWED\n" +
                        "   • User is now borrowing this book again\n" +
                        "   • Original due date remains unchanged\n" +
                        "══════════════════════════════════════════════════",
                record.getBook().getTitle(),
                record.getUser().getName(),
                dateFormat.format(record.getBorrowDate()),
                dateFormat.format(record.getDueDate()),
                record.getRecordId()
        );

        operationLogArea.append(message + "\n\n");
        refreshLists();

        JOptionPane.showMessageDialog(this,
                String.format(
                        "↩️  Return operation undone successfully!\n\n" +
                                "📚 Book: %s\n" +
                                "👤 User: %s\n" +
                                "📅 Borrow Date: %s\n" +
                                "📅 Due Date: %s\n" +
                                "📝 Record ID: %s\n\n" +
                                "The user is now borrowing this book again.",
                        record.getBook().getTitle(),
                        record.getUser().getName(),
                        dateFormat.format(record.getBorrowDate()),
                        dateFormat.format(record.getDueDate()),
                        record.getRecordId()
                ),
                "Undo Return Successful",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void refreshLists() {
        userComboBox.removeAllItems();
        bookComboBox.removeAllItems();

        // Always load users and books, even if empty
        for (User user : libraryService.getAllUsers()) {
            userComboBox.addItem(user.getUserId() + " - " + user.getName() +
                    " (" + user.getMembershipType().getDisplayName() + ")");
        }

        for (Book book : libraryService.getAllBooks()) {
            String status = book.getStatus().getDisplayName();
            String availableText = book.getStatus().toString().equals("AVAILABLE") ? "✅ Available" : "❌ " + status;
            bookComboBox.addItem(book.getBookId() + " - " + book.getTitle() + " - " + availableText);
        }

        // Update info labels
        updateUserInfo();
        updateBookInfo();

        // Update status area (shows only current status, no instructions)
        updateStatusArea();

        // Force UI update
        revalidate();
        repaint();
    }

    private void updateStatusArea() {
        int totalBooks = libraryService.getAllBooks().size();
        int totalUsers = libraryService.getAllUsers().size();
        long borrowedBooks = libraryService.getAllBorrowRecords().stream()
                .filter(r -> r.getReturnDate() == null).count();

        if (totalBooks == 0 && totalUsers == 0) {
            statusArea.setText("📚 LIBRARY STATUS\n" +
                    "══════════════════════════════════════════════════\n" +
                    "• Total Books: 0\n" +
                    "• Total Users: 0\n" +
                    "• Currently Borrowed: 0\n" +
                    "• Recent Borrows (undoable): " + recentBorrows.size() + "\n" +
                    "• Recent Returns (undoable): " + recentReturns.size() + "\n" +
                    "══════════════════════════════════════════════════");
        } else if (totalUsers == 0) {
            statusArea.setText("📚 LIBRARY STATUS\n" +
                    "══════════════════════════════════════════════════\n" +
                    "• Total Books: " + totalBooks + "\n" +
                    "• Total Users: 0\n" +
                    "• Currently Borrowed: 0\n" +
                    "• Recent Borrows (undoable): " + recentBorrows.size() + "\n" +
                    "• Recent Returns (undoable): " + recentReturns.size() + "\n" +
                    "══════════════════════════════════════════════════");
        } else if (totalBooks == 0) {
            statusArea.setText("📚 LIBRARY STATUS\n" +
                    "══════════════════════════════════════════════════\n" +
                    "• Total Books: 0\n" +
                    "• Total Users: " + totalUsers + "\n" +
                    "• Currently Borrowed: 0\n" +
                    "• Recent Borrows (undoable): " + recentBorrows.size() + "\n" +
                    "• Recent Returns (undoable): " + recentReturns.size() + "\n" +
                    "══════════════════════════════════════════════════");
        } else {
            statusArea.setText("📚 LIBRARY STATUS\n" +
                    "══════════════════════════════════════════════════\n" +
                    "• Total Books: " + totalBooks + "\n" +
                    "• Total Users: " + totalUsers + "\n" +
                    "• Currently Borrowed: " + borrowedBooks + "\n" +
                    "• Available Books: " + (totalBooks - borrowedBooks) + "\n" +
                    "• Recent Borrows (undoable): " + recentBorrows.size() + "\n" +
                    "• Recent Returns (undoable): " + recentReturns.size() + "\n" +
                    "══════════════════════════════════════════════════");
        }
    }
}