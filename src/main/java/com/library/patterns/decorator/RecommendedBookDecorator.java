package com.library.patterns.decorator;

import com.library.models.Book;

public class RecommendedBookDecorator extends BookDecorator {
    public RecommendedBookDecorator(Book book) {
        super(book);
    }

    @Override
    public String getEnhancedDescription() {
        return "👍 RECOMMENDED 👍 " + book.toString();
    }
}