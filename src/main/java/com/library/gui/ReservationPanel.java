package com.library.gui;

import com.library.services.LibraryService;
import com.library.models.Reservation;
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

public class ReservationPanel extends JPanel {
    private LibraryService libraryService;
    private JTable reservationTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> userComboBox;
    private JComboBox<String> bookComboBox;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Search components
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> tableSorter;

    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 150); 

    public ReservationPanel() {
        libraryService = LibraryService.getInstance(); 
        initializeUI();
        refreshLists();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        // Create main container with BorderLayout for better stretching
        JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
        mainContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Top panel - Make Reservation
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Make Reservation"));

        // Panel for user and book selection
        JPanel selectionPanel = new JPanel(new GridLayout(2, 2, 10, 10));

        // Create searchable combo boxes
        userComboBox = createSearchableComboBox();
        bookComboBox = createSearchableComboBox();

        selectionPanel.add(new JLabel("Select User:"));
        selectionPanel.add(userComboBox);
        selectionPanel.add(new JLabel("Select Book:"));
        selectionPanel.add(bookComboBox);

        // Make Reservation button
        JButton reserveButton = new JButton("Make Reservation");
        reserveButton.setBorder(new LineBorder(new Color(76, 175, 80), 2));
        reserveButton.addActionListener(e -> makeReservation());

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        buttonPanel.add(reserveButton, BorderLayout.CENTER);

        controlPanel.add(selectionPanel, BorderLayout.NORTH);
        controlPanel.add(buttonPanel, BorderLayout.CENTER);

        // 2. Center Panel - Contains search and table
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // Search Panel - Stretched vertically
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Reservations"));
        searchPanel.setPreferredSize(new Dimension(0, 60)); // Set preferred height

        // Inner panel for search components with GridBagLayout for better control
        JPanel searchComponentsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Search label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        searchComponentsPanel.add(new JLabel("Search:"), gbc);

        // Search field - will stretch horizontally
        searchField = new JTextField();
        searchField.setToolTipText("Search by Reservation ID, Book, User, Date, or Status...");
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 2.5; // Take all available horizontal space
        gbc.fill = GridBagConstraints.HORIZONTAL;
        searchComponentsPanel.add(searchField, gbc);

        // Search button
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        searchComponentsPanel.add(searchButton, gbc);

        // Clear button
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearSearch());
        gbc.gridx = 3;
        gbc.gridy = 0;
        searchComponentsPanel.add(clearButton, gbc);

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

        // Table Panel
        String[] columns = {"Reservation ID", "Book", "User", "Reservation Date", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        reservationTable = new JTable(tableModel) {
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

        JScrollPane scrollPane = new JScrollPane(reservationTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Reservations List"));

        // Add search panel and table to center panel
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom button panel
        JButton cancelButton = new JButton("Cancel Reservation");
        JButton refreshButton = new JButton("Refresh Lists");

        cancelButton.setBorder(new LineBorder(new Color(244, 67, 54), 2));
        refreshButton.setBorder(new LineBorder(new Color(255, 193, 7), 2));

        cancelButton.addActionListener(e -> cancelReservation());
        refreshButton.addActionListener(e -> refreshLists());

        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // Add components to main container
        mainContainer.add(controlPanel, BorderLayout.NORTH);
        mainContainer.add(centerPanel, BorderLayout.CENTER);
        mainContainer.add(bottomButtonPanel, BorderLayout.SOUTH);

        // Add bottom buttons AFTER adding to container
        bottomButtonPanel.add(cancelButton);
        bottomButtonPanel.add(refreshButton);

        // Add main container to the panel
        add(mainContainer, BorderLayout.CENTER);

        // Setup table sorter for search
        tableSorter = new TableRowSorter<>(tableModel);
        reservationTable.setRowSorter(tableSorter);

        refreshLists();
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
        reservationTable.repaint();

        // Show notification only if no results found
        int resultCount = reservationTable.getRowCount();
        if (resultCount == 0) {
            JOptionPane.showMessageDialog(this,
                    "No reservations found for '" + searchText + "'",
                    "Search Results",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void clearSearch() {
        searchField.setText("");
        tableSorter.setRowFilter(null);
        reservationTable.repaint();
        searchField.requestFocus();
    }

    private JComboBox<String> createSearchableComboBox() {
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.setEditable(true);

        // Add key listener to the editor component for filtering
        JTextField editor = (JTextField) comboBox.getEditor().getEditorComponent();
        editor.addKeyListener(new java.awt.event.KeyAdapter() {
            private boolean shouldFilter = true;
            private String lastSearch = "";

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

    private void makeReservation() {
        String userSelection = (String) userComboBox.getSelectedItem();
        String bookSelection = (String) bookComboBox.getSelectedItem();

        if (userSelection == null || bookSelection == null) {
            JOptionPane.showMessageDialog(this, "Please select both user and book");
            return;
        }

        String userId = userSelection.split(" - ")[0].trim();
        String bookId = bookSelection.split(" - ")[0].trim();

        // Get user and book objects
        com.library.models.User user = libraryService.getUser(userId);
        com.library.models.Book book = libraryService.getBook(bookId);

        if (user == null || book == null) {
            JOptionPane.showMessageDialog(this, "User or book not found");
            return;
        }

        // Check if the user has already borrowed this book
        boolean hasBorrowedBook = false;
        for (com.library.models.Book borrowedBook : user.getBorrowedBooks()) {
            if (borrowedBook.getBookId().equals(bookId)) {
                hasBorrowedBook = true;
                break;
            }
        }

        if (hasBorrowedBook) {
            JOptionPane.showMessageDialog(this,
                    "❌ You cannot reserve a book that you have currently borrowed.\n" +
                            "📚 Book: " + book.getTitle() + "\n" +
                            "👤 User: " + user.getName() + "\n\n" +
                            "You must return the book before someone else can reserve it.",
                    "Cannot Reserve Own Book",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Reservation reservation = libraryService.reserveBook(userId, bookId);
        if (reservation != null) {
            // Check if the book is already borrowed (will show Borrowed/Reserved status)
            String status = reservation.getBook().getStatus().getDisplayName();

            JOptionPane.showMessageDialog(this,
                    "✅ Reservation made successfully!\n" +
                            "📝 Reservation ID: " + reservation.getReservationId() + "\n" +
                            "📚 Book: " + reservation.getBook().getTitle() + "\n" +
                            "👤 User: " + reservation.getUser().getName() + "\n" +
                            "📊 Status: " + status + "\n\n" +
                            "You will be notified when the book becomes available.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshLists();
            clearSearch(); // Clear search after new reservation
        } else {
            JOptionPane.showMessageDialog(this,
                    "❌ Failed to make reservation. Possible reasons:\n" +
                            "1. Book is not currently borrowed\n" +
                            "2. Book is already reserved by you\n" +
                            "3. You already have a reservation for this book\n" +
                            "4. Book is not available for reservation\n" +
                            "5. You have currently borrowed this book",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelReservation() {
        int selectedRow = reservationTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a reservation to cancel",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert view row to model row (important if table is filtered/sorted)
        int modelRow = reservationTable.convertRowIndexToModel(selectedRow);
        String reservationId = tableModel.getValueAt(modelRow, 0).toString();
        String bookTitle = tableModel.getValueAt(modelRow, 1).toString();
        String userName = tableModel.getValueAt(modelRow, 2).toString();
        String status = tableModel.getValueAt(modelRow, 4).toString();

        // Check if reservation is already notified
        if ("Notified".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "❌ Cannot cancel a reservation that has been notified.\n" +
                            "The book is already available for pickup.\n\n" +
                            "📚 Book: " + bookTitle + "\n" +
                            "👤 User: " + userName + "\n" +
                            "📝 Reservation ID: " + reservationId,
                    "Cannot Cancel",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Confirm cancellation
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel this reservation?\n\n" +
                        "📝 Reservation ID: " + reservationId + "\n" +
                        "📚 Book: " + bookTitle + "\n" +
                        "👤 User: " + userName + "\n" +
                        "📊 Status: " + status + "\n\n" +
                        "This action cannot be undone.",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            // Call the library service to cancel the reservation
            boolean success = libraryService.cancelReservation(reservationId);

            if (success) {
                JOptionPane.showMessageDialog(this,
                        "✅ Reservation cancelled successfully!\n\n" +
                                "📝 Reservation ID: " + reservationId + "\n" +
                                "📚 Book: " + bookTitle + "\n" +
                                "👤 User: " + userName + "\n\n" +
                                "The user has been notified of the cancellation.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                refreshLists(); // Refresh the table
                clearSearch(); // Clear search
            } else {
                JOptionPane.showMessageDialog(this,
                        "❌ Failed to cancel reservation.\n" +
                                "Possible reasons:\n" +
                                "1. Reservation not found\n" +
                                "2. Reservation is already notified\n" +
                                "3. Reservation has expired\n" +
                                "4. System error",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshLists() {
        userComboBox.removeAllItems();
        bookComboBox.removeAllItems();

        // Load users
        for (com.library.models.User user : libraryService.getAllUsers()) {
            userComboBox.addItem(user.getUserId() + " - " + user.getName());
        }

        // Load borrowed books only (including those that are Borrowed/Reserved)
        for (com.library.models.Book book : libraryService.getAllBooks()) {
            String status = book.getStatus().toString();
            if (status.equals("BORROWED") || status.equals("RESERVED")) {
                bookComboBox.addItem(book.getBookId() + " - " + book.getTitle() +
                        " (" + book.getStatus().getDisplayName() + ")");
            }
        }

        refreshTable();
        clearSearch(); // Clear search when refreshing lists
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        for (Reservation reservation : libraryService.getAllReservations()) {
            tableModel.addRow(new Object[]{
                    reservation.getReservationId(),
                    reservation.getBook().getTitle(),
                    reservation.getUser().getName(),
                    dateFormat.format(reservation.getReservationDate()),
                    reservation.isNotified() ? "Notified" : "Pending"
            });
        }
    }
}