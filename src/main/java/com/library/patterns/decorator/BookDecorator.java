package com.library.patterns.decorator;

import com.library.models.Book;

public abstract class BookDecorator {
    protected Book book;

    public BookDecorator(Book book) {
        this.book = book;
    }

    public abstract String getEnhancedDescription();
}