package com.library.patterns.state;

public interface BookState {
    void borrow();
    void returnBook();
    void reserve();
    String getStatusName();
}