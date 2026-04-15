package com.library.gui;

import com.library.services.LibraryService;
import com.library.models.User;
import com.library.enums.MembershipType;
import com.library.models.BorrowRecord;
import com.library.models.Reservation;
import com.library.models.Book;
import com.library.models.Review;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.RowFilter;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import java.net.URL;
import java.awt.image.BufferedImage;
import java.awt.Toolkit;

public class UserManagementPanel extends JPanel {
    private LibraryService libraryService;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextField nameField, emailField, phoneField;
    private JTextField userIdField;
    private JLabel idLabel;
    private JComboBox<MembershipType> membershipComboBox;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private String currentSearchText = "";
    private JPanel userDetailPanel;
    private boolean isDetailVisible = false;

    public UserManagementPanel() {
        libraryService = LibraryService.getInstance(); 
        initializeUI();
        refreshTable();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        // Create search panel at the top
        JPanel searchPanel = createSearchPanel();
        add(searchPanel, BorderLayout.NORTH);

        // Create center panel with table and user detail panel
        JPanel centerPanel = new JPanel(new BorderLayout(0, 5));

        // Table for displaying users
        JScrollPane tableScrollPane = createUserTable();
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        // User detail panel (initially hidden)
        userDetailPanel = createUserDetailPanel();
        userDetailPanel.setVisible(false);
        centerPanel.add(userDetailPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Create user management form at the bottom
        JPanel userManagementForm = createUserManagementForm();
        add(userManagementForm, BorderLayout.SOUTH);

        // Add input validation listeners
        addInputValidationListeners();
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Users"));

        JPanel searchInputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints searchGbc = new GridBagConstraints();
        searchGbc.insets = new Insets(5, 5, 5, 5);
        searchGbc.fill = GridBagConstraints.HORIZONTAL;

        // Search 
        searchGbc.gridx = 0;
        searchGbc.gridy = 0;
        searchGbc.weightx = 0.0;
        searchInputPanel.add(new JLabel("Search:"), searchGbc);

        searchGbc.gridx = 1;
        searchGbc.gridy = 0;
        searchGbc.weightx = 1.0;
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(400, 25)); // Wider search field
        searchField.putClientProperty("JTextField.placeholderText", "Search users by ID, name, email, phone, or membership type...");
        searchInputPanel.add(searchField, searchGbc);

        // Search button
        searchGbc.gridx = 2;
        searchGbc.gridy = 0;
        searchGbc.weightx = 0.0;
        JButton searchButton = new JButton("🔍 Search");
        searchButton.setToolTipText("Search users");
        searchInputPanel.add(searchButton, searchGbc);

        // Clear button
        searchGbc.gridx = 3;
        searchGbc.gridy = 0;
        searchGbc.weightx = 0.0;
        JButton clearSearchButton = new JButton("Clear");
        clearSearchButton.setToolTipText("Clear search");
        searchInputPanel.add(clearSearchButton, searchGbc);

        searchPanel.add(searchInputPanel, BorderLayout.NORTH);

        // Add key listener to search field for real-time filtering
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                String searchText = searchField.getText().trim();
                if (!searchText.isEmpty()) {
                    currentSearchText = searchText.toLowerCase();
                    highlightUsersInTable(searchText);
                } else {
                    clearUserHighlights();
                }
            }
        });

        // Search button action
        searchButton.addActionListener(e -> {
            String searchText = searchField.getText().trim();
            if (!searchText.isEmpty()) {
                currentSearchText = searchText.toLowerCase();
                highlightUsersInTable(searchText);
            }
        });

        // Clear button action
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            currentSearchText = "";
            clearUserHighlights();
            searchField.requestFocus();
        });

        return searchPanel;
    }

    private JScrollPane createUserTable() {
        // Table for displaying users with custom renderer for highlighting
        String[] columns = {"User ID", "Name", "Email", "Phone", "Membership Type", "Books Borrowed", "Total Fine (LKR)", "Reservations"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Specify column classes for proper sorting
                switch (columnIndex) {
                    case 5: // Books Borrowed
                        return Integer.class;
                    case 6: // Total Fine
                        return Double.class;
                    case 7: // Has Reservations
                        return String.class;
                    default:
                        return String.class;
                }
            }
        };

        userTable = new JTable(tableModel);
        userTable.setDefaultRenderer(Object.class, new HighlightCellRenderer());

        // Set custom renderer for the Books Borrowed column (centered)
        userTable.getColumnModel().getColumn(5).setCellRenderer(new CenteredNumberRenderer());
        // Set custom renderer for the Total Fine column - using the same renderer as other columns
        userTable.getColumnModel().getColumn(6).setCellRenderer(new StandardCellRenderer());
        // Set custom renderer for the Has Reservations column
        userTable.getColumnModel().getColumn(7).setCellRenderer(new ReservationCellRenderer());

        tableSorter = new TableRowSorter<>(tableModel);
        userTable.setRowSorter(tableSorter);

        JScrollPane scrollPane = new JScrollPane(userTable);

        // Add selection listener to table - shows user detail panel
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && userTable.getSelectedRow() != -1) {
                int modelRow = userTable.convertRowIndexToModel(userTable.getSelectedRow());
                userIdField.setText(tableModel.getValueAt(modelRow, 0).toString());
                nameField.setText(tableModel.getValueAt(modelRow, 1).toString());
                emailField.setText(tableModel.getValueAt(modelRow, 2).toString());
                phoneField.setText(tableModel.getValueAt(modelRow, 3).toString());
                membershipComboBox.setSelectedItem(MembershipType.valueOf(
                        tableModel.getValueAt(modelRow, 4).toString().toUpperCase()));
                idLabel.setText("(Select 'Clear Form' to add new user)");
                idLabel.setForeground(Color.RED);

                // Show user detail panel
                showUserDetail(tableModel.getValueAt(modelRow, 0).toString());
            }
        });

        return scrollPane;
    }

    private JPanel createUserDetailPanel() {
        JPanel userDetailPanel = new JPanel(new BorderLayout());
        userDetailPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        userDetailPanel.setBackground(new Color(245, 245, 245));

        // Header with title and close button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(245, 245, 245));

        // Title - LEFT ALIGNED
        JLabel titleLabel = new JLabel("User Details");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        // Close button at right
        JButton closeButton = new JButton("✕ Close");
        closeButton.addActionListener(e -> hideUserDetail());
        closeButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        closeButton.setBackground(new Color(220, 220, 220));
        closeButton.setFocusPainted(false);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(closeButton, BorderLayout.EAST);

        userDetailPanel.add(headerPanel, BorderLayout.NORTH);

        // Content area will be filled dynamically
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        userDetailPanel.add(scrollPane, BorderLayout.CENTER);

        return userDetailPanel;
    }

    private JPanel createUserManagementForm() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("User Management"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // User ID field (for update) - Row 0
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("User ID (for update):"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        userIdField = new JTextField();
        userIdField.setEditable(false);
        userIdField.setBackground(new Color(240, 240, 240));
        formPanel.add(userIdField, gbc);

        // ID Label (for new users) - Row 1
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("New User ID:"), gbc);

        gbc.gridx = 1;
        idLabel = new JLabel("(Auto-generated)");
        idLabel.setForeground(Color.BLUE);
        formPanel.add(idLabel, gbc);

        // Name field - Row 2
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Name *:"), gbc);

        gbc.gridx = 1;
        nameField = new JTextField();
        nameField.putClientProperty("JTextField.placeholderText", "Enter full name (letters only)");
        formPanel.add(nameField, gbc);

        // Email field - Row 3
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Email *:"), gbc);

        gbc.gridx = 1;
        emailField = new JTextField();
        emailField.putClientProperty("JTextField.placeholderText", "Enter email (user@example.com)");
        formPanel.add(emailField, gbc);

        // Phone field - Row 4
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Phone *:"), gbc);

        gbc.gridx = 1;
        phoneField = new JTextField();
        phoneField.putClientProperty("JTextField.placeholderText", "Enter phone number (digits only)");
        formPanel.add(phoneField, gbc);

        // Membership Type field - Row 5
        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(new JLabel("Membership Type *:"), gbc);

        gbc.gridx = 1;
        membershipComboBox = new JComboBox<>(MembershipType.values());
        formPanel.add(membershipComboBox, gbc);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton addButton = new JButton("Add New User");
        JButton updateButton = new JButton("Update Selected User");
        JButton deleteButton = new JButton("Delete Selected User");
        JButton clearButton = new JButton("Clear Form");
        JButton refreshButton = new JButton("Refresh Table");

        // Set colored borders only (no background colors)
        addButton.setBorder(new LineBorder(new Color(76, 175, 80), 2));
        updateButton.setBorder(new LineBorder(new Color(33, 150, 243), 2));
        deleteButton.setBorder(new LineBorder(new Color(244, 67, 54), 2));
        clearButton.setBorder(new LineBorder(new Color(158, 158, 158), 2));
        refreshButton.setBorder(new LineBorder(new Color(255, 193, 7), 2));

        addButton.addActionListener(e -> addUser());
        updateButton.addActionListener(e -> updateUser());
        deleteButton.addActionListener(e -> deleteUser());
        clearButton.addActionListener(e -> {
            clearForm();
            idLabel.setText("(Auto-generated)");
            idLabel.setForeground(Color.BLUE);
            hideUserDetail(); // Also hide user detail panel when clearing form
        });
        refreshButton.addActionListener(e -> {
            refreshTable();
            hideUserDetail(); // Hide the user detail panel when refreshing
        });

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(refreshButton);

        // Add form and button panel to a container
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(formPanel, BorderLayout.CENTER);
        containerPanel.add(buttonPanel, BorderLayout.SOUTH);

        return containerPanel;
    }

    private void showUserDetail(String userId) {
        // Get user details
        User user = libraryService.getUser(userId);
        if (user == null) {
            hideUserDetail();
            return;
        }

        // Get the content panel from the scroll pane
        JScrollPane scrollPane = (JScrollPane) userDetailPanel.getComponent(1);
        JPanel contentPanel = (JPanel) scrollPane.getViewport().getView();
        contentPanel.removeAll();

        // Create 4-column layout for user info, fines, reservations, and borrow history
        JPanel columnsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        columnsPanel.setBackground(Color.WHITE);

        // Column 1: User Information
        JPanel userInfoPanel = createUserInfoPanel(user);
        columnsPanel.add(userInfoPanel);

        // Column 2: Fines Information
        JPanel finesPanel = createFinesPanel(user);
        columnsPanel.add(finesPanel);

        // Column 3: Reservations
        JPanel reservationsPanel = createReservationsPanel(user);
        columnsPanel.add(reservationsPanel);

        // Column 4: Borrow History
        JPanel borrowHistoryPanel = createBorrowHistoryPanel(user);
        columnsPanel.add(borrowHistoryPanel);

        contentPanel.add(columnsPanel);

        // Bottom Panel: User Reviews (with updated layout)
        JPanel reviewsPanel = createUserReviewsPanel(user);
        contentPanel.add(reviewsPanel);

        // Show the panel
        userDetailPanel.setVisible(true);
        isDetailVisible = true;

        // Set the preferred height to be 25% taller (500px instead of 400px)
        userDetailPanel.setPreferredSize(new Dimension(0, 500));

        // Refresh the layout
        contentPanel.revalidate();
        contentPanel.repaint();

        // Scroll the detail panel to top
        scrollPane.getVerticalScrollBar().setValue(0);
    }

    private void hideUserDetail() {
        userDetailPanel.setVisible(false);
        isDetailVisible = false;
        userTable.clearSelection();
        clearForm();
    }

    private JPanel createUserInfoPanel(User user) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("User Information"));
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // User ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("User ID:"), gbc);

        gbc.gridx = 1;
        JLabel userIdLabel = new JLabel(user.getUserId());
        userIdLabel.setFont(userIdLabel.getFont().deriveFont(Font.BOLD));
        panel.add(userIdLabel, gbc);

        // Name
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        JLabel nameLabel = new JLabel(user.getName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(nameLabel, gbc);

        // Email
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Email:"), gbc);

        gbc.gridx = 1;
        panel.add(new JLabel(user.getEmail()), gbc);

        // Phone
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Phone:"), gbc);

        gbc.gridx = 1;
        panel.add(new JLabel(user.getContactNumber()), gbc);

        // Membership Type
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Membership Type:"), gbc);

        gbc.gridx = 1;
        JLabel membershipLabel = new JLabel(user.getMembershipType().getDisplayName());
        membershipLabel.setFont(membershipLabel.getFont().deriveFont(Font.BOLD));
        membershipLabel.setForeground(getMembershipColor(user.getMembershipType()));
        panel.add(membershipLabel, gbc);

        // Membership Benefits
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        gbc.gridy = 6;
        panel.add(new JLabel("Membership Benefits:"), gbc);

        gbc.gridy = 7;
        JTextArea benefitsArea = new JTextArea(
                String.format("• Borrowing Limit: %d books\n• Loan Period: %d days\n• Daily Fine Rate: LKR %.2f",
                        user.getMembershipType().getBorrowingLimit(),
                        user.getMembershipType().getLoanPeriod(),
                        user.getMembershipType().getDailyFine()
                )
        );
        benefitsArea.setEditable(false);
        benefitsArea.setBackground(panel.getBackground());
        benefitsArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(benefitsArea, gbc);

        return panel;
    }

    private JPanel createFinesPanel(User user) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Fines Information"));
        panel.setBackground(Color.WHITE);

        double totalFine = calculateUserTotalFine(user);

        // Total Fine display - LEFT ALIGNED, RED if > 0, GREEN if = 0
        JPanel totalFinePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        totalFinePanel.setBackground(Color.WHITE);
        JLabel totalFineLabel = new JLabel(String.format("Total Fine: LKR %.2f", totalFine));
        totalFineLabel.setFont(totalFineLabel.getFont().deriveFont(Font.BOLD, 14f));

        // Red for any fine > 0, Green for 0
        if (totalFine > 0) {
            totalFineLabel.setForeground(Color.RED);
        } else {
            totalFineLabel.setForeground(new Color(0, 150, 0)); // Green
        }

        totalFinePanel.add(totalFineLabel);
        panel.add(totalFinePanel, BorderLayout.NORTH);

        // Fine calculation details
        JTextArea fineDetails = new JTextArea();
        fineDetails.setEditable(false);
        fineDetails.setLineWrap(true);
        fineDetails.setWrapStyleWord(true);
        fineDetails.setBackground(Color.WHITE);
        fineDetails.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Get user's borrow records
        List<BorrowRecord> userBorrowRecords = libraryService.getAllBorrowRecords()
                .stream()
                .filter(record -> record.getUser() != null &&
                        record.getUser().getUserId().equals(user.getUserId()))
                .collect(Collectors.toList());

        List<BorrowRecord> overdueRecords = userBorrowRecords.stream()
                .filter(record -> record.isOverdue() && !record.isFinePaid())
                .collect(Collectors.toList());

        StringBuilder details = new StringBuilder();
        if (overdueRecords.isEmpty()) {
            details.append("No overdue fines.\n");
        } else {
            details.append("Fine Calculation Breakdown:\n\n");
            details.append(String.format("Membership Type: %s\n", user.getMembershipType().getDisplayName()));
            details.append(String.format("Daily Fine Rate: LKR %.2f per day\n\n", user.getMembershipType().getDailyFine()));
            details.append("Overdue Books:\n");

            for (BorrowRecord record : overdueRecords) {
                Book book = record.getBook();
                int overdueDays = record.getOverdueDays();
                double fineForThisBook = overdueDays * user.getMembershipType().getDailyFine();

                details.append(String.format("\n📚 %s\n", book.getTitle()));
                details.append(String.format("   Book ID: %s\n", book.getBookId()));
                details.append(String.format("   Due Date: %s\n",
                        new java.text.SimpleDateFormat("yyyy-MM-dd").format(record.getDueDate())));
                details.append(String.format("   Overdue Days: %d\n", overdueDays));
                details.append(String.format("   Fine: LKR %.2f × %d days = LKR %.2f\n",
                        user.getMembershipType().getDailyFine(), overdueDays, fineForThisBook));
            }
        }

        fineDetails.setText(details.toString());

        JScrollPane scrollPane = new JScrollPane(fineDetails);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.setPreferredSize(new Dimension(0, 250)); // Increased height

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createReservationsPanel(User user) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Reservations"));
        panel.setBackground(Color.WHITE);

        // Get user's reservations
        List<Reservation> userReservations = libraryService.getAllReservations()
                .stream()
                .filter(reservation -> reservation.getUser() != null &&
                        reservation.getUser().getUserId().equals(user.getUserId()))
                .collect(Collectors.toList());

        int totalReservations = userReservations.size();
        JLabel countLabel = new JLabel(String.format("Total Reservations: %d", totalReservations));
        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD));
        countLabel.setBackground(Color.WHITE);
        panel.add(countLabel, BorderLayout.NORTH);

        if (userReservations.isEmpty()) {
            JLabel noReservationsLabel = new JLabel("No books reserved", SwingConstants.CENTER);
            noReservationsLabel.setForeground(Color.GRAY);
            noReservationsLabel.setBackground(Color.WHITE);
            panel.add(noReservationsLabel, BorderLayout.CENTER);
        } else {
            // Create table for reservations
            String[] columns = {"Cover", "Book Details", "Status", "Reservation Date"};
            DefaultTableModel reservationsModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return columnIndex == 0 ? ImageIcon.class : String.class;
                }
            };

            // Sort by reservation date (newest first)
            userReservations.sort((r1, r2) -> r2.getReservationDate().compareTo(r1.getReservationDate()));

            // Add reservations to table
            for (Reservation reservation : userReservations) {
                Book book = reservation.getBook();
                ImageIcon coverImage = loadBookCover(book);

                String status = reservation.isNotified() ? "Notified" : "Waiting";
                String reservationDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(reservation.getReservationDate());

                reservationsModel.addRow(new Object[]{
                        coverImage,
                        book.getBookId(), // Will be formatted by renderer
                        status,
                        reservationDate
                });
            }

            JTable reservationsTable = new JTable(reservationsModel);
            reservationsTable.setRowHeight(80); // Taller rows for book details
            reservationsTable.getTableHeader().setReorderingAllowed(false);

            // Set custom renderer for cover images
            reservationsTable.getColumnModel().getColumn(0).setCellRenderer(new BookCoverRenderer());
            reservationsTable.getColumnModel().getColumn(0).setPreferredWidth(40);

            // Set custom renderer for book details column
            reservationsTable.getColumnModel().getColumn(1).setCellRenderer(new BookDetailsRenderer());
            reservationsTable.getColumnModel().getColumn(1).setPreferredWidth(150);

            // Set custom renderer for status column
            reservationsTable.getColumnModel().getColumn(2).setCellRenderer(new StatusRenderer());
            reservationsTable.getColumnModel().getColumn(2).setPreferredWidth(80);

            reservationsTable.getColumnModel().getColumn(3).setPreferredWidth(90);

            JScrollPane scrollPane = new JScrollPane(reservationsTable);
            scrollPane.setPreferredSize(new Dimension(0, 250)); // Increased height
            panel.add(scrollPane, BorderLayout.CENTER);
        }

        return panel;
    }

    private JPanel createBorrowHistoryPanel(User user) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Borrow History"));
        panel.setBackground(Color.WHITE);

        // Get ALL borrow records for this user (history)
        List<BorrowRecord> userBorrowRecords = libraryService.getAllBorrowRecords()
                .stream()
                .filter(record -> record.getUser() != null &&
                        record.getUser().getUserId().equals(user.getUserId()))
                .collect(Collectors.toList());

        int totalBorrowed = userBorrowRecords.size();
        JLabel countLabel = new JLabel(String.format("Total Books Borrowed: %d", totalBorrowed));
        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD));
        countLabel.setBackground(Color.WHITE);
        panel.add(countLabel, BorderLayout.NORTH);

        if (userBorrowRecords.isEmpty()) {
            JLabel noBooksLabel = new JLabel("No books borrowed yet", SwingConstants.CENTER);
            noBooksLabel.setForeground(Color.GRAY);
            noBooksLabel.setBackground(Color.WHITE);
            panel.add(noBooksLabel, BorderLayout.CENTER);
        } else {
            // Create table for borrow history
            String[] columns = {"Cover", "Book Details", "Status", "Borrow Date", "Return Date"};
            DefaultTableModel historyModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return columnIndex == 0 ? ImageIcon.class : String.class;
                }
            };

            // Sort by borrow date (newest first)
            userBorrowRecords.sort((r1, r2) -> r2.getBorrowDate().compareTo(r1.getBorrowDate()));

            // Add borrow history to table
            for (BorrowRecord record : userBorrowRecords) {
                Book book = record.getBook();
                ImageIcon coverImage = loadBookCover(book);

                String status = record.getReturnDate() == null ? "Borrowed" : "Returned";
                String borrowDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(record.getBorrowDate());
                String returnDate = record.getReturnDate() != null ?
                        new java.text.SimpleDateFormat("yyyy-MM-dd").format(record.getReturnDate()) : "Not returned";

                historyModel.addRow(new Object[]{
                        coverImage,
                        book.getBookId(), // Will be formatted by renderer
                        status,
                        borrowDate,
                        returnDate
                });
            }

            JTable historyTable = new JTable(historyModel);
            historyTable.setRowHeight(80); // Taller rows for book details
            historyTable.getTableHeader().setReorderingAllowed(false);

            // Set custom renderer for cover images
            historyTable.getColumnModel().getColumn(0).setCellRenderer(new BookCoverRenderer());
            historyTable.getColumnModel().getColumn(0).setPreferredWidth(40);

            // Set custom renderer for book details column
            historyTable.getColumnModel().getColumn(1).setCellRenderer(new BookDetailsRenderer());
            historyTable.getColumnModel().getColumn(1).setPreferredWidth(150);

            // Set custom renderer for status column
            historyTable.getColumnModel().getColumn(2).setCellRenderer(new StatusRenderer());
            historyTable.getColumnModel().getColumn(2).setPreferredWidth(100);

            historyTable.getColumnModel().getColumn(3).setPreferredWidth(90);
            historyTable.getColumnModel().getColumn(4).setPreferredWidth(90);

            JScrollPane scrollPane = new JScrollPane(historyTable);
            scrollPane.setPreferredSize(new Dimension(0, 250)); // Increased height
            panel.add(scrollPane, BorderLayout.CENTER);
        }

        return panel;
    }

    private JPanel createUserReviewsPanel(User user) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("User Reviews"));
        panel.setBackground(Color.WHITE);

        // Get all reviews by this user
        List<Book> allBooks = libraryService.getAllBooks();
        List<Review> userReviewsList = new ArrayList<>();
        Map<String, Book> bookMap = new HashMap<>();

        for (Book book : allBooks) {
            for (Review review : book.getReviews()) {
                // Use getUserId() instead of getUser().getUserId()
                if (review.getUserId() != null && review.getUserId().equals(user.getUserId())) {
                    userReviewsList.add(review);
                    bookMap.put(review.getReviewId(), book);
                }
            }
        }

        if (userReviewsList.isEmpty()) {
            JLabel noReviewsLabel = new JLabel("No reviews submitted by this user", SwingConstants.CENTER);
            noReviewsLabel.setForeground(Color.GRAY);
            noReviewsLabel.setBackground(Color.WHITE);
            panel.add(noReviewsLabel, BorderLayout.CENTER);
        } else {
            // Create panel for reviews with updated layout
            JPanel reviewsContainer = new JPanel();
            reviewsContainer.setLayout(new BoxLayout(reviewsContainer, BoxLayout.Y_AXIS));
            reviewsContainer.setBackground(Color.WHITE);

            // Sort by date (newest first)
            userReviewsList.sort((r1, r2) -> r2.getReviewDate().compareTo(r1.getReviewDate()));

            // Create a review panel for each review
            for (Review review : userReviewsList) {
                Book book = bookMap.get(review.getReviewId());
                if (book == null) continue;

                JPanel reviewPanel = new JPanel();
                reviewPanel.setLayout(new BoxLayout(reviewPanel, BoxLayout.Y_AXIS));
                reviewPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
                reviewPanel.setBackground(Color.WHITE);
                reviewPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

                // First row: Book title on left, rating on right with (X/5 format) - LIGHT GRAY BACKGROUND
                JPanel firstRow = new JPanel(new BorderLayout());
                firstRow.setBackground(new Color(240, 240, 240)); // Light gray background
                firstRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                firstRow.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

                // Book title on left
                JLabel bookTitleLabel = new JLabel(book.getTitle());
                bookTitleLabel.setFont(bookTitleLabel.getFont().deriveFont(Font.BOLD));
                firstRow.add(bookTitleLabel, BorderLayout.WEST);

                // Rating on right with (X/5) format
                String ratingText = "★".repeat(review.getRating()) + "☆".repeat(5 - review.getRating()) +
                        " (" + review.getRating() + "/5)";
                JLabel ratingLabel = new JLabel(ratingText);
                ratingLabel.setForeground(Color.ORANGE);
                ratingLabel.setFont(ratingLabel.getFont().deriveFont(Font.BOLD));
                firstRow.add(ratingLabel, BorderLayout.EAST);

                // Second row: Review comment
                JPanel secondRow = new JPanel(new BorderLayout());
                secondRow.setBackground(Color.WHITE);
                secondRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
                secondRow.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JTextArea commentArea = new JTextArea(review.getComment());
                commentArea.setEditable(false);
                commentArea.setLineWrap(true);
                commentArea.setWrapStyleWord(true);
                commentArea.setBackground(Color.WHITE);
                commentArea.setFont(commentArea.getFont().deriveFont(Font.PLAIN, 12f));
                secondRow.add(commentArea, BorderLayout.CENTER);

                // Third row: Book ID left, review date right - LIGHT GRAY BACKGROUND
                JPanel thirdRow = new JPanel(new BorderLayout());
                thirdRow.setBackground(new Color(240, 240, 240)); // Light gray background
                thirdRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
                thirdRow.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

                // Book ID on left
                JLabel bookIdLabel = new JLabel("Book ID: " + book.getBookId());
                bookIdLabel.setForeground(Color.DARK_GRAY);
                bookIdLabel.setFont(bookIdLabel.getFont().deriveFont(Font.PLAIN, 11f));
                thirdRow.add(bookIdLabel, BorderLayout.WEST);

                // Review date on right
                String reviewDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(review.getReviewDate());
                JLabel dateLabel = new JLabel("Reviewed: " + reviewDate);
                dateLabel.setForeground(Color.GRAY);
                dateLabel.setFont(dateLabel.getFont().deriveFont(Font.PLAIN, 11f));
                dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                thirdRow.add(dateLabel, BorderLayout.EAST);

                // Add all rows to review panel
                reviewPanel.add(firstRow);
                reviewPanel.add(secondRow);
                reviewPanel.add(thirdRow);

                // Add review panel to container
                reviewsContainer.add(reviewPanel);
            }

            JScrollPane scrollPane = new JScrollPane(reviewsContainer);
            scrollPane.setPreferredSize(new Dimension(0, 200)); // Increased height
            panel.add(scrollPane, BorderLayout.CENTER);
        }

        return panel;
    }

    private ImageIcon loadBookCover(Book book) {
        try {
            if (book.getCoverImagePath() != null && !book.getCoverImagePath().isEmpty()) {
                // Try to load from file path
                return new ImageIcon(book.getCoverImagePath());
            }
        } catch (Exception e) {
            // Fall through to default
        }

        // Default book cover icon
        return createDefaultBookCover(book.getTitle());
    }

    private ImageIcon createDefaultBookCover(String title) {
        // Create a simple default book cover with first letter of title
        int size = 30;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Background
        g2d.setColor(new Color(70, 130, 180));
        g2d.fillRect(0, 0, size, size);

        // Border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(0, 0, size-1, size-1);

        // Text (first letter of title)
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String firstLetter = title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase();
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(firstLetter);
        int textHeight = fm.getHeight();
        g2d.drawString(firstLetter, (size - textWidth) / 2, (size + textHeight) / 2 - 4);

        g2d.dispose();

        return new ImageIcon(image);
    }

    private Color getMembershipColor(MembershipType type) {
        switch (type) {
            case STUDENT:
                return new Color(0, 128, 0); // Green
            case FACULTY:
                return new Color(0, 0, 128); // Blue
            case GUEST:
                return new Color(128, 0, 0); // Red
            default:
                return Color.BLACK;
        }
    }

    // Custom renderer for book details column (title, author, book ID in one column)
    private class BookDetailsRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            if (value != null) {
                String bookId = value.toString();

                // Get book from the model (we stored book ID in the cell)
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                Book book = null;

                // Try to find the book in the reservations or borrow history
                if (table.getColumnName(column).equals("Book Details")) {
                    // We need to get the book from LibraryService
                    book = libraryService.getBook(bookId);
                }

                if (book != null) {
                    // Title
                    JLabel titleLabel = new JLabel("<html><b>" + book.getTitle() + "</b></html>");
                    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));
                    titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                    // Author
                    JLabel authorLabel = new JLabel("Author: " + book.getAuthor());
                    authorLabel.setFont(authorLabel.getFont().deriveFont(Font.PLAIN, 10f));
                    authorLabel.setForeground(Color.DARK_GRAY);
                    authorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                    // Book ID
                    JLabel idLabel = new JLabel("ID: " + book.getBookId());
                    idLabel.setFont(idLabel.getFont().deriveFont(Font.PLAIN, 9f));
                    idLabel.setForeground(Color.GRAY);
                    idLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

                    panel.add(titleLabel);
                    panel.add(Box.createVerticalStrut(2));
                    panel.add(authorLabel);
                    panel.add(Box.createVerticalStrut(2));
                    panel.add(idLabel);
                } else {
                    // Fallback if book not found
                    JLabel errorLabel = new JLabel("<html><i>Book details not available</i></html>");
                    errorLabel.setForeground(Color.RED);
                    panel.add(errorLabel);
                }
            }

            return panel;
        }
    }

    // Custom renderer for status column
    private class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value != null) {
                String status = value.toString();
                if ("Borrowed".equals(status) || "Waiting".equals(status)) {
                    label.setForeground(Color.RED);
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                } else if ("Returned".equals(status) || "Notified".equals(status)) {
                    label.setForeground(new Color(0, 128, 0)); // Green
                } else {
                    label.setForeground(Color.BLACK);
                }
            }

            return label;
        }
    }

    // Custom renderer for book cover images
    private class BookCoverRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = new JLabel();
            label.setHorizontalAlignment(SwingConstants.CENTER);

            if (value instanceof ImageIcon) {
                label.setIcon((ImageIcon) value);
            } else {
                label.setIcon(createDefaultBookCover(""));
            }

            // Selection background
            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
                label.setOpaque(true);
            } else {
                label.setBackground(table.getBackground());
                label.setOpaque(false);
            }

            return label;
        }
    }

    // Custom cell renderer for centering numbers in the Books Borrowed column
    private class CenteredNumberRenderer extends DefaultTableCellRenderer {
        public CenteredNumberRenderer() {
            setHorizontalAlignment(JLabel.CENTER);  // Center align
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Ensure numbers are centered
            if (c instanceof JLabel) {
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
            }

            // Use the same color as other columns
            c.setForeground(Color.BLACK);

            return c;
        }
    }

    // Standard cell renderer for the Total Fine column (normal design like other columns)
    private class StandardCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof Double) {
                double fine = (Double) value;
                // Format with 2 decimal places
                ((JLabel) c).setText(String.format("LKR %.2f", fine));
            }

            // Use the same styling as other columns
            c.setForeground(Color.BLACK);
            if (c instanceof JLabel) {
                ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
            }

            return c;
        }
    }

    // Custom cell renderer for highlighting search text
    private class HighlightCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!currentSearchText.isEmpty() && value != null) {
                String cellText = value.toString();
                String lowerCellText = cellText.toLowerCase();

                // Check if this cell contains the search text
                if (lowerCellText.contains(currentSearchText)) {
                    // Highlight the entire row background
                    c.setBackground(new Color(255, 255, 200)); // Light yellow

                    // Highlight the matching text within the cell
                    if (c instanceof JLabel) {
                        JLabel label = (JLabel) c;
                        // Find all occurrences and highlight them
                        String highlightedText = highlightText(cellText, currentSearchText);
                        label.setText("<html>" + highlightedText + "</html>");
                    }
                } else {
                    // Check if any cell in this row matches
                    int modelRow = table.convertRowIndexToModel(row);
                    boolean rowMatches = false;
                    for (int col = 0; col < table.getColumnCount(); col++) {
                        Object cellValue = tableModel.getValueAt(modelRow, col);
                        if (cellValue != null && cellValue.toString().toLowerCase().contains(currentSearchText)) {
                            rowMatches = true;
                            break;
                        }
                    }

                    if (rowMatches) {
                        c.setBackground(new Color(255, 255, 200)); // Light yellow for entire row
                    } else {
                        c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    }
                }
            } else {
                c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            }

            return c;
        }

        private String highlightText(String text, String search) {
            if (text == null || search == null || search.isEmpty()) {
                return text;
            }

            // Escape HTML special characters
            text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

            // Case-insensitive replacement with highlighting
            String lowerText = text.toLowerCase();
            String lowerSearch = search.toLowerCase();

            StringBuilder result = new StringBuilder();
            int lastIndex = 0;
            int index = lowerText.indexOf(lowerSearch);

            while (index >= 0) {
                // Add text before match
                result.append(text.substring(lastIndex, index));

                // Add highlighted match
                result.append("<span style='background-color: #FFD700; font-weight: bold; color: #000000;'>");
                result.append(text.substring(index, index + search.length()));
                result.append("</span>");

                lastIndex = index + search.length();
                index = lowerText.indexOf(lowerSearch, lastIndex);
            }

            // Add remaining text
            result.append(text.substring(lastIndex));

            return result.toString();
        }
    }

    // Custom cell renderer for the Has Reservations column
    private class ReservationCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value != null) {
                String status = value.toString();

                // Use the same color as other columns
                if ("Yes".equals(status)) {
                    c.setForeground(Color.BLACK); // Same color as other columns
                    ((JLabel) c).setText("Yes");
                } else {
                    c.setForeground(Color.BLACK); // Same color as other columns
                    ((JLabel) c).setText("No");
                }
            }

            return c;
        }
    }

    private void highlightUsersInTable(String searchText) {
        if (searchText.trim().isEmpty()) {
            clearUserHighlights();
            return;
        }

        String lowerSearch = searchText.toLowerCase();

        // Apply row filter to show only matching users
        tableSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                // Check all columns for the search text
                for (int i = 0; i < entry.getValueCount(); i++) {
                    Object value = entry.getValue(i);
                    if (value != null && value.toString().toLowerCase().contains(lowerSearch)) {
                        return true;
                    }
                }
                return false;
            }
        });

        userTable.repaint();
    }

    private void clearUserHighlights() {
        tableSorter.setRowFilter(null);
        currentSearchText = "";
        userTable.repaint();
    }

    private void addInputValidationListeners() {
        // Phone field validation - only allow digits and common phone characters
        phoneField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                // Allow digits, space, hyphen, parentheses, plus sign, backspace, delete
                if (!(Character.isDigit(c) ||
                        c == ' ' || c == '-' || c == '(' || c == ')' ||
                        c == '+' || c == '\b' || c == '\u007F')) {
                    e.consume();
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });

        // Name field validation - only allow letters and basic punctuation
        nameField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                // Allow letters, space, hyphen, apostrophe, period, backspace, delete
                if (!(Character.isLetter(c) ||
                        c == ' ' || c == '-' || c == '\'' || c == '.' ||
                        c == '\b' || c == '\u007F')) {
                    e.consume();
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });

        // Email field validation - basic validation
        emailField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                String email = emailField.getText().trim();
                if (!email.isEmpty()) {
                    String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
                    if (!email.matches(emailRegex)) {
                        emailField.setBackground(new Color(255, 200, 200)); // Light red
                        emailField.setToolTipText("Invalid email format. Example: user@example.com");
                    } else {
                        emailField.setBackground(Color.WHITE);
                        emailField.setToolTipText(null);
                    }
                } else {
                    emailField.setBackground(Color.WHITE);
                    emailField.setToolTipText(null);
                }
            }
        });
    }

    private void addUser() {
        try {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            MembershipType membershipType = (MembershipType) membershipComboBox.getSelectedItem();

            // Validate name
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter the user's name",
                        "Name Required",
                        JOptionPane.ERROR_MESSAGE);
                nameField.requestFocus();
                return;
            }

            // Name should contain only letters, spaces, hyphens, and apostrophes
            if (!name.matches("^[a-zA-Z\\s\\-'.]+$")) {
                JOptionPane.showMessageDialog(this,
                        "Name can only contain letters, spaces, hyphens (-), apostrophes (') and periods (.)",
                        "Invalid Name",
                        JOptionPane.ERROR_MESSAGE);
                nameField.requestFocus();
                return;
            }

            // Name should be at least 2 characters long
            if (name.length() < 2) {
                JOptionPane.showMessageDialog(this,
                        "Name must be at least 2 characters long",
                        "Invalid Name",
                        JOptionPane.ERROR_MESSAGE);
                nameField.requestFocus();
                return;
            }

            // Validate email
            if (email.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter the user's email address",
                        "Email Required",
                        JOptionPane.ERROR_MESSAGE);
                emailField.requestFocus();
                return;
            }

            // Basic email validation
            String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
            if (!email.matches(emailRegex)) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a valid email address (e.g., user@example.com)",
                        "Invalid Email",
                        JOptionPane.ERROR_MESSAGE);
                emailField.requestFocus();
                return;
            }

            // Validate phone number
            if (phone.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter the user's phone number",
                        "Phone Required",
                        JOptionPane.ERROR_MESSAGE);
                phoneField.requestFocus();
                return;
            }

            // Phone number validation - allows numbers, spaces, parentheses, hyphens, plus sign
            // Removes all non-digit characters first
            String digitsOnly = phone.replaceAll("[^0-9]", "");

            // Check if phone contains any letters
            if (phone.matches(".*[a-zA-Z].*")) {
                JOptionPane.showMessageDialog(this,
                        "Phone number cannot contain letters",
                        "Invalid Phone Number",
                        JOptionPane.ERROR_MESSAGE);
                phoneField.requestFocus();
                return;
            }

            // Check if phone has at least 7 digits (minimum for a phone number)
            if (digitsOnly.length() < 7) {
                JOptionPane.showMessageDialog(this,
                        "Phone number must contain at least 7 digits",
                        "Invalid Phone Number",
                        JOptionPane.ERROR_MESSAGE);
                phoneField.requestFocus();
                return;
            }

            // Check if phone has more than 15 digits (unlikely for a valid phone number)
            if (digitsOnly.length() > 15) {
                JOptionPane.showMessageDialog(this,
                        "Phone number is too long. Maximum 15 digits allowed.",
                        "Invalid Phone Number",
                        JOptionPane.ERROR_MESSAGE);
                phoneField.requestFocus();
                return;
            }

            // Create and add user
            User user = libraryService.addUser(name, email, phone, membershipType);
            refreshTable();

            // Show success message with details
            String message = String.format(
                    "✅ User added successfully!\n\n" +
                            "📋 User Details:\n" +
                            "• User ID: %s\n" +
                            "• Name: %s\n" +
                            "• Email: %s\n" +
                            "• Phone: %s\n" +
                            "• Membership Type: %s\n" +
                            "• Borrowing Limit: %d books\n" +
                            "• Loan Period: %d days\n" +
                            "• Daily Fine Rate: LKR %.2f\n\n" +
                            "The user can now borrow books from the library.",
                    user.getUserId(),
                    user.getName(),
                    user.getEmail(),
                    user.getContactNumber(),
                    user.getMembershipType().getDisplayName(),
                    user.getMembershipType().getBorrowingLimit(),
                    user.getMembershipType().getLoanPeriod(),
                    user.getMembershipType().getDailyFine()
            );

            JOptionPane.showMessageDialog(this,
                    message,
                    "User Added Successfully",
                    JOptionPane.INFORMATION_MESSAGE);

            clearForm();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error adding user: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a user from the table to update",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = userIdField.getText().trim();
        if (userId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No user ID found. Please select a user from the table.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = libraryService.getUser(userId);
        if (user == null) {
            JOptionPane.showMessageDialog(this,
                    "User not found with ID: " + userId,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        // Validate name
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter the user's name",
                    "Name Required",
                    JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return;
        }

        // Name should contain only letters, spaces, hyphens, and apostrophes
        if (!name.matches("^[a-zA-Z\\s\\-'.]+$")) {
            JOptionPane.showMessageDialog(this,
                    "Name can only contain letters, spaces, hyphens (-), apostrophes (') and periods (.)",
                    "Invalid Name",
                    JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return;
        }

        // Name should be at least 2 characters long
        if (name.length() < 2) {
            JOptionPane.showMessageDialog(this,
                    "Name must be at least 2 characters long",
                    "Invalid Name",
                    JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return;
        }

        // Validate email
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter the user's email address",
                    "Email Required",
                    JOptionPane.ERROR_MESSAGE);
            emailField.requestFocus();
            return;
        }

        // Basic email validation
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (!email.matches(emailRegex)) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid email address (e.g., user@example.com)",
                    "Invalid Email",
                    JOptionPane.ERROR_MESSAGE);
            emailField.requestFocus();
            return;
        }

        // Validate phone number
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter the user's phone number",
                    "Phone Required",
                    JOptionPane.ERROR_MESSAGE);
            phoneField.requestFocus();
            return;
        }

        // Phone number validation - allows numbers, spaces, parentheses, hyphens, plus sign
        // Removes all non-digit characters first
        String digitsOnly = phone.replaceAll("[^0-9]", "");

        // Check if phone contains any letters
        if (phone.matches(".*[a-zA-Z].*")) {
            JOptionPane.showMessageDialog(this,
                    "Phone number cannot contain letters",
                    "Invalid Phone Number",
                    JOptionPane.ERROR_MESSAGE);
            phoneField.requestFocus();
            return;
        }

        // Check if phone has at least 7 digits (minimum for a phone number)
        if (digitsOnly.length() < 7) {
            JOptionPane.showMessageDialog(this,
                    "Phone number must contain at least 7 digits",
                    "Invalid Phone Number",
                    JOptionPane.ERROR_MESSAGE);
            phoneField.requestFocus();
            return;
        }

        // Check if phone has more than 15 digits (unlikely for a valid phone number)
        if (digitsOnly.length() > 15) {
            JOptionPane.showMessageDialog(this,
                    "Phone number is too long. Maximum 15 digits allowed.",
                    "Invalid Phone Number",
                    JOptionPane.ERROR_MESSAGE);
            phoneField.requestFocus();
            return;
        }

        // Update user details
        user.setName(name);
        user.setEmail(email);
        user.setContactNumber(phone);
        user.setMembershipType((MembershipType) membershipComboBox.getSelectedItem());

        refreshTable();

        // Show success message
        String message = String.format(
                "✅ User updated successfully!\n\n" +
                        "📋 Updated Details:\n" +
                        "• User ID: %s\n" +
                        "• Name: %s\n" +
                        "• Email: %s\n" +
                        "• Phone: %s\n" +
                        "• Membership Type: %s\n" +
                        "• Books Borrowed: %d\n" +
                        "• Total Fine: LKR %.2f\n" +
                        "• Active Reservations: %s",
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getContactNumber(),
                user.getMembershipType().getDisplayName(),
                user.getBorrowedBooks().size(),
                calculateUserTotalFine(user),
                hasActiveReservations(user) ? "Yes" : "No"
        );

        JOptionPane.showMessageDialog(this,
                message,
                "User Updated Successfully",
                JOptionPane.INFORMATION_MESSAGE);

        clearForm();
    }

    private void deleteUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a user to delete",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = userIdField.getText().trim();
        String userName = nameField.getText().trim();

        // Check if user has borrowed books
        User user = libraryService.getUser(userId);
        if (user != null && !user.getBorrowedBooks().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    String.format(
                            "Cannot delete user '%s' (ID: %s) because they have %d borrowed book(s).\n\n" +
                                    "Please ensure all books are returned before deleting the user.",
                            userName, userId, user.getBorrowedBooks().size()
                    ),
                    "Cannot Delete User",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if user has unpaid fines
        double totalFine = calculateUserTotalFine(user);
        if (totalFine > 0) {
            JOptionPane.showMessageDialog(this,
                    String.format(
                            "Cannot delete user '%s' (ID: %s) because they have unpaid fines of LKR %.2f.\n\n" +
                                    "Please ensure all fines are paid before deleting the user.",
                            userName, userId, totalFine
                    ),
                    "Cannot Delete User",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format(
                        "Are you sure you want to delete this user?\n\n" +
                                "👤 User ID: %s\n" +
                                "📛 Name: %s\n" +
                                "📧 Email: %s\n\n" +
                                "This action cannot be undone.",
                        userId, userName, emailField.getText().trim()
                ),
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            // Remove user from the system
            libraryService.updateUser(userId, null);
            refreshTable();
            clearForm();

            JOptionPane.showMessageDialog(this,
                    String.format(
                            "✅ User deleted successfully!\n\n" +
                                    "User '%s' (ID: %s) has been removed from the system.",
                            userName, userId
                    ),
                    "User Deleted",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void clearForm() {
        userIdField.setText("");
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        membershipComboBox.setSelectedIndex(0);
        if (!isDetailVisible) {
            userTable.clearSelection();
        }
        idLabel.setText("(Auto-generated)");
        idLabel.setForeground(Color.BLUE);

        // Reset field backgrounds
        nameField.setBackground(Color.WHITE);
        emailField.setBackground(Color.WHITE);
        phoneField.setBackground(Color.WHITE);
        nameField.setToolTipText(null);
        emailField.setToolTipText(null);
        phoneField.setToolTipText(null);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        for (User user : libraryService.getAllUsers()) {
            double totalFine = calculateUserTotalFine(user);
            boolean hasReservations = hasActiveReservations(user);

            tableModel.addRow(new Object[]{
                    user.getUserId(),
                    user.getName(),
                    user.getEmail(),
                    user.getContactNumber(),
                    user.getMembershipType().getDisplayName(),
                    user.getBorrowedBooks().size(),
                    totalFine,
                    hasReservations ? "Yes" : "No"
            });
        }
        clearUserHighlights();
    }

    // Helper method to calculate total fine for a user
    private double calculateUserTotalFine(User user) {
        if (user == null) return 0.0;

        double totalFine = 0.0;

        // Get all borrow records for the user
        List<BorrowRecord> userBorrowRecords = libraryService.getAllBorrowRecords()
                .stream()
                .filter(record -> record.getUser() != null &&
                        record.getUser().getUserId().equals(user.getUserId()))
                .collect(Collectors.toList());

        // Calculate fine for each overdue book
        for (BorrowRecord record : userBorrowRecords) {
            if (record.isOverdue() && !record.isFinePaid()) {
                // Calculate overdue days
                int overdueDays = record.getOverdueDays();

                // Calculate fine based on membership type
                double dailyFineRate = 0.0;
                switch (user.getMembershipType()) {
                    case STUDENT:
                        dailyFineRate = 50.0; // LKR 50 per day for students
                        break;
                    case FACULTY:
                        dailyFineRate = 20.0; // LKR 20 per day for faculty
                        break;
                    case GUEST:
                        dailyFineRate = 100.0; // LKR 100 per day for guests
                        break;
                }

                double fine = overdueDays * dailyFineRate;
                totalFine += fine;
            }
        }

        return totalFine;
    }

    // Helper method to check if user has active reservations
    private boolean hasActiveReservations(User user) {
        if (user == null) return false;

        // Get all reservations for the user
        List<Reservation> userReservations = libraryService.getAllReservations()
                .stream()
                .filter(reservation -> reservation.getUser() != null &&
                        reservation.getUser().getUserId().equals(user.getUserId()))
                .collect(Collectors.toList());

        // Check if any reservation is active (not notified)
        for (Reservation reservation : userReservations) {
            if (!reservation.isNotified()) {
                return true;
            }
        }

        return false;
    }
}