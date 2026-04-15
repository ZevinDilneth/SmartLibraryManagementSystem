package com.library.patterns.state;

import com.library.enums.BookStatus;

public class BorrowedState implements BookState {
    @Override
    public void borrow() {
        System.out.println("Book is already borrowed");
    }

    @Override
    public void returnBook() {
        System.out.println("Book can be returned");
    }

    @Override
    public void reserve() {
        System.out.println("Book can be reserved");
    }

    @Override
    public String getStatusName() {
        return BookStatus.BORROWED.getDisplayName();
    }
}