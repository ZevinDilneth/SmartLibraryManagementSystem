package com.library.patterns.decorator;

import com.library.models.Book;

public class FeaturedBookDecorator extends BookDecorator {
    public FeaturedBookDecorator(Book book) {
        super(book);
    }

    @Override
    public String getEnhancedDescription() {
        return "⭐ FEATURED ⭐ " + book.toString();
    }
}