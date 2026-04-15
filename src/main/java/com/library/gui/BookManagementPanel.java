package com.library.gui;

import com.library.services.LibraryService;
import com.library.models.Book;
import com.library.models.Review;
import com.library.models.User;
import com.library.enums.BookStatus;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.RowFilter;
import java.awt.*;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class BookManagementPanel extends JPanel {
    private LibraryService libraryService;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JTextField titleField, authorField, categoryField, isbnField;
    private JTextField bookIdField;
    private JLabel idLabel;
    private JTextField searchField;
    private JComboBox<BookStatus> statusComboBox;
    private JCheckBox featuredCheckBox, recommendedCheckBox, specialEditionCheckBox;
    private JTextField editionField, publisherField;
    private JSpinner publicationDateSpinner;
    private JPanel optionalFeaturesPanel;
    private JLabel showOptionalFeaturesLabel;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private String currentSearchText = "";
    private boolean optionalFeaturesVisible = false;

    // Extended metadata fields
    private JTextField tagsField, additionalAuthorsField;
    private JTextArea descriptionArea;
    private JTextField languageField, pageCountField;
    private JPanel ratingPanel;
    private JRadioButton[] ratingButtons;
    private ButtonGroup ratingGroup;
    private JButton coverImageButton;
    private JLabel coverImageLabel;
    private String coverImagePath = "";

    // Details panel
    private JPanel detailsPanel;
    private Map<String, Component> detailsPanelComponents;
    private boolean detailsVisible = false;

    public BookManagementPanel() {
        libraryService = LibraryService.getInstance();
        initializeUI();
        refreshTable();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        // Create search panel at the top
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Books"));

        JPanel searchInputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints searchGbc = new GridBagConstraints();
        searchGbc.insets = new Insets(5, 5, 5, 5);
        searchGbc.fill = GridBagConstraints.HORIZONTAL;

        // Search label
        searchGbc.gridx = 0;
        searchGbc.gridy = 0;
        searchGbc.weightx = 0.0;
        searchInputPanel.add(new JLabel("Search:"), searchGbc);

        // Search field
        searchGbc.gridx = 1;
        searchGbc.gridy = 0;
        searchGbc.weightx = 1.0;
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(400, 25));
        searchField.putClientProperty("JTextField.placeholderText", "Search books by ID, title, author, ISBN, or category...");
        searchInputPanel.add(searchField, searchGbc);

        // Search button
        searchGbc.gridx = 2;
        searchGbc.gridy = 0;
        searchGbc.weightx = 0.0;
        JButton searchButton = new JButton("🔍 Search");
        searchButton.setToolTipText("Search books");
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
                    highlightBooksInTable(searchText);
                } else {
                    clearBookHighlights();
                }
            }
        });

        // Search button action
        searchButton.addActionListener(e -> {
            String searchText = searchField.getText().trim();
            if (!searchText.isEmpty()) {
                currentSearchText = searchText.toLowerCase();
                highlightBooksInTable(searchText);
            }
        });

        // Clear button action
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            currentSearchText = "";
            clearBookHighlights();
            searchField.requestFocus();
        });

        // Table for displaying books
        String[] columns = {"Cover", "Book ID", "Title", "Author", "Category", "ISBN", "Status", "Features", "Edition", "Borrow History"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return ImageIcon.class;
                }
                return Object.class;
            }
        };

        bookTable = new JTable(tableModel);
        bookTable.setDefaultRenderer(Object.class, new HighlightCellRenderer());
        bookTable.setRowHeight(40);

        // Set custom renderer for cover column
        bookTable.getColumnModel().getColumn(0).setCellRenderer(new CoverImageRenderer());

        tableSorter = new TableRowSorter<>(tableModel);
        bookTable.setRowSorter(tableSorter);

        JScrollPane tableScrollPane = new JScrollPane(bookTable);
        tableScrollPane.setPreferredSize(new Dimension(0, 350));

        // Create a container panel for the middle section (table + details)
        JPanel middleSection = new JPanel(new BorderLayout(10, 10));
        middleSection.add(tableScrollPane, BorderLayout.CENTER);

        // Create details panel (initially hidden)
        detailsPanel = createDetailsPanel();
        detailsPanel.setVisible(false);
        middleSection.add(detailsPanel, BorderLayout.SOUTH);

        // Create book management section (always visible)
        JPanel bookManagementSection = createBookManagementSection();

        // Create main content panel
        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10));
        mainContentPanel.add(searchPanel, BorderLayout.NORTH);
        mainContentPanel.add(middleSection, BorderLayout.CENTER);
        mainContentPanel.add(bookManagementSection, BorderLayout.SOUTH);

        // Add components to main panel
        add(mainContentPanel);

        // Add selection listener to table
        bookTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && bookTable.getSelectedRow() != -1) {
                int modelRow = bookTable.convertRowIndexToModel(bookTable.getSelectedRow());
                String bookId = tableModel.getValueAt(modelRow, 1).toString();
                loadBookDetails(bookId);

                if (!detailsVisible) {
                    toggleDetailsPanel();
                }
            } else {
                if (detailsVisible) {
                    toggleDetailsPanel();
                }
            }
        });
    }

    private JPanel createBookManagementSection() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

        // Create the book management form
        JPanel bookManagementForm = createBookManagementForm();

        // Create scroll pane for the form
        JScrollPane formScrollPane = new JScrollPane(bookManagementForm);
        formScrollPane.setBorder(BorderFactory.createTitledBorder("Book Management"));
        formScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScrollPane.setPreferredSize(new Dimension(0, 300));

        mainPanel.add(formScrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createBookManagementForm() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Book ID field (for update) - Row 0
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Book ID (for update):"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        bookIdField = new JTextField();
        bookIdField.setEditable(false);
        bookIdField.setBackground(new Color(240, 240, 240));
        formPanel.add(bookIdField, gbc);

        // ID Label (for new books) - Row 1
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("New Book ID:"), gbc);

        gbc.gridx = 1;
        idLabel = new JLabel("(Auto-generated)");
        idLabel.setForeground(Color.BLUE);
        formPanel.add(idLabel, gbc);

        // Title field - Row 2
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Title *:"), gbc);

        gbc.gridx = 1;
        titleField = new JTextField();
        formPanel.add(titleField, gbc);

        // Author field - Row 3
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Author *:"), gbc);

        gbc.gridx = 1;
        authorField = new JTextField();
        formPanel.add(authorField, gbc);

        // Category field - Row 4
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Category *:"), gbc);

        gbc.gridx = 1;
        categoryField = new JTextField();
        formPanel.add(categoryField, gbc);

        // ISBN field - Row 5
        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(new JLabel("ISBN *:"), gbc);

        gbc.gridx = 1;
        isbnField = new JTextField();
        formPanel.add(isbnField, gbc);

        // Status field - Row 6
        gbc.gridx = 0;
        gbc.gridy = 6;
        formPanel.add(new JLabel("Status:"), gbc);

        gbc.gridx = 1;
        statusComboBox = new JComboBox<>(BookStatus.values());
        statusComboBox.setEnabled(false);
        formPanel.add(statusComboBox, gbc);

        // Optional Features Toggle - Row 7
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        showOptionalFeaturesLabel = new JLabel("▶ Show Optional Features & Metadata");
        showOptionalFeaturesLabel.setForeground(new Color(0, 102, 204));
        showOptionalFeaturesLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showOptionalFeaturesLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                toggleOptionalFeatures();
            }
        });
        formPanel.add(showOptionalFeaturesLabel, gbc);
        gbc.gridwidth = 1;

        // Optional Features Panel - Row 8 (initially hidden)
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        optionalFeaturesPanel = createOptionalFeaturesPanel();
        optionalFeaturesPanel.setVisible(false);
        formPanel.add(optionalFeaturesPanel, gbc);
        gbc.gridwidth = 1;

        // Add space above the separator
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        formPanel.add(Box.createVerticalStrut(20), gbc);
        gbc.gridwidth = 1;

        // Add a separator line
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(180, 180, 180));
        formPanel.add(separator, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Add a small spacer after the separator
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 2;
        formPanel.add(Box.createVerticalStrut(50), gbc);
        gbc.gridwidth = 1;

        // Action buttons - Row 10
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton addButton = new JButton("Add New Book");
        JButton updateButton = new JButton("Update Book");
        JButton deleteButton = new JButton("Delete Selected Book");
        JButton clearButton = new JButton("Clear Form");
        JButton refreshButton = new JButton("Refresh Table");
        JButton addReviewButton = new JButton("Add Review");

        // Set colored borders
        addButton.setBorder(new LineBorder(new Color(76, 175, 80), 2));
        updateButton.setBorder(new LineBorder(new Color(33, 150, 243), 2));
        deleteButton.setBorder(new LineBorder(new Color(244, 67, 54), 2));
        clearButton.setBorder(new LineBorder(new Color(158, 158, 158), 2));
        refreshButton.setBorder(new LineBorder(new Color(255, 193, 7), 2));
        addReviewButton.setBorder(new LineBorder(new Color(255, 152, 0), 2));

        addButton.addActionListener(e -> addBook());
        updateButton.addActionListener(e -> updateBook());
        deleteButton.addActionListener(e -> deleteBook());
        clearButton.addActionListener(e -> clearForm());
        refreshButton.addActionListener(e -> refreshTable());

        // Add Review button action
        addReviewButton.addActionListener(e -> {
            int selectedRow = bookTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this,
                        "Please select a book to add a review",
                        "No Selection",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String bookId = bookIdField.getText().trim();
            Book book = libraryService.getBook(bookId);
            if (book != null) {
                addReview(book);
            }
        });

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(addReviewButton);

        formPanel.add(buttonPanel, gbc);
        gbc.gridwidth = 1;

        // Add ActionListener to clear form
        clearButton.addActionListener(e -> {
            clearForm();
            idLabel.setText("(Auto-generated)");
            idLabel.setForeground(Color.BLUE);
            statusComboBox.setEnabled(false);

            if (!featuredCheckBox.isSelected() && !recommendedCheckBox.isSelected() &&
                    !specialEditionCheckBox.isSelected() && optionalFeaturesVisible) {
                toggleOptionalFeatures();
            }

            if (detailsVisible) {
                toggleDetailsPanel();
            }
        });

        return formPanel;
    }

    private JPanel createOptionalFeaturesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Extended Features & Metadata"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Feature checkboxes - Row 0
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        featuredCheckBox = new JCheckBox("⭐ Featured");
        recommendedCheckBox = new JCheckBox("👍 Recommended");
        specialEditionCheckBox = new JCheckBox("🌟 Special Edition");
        checkboxPanel.add(featuredCheckBox);
        checkboxPanel.add(recommendedCheckBox);
        checkboxPanel.add(specialEditionCheckBox);
        panel.add(checkboxPanel, gbc);
        gbc.gridwidth = 1;
        row++;

        // Edition field - Row 1
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Edition:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        editionField = new JTextField();
        editionField.putClientProperty("JTextField.placeholderText", "Optional edition number");
        panel.add(editionField, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        row++;

        // Publisher field - Row 2
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Publisher:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        publisherField = new JTextField();
        publisherField.putClientProperty("JTextField.placeholderText", "Optional publisher name");
        panel.add(publisherField, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        row++;

        // Publication date - Row 3
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Publication Date:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        publicationDateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(publicationDateSpinner, "yyyy-MM-dd");
        publicationDateSpinner.setEditor(dateEditor);
        panel.add(publicationDateSpinner, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        row++;

        // Separator
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        panel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;
        row++;

        // Cover Image Upload - Row 5
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Cover Image:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        coverImageButton = new JButton("📷 Upload Image");
        coverImageButton.addActionListener(e -> uploadCoverImage());
        panel.add(coverImageButton, gbc);

        gbc.gridx = 2;
        coverImageLabel = new JLabel("No image selected");
        coverImageLabel.setForeground(Color.GRAY);
        panel.add(coverImageLabel, gbc);
        row++;

        // Tags - Row 6
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Tags:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        tagsField = new JTextField();
        tagsField.putClientProperty("JTextField.placeholderText", "Comma-separated tags (fiction,sci-fi,bestseller)");
        panel.add(tagsField, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        row++;

        // Additional Authors - Row 7
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Additional Authors:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        additionalAuthorsField = new JTextField();
        additionalAuthorsField.putClientProperty("JTextField.placeholderText", "Comma-separated additional authors");
        panel.add(additionalAuthorsField, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        row++;

        // Description - Row 8
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Description:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = 2;
        descriptionArea = new JTextArea(2, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        panel.add(descriptionScroll, gbc);
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        row += 2;

        // Language - Row 10
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Language:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        languageField = new JTextField();
        languageField.putClientProperty("JTextField.placeholderText", "e.g., English, Spanish, French");
        panel.add(languageField, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        row++;

        // Page Count - Row 11
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Page Count:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        pageCountField = new JTextField();
        pageCountField.putClientProperty("JTextField.placeholderText", "Number of pages");
        panel.add(pageCountField, gbc);
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        row++;

        // Rating - Row 12
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Rating:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        ratingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ratingGroup = new ButtonGroup();
        ratingButtons = new JRadioButton[6];

        // Add "No rating" option
        JRadioButton noRatingButton = new JRadioButton("None");
        noRatingButton.setSelected(true);
        ratingPanel.add(noRatingButton);
        ratingGroup.add(noRatingButton);
        ratingButtons[0] = noRatingButton;

        // Add star rating buttons
        for (int i = 1; i <= 5; i++) {
            String stars = "★".repeat(i);
            JRadioButton starButton = new JRadioButton(
                    "<html><span style='color:gold;'>" + stars + "</span> (" + i + ")</html>"
            );

            ratingPanel.add(starButton);
            ratingGroup.add(starButton);
            ratingButtons[i] = starButton;
        }

        panel.add(ratingPanel, gbc);

        return panel;
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("📚 Book Profile"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        panel.setPreferredSize(new Dimension(0, 500));

        // Create a main container with 2 sections: book info and reviews
        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));

        // Add close button in the top right corner
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel("Book Profile");
        headerLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        headerPanel.add(headerLabel, BorderLayout.WEST);

        JButton closeButton = new JButton("✕ Close");
        closeButton.setFont(new Font("Dialog", Font.PLAIN, 11));
        closeButton.setForeground(Color.RED);
        closeButton.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        closeButton.addActionListener(e -> toggleDetailsPanel());
        headerPanel.add(closeButton, BorderLayout.EAST);

        mainContainer.add(headerPanel);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 5)));

        // BOOK INFO SECTION (3 columns)
        JPanel bookInfoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;

        // LEFT column - Cover image and basic info (ALIGNED LEFT) - HALF THE ORIGINAL SIZE
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 15));

        // Cover image (aligned left)
        JLabel coverLabel = new JLabel("", JLabel.LEFT);
        coverLabel.setPreferredSize(new Dimension(150, 200));
        coverLabel.setMinimumSize(new Dimension(150, 200));
        coverLabel.setMaximumSize(new Dimension(150, 200));
        coverLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        coverLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(coverLabel);

        leftPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Book title under cover (aligned left)
        JLabel titleLabel = new JLabel("", JLabel.LEFT);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(titleLabel);

        leftPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Author under cover (aligned left)
        JLabel authorLabel = new JLabel("", JLabel.LEFT);
        authorLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        authorLabel.setForeground(new Color(100, 100, 100));
        authorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(authorLabel);

        leftPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // Availability status under cover (aligned left)
        JLabel statusLabel = new JLabel("", JLabel.LEFT);
        statusLabel.setFont(new Font("Dialog", Font.BOLD, 11));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(statusLabel);

        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Book ID under cover (aligned left)
        JLabel idLabel = new JLabel("", JLabel.LEFT);
        idLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        idLabel.setForeground(new Color(150, 150, 150));
        idLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(idLabel);

        // Add spacer to push content to top
        leftPanel.add(Box.createVerticalGlue());

        // Add left panel to card - HALF THE ORIGINAL WIDTH (0.125 instead of 0.25)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.125; // Changed from 0.25 to 0.125 (half the size)
        gbc.weighty = 1.0;
        gbc.gridheight = 1;
        bookInfoPanel.add(leftPanel, gbc);

        // MIDDLE column - Detailed information - INCREASED SIZE
        JPanel middlePanel = new JPanel();
        middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.Y_AXIS));
        middlePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 15));

        // Top row - Category and Rating
        JPanel topRow = new JPanel(new BorderLayout());

        // Category
        JPanel categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        categoryPanel.add(new JLabel("📂 Category: "));
        JLabel categoryValue = new JLabel();
        categoryValue.setFont(new Font("Dialog", Font.BOLD, 12));
        categoryPanel.add(categoryValue);
        topRow.add(categoryPanel, BorderLayout.WEST);

        // Rating
        JPanel ratingPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        ratingPanel.add(new JLabel("⭐ Rating: "));
        JLabel ratingValue = new JLabel();
        ratingValue.setFont(new Font("Dialog", Font.BOLD, 12));
        ratingPanel.add(ratingValue);
        topRow.add(ratingPanel, BorderLayout.EAST);

        middlePanel.add(topRow);
        middlePanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Details grid (2 columns)
        JPanel detailsGrid = new JPanel(new GridBagLayout());
        GridBagConstraints dgbc = new GridBagConstraints();
        dgbc.insets = new Insets(3, 5, 3, 15);
        dgbc.anchor = GridBagConstraints.WEST;
        dgbc.fill = GridBagConstraints.HORIZONTAL;

        // ISBN
        dgbc.gridx = 0; dgbc.gridy = 0; dgbc.weightx = 0.0;
        detailsGrid.add(new JLabel("📋 ISBN:"), dgbc);
        dgbc.gridx = 1; dgbc.weightx = 1.0;
        JLabel isbnValue = new JLabel();
        detailsGrid.add(isbnValue, dgbc);

        // Edition
        dgbc.gridx = 0; dgbc.gridy = 1; dgbc.weightx = 0.0;
        detailsGrid.add(new JLabel("📖 Edition:"), dgbc);
        dgbc.gridx = 1; dgbc.weightx = 1.0;
        JLabel editionValue = new JLabel();
        detailsGrid.add(editionValue, dgbc);

        // Publisher
        dgbc.gridx = 0; dgbc.gridy = 2; dgbc.weightx = 0.0;
        detailsGrid.add(new JLabel("🏢 Publisher:"), dgbc);
        dgbc.gridx = 1; dgbc.weightx = 1.0;
        JLabel publisherValue = new JLabel();
        detailsGrid.add(publisherValue, dgbc);

        // Publication Date
        dgbc.gridx = 0; dgbc.gridy = 3; dgbc.weightx = 0.0;
        detailsGrid.add(new JLabel("📅 Pub Date:"), dgbc);
        dgbc.gridx = 1; dgbc.weightx = 1.0;
        JLabel pubDateValue = new JLabel();
        detailsGrid.add(pubDateValue, dgbc);

        // Language
        dgbc.gridx = 0; dgbc.gridy = 4; dgbc.weightx = 0.0;
        detailsGrid.add(new JLabel("🌐 Language:"), dgbc);
        dgbc.gridx = 1; dgbc.weightx = 1.0;
        JLabel languageValue = new JLabel();
        detailsGrid.add(languageValue, dgbc);

        // Page Count
        dgbc.gridx = 0; dgbc.gridy = 5; dgbc.weightx = 0.0;
        detailsGrid.add(new JLabel("📄 Pages:"), dgbc);
        dgbc.gridx = 1; dgbc.weightx = 1.0;
        JLabel pageCountValue = new JLabel();
        detailsGrid.add(pageCountValue, dgbc);

        // Borrow History
        dgbc.gridx = 0; dgbc.gridy = 6; dgbc.weightx = 0.0;
        detailsGrid.add(new JLabel("📈 Borrows:"), dgbc);
        dgbc.gridx = 1; dgbc.weightx = 1.0;
        JLabel borrowHistoryValue = new JLabel();
        detailsGrid.add(borrowHistoryValue, dgbc);

        // Reviews count
        dgbc.gridx = 0; dgbc.gridy = 7; dgbc.weightx = 0.0;
        detailsGrid.add(new JLabel("💬 Reviews:"), dgbc);
        dgbc.gridx = 1; dgbc.weightx = 1.0;
        JLabel reviewsCountValue = new JLabel();
        detailsGrid.add(reviewsCountValue, dgbc);

        middlePanel.add(detailsGrid);
        middlePanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Description area
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.setBorder(BorderFactory.createTitledBorder("📝 Description"));
        JTextArea descriptionArea = new JTextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBackground(new Color(248, 249, 250));
        descriptionArea.setFont(new Font("Dialog", Font.PLAIN, 11));
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        descriptionScroll.setPreferredSize(new Dimension(0, 150));
        descriptionPanel.add(descriptionScroll, BorderLayout.CENTER);
        middlePanel.add(descriptionPanel);

        // Add middle panel to card - INCREASED SIZE
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.4375; 
        gbc.weighty = 1.0;
        gbc.gridheight = 1;
        bookInfoPanel.add(middlePanel, gbc);

        // RIGHT column - Features, Tags, and Additional Authors - INCREASED SIZE
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Features Section - NO SCROLLBAR
        JPanel featuresPanel = new JPanel(new BorderLayout());
        featuresPanel.setBorder(BorderFactory.createTitledBorder("✨ Special Features"));
        JPanel featuresContent = new JPanel();
        featuresContent.setLayout(new BoxLayout(featuresContent, BoxLayout.Y_AXIS));
        featuresContent.setBackground(new Color(248, 249, 250));
        featuresContent.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Feature labels
        JLabel featuredLabel = new JLabel("⭐ Featured Book");
        JLabel recommendedLabel = new JLabel("👍 Recommended");
        JLabel specialEditionLabel = new JLabel("🌟 Special Edition");

        featuredLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        recommendedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        specialEditionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        featuresContent.add(featuredLabel);
        featuresContent.add(Box.createRigidArea(new Dimension(0, 8)));
        featuresContent.add(recommendedLabel);
        featuresContent.add(Box.createRigidArea(new Dimension(0, 8)));
        featuresContent.add(specialEditionLabel);

        featuresPanel.add(featuresContent, BorderLayout.NORTH);
        rightPanel.add(featuresPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Tags Section - NO SCROLLBAR, EXPANDS LIKE FEATURES
        JPanel tagsPanelContainer = new JPanel(new BorderLayout());
        tagsPanelContainer.setBorder(BorderFactory.createTitledBorder("🏷️ Tags"));
        JPanel tagsPanel = new JPanel();
        tagsPanel.setLayout(new BoxLayout(tagsPanel, BoxLayout.Y_AXIS));
        tagsPanel.setBackground(new Color(248, 249, 250));
        tagsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Tags will be added dynamically
        JLabel tagsPlaceholder = new JLabel("No tags added");
        tagsPlaceholder.setForeground(Color.GRAY);
        tagsPlaceholder.setFont(new Font("Dialog", Font.ITALIC, 11));
        tagsPlaceholder.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagsPanel.add(tagsPlaceholder);

        tagsPanelContainer.add(tagsPanel, BorderLayout.CENTER);
        rightPanel.add(tagsPanelContainer);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Additional Authors Section - NO SCROLLBAR, EXPANDS LIKE FEATURES
        JPanel authorsPanel = new JPanel(new BorderLayout());
        authorsPanel.setBorder(BorderFactory.createTitledBorder("👥 Additional Authors"));
        JPanel authorsContent = new JPanel();
        authorsContent.setLayout(new BoxLayout(authorsContent, BoxLayout.Y_AXIS));
        authorsContent.setBackground(new Color(248, 249, 250));
        authorsContent.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Authors will be added dynamically as tags
        JLabel authorsPlaceholder = new JLabel("No additional authors");
        authorsPlaceholder.setForeground(Color.GRAY);
        authorsPlaceholder.setFont(new Font("Dialog", Font.ITALIC, 11));
        authorsPlaceholder.setAlignmentX(Component.LEFT_ALIGNMENT);
        authorsContent.add(authorsPlaceholder);

        authorsPanel.add(authorsContent, BorderLayout.CENTER);
        rightPanel.add(authorsPanel);

        // Add right panel to card - INCREASED SIZE
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.4375; 
        gbc.weighty = 1.0;
        gbc.gridheight = 1;
        bookInfoPanel.add(rightPanel, gbc);

        // Add book info panel to main container
        mainContainer.add(bookInfoPanel);

        // Add separator between book info and reviews
        mainContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        mainContainer.add(new JSeparator());
        mainContainer.add(Box.createRigidArea(new Dimension(0, 10)));

        // REVIEWS SECTION - BELOW ALL 3 COLUMNS (Show all reviews one below another)
        JPanel reviewsPanel = new JPanel(new BorderLayout(5, 5));
        reviewsPanel.setBorder(BorderFactory.createTitledBorder("📝 Reviews"));

        JPanel reviewsContainer = new JPanel();
        reviewsContainer.setLayout(new BoxLayout(reviewsContainer, BoxLayout.Y_AXIS));
        reviewsContainer.setBackground(new Color(248, 249, 250));
        reviewsContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Default message
        JLabel noReviewsLabel = new JLabel("No reviews yet. Be the first to review this book!");
        noReviewsLabel.setForeground(Color.GRAY);
        noReviewsLabel.setFont(new Font("Dialog", Font.ITALIC, 11));
        noReviewsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        reviewsContainer.add(noReviewsLabel);

        reviewsPanel.add(reviewsContainer, BorderLayout.CENTER);

        // Set preferred size for reviews panel
        reviewsPanel.setPreferredSize(new Dimension(0, 200));
        reviewsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        mainContainer.add(reviewsPanel);

        // Store references for updating
        detailsPanelComponents = new HashMap<>();
        detailsPanelComponents.put("coverLabel", coverLabel);
        detailsPanelComponents.put("titleLabel", titleLabel);
        detailsPanelComponents.put("authorLabel", authorLabel);
        detailsPanelComponents.put("statusLabel", statusLabel);
        detailsPanelComponents.put("idLabel", idLabel);
        detailsPanelComponents.put("categoryValue", categoryValue);
        detailsPanelComponents.put("ratingValue", ratingValue);
        detailsPanelComponents.put("isbnValue", isbnValue);
        detailsPanelComponents.put("editionValue", editionValue);
        detailsPanelComponents.put("publisherValue", publisherValue);
        detailsPanelComponents.put("pubDateValue", pubDateValue);
        detailsPanelComponents.put("languageValue", languageValue);
        detailsPanelComponents.put("pageCountValue", pageCountValue);
        detailsPanelComponents.put("borrowHistoryValue", borrowHistoryValue);
        detailsPanelComponents.put("reviewsCountValue", reviewsCountValue);
        detailsPanelComponents.put("descriptionArea", descriptionArea);
        detailsPanelComponents.put("featuredLabel", featuredLabel);
        detailsPanelComponents.put("recommendedLabel", recommendedLabel);
        detailsPanelComponents.put("specialEditionLabel", specialEditionLabel);
        detailsPanelComponents.put("tagsPanel", tagsPanel);
        detailsPanelComponents.put("tagsPlaceholder", tagsPlaceholder);
        detailsPanelComponents.put("authorsContent", authorsContent);
        detailsPanelComponents.put("authorsPlaceholder", authorsPlaceholder);
        detailsPanelComponents.put("reviewsContainer", reviewsContainer);
        detailsPanelComponents.put("noReviewsLabel", noReviewsLabel);
        detailsPanelComponents.put("closeButton", closeButton);

        // Put everything in a scroll pane so everything is visible
        JScrollPane scrollPane = new JScrollPane(mainContainer);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void toggleOptionalFeatures() {
        optionalFeaturesVisible = !optionalFeaturesVisible;
        optionalFeaturesPanel.setVisible(optionalFeaturesVisible);
        showOptionalFeaturesLabel.setText(optionalFeaturesVisible ?
                "▼ Hide Extended Features & Metadata" : "▶ Show Optional Features & Metadata");

        optionalFeaturesPanel.getParent().revalidate();
        optionalFeaturesPanel.getParent().repaint();
    }

    private void toggleDetailsPanel() {
        detailsVisible = !detailsVisible;
        detailsPanel.setVisible(detailsVisible);

        // Clear selection when closing
        if (!detailsVisible) {
            bookTable.clearSelection();
            clearForm();
        }

        // Revalidate and repaint the parent container
        detailsPanel.getParent().revalidate();
        detailsPanel.getParent().repaint();
    }

    private void uploadCoverImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Book Cover Image");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image files",
                ImageIO.getReaderFileSuffixes()));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            coverImagePath = selectedFile.getAbsolutePath();
            coverImageLabel.setText(selectedFile.getName());
            coverImageLabel.setForeground(Color.BLUE);

            // Show preview
            try {
                ImageIcon icon = new ImageIcon(coverImagePath);
                Image scaledImage = icon.getImage().getScaledInstance(100, 150, Image.SCALE_SMOOTH);
                coverImageLabel.setIcon(new ImageIcon(scaledImage));
                coverImageLabel.setText("");
            } catch (Exception e) {
                coverImageLabel.setText("Preview not available");
                coverImageLabel.setIcon(null);
            }
        }
    }

    private void loadBookDetails(String bookId) {
        Book book = libraryService.getBook(bookId);
        if (book == null) return;

        // Load basic fields
        bookIdField.setText(book.getBookId());
        titleField.setText(book.getTitle());
        authorField.setText(book.getAuthor());
        categoryField.setText(book.getCategory());
        isbnField.setText(book.getIsbn());
        statusComboBox.setSelectedItem(book.getStatus());

        // Load optional features
        featuredCheckBox.setSelected(book.isFeatured());
        recommendedCheckBox.setSelected(book.isRecommended());
        specialEditionCheckBox.setSelected(book.isSpecialEdition());

        editionField.setText(book.getEdition() > 0 ? String.valueOf(book.getEdition()) : "");
        publisherField.setText(book.getPublisher() != null ? book.getPublisher() : "");
        if (book.getPublicationDate() != null) {
            publicationDateSpinner.setValue(book.getPublicationDate());
        } else {
            publicationDateSpinner.setValue(new Date());
        }

        // Load extended metadata
        coverImagePath = book.getCoverImagePath();
        if (!coverImagePath.isEmpty()) {
            File imageFile = new File(coverImagePath);
            if (imageFile.exists()) {
                coverImageLabel.setText(imageFile.getName());
                coverImageLabel.setForeground(Color.BLUE);
                try {
                    ImageIcon icon = new ImageIcon(coverImagePath);
                    Image scaledImage = icon.getImage().getScaledInstance(100, 150, Image.SCALE_SMOOTH);
                    coverImageLabel.setIcon(new ImageIcon(scaledImage));
                    coverImageLabel.setText("");
                } catch (Exception e) {
                    coverImageLabel.setText("Preview not available");
                    coverImageLabel.setIcon(null);
                }
            } else {
                coverImageLabel.setText("Image not found");
                coverImageLabel.setIcon(null);
                coverImageLabel.setForeground(Color.RED);
            }
        } else {
            coverImageLabel.setText("No image selected");
            coverImageLabel.setIcon(null);
            coverImageLabel.setForeground(Color.GRAY);
        }

        tagsField.setText(String.join(", ", book.getTags()));
        additionalAuthorsField.setText(String.join(", ", book.getAdditionalAuthors()));
        descriptionArea.setText(book.getDescription());
        languageField.setText(book.getLanguage());
        pageCountField.setText(book.getPageCount() > 0 ? String.valueOf(book.getPageCount()) : "");

        // Set rating radio button
        int rating = (int) Math.round(book.getRating());
        if (rating >= 0 && rating <= 5) {
            ratingButtons[rating].setSelected(true);
        }

        // Update ID label
        idLabel.setText("(Select 'Clear Form' to add new book)");
        idLabel.setForeground(Color.RED);
        statusComboBox.setEnabled(true);

        // Update details panel
        updateDetailsPanel(book);
    }

    private void updateDetailsPanel(Book book) {
        if (detailsPanelComponents == null) return;

        // Update cover image
        JLabel coverLabel = (JLabel) detailsPanelComponents.get("coverLabel");
        if (!book.getCoverImagePath().isEmpty()) {
            try {
                File imageFile = new File(book.getCoverImagePath());
                if (imageFile.exists()) {
                    ImageIcon icon = new ImageIcon(book.getCoverImagePath());
                    Image scaledImage = icon.getImage().getScaledInstance(140, 190, Image.SCALE_SMOOTH);
                    coverLabel.setIcon(new ImageIcon(scaledImage));
                    coverLabel.setText("");
                } else {
                    coverLabel.setIcon(null);
                    coverLabel.setText("No Cover");
                    coverLabel.setForeground(Color.GRAY);
                }
            } catch (Exception e) {
                coverLabel.setIcon(null);
                coverLabel.setText("No Cover");
                coverLabel.setForeground(Color.GRAY);
            }
        } else {
            coverLabel.setIcon(null);
            coverLabel.setText("No Cover");
            coverLabel.setForeground(Color.GRAY);
        }

        // Update title, author, and status under cover (ALIGNED LEFT)
        ((JLabel) detailsPanelComponents.get("titleLabel")).setText("<html>" +
                book.getTitle() + "</html>");
        ((JLabel) detailsPanelComponents.get("authorLabel")).setText("by " + book.getAuthor());

        // Update status with colored indicator
        JLabel statusLabel = (JLabel) detailsPanelComponents.get("statusLabel");
        BookStatus status = book.getStatus();
        statusLabel.setText("Status: " + status.getDisplayName());

        // Color code the status
        switch (status) {
            case AVAILABLE:
                statusLabel.setForeground(new Color(0, 128, 0)); // Green
                break;
            case BORROWED:
                statusLabel.setForeground(new Color(220, 20, 60)); // Crimson red
                break;
            case RESERVED:
                statusLabel.setForeground(new Color(255, 140, 0)); // Dark orange
                break;
            case BORROWED_RESERVED:
                statusLabel.setForeground(new Color(148, 0, 211)); // Dark violet
                break;
            default:
                statusLabel.setForeground(Color.BLACK);
        }

        ((JLabel) detailsPanelComponents.get("idLabel")).setText("ID: " + book.getBookId());

        // Update middle column details
        ((JLabel) detailsPanelComponents.get("categoryValue")).setText(book.getCategory());
        ((JLabel) detailsPanelComponents.get("ratingValue")).setText(String.format("%.1f/5.0",
                book.calculateAverageRating()));
        ((JLabel) detailsPanelComponents.get("isbnValue")).setText(book.getIsbn());
        ((JLabel) detailsPanelComponents.get("editionValue")).setText(book.getEdition() > 0 ?
                "Edition " + book.getEdition() : "First Edition");
        ((JLabel) detailsPanelComponents.get("publisherValue")).setText(
                book.getPublisher() != null && !book.getPublisher().isEmpty() ? book.getPublisher() : "Not specified");
        ((JLabel) detailsPanelComponents.get("pubDateValue")).setText(
                book.getPublicationDate() != null ?
                        new java.text.SimpleDateFormat("MMM dd, yyyy").format(book.getPublicationDate()) : "Not specified");
        ((JLabel) detailsPanelComponents.get("languageValue")).setText(book.getLanguage());
        ((JLabel) detailsPanelComponents.get("pageCountValue")).setText(
                book.getPageCount() > 0 ? book.getPageCount() + " pages" : "Not specified");

        // Update borrow history and reviews count
        ((JLabel) detailsPanelComponents.get("borrowHistoryValue")).setText(
                book.getBorrowHistory().size() + " time" + (book.getBorrowHistory().size() != 1 ? "s" : ""));
        ((JLabel) detailsPanelComponents.get("reviewsCountValue")).setText(
                book.getReviews().size() + " review" + (book.getReviews().size() != 1 ? "s" : ""));

        // Update description
        JTextArea descriptionArea = (JTextArea) detailsPanelComponents.get("descriptionArea");
        if (!book.getDescription().isEmpty()) {
            descriptionArea.setText(book.getDescription());
            descriptionArea.setForeground(Color.BLACK);
        } else {
            descriptionArea.setText("No description available");
            descriptionArea.setForeground(Color.GRAY);
        }

        // Update right column - Features with labels (black text and blue border when selected)
        JLabel featuredLabel = (JLabel) detailsPanelComponents.get("featuredLabel");
        JLabel recommendedLabel = (JLabel) detailsPanelComponents.get("recommendedLabel");
        JLabel specialEditionLabel = (JLabel) detailsPanelComponents.get("specialEditionLabel");

        // Style the labels with blue border and black text when selected
        if (book.isFeatured()) {
            featuredLabel.setForeground(Color.BLACK); // Black text
            featuredLabel.setFont(new Font("Dialog", Font.BOLD, 12));
            featuredLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(33, 150, 243), 2), // Blue border
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
            featuredLabel.setOpaque(false); // No background
        } else {
            featuredLabel.setForeground(new Color(150, 150, 150)); // Light gray when not selected
            featuredLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
            featuredLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8)); // No border
            featuredLabel.setOpaque(false);
        }

        if (book.isRecommended()) {
            recommendedLabel.setForeground(Color.BLACK); // Black text
            recommendedLabel.setFont(new Font("Dialog", Font.BOLD, 12));
            recommendedLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(33, 150, 243), 2), // Blue border
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
            recommendedLabel.setOpaque(false); // No background
        } else {
            recommendedLabel.setForeground(new Color(150, 150, 150)); // Light gray when not selected
            recommendedLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
            recommendedLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8)); // No border
            recommendedLabel.setOpaque(false);
        }

        if (book.isSpecialEdition()) {
            specialEditionLabel.setForeground(Color.BLACK); // Black text
            specialEditionLabel.setFont(new Font("Dialog", Font.BOLD, 12));
            specialEditionLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(33, 150, 243), 2), // Blue border
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
            specialEditionLabel.setOpaque(false); // No background
        } else {
            specialEditionLabel.setForeground(new Color(150, 150, 150)); // Light gray when not selected
            specialEditionLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
            specialEditionLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8)); // No border
            specialEditionLabel.setOpaque(false);
        }

        // Update right column - Tags (NO SCROLLBAR)
        JPanel tagsPanel = (JPanel) detailsPanelComponents.get("tagsPanel");
        JLabel tagsPlaceholder = (JLabel) detailsPanelComponents.get("tagsPlaceholder");
        tagsPanel.removeAll();

        // Add tags as badges
        for (String tag : book.getTags()) {
            JPanel tagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            tagRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            tagRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            JLabel tagLabel = new JLabel(tag);
            tagLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
            tagLabel.setBackground(new Color(240, 240, 240));
            tagLabel.setOpaque(true);
            tagLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
            tagRow.add(tagLabel);

            tagsPanel.add(tagRow);
        }

        if (book.getTags().isEmpty()) {
            tagsPanel.add(tagsPlaceholder);
        }

        tagsPanel.revalidate();
        tagsPanel.repaint();

        // Update right column - Additional Authors as Tags (NO SCROLLBAR)
        JPanel authorsContent = (JPanel) detailsPanelComponents.get("authorsContent");
        JLabel authorsPlaceholder = (JLabel) detailsPanelComponents.get("authorsPlaceholder");
        authorsContent.removeAll();

        // Add additional authors as badges
        for (String author : book.getAdditionalAuthors()) {
            JPanel authorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            authorRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            authorRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            JLabel authorLabel = new JLabel("👤 " + author);
            authorLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 220, 240), 1),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
            authorLabel.setBackground(new Color(240, 245, 255));
            authorLabel.setOpaque(true);
            authorLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
            authorRow.add(authorLabel);

            authorsContent.add(authorRow);
        }

        if (book.getAdditionalAuthors().isEmpty()) {
            authorsContent.add(authorsPlaceholder);
        }

        authorsContent.revalidate();
        authorsContent.repaint();

        // Update reviews section - Show ALL reviews one below another
        JPanel reviewsContainer = (JPanel) detailsPanelComponents.get("reviewsContainer");
        JLabel noReviewsLabel = (JLabel) detailsPanelComponents.get("noReviewsLabel");

        reviewsContainer.removeAll();

        List<Review> reviews = book.getReviews();
        if (!reviews.isEmpty()) {
            // Show ALL reviews one below another
            for (int i = 0; i < reviews.size(); i++) {
                Review review = reviews.get(i);
                JPanel reviewPanel = createReviewPanel(review);
                reviewPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                reviewsContainer.add(reviewPanel);

                // Add separator between reviews (except after the last one)
                if (i < reviews.size() - 1) {
                    reviewsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
                }
            }
        } else {
            reviewsContainer.add(noReviewsLabel);
        }

        reviewsContainer.revalidate();
        reviewsContainer.repaint();

        // Revalidate and repaint the panel
        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    // Custom cell renderers
    private class HighlightCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (column == 0) {
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!currentSearchText.isEmpty() && value != null) {
                String cellText = value.toString();
                String lowerCellText = cellText.toLowerCase();

                if (lowerCellText.contains(currentSearchText)) {
                    c.setBackground(new Color(255, 255, 200));

                    if (c instanceof JLabel) {
                        JLabel label = (JLabel) c;
                        String highlightedText = highlightText(cellText, currentSearchText);
                        label.setText("<html>" + highlightedText + "</html>");
                    }
                } else {
                    int modelRow = table.convertRowIndexToModel(row);
                    boolean rowMatches = false;
                    for (int col = 0; col < table.getColumnCount(); col++) {
                        if (col == 0) continue;
                        Object cellValue = tableModel.getValueAt(modelRow, col);
                        if (cellValue != null && cellValue.toString().toLowerCase().contains(currentSearchText)) {
                            rowMatches = true;
                            break;
                        }
                    }

                    if (rowMatches) {
                        c.setBackground(new Color(255, 255, 200));
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

            text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            String lowerText = text.toLowerCase();
            String lowerSearch = search.toLowerCase();

            StringBuilder result = new StringBuilder();
            int lastIndex = 0;
            int index = lowerText.indexOf(lowerSearch);

            while (index >= 0) {
                result.append(text.substring(lastIndex, index));
                result.append("<span style='background-color: #FFD700; font-weight: bold; color: #000000;'>");
                result.append(text.substring(index, index + search.length()));
                result.append("</span>");
                lastIndex = index + search.length();
                index = lowerText.indexOf(lowerSearch, lastIndex);
            }

            result.append(text.substring(lastIndex));
            return result.toString();
        }
    }

    private class CoverImageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof ImageIcon) {
                label.setIcon((ImageIcon) value);
                label.setText("");
                label.setHorizontalAlignment(JLabel.CENTER);
            } else {
                label.setIcon(null);
                label.setText("📚");
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setFont(new Font("Arial", Font.PLAIN, 20));
            }

            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
            } else {
                label.setBackground(table.getBackground());
                label.setForeground(table.getForeground());
            }

            label.setOpaque(true);
            return label;
        }
    }

    private void highlightBooksInTable(String searchText) {
        if (searchText.trim().isEmpty()) {
            clearBookHighlights();
            return;
        }

        String lowerSearch = searchText.toLowerCase();

        tableSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                for (int i = 1; i < entry.getValueCount(); i++) {
                    Object value = entry.getValue(i);
                    if (value != null && value.toString().toLowerCase().contains(lowerSearch)) {
                        return true;
                    }
                }
                return false;
            }
        });

        bookTable.repaint();
    }

    private void clearBookHighlights() {
        tableSorter.setRowFilter(null);
        currentSearchText = "";
        bookTable.repaint();
    }

    private void addBook() {
        try {
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String category = categoryField.getText().trim();
            String isbn = isbnField.getText().trim();

            if (title.isEmpty() || author.isEmpty() || category.isEmpty() || isbn.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please fill in all required fields (*)",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get optional features
            boolean featured = featuredCheckBox.isSelected();
            boolean recommended = recommendedCheckBox.isSelected();
            boolean specialEdition = specialEditionCheckBox.isSelected();

            // Get edition, publisher, and publication date
            int edition = 0;
            try {
                String editionText = editionField.getText().trim();
                if (!editionText.isEmpty()) {
                    edition = Integer.parseInt(editionText);
                    if (edition < 0) {
                        JOptionPane.showMessageDialog(this,
                                "Edition number must be positive",
                                "Validation Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a valid edition number or leave it empty",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String publisher = publisherField.getText().trim();
            Date publicationDate = null;
            Date selectedDate = (Date) publicationDateSpinner.getValue();
            if (selectedDate != null) {
                publicationDate = selectedDate;
            }

            // Create book
            Book book = libraryService.addBook(title, author, category, isbn,
                    featured, recommended, specialEdition,
                    edition, publisher, publicationDate);

            // Set extended metadata
            setExtendedMetadata(book);

            refreshTable();

            // Show success message
            StringBuilder message = new StringBuilder();
            message.append("✅ Book added successfully!\n\n");
            message.append("📋 Book Details:\n");
            message.append("• Book ID: ").append(book.getBookId()).append("\n");
            message.append("• Title: ").append(book.getTitle()).append("\n");
            message.append("• Author: ").append(book.getAuthor()).append("\n");
            message.append("• Category: ").append(book.getCategory()).append("\n");
            message.append("• ISBN: ").append(book.getIsbn()).append("\n");
            message.append("• Status: ").append(book.getStatus().getDisplayName()).append("\n");

            // Show optional features if they exist
            if (book.getEdition() > 0) {
                message.append("• Edition: ").append(book.getEdition()).append("\n");
            }
            if (book.getPublisher() != null && !book.getPublisher().isEmpty()) {
                message.append("• Publisher: ").append(book.getPublisher()).append("\n");
            }
            if (book.isSpecialEdition()) {
                message.append("• Special Edition: 🌟 Yes\n");
            }
            if (book.isFeatured()) {
                message.append("• Featured: ⭐ Yes\n");
            }
            if (book.isRecommended()) {
                message.append("• Recommended: 👍 Yes\n");
            }

            // Show extended metadata if any exists
            if (!book.getDescription().isEmpty() ||
                    !book.getTags().isEmpty() ||
                    book.getRating() > 0) {
                message.append("\n【 Extended Metadata 】\n");
                if (!book.getDescription().isEmpty()) {
                    message.append("• Description: Added\n");
                }
                if (!book.getTags().isEmpty()) {
                    message.append("• Tags: ").append(book.getTags().size()).append(" tags added\n");
                }
                if (book.getRating() > 0) {
                    message.append("• Rating: ").append(book.getRating()).append("/5\n");
                }
                if (!book.getCoverImagePath().isEmpty()) {
                    message.append("• Cover Image: Uploaded\n");
                }
            }

            message.append("\nThe book is now available in the library system.");

            JOptionPane.showMessageDialog(this,
                    message.toString(),
                    "Book Added Successfully",
                    JOptionPane.INFORMATION_MESSAGE);

            clearForm();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error adding book: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void setExtendedMetadata(Book book) {
        // Set cover image path
        book.setCoverImagePath(coverImagePath);

        // Set tags
        String tagsText = tagsField.getText().trim();
        if (!tagsText.isEmpty()) {
            String[] tags = tagsText.split(",");
            for (String tag : tags) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    book.addTag(trimmedTag);
                }
            }
        }

        // Set additional authors
        String authorsText = additionalAuthorsField.getText().trim();
        if (!authorsText.isEmpty()) {
            String[] authors = authorsText.split(",");
            for (String author : authors) {
                String trimmedAuthor = author.trim();
                if (!trimmedAuthor.isEmpty()) {
                    book.addAdditionalAuthor(trimmedAuthor);
                }
            }
        }

        // Set description
        book.setDescription(descriptionArea.getText().trim());

        // Set language
        book.setLanguage(languageField.getText().trim());

        // Set page count
        try {
            String pageCountText = pageCountField.getText().trim();
            if (!pageCountText.isEmpty()) {
                int pageCount = Integer.parseInt(pageCountText);
                if (pageCount > 0) {
                    book.setPageCount(pageCount);
                }
            }
        } catch (NumberFormatException e) {
            // Ignore invalid page count
        }

        // Set rating from radio buttons
        double rating = 0.0;
        for (int i = 1; i <= 5; i++) {
            if (ratingButtons[i].isSelected()) {
                rating = i;
                break;
            }
        }
        book.setRating(rating);
    }

    private void updateBook() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a book from the table to update",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String bookId = bookIdField.getText().trim();
        if (bookId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No book ID found. Please select a book from the table.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Book book = libraryService.getBook(bookId);
        if (book == null) {
            JOptionPane.showMessageDialog(this,
                    "Book not found with ID: " + bookId,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        BookStatus oldStatus = book.getStatus();
        BookStatus newStatus = (BookStatus) statusComboBox.getSelectedItem();

        // Check what fields have changed
        boolean titleChanged = !book.getTitle().equals(titleField.getText().trim());
        boolean authorChanged = !book.getAuthor().equals(authorField.getText().trim());
        boolean categoryChanged = !book.getCategory().equals(categoryField.getText().trim());
        boolean isbnChanged = !book.getIsbn().equals(isbnField.getText().trim());
        boolean statusChanged = oldStatus != newStatus;
        boolean featuredChanged = book.isFeatured() != featuredCheckBox.isSelected();
        boolean recommendedChanged = book.isRecommended() != recommendedCheckBox.isSelected();
        boolean specialEditionChanged = book.isSpecialEdition() != specialEditionCheckBox.isSelected();

        // Check optional fields changes
        int newEdition = 0;
        try {
            String editionText = editionField.getText().trim();
            newEdition = editionText.isEmpty() ? 0 : Integer.parseInt(editionText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid edition number. Please enter a valid number or leave it empty.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean editionChanged = book.getEdition() != newEdition;

        String newPublisher = publisherField.getText().trim();
        boolean publisherChanged = (book.getPublisher() == null && !newPublisher.isEmpty()) ||
                (book.getPublisher() != null && !book.getPublisher().equals(newPublisher));

        Date newPublicationDate = (Date) publicationDateSpinner.getValue();
        boolean publicationDateChanged = (book.getPublicationDate() == null && newPublicationDate != null) ||
                (book.getPublicationDate() != null && !book.getPublicationDate().equals(newPublicationDate));

        // Check extended metadata changes
        boolean extendedMetadataChanged = checkExtendedMetadataChanges(book);

        // Check if any changes were made
        if (!titleChanged && !authorChanged && !categoryChanged && !isbnChanged &&
                !statusChanged && !featuredChanged && !recommendedChanged && !specialEditionChanged &&
                !editionChanged && !publisherChanged && !publicationDateChanged && !extendedMetadataChanged) {
            JOptionPane.showMessageDialog(this,
                    "No changes detected. Please modify at least one field before updating.",
                    "No Changes",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Update book details
        book.setTitle(titleField.getText().trim());
        book.setAuthor(authorField.getText().trim());
        book.setCategory(categoryField.getText().trim());
        book.setIsbn(isbnField.getText().trim());
        book.setStatus(newStatus);
        book.setFeatured(featuredCheckBox.isSelected());
        book.setRecommended(recommendedCheckBox.isSelected());
        book.setSpecialEdition(specialEditionCheckBox.isSelected());
        book.setEdition(newEdition);
        book.setPublisher(newPublisher.isEmpty() ? null : newPublisher);
        book.setPublicationDate(newPublicationDate);

        // Update extended metadata
        updateExtendedMetadata(book);

        // Update the book in the library service
        libraryService.updateBook(bookId, book);

        refreshTable();

        // Build change summary message
        StringBuilder changeSummary = new StringBuilder();
        changeSummary.append("✅ Book updated successfully!\n\n📋 Updated Details:\n");
        changeSummary.append("• Book ID: ").append(book.getBookId()).append("\n");

        if (titleChanged) {
            changeSummary.append("• Title: ").append(book.getTitle()).append(" (Changed)\n");
        } else {
            changeSummary.append("• Title: ").append(book.getTitle()).append("\n");
        }

        if (authorChanged) {
            changeSummary.append("• Author: ").append(book.getAuthor()).append(" (Changed)\n");
        } else {
            changeSummary.append("• Author: ").append(book.getAuthor()).append("\n");
        }

        if (categoryChanged) {
            changeSummary.append("• Category: ").append(book.getCategory()).append(" (Changed)\n");
        } else {
            changeSummary.append("• Category: ").append(book.getCategory()).append("\n");
        }

        if (isbnChanged) {
            changeSummary.append("• ISBN: ").append(book.getIsbn()).append(" (Changed)\n");
        } else {
            changeSummary.append("• ISBN: ").append(book.getIsbn()).append("\n");
        }

        if (statusChanged) {
            changeSummary.append("• Status: ").append(book.getStatus().getDisplayName())
                    .append(" (Changed from ").append(oldStatus.getDisplayName()).append(")\n");
        } else {
            changeSummary.append("• Status: ").append(book.getStatus().getDisplayName()).append("\n");
        }

        if (featuredChanged) {
            changeSummary.append("• Featured: ").append(book.isFeatured() ? "⭐ Added" : "Removed").append("\n");
        }

        if (recommendedChanged) {
            changeSummary.append("• Recommended: ").append(book.isRecommended() ? "👍 Added" : "Removed").append("\n");
        }

        if (specialEditionChanged) {
            changeSummary.append("• Special Edition: ").append(book.isSpecialEdition() ? "🌟 Added" : "Removed").append("\n");
        }

        if (editionChanged) {
            if (book.getEdition() > 0) {
                changeSummary.append("• Edition: ").append(book.getEdition()).append(" (Changed)\n");
            } else {
                changeSummary.append("• Edition: Removed\n");
            }
        }

        if (publisherChanged) {
            if (book.getPublisher() != null && !book.getPublisher().isEmpty()) {
                changeSummary.append("• Publisher: ").append(book.getPublisher()).append(" (Changed)\n");
            } else {
                changeSummary.append("• Publisher: Removed\n");
            }
        }

        if (publicationDateChanged) {
            if (book.getPublicationDate() != null) {
                changeSummary.append("• Publication Date: Updated\n");
            } else {
                changeSummary.append("• Publication Date: Removed\n");
            }
        }

        if (extendedMetadataChanged) {
            changeSummary.append("• Extended Metadata: Updated\n");
        }

        JOptionPane.showMessageDialog(this,
                changeSummary.toString(),
                "Book Updated Successfully",
                JOptionPane.INFORMATION_MESSAGE);

        clearForm();
    }

    private boolean checkExtendedMetadataChanges(Book book) {
        // Check cover image
        if (!book.getCoverImagePath().equals(coverImagePath)) return true;

        // Check tags
        String currentTags = String.join(", ", book.getTags());
        String newTags = tagsField.getText().trim();
        if (!currentTags.equals(newTags)) return true;

        // Check additional authors
        String currentAuthors = String.join(", ", book.getAdditionalAuthors());
        String newAuthors = additionalAuthorsField.getText().trim();
        if (!currentAuthors.equals(newAuthors)) return true;

        // Check description
        if (!book.getDescription().equals(descriptionArea.getText().trim())) return true;

        // Check language
        if (!book.getLanguage().equals(languageField.getText().trim())) return true;

        // Check page count
        try {
            String pageCountText = pageCountField.getText().trim();
            int newPageCount = pageCountText.isEmpty() ? 0 : Integer.parseInt(pageCountText);
            if (book.getPageCount() != newPageCount) return true;
        } catch (NumberFormatException e) {
            return true;
        }

        // Check rating
        double newRating = 0.0;
        for (int i = 1; i <= 5; i++) {
            if (ratingButtons[i].isSelected()) {
                newRating = i;
                break;
            }
        }
        if (Math.abs(book.getRating() - newRating) > 0.01) return true;

        return false;
    }

    private void updateExtendedMetadata(Book book) {
        // Update cover image path
        book.setCoverImagePath(coverImagePath);

        // Update tags
        book.getTags().clear();
        String tagsText = tagsField.getText().trim();
        if (!tagsText.isEmpty()) {
            String[] tags = tagsText.split(",");
            for (String tag : tags) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    book.addTag(trimmedTag);
                }
            }
        }

        // Update additional authors
        book.getAdditionalAuthors().clear();
        String authorsText = additionalAuthorsField.getText().trim();
        if (!authorsText.isEmpty()) {
            String[] authors = authorsText.split(",");
            for (String author : authors) {
                String trimmedAuthor = author.trim();
                if (!trimmedAuthor.isEmpty()) {
                    book.addAdditionalAuthor(trimmedAuthor);
                }
            }
        }

        // Update description
        book.setDescription(descriptionArea.getText().trim());

        // Update language
        book.setLanguage(languageField.getText().trim());

        // Update page count
        try {
            String pageCountText = pageCountField.getText().trim();
            if (!pageCountText.isEmpty()) {
                int pageCount = Integer.parseInt(pageCountText);
                book.setPageCount(pageCount > 0 ? pageCount : 0);
            } else {
                book.setPageCount(0);
            }
        } catch (NumberFormatException e) {
            book.setPageCount(0);
        }

        // Update rating from radio buttons
        double rating = 0.0;
        for (int i = 1; i <= 5; i++) {
            if (ratingButtons[i].isSelected()) {
                rating = i;
                break;
            }
        }
        book.setRating(rating);
    }

    private void deleteBook() {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a book to delete",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String bookId = bookIdField.getText().trim();
        String bookTitle = titleField.getText().trim();

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format(
                        "Are you sure you want to delete this book?\n\n" +
                                "📚 Book ID: %s\n" +
                                "📖 Title: %s\n\n" +
                                "This action cannot be undone.",
                        bookId, bookTitle
                ),
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            libraryService.removeBook(bookId);
            refreshTable();
            clearForm();

            JOptionPane.showMessageDialog(this,
                    String.format(
                            "✅ Book deleted successfully!\n\n" +
                                    "Book '%s' (ID: %s) has been removed from the system.",
                            bookTitle, bookId
                    ),
                    "Book Deleted",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void addReview(Book book) {
        // Create a custom dialog with expanded size
        JDialog reviewDialog = new JDialog();
        reviewDialog.setTitle("Add Review for: " + book.getTitle());
        reviewDialog.setModal(true);
        reviewDialog.setSize(500, 500);
        reviewDialog.setLocationRelativeTo(this);
        reviewDialog.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header with book info
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel("<html><b>Reviewing:</b> " + book.getTitle() + "<br><small>by " + book.getAuthor() + "</small></html>");
        headerLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        headerPanel.add(headerLabel, BorderLayout.NORTH);
        headerPanel.add(new JSeparator(), BorderLayout.SOUTH);

        // Input fields panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        // User selection (dropdown with search/type)
        JPanel userPanel = new JPanel(new BorderLayout(5, 5));
        JLabel userLabel = new JLabel("Select Registered User:");
        userLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        userPanel.add(userLabel, BorderLayout.NORTH);

        // Get all users from library service
        List<User> allUsers = libraryService.getAllUsers();

        if (allUsers.isEmpty()) {
            // No users registered
            JOptionPane.showMessageDialog(this,
                    "No registered users found. Please register users first before adding reviews.",
                    "No Users Found",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create user display strings with all searchable info
        List<String> userDisplayStrings = new ArrayList<>();
        Map<String, User> userMap = new HashMap<>(); // Map display string to User object

        for (User user : allUsers) {
            String displayString = String.format("%s [ID: %s] | Email: %s | Phone: %s",
                    user.getName(),
                    user.getUserId(),
                    user.getEmail(),
                    user.getContactNumber());
            userDisplayStrings.add(displayString);
            userMap.put(displayString, user);
        }

        // Create combo box with auto-complete functionality
        JComboBox<String> userComboBox = new JComboBox<>(userDisplayStrings.toArray(new String[0]));
        userComboBox.setEditable(true); // Allow typing to search
        userComboBox.setPreferredSize(new Dimension(0, 35));

        // Store the original model for filtering
        DefaultComboBoxModel<String> originalModel = new DefaultComboBoxModel<>(userDisplayStrings.toArray(new String[0]));

        // Add key listener for real-time search functionality
        ((JTextField) userComboBox.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String searchText = ((JTextField) userComboBox.getEditor().getEditorComponent()).getText().trim();

                if (searchText.isEmpty()) {
                    // Reset to all users when search is empty
                    userComboBox.setModel(originalModel);
                    userComboBox.hidePopup();
                    return;
                }

                // Filter users based on search text (search in name, ID, email, or phone)
                List<String> filteredUsers = new ArrayList<>();
                String lowerSearch = searchText.toLowerCase();

                for (String userDisplay : userDisplayStrings) {
                    // Convert to lowercase for case-insensitive search
                    String lowerDisplay = userDisplay.toLowerCase();

                    // Check if search text appears in any part of the display string
                    if (lowerDisplay.contains(lowerSearch)) {
                        filteredUsers.add(userDisplay);
                    }
                }

                if (!filteredUsers.isEmpty()) {
                    DefaultComboBoxModel<String> filteredModel = new DefaultComboBoxModel<>(
                            filteredUsers.toArray(new String[0]));
                    userComboBox.setModel(filteredModel);

                    // Only show popup if user is typing (not using arrow keys)
                    if (!e.isActionKey()) {
                        userComboBox.showPopup();
                        // Select the first matching item
                        if (filteredModel.getSize() > 0) {
                            userComboBox.setSelectedIndex(0);
                        }
                    }
                } else {
                    // No matches found
                    userComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"No matching users found"}));
                    userComboBox.setSelectedIndex(0);
                }
            }
        });

        // Add focus listener to reset when focus is lost
        userComboBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String currentText = ((JTextField) userComboBox.getEditor().getEditorComponent()).getText();
                boolean found = false;

                // Check if current text matches any user
                for (String userDisplay : userDisplayStrings) {
                    if (userDisplay.equals(currentText)) {
                        found = true;
                        break;
                    }
                }

                if (!found && !currentText.isEmpty()) {
                    // If text doesn't match any user, reset to original
                    userComboBox.setModel(originalModel);
                    userComboBox.setSelectedItem(currentText); // Keep typed text
                }
            }
        });

        userPanel.add(userComboBox, BorderLayout.CENTER);

        // Add info label about search
        JLabel searchInfoLabel = new JLabel("<html><small><i>Type to search by name, user ID, email, or phone number</i></small></html>");
        searchInfoLabel.setForeground(new Color(100, 100, 100));
        userPanel.add(searchInfoLabel, BorderLayout.SOUTH);

        // Rating panel
        JPanel ratingPanel = new JPanel(new BorderLayout(5, 5));
        JLabel ratingLabel = new JLabel("Rating (1-5 stars):");
        ratingLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        ratingPanel.add(ratingLabel, BorderLayout.NORTH);

        JPanel starsPanel = new JPanel(new GridLayout(1, 5, 10, 0));
        ButtonGroup ratingGroup = new ButtonGroup();
        JRadioButton[] starButtons = new JRadioButton[5];

        for (int i = 0; i < 5; i++) {
            starButtons[i] = new JRadioButton(String.valueOf(i + 1));
            starButtons[i].setFont(new Font("Dialog", Font.BOLD, 14));
            starButtons[i].setHorizontalAlignment(SwingConstants.CENTER);
            ratingGroup.add(starButtons[i]);
            starsPanel.add(starButtons[i]);
        }
        starButtons[4].setSelected(true); // Default to 5 stars

        ratingPanel.add(starsPanel, BorderLayout.CENTER);

        // Star rating explanation
        JPanel starExplanationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        starExplanationPanel.add(new JLabel("⭐".repeat(1) + "=Poor"));
        starExplanationPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        starExplanationPanel.add(new JLabel("⭐".repeat(3) + "=Good"));
        starExplanationPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        starExplanationPanel.add(new JLabel("⭐".repeat(5) + "=Excellent"));
        ratingPanel.add(starExplanationPanel, BorderLayout.SOUTH);

        // Comment area
        JPanel commentPanel = new JPanel(new BorderLayout(5, 5));
        JLabel commentLabel = new JLabel("Your Review:");
        commentLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        commentPanel.add(commentLabel, BorderLayout.NORTH);

        JTextArea commentArea = new JTextArea(8, 40);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        JScrollPane commentScroll = new JScrollPane(commentArea);
        commentScroll.setPreferredSize(new Dimension(0, 150));
        commentPanel.add(commentScroll, BorderLayout.CENTER);

        // Add components to input panel
        inputPanel.add(userPanel);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        inputPanel.add(ratingPanel);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        inputPanel.add(commentPanel);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelButton = new JButton("Cancel");
        JButton submitButton = new JButton("Submit Review");

        // Style buttons to match existing theme
        cancelButton.setBorder(new LineBorder(new Color(158, 158, 158), 2));
        cancelButton.setBackground(Color.WHITE);
        cancelButton.setForeground(Color.BLACK);

        submitButton.setBorder(new LineBorder(new Color(76, 175, 80), 2)); // Green border like Add button
        submitButton.setBackground(Color.WHITE);
        submitButton.setForeground(Color.BLACK);

        cancelButton.addActionListener(e -> reviewDialog.dispose());
        submitButton.addActionListener(e -> {
            Object selectedItem = userComboBox.getSelectedItem();
            String comment = commentArea.getText().trim();

            if (selectedItem == null) {
                JOptionPane.showMessageDialog(reviewDialog,
                        "Please select a registered user",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String selectedDisplay = selectedItem.toString();

            // Check if "No matching users found" is selected
            if (selectedDisplay.equals("No matching users found")) {
                JOptionPane.showMessageDialog(reviewDialog,
                        "Please select a valid registered user",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get the User object from the map
            User selectedUser = userMap.get(selectedDisplay);

            if (selectedUser == null) {
                // User typed something that doesn't match exactly
                JOptionPane.showMessageDialog(reviewDialog,
                        "Please select a user from the dropdown list",
                        "Invalid User",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (comment.isEmpty()) {
                JOptionPane.showMessageDialog(reviewDialog,
                        "Please write your review",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Get selected rating
            int rating = 5; // default
            for (int i = 0; i < 5; i++) {
                if (starButtons[i].isSelected()) {
                    rating = i + 1;
                    break;
                }
            }

            // Create and add review
            Review review = new Review(selectedUser.getUserId(), selectedUser.getName(), comment, rating);
            book.addReview(review);

            // Update average rating
            book.setRating(book.calculateAverageRating());

            // Update book in library service
            libraryService.updateBook(book.getBookId(), book);

            // Update details panel
            updateDetailsPanel(book);

            JOptionPane.showMessageDialog(reviewDialog,
                    "Review submitted successfully!\n" +
                            "User: " + selectedUser.getName() + "\n" +
                            "Rating: " + rating + "/5",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            reviewDialog.dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);

        // Add all panels to dialog
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        reviewDialog.add(mainPanel);
        reviewDialog.setVisible(true);
    }

    private JPanel createReviewPanel(Review review) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        panel.setBackground(new Color(252, 252, 252));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Top row with name and rating on opposite ends
        JPanel topRow = new JPanel();
        topRow.setLayout(new BorderLayout());
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        // Name on left
        JLabel nameLabel = new JLabel("👤 " + review.getUserName());
        nameLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        topRow.add(nameLabel, BorderLayout.WEST);

        // Rating on right
        JLabel ratingLabel = new JLabel("⭐".repeat(review.getRating()) + " (" + review.getRating() + "/5)");
        ratingLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        ratingLabel.setForeground(new Color(255, 193, 7));
        topRow.add(ratingLabel, BorderLayout.EAST);

        // Comment
        JTextArea commentArea = new JTextArea(review.getComment());
        commentArea.setEditable(false);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        commentArea.setBackground(new Color(252, 252, 252));
        commentArea.setFont(new Font("Dialog", Font.PLAIN, 11));
        commentArea.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        // Date at bottom right
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JLabel dateLabel = new JLabel(new SimpleDateFormat("MMM dd, yyyy").format(review.getReviewDate()));
        dateLabel.setForeground(new Color(150, 150, 150));
        dateLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        datePanel.add(dateLabel);

        panel.add(topRow);
        panel.add(commentArea);
        panel.add(datePanel);

        return panel;
    }

    private void markBooksAsFeatured() {
        String bookId = JOptionPane.showInputDialog(this,
                "Enter Book ID to mark as featured:",
                "Mark as Featured",
                JOptionPane.QUESTION_MESSAGE);

        if (bookId != null && !bookId.trim().isEmpty()) {
            libraryService.markAsFeatured(bookId.trim(), true);
            refreshTable();
            JOptionPane.showMessageDialog(this,
                    "Book marked as featured successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void markBooksAsRecommended() {
        String bookId = JOptionPane.showInputDialog(this,
                "Enter Book ID to mark as recommended:",
                "Mark as Recommended",
                JOptionPane.QUESTION_MESSAGE);

        if (bookId != null && !bookId.trim().isEmpty()) {
            libraryService.markAsRecommended(bookId.trim(), true);
            refreshTable();
            JOptionPane.showMessageDialog(this,
                    "Book marked as recommended successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void markBooksAsSpecialEdition() {
        String bookId = JOptionPane.showInputDialog(this,
                "Enter Book ID to mark as special edition:",
                "Mark as Special Edition",
                JOptionPane.QUESTION_MESSAGE);

        if (bookId != null && !bookId.trim().isEmpty()) {
            String editionStr = JOptionPane.showInputDialog(this,
                    "Enter edition number (optional):",
                    "Edition Number",
                    JOptionPane.QUESTION_MESSAGE);

            int edition = 0;
            if (editionStr != null && !editionStr.trim().isEmpty()) {
                try {
                    edition = Integer.parseInt(editionStr.trim());
                    if (edition < 0) {
                        JOptionPane.showMessageDialog(this,
                                "Edition number must be positive",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this,
                            "Invalid edition number!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            libraryService.markAsSpecialEdition(bookId.trim(), true, edition);
            refreshTable();
            JOptionPane.showMessageDialog(this,
                    "Book marked as special edition successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void removeBookFeatures() {
        String bookId = JOptionPane.showInputDialog(this,
                "Enter Book ID to remove all special features:",
                "Remove Features",
                JOptionPane.QUESTION_MESSAGE);

        if (bookId != null && !bookId.trim().isEmpty()) {
            Book book = libraryService.getBook(bookId.trim());
            if (book != null) {
                book.setFeatured(false);
                book.setRecommended(false);
                book.setSpecialEdition(false);
                libraryService.updateBook(bookId.trim(), book);
                refreshTable();
                JOptionPane.showMessageDialog(this,
                        "All special features (Featured/Recommended/Special Edition) removed from book!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void viewBooksByFeature(String feature) {
        List<Book> books = libraryService.getBooksByFeature(feature);

        if (books.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No " + feature + " books found!",
                    "No Results",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder message = new StringBuilder();
        String featureName = feature.equals("special") ? "Special Edition" :
                feature.substring(0, 1).toUpperCase() + feature.substring(1);
        message.append("📚 ").append(featureName).append(" Books:\n\n");

        for (Book book : books) {
            message.append("• ").append(book.getTitle())
                    .append(" by ").append(book.getAuthor())
                    .append(" [").append(book.getBookId()).append("]");

            if (book.getEdition() > 0) {
                message.append(" (Edition ").append(book.getEdition()).append(")");
            }

            if (book.getRating() > 0) {
                message.append(" ⭐ ").append(String.format("%.1f", book.getRating())).append("/5");
            }
            message.append("\n");
        }

        JOptionPane.showMessageDialog(this,
                message.toString(),
                featureName + " Books",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearForm() {
        bookIdField.setText("");
        titleField.setText("");
        authorField.setText("");
        categoryField.setText("");
        isbnField.setText("");
        statusComboBox.setSelectedIndex(0);
        featuredCheckBox.setSelected(false);
        recommendedCheckBox.setSelected(false);
        specialEditionCheckBox.setSelected(false);
        editionField.setText("");
        publisherField.setText("");
        publicationDateSpinner.setValue(new Date());

        // Clear extended metadata
        coverImagePath = "";
        coverImageLabel.setText("No image selected");
        coverImageLabel.setIcon(null);
        coverImageLabel.setForeground(Color.GRAY);
        tagsField.setText("");
        additionalAuthorsField.setText("");
        descriptionArea.setText("");
        languageField.setText("");
        pageCountField.setText("");
        ratingButtons[0].setSelected(true); // Select "None" for rating

        bookTable.clearSelection();
        idLabel.setText("(Auto-generated)");
        idLabel.setForeground(Color.BLUE);
        clearBookHighlights();
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        for (Book book : libraryService.getAllBooks()) {
            String displayStatus = book.getStatus().getDisplayName();

            if (book.getStatus() == BookStatus.BORROWED) {
                boolean hasActiveReservations = libraryService.getAllReservations().stream()
                        .anyMatch(reservation ->
                                reservation.getBook().getBookId().equals(book.getBookId()) &&
                                        !reservation.isNotified());

                if (hasActiveReservations) {
                    displayStatus = BookStatus.BORROWED_RESERVED.getDisplayName();
                }
            }

            StringBuilder features = new StringBuilder();
            if (book.isSpecialEdition()) features.append("🌟 ");
            if (book.isFeatured()) features.append("⭐ ");
            if (book.isRecommended()) features.append("👍 ");

            String editionDisplay = book.getEdition() > 0 ? String.valueOf(book.getEdition()) : "-";

            // Create cover image icon
            ImageIcon coverIcon = null;
            if (!book.getCoverImagePath().isEmpty()) {
                try {
                    File imageFile = new File(book.getCoverImagePath());
                    if (imageFile.exists()) {
                        ImageIcon originalIcon = new ImageIcon(book.getCoverImagePath());
                        Image scaledImage = originalIcon.getImage().getScaledInstance(40, 56, Image.SCALE_SMOOTH);
                        coverIcon = new ImageIcon(scaledImage);
                    }
                } catch (Exception e) {
                }
            }

            if (coverIcon == null) {
                coverIcon = createPlaceholderCover(book);
            }

            tableModel.addRow(new Object[]{
                    coverIcon,
                    book.getBookId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getCategory(),
                    book.getIsbn(),
                    displayStatus,
                    features.toString().trim(),
                    editionDisplay,
                    book.getBorrowHistory().size()
            });
        }
        clearBookHighlights();
    }

    private ImageIcon createPlaceholderCover(Book book) {
        java.awt.Image img = new java.awt.image.BufferedImage(50, 70, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = (java.awt.Graphics2D) img.getGraphics();

        if (book.isSpecialEdition()) {
            g2d.setColor(new Color(255, 215, 0));
        } else if (book.isFeatured()) {
            g2d.setColor(new Color(30, 144, 255));
        } else if (book.isRecommended()) {
            g2d.setColor(new Color(50, 205, 50));
        } else {
            g2d.setColor(new Color(240, 240, 240));
        }

        g2d.fillRect(0, 0, 50, 70);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        String initials = book.getTitle().substring(0, Math.min(2, book.getTitle().length())).toUpperCase();
        g2d.drawString(initials, 15, 35);

        g2d.dispose();
        return new ImageIcon(img);
    }
}