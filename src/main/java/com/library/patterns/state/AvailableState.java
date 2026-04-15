package com.library.patterns.state;

import com.library.enums.BookStatus;

public class AvailableState implements BookState {
    @Override
    public void borrow() {
        System.out.println("Book can be borrowed");
    }

    @Override
    public void returnBook() {
        System.out.println("Book is already available");
    }

    @Override
    public void reserve() {
        System.out.println("Book can be reserved");
    }

    @Override
    public String getStatusName() {
        return BookStatus.AVAILABLE.getDisplayName();
    }
}