package com.sjbit.library.service;

import com.sjbit.library.entity.Book;
import com.sjbit.library.entity.BookCopy;
import com.sjbit.library.repository.BookCopyRepository;
import com.sjbit.library.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;

    public BookService(BookRepository bookRepository, BookCopyRepository bookCopyRepository) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book save(Book book) {
        // make sure each copy points back to this book (needed for the FK column)
        if (book.getEditions() != null) {
            book.getEditions().forEach(c -> c.setBook(book));
        }
        return bookRepository.save(book);
    }

    public void delete(String bookId) {
        bookRepository.deleteById(bookId);
    }

    public Book toggleCopyAvailability(String copyId) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new IllegalArgumentException("Copy not found"));
        copy.setAvailable(!copy.isAvailable());
        bookCopyRepository.save(copy);
        return copy.getBook();
    }

    public BookCopy findAvailableCopy(String bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));
        return book.getEditions().stream()
                .filter(BookCopy::isAvailable)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No available copies right now"));
    }

    public BookCopy findCopy(String copyId) {
        return bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new IllegalArgumentException("Copy not found"));
    }

    public void saveCopy(BookCopy copy) {
        bookCopyRepository.save(copy);
    }
}
