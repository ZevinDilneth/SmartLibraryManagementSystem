package com.library.patterns.builder;

import com.library.models.Book;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BookBuilder {
    private String title;
    private String author;
    private String category;
    private String isbn;
    private int edition = 1;
    private String publisher;
    private Date publicationDate;
    private List<String> tags = new ArrayList<>();

    public BookBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public BookBuilder setAuthor(String author) {
        this.author = author;
        return this;
    }

    public BookBuilder setCategory(String category) {
        this.category = category;
        return this;
    }

    public BookBuilder setIsbn(String isbn) {
        this.isbn = isbn;
        return this;
    }

    public BookBuilder setEdition(int edition) {
        this.edition = edition;
        return this;
    }

    public BookBuilder setPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    public BookBuilder setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
        return this;
    }

    public BookBuilder addTag(String tag) {
        this.tags.add(tag);
        return this;
    }

    public Book build() {
        Book book = new Book(title, author, category, isbn);
        book.setEdition(edition);
        book.setPublisher(publisher);
        book.setPublicationDate(publicationDate);
        for (String tag : tags) {
            book.addTag(tag);
        }
        return book;
    }
}