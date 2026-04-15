package com.library.gui;

import com.library.services.LibraryService;
import com.library.models.Book;
import com.library.models.User;
import com.library.models.BorrowRecord;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ReportsPanel extends JPanel {
    private LibraryService libraryService;
    private JTabbedPane reportTabs;
    private JTable mostBorrowedTable;
    private JTable activeUsersTable;
    private JTable overdueBooksTable;

    // Search components
    private JTextField searchField;
    private JComboBox<String> searchTypeComboBox;
    private JButton searchButton;
    private JButton clearButton;
    private TableRowSorter<DefaultTableModel> borrowedSorter;
    private TableRowSorter<DefaultTableModel> usersSorter;
    private TableRowSorter<DefaultTableModel> overdueSorter;

    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 150); // Light yellow

    public ReportsPanel() {
        libraryService = LibraryService.getInstance(); 
        initializeUI();
        initializeSearchFilters();
        generateAllReports(false); // Generate reports on initialization without notification
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Create search panel at the top
        JPanel searchPanel = createSearchPanel();

        reportTabs = new JTabbedPane();

        // Most Borrowed Books Tab
        String[] borrowedColumns = {"Rank", "Book ID", "Title", "Author", "Borrow History"};
        DefaultTableModel borrowedModel = new DefaultTableModel(borrowedColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        mostBorrowedTable = new JTable(borrowedModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                // Convert view row to model row
                int modelRow = convertRowIndexToModel(row);
                String searchText = searchField.getText().trim().toLowerCase();

                if (!searchText.isEmpty()) {
                    String selectedTab = (String) searchTypeComboBox.getSelectedItem();
                    boolean shouldHighlight = false;

                    if (selectedTab.equals("All Tabs") || selectedTab.equals("Most Borrowed")) {
                        shouldHighlight = true;
                    }

                    if (shouldHighlight) {
                        DefaultTableModel model = (DefaultTableModel) getModel();
                        for (int col = 0; col < model.getColumnCount(); col++) {
                            Object value = model.getValueAt(modelRow, col);
                            if (value != null && value.toString().toLowerCase().contains(searchText)) {
                                c.setBackground(HIGHLIGHT_COLOR);
                                return c;
                            }
                        }
                    }
                }

                // Default background (alternating for better readability)
                if (!c.getBackground().equals(getSelectionBackground())) {
                    Color bg = (row % 2 == 0) ? Color.WHITE : new Color(245, 245, 245);
                    c.setBackground(bg);
                }
                return c;
            }
        };
        JScrollPane borrowedScroll = new JScrollPane(mostBorrowedTable);
        reportTabs.addTab("Most Borrowed Books", borrowedScroll);

        // Active Users Tab
        String[] usersColumns = {"Rank", "User ID", "Name", "Membership", "Books Borrowed"};
        DefaultTableModel usersModel = new DefaultTableModel(usersColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        activeUsersTable = new JTable(usersModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                // Convert view row to model row
                int modelRow = convertRowIndexToModel(row);
                String searchText = searchField.getText().trim().toLowerCase();

                if (!searchText.isEmpty()) {
                    String selectedTab = (String) searchTypeComboBox.getSelectedItem();
                    boolean shouldHighlight = false;

                    if (selectedTab.equals("All Tabs") || selectedTab.equals("Active Users")) {
                        shouldHighlight = true;
                    }

                    if (shouldHighlight) {
                        DefaultTableModel model = (DefaultTableModel) getModel();
                        for (int col = 0; col < model.getColumnCount(); col++) {
                            Object value = model.getValueAt(modelRow, col);
                            if (value != null && value.toString().toLowerCase().contains(searchText)) {
                                c.setBackground(HIGHLIGHT_COLOR);
                                return c;
                            }
                        }
                    }
                }

                // Default background
                if (!c.getBackground().equals(getSelectionBackground())) {
                    Color bg = (row % 2 == 0) ? Color.WHITE : new Color(245, 245, 245);
                    c.setBackground(bg);
                }
                return c;
            }
        };
        JScrollPane usersScroll = new JScrollPane(activeUsersTable);
        reportTabs.addTab("Active Users", usersScroll);

        // Overdue Books Tab
        String[] overdueColumns = {"Book ID", "Title", "User", "Due Date", "Overdue Days", "Fine (LKR)"};
        DefaultTableModel overdueModel = new DefaultTableModel(overdueColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        overdueBooksTable = new JTable(overdueModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                // Convert view row to model row
                int modelRow = convertRowIndexToModel(row);
                String searchText = searchField.getText().trim().toLowerCase();

                if (!searchText.isEmpty()) {
                    String selectedTab = (String) searchTypeComboBox.getSelectedItem();
                    boolean shouldHighlight = false;

                    if (selectedTab.equals("All Tabs") || selectedTab.equals("Overdue Books")) {
                        shouldHighlight = true;
                    }

                    if (shouldHighlight) {
                        DefaultTableModel model = (DefaultTableModel) getModel();
                        for (int col = 0; col < model.getColumnCount(); col++) {
                            Object value = model.getValueAt(modelRow, col);
                            if (value != null && value.toString().toLowerCase().contains(searchText)) {
                                c.setBackground(HIGHLIGHT_COLOR);
                                return c;
                            }
                        }
                    }
                }

                // Default background
                if (!c.getBackground().equals(getSelectionBackground())) {
                    Color bg = (row % 2 == 0) ? Color.WHITE : new Color(245, 245, 245);
                    c.setBackground(bg);
                }
                return c;
            }
        };
        JScrollPane overdueScroll = new JScrollPane(overdueBooksTable);
        reportTabs.addTab("Overdue Books", overdueScroll);

        // Control Panel with Generate All Reports Button
        JPanel controlPanel = new JPanel();
        JButton generateAllButton = new JButton("Generate All Reports");
        generateAllButton.setBorder(new LineBorder(new Color(255, 193, 7), 2));
        generateAllButton.addActionListener(e -> generateAllReports(true)); // true = show notification
        controlPanel.add(generateAllButton);

        // Add components to the panel
        add(searchPanel, BorderLayout.NORTH);
        add(reportTabs, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout(10, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Reports"));

        // Use BoxLayout for horizontal arrangement that stretches
        JPanel mainSearchPanel = new JPanel();
        mainSearchPanel.setLayout(new BoxLayout(mainSearchPanel, BoxLayout.X_AXIS));

        // Search type combo box with label
        JPanel searchTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        searchTypeComboBox = new JComboBox<>(new String[]{"All Tabs", "Most Borrowed", "Active Users", "Overdue Books"});
        searchTypeComboBox.setPreferredSize(new Dimension(150, 25));
        searchTypeComboBox.setMaximumSize(new Dimension(150, 25));
        searchTypePanel.add(new JLabel("Search in:"));
        searchTypePanel.add(searchTypeComboBox);
        mainSearchPanel.add(searchTypePanel);

        // Add spacing
        mainSearchPanel.add(Box.createRigidArea(new Dimension(10, 0)));

        // Search label
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        mainSearchPanel.add(searchLabel);

        // Add spacing
        mainSearchPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        // Search field - this will stretch to fill all available space
        searchField = new JTextField();
        searchField.setToolTipText("Enter search term...");
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        searchField.setAlignmentY(Component.CENTER_ALIGNMENT);
        mainSearchPanel.add(searchField);

        // Add spacing
        mainSearchPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        // Search button
        searchButton = new JButton("Search");
        searchButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        searchButton.addActionListener(e -> performSearch());
        mainSearchPanel.add(searchButton);

        // Add spacing
        mainSearchPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        // Clear button
        clearButton = new JButton("Clear");
        clearButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        clearButton.addActionListener(e -> clearSearch());
        mainSearchPanel.add(clearButton);

        // Add key listener for real-time search (Enter key)
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performSearch();
                }
            }
        });

        searchPanel.add(mainSearchPanel, BorderLayout.CENTER);
        return searchPanel;
    }

    private void initializeSearchFilters() {
        borrowedSorter = new TableRowSorter<>((DefaultTableModel) mostBorrowedTable.getModel());
        mostBorrowedTable.setRowSorter(borrowedSorter);

        usersSorter = new TableRowSorter<>((DefaultTableModel) activeUsersTable.getModel());
        activeUsersTable.setRowSorter(usersSorter);

        overdueSorter = new TableRowSorter<>((DefaultTableModel) overdueBooksTable.getModel());
        overdueBooksTable.setRowSorter(overdueSorter);
    }

    private void performSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        String selectedTab = (String) searchTypeComboBox.getSelectedItem();

        if (searchText.isEmpty()) {
            clearSearch();
            return;
        }

        // Clear all filters first
        borrowedSorter.setRowFilter(null);
        usersSorter.setRowFilter(null);
        overdueSorter.setRowFilter(null);

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

        // Apply filter based on selected search type
        switch (selectedTab) {
            case "All Tabs":
                borrowedSorter.setRowFilter(filter);
                usersSorter.setRowFilter(filter);
                overdueSorter.setRowFilter(filter);
                break;
            case "Most Borrowed":
                borrowedSorter.setRowFilter(filter);
                break;
            case "Active Users":
                usersSorter.setRowFilter(filter);
                break;
            case "Overdue Books":
                overdueSorter.setRowFilter(filter);
                break;
        }

        // Force table repaint to update highlights
        mostBorrowedTable.repaint();
        activeUsersTable.repaint();
        overdueBooksTable.repaint();

        // Show search results summary
        showSearchResultsSummary(searchText, selectedTab);
    }

    private void clearSearch() {
        searchField.setText("");

        // Clear all filters
        borrowedSorter.setRowFilter(null);
        usersSorter.setRowFilter(null);
        overdueSorter.setRowFilter(null);

        // Reset table backgrounds
        mostBorrowedTable.repaint();
        activeUsersTable.repaint();
        overdueBooksTable.repaint();

        // Focus back to search field
        searchField.requestFocus();
    }

    private void showSearchResultsSummary(String searchText, String selectedTab) {
        int mostBorrowedResults = mostBorrowedTable.getRowCount();
        int activeUsersResults = activeUsersTable.getRowCount();
        int overdueBooksResults = overdueBooksTable.getRowCount();
        int totalResults = mostBorrowedResults + activeUsersResults + overdueBooksResults;

        // Update the search field tooltip
        searchField.setToolTipText("Found " + totalResults + " results for '" + searchText + "'");

        switch (selectedTab) {
            case "All Tabs":
                if (totalResults > 0) {
                    // Create a custom dialog with buttons to navigate to tabs
                    JDialog resultsDialog = new JDialog(
                            (Frame) SwingUtilities.getWindowAncestor(this),
                            "Search Results Summary",
                            true
                    );
                    resultsDialog.setLayout(new BorderLayout());

                    StringBuilder message = new StringBuilder();
                    message.append("Search Results for '").append(searchText).append("':\n\n");
                    message.append("Searching across all tabs:\n");
                    message.append("• Most Borrowed Books: ").append(mostBorrowedResults).append(" results\n");
                    message.append("• Active Users: ").append(activeUsersResults).append(" results\n");
                    message.append("• Overdue Books: ").append(overdueBooksResults).append(" results\n");
                    message.append("\nTotal: ").append(totalResults).append(" results found");

                    JTextArea resultsText = new JTextArea(message.toString());
                    resultsText.setEditable(false);
                    resultsText.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    resultsText.setBackground(new Color(240, 240, 240));

                    JPanel buttonPanel = new JPanel(new FlowLayout());

                    if (mostBorrowedResults > 0) {
                        JButton goToBorrowedButton = new JButton("View Most Borrowed (" + mostBorrowedResults + ")");
                        goToBorrowedButton.addActionListener(e -> {
                            reportTabs.setSelectedIndex(0);
                            resultsDialog.dispose();
                        });
                        buttonPanel.add(goToBorrowedButton);
                    }

                    if (activeUsersResults > 0) {
                        JButton goToUsersButton = new JButton("View Active Users (" + activeUsersResults + ")");
                        goToUsersButton.addActionListener(e -> {
                            reportTabs.setSelectedIndex(1);
                            resultsDialog.dispose();
                        });
                        buttonPanel.add(goToUsersButton);
                    }

                    if (overdueBooksResults > 0) {
                        JButton goToOverdueButton = new JButton("View Overdue Books (" + overdueBooksResults + ")");
                        goToOverdueButton.addActionListener(e -> {
                            reportTabs.setSelectedIndex(2);
                            resultsDialog.dispose();
                        });
                        buttonPanel.add(goToOverdueButton);
                    }

                    JButton closeButton = new JButton("Close");
                    closeButton.addActionListener(e -> resultsDialog.dispose());
                    buttonPanel.add(closeButton);

                    resultsDialog.add(new JScrollPane(resultsText), BorderLayout.CENTER);
                    resultsDialog.add(buttonPanel, BorderLayout.SOUTH);

                    resultsDialog.setSize(500, 300);
                    resultsDialog.setLocationRelativeTo(this);
                    resultsDialog.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "No results found for '" + searchText + "' in any tab.",
                            "Search Results",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                break;

            case "Most Borrowed":
                // Auto-switch to the tab
                reportTabs.setSelectedIndex(0);
                // Show notification only if no results found
                if (mostBorrowedResults == 0) {
                    JOptionPane.showMessageDialog(this,
                            "No results found for '" + searchText + "' in Most Borrowed Books",
                            "Search Results",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                break;

            case "Active Users":
                // Auto-switch to the tab
                reportTabs.setSelectedIndex(1);
                // Show notification only if no results found
                if (activeUsersResults == 0) {
                    JOptionPane.showMessageDialog(this,
                            "No results found for '" + searchText + "' in Active Users",
                            "Search Results",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                break;

            case "Overdue Books":
                // Auto-switch to the tab
                reportTabs.setSelectedIndex(2);
                // Show notification only if no results found
                if (overdueBooksResults == 0) {
                    JOptionPane.showMessageDialog(this,
                            "No results found for '" + searchText + "' in Overdue Books",
                            "Search Results",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                break;
        }
    }

    private void generateMostBorrowedReport() {
        DefaultTableModel model = (DefaultTableModel) mostBorrowedTable.getModel();
        model.setRowCount(0);

        java.util.List<Book> books = libraryService.getMostBorrowedBooks(10);
        if (books.isEmpty()) {
            model.addRow(new Object[]{"-", "No books available", "-", "-", "0"});
        } else {
            int rank = 1;
            for (Book book : books) {
                model.addRow(new Object[]{
                        rank++,
                        book.getBookId(),
                        book.getTitle(),
                        book.getAuthor(),
                        book.getBorrowHistory().size()
                });
            }
        }
    }

    private void generateActiveUsersReport() {
        DefaultTableModel model = (DefaultTableModel) activeUsersTable.getModel();
        model.setRowCount(0);

        java.util.List<User> users = libraryService.getActiveBorrowers(10);
        if (users.isEmpty()) {
            model.addRow(new Object[]{"-", "No users available", "-", "-", "0"});
        } else {
            int rank = 1;
            for (User user : users) {
                model.addRow(new Object[]{
                        rank++,
                        user.getUserId(),
                        user.getName(),
                        user.getMembershipType().getDisplayName(),
                        user.getBorrowedBooks().size()
                });
            }
        }
    }

    private void generateOverdueBooksReport() {
        DefaultTableModel model = (DefaultTableModel) overdueBooksTable.getModel();
        model.setRowCount(0);

        java.util.List<BorrowRecord> records = libraryService.getOverdueBooks();
        if (records.isEmpty()) {
            model.addRow(new Object[]{"-", "No overdue books", "-", "-", "0", "0.00"});
        } else {
            for (BorrowRecord record : records) {
                double fine = libraryService.calculateFine(record);
                model.addRow(new Object[]{
                        record.getBook().getBookId(),
                        record.getBook().getTitle(),
                        record.getUser().getName(),
                        record.getDueDate(),
                        record.getOverdueDays(),
                        String.format("%.2f", fine)
                });
            }
        }
    }

    public void generateAllReports(boolean showNotification) {
        generateMostBorrowedReport();
        generateActiveUsersReport();
        generateOverdueBooksReport();

        // Reset search after generating new reports
        clearSearch();

        if (showNotification) {
            JOptionPane.showMessageDialog(this, "All reports generated successfully!");
        }
    }
}