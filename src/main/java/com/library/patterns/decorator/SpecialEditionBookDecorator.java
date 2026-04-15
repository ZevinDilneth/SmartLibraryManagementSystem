package com.library.patterns.decorator;

import com.library.models.Book;

public class SpecialEditionBookDecorator extends BookDecorator {
    private int editionNumber;
    private String specialFeatures;

    public SpecialEditionBookDecorator(Book book, int editionNumber, String specialFeatures) {
        super(book);
        this.editionNumber = editionNumber;
        this.specialFeatures = specialFeatures;
    }

    @Override
    public String getEnhancedDescription() {
        String base = "🌟 SPECIAL EDITION 🌟 ";
        base += book.toString();
        base += "\nEdition: " + editionNumber;
        if (specialFeatures != null && !specialFeatures.isEmpty()) {
            base += "\nSpecial Features: " + specialFeatures;
        }
        return base;
    }

    // Additional methods for special edition features
    public int getEditionNumber() {
        return editionNumber;
    }

    public String getSpecialFeatures() {
        return specialFeatures;
    }
}