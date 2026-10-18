package com.mohan.library;

/** Thrown when a library business rule is broken (e.g. no copies left, book already returned). */
public class LibraryException extends Exception {
    public LibraryException(String message) {
        super(message);
    }
}
