package com.sjbit.library.controller;

import com.sjbit.library.entity.Book;
import com.sjbit.library.service.BookService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@CrossOrigin
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<Book> all() {
        return bookService.findAll();
    }

    @PostMapping
    public Book add(@RequestBody Book book) {
        return bookService.save(book);
    }

    @DeleteMapping("/{id}")
    public void remove(@PathVariable String id) {
        bookService.delete(id);
    }

    @PutMapping("/copies/{copyId}/toggle")
    public Book toggleAvailability(@PathVariable String copyId) {
        return bookService.toggleCopyAvailability(copyId);
    }
}
