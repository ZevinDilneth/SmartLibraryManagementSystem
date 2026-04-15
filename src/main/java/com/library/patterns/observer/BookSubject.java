package com.library.patterns.observer;

import java.util.HashMap;
import java.util.Map;

public class BookSubject {
    private Map<String, BookObserver> observers = new HashMap<>();

    public void registerObserver(BookObserver observer) {
        observers.put(observer.getObserverId(), observer);
    }

    public void removeObserver(BookObserver observer) {
        observers.remove(observer.getObserverId());
    }

    public void removeObserverById(String observerId) {
        observers.remove(observerId);
    }

    public void notifyObservers(String message) {
        for (BookObserver observer : observers.values()) {
            observer.update(message);
        }
    }

    public void notifyObserver(String observerId, String message) {
        BookObserver observer = observers.get(observerId);
        if (observer != null) {
            observer.update(message);
        }
    }
}