package com.library.patterns.observer;

public interface BookObserver {
    void update(String message);
    String getObserverId();
}