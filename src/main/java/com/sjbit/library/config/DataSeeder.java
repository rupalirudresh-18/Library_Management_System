package com.sjbit.library.config;

import com.sjbit.library.entity.Book;
import com.sjbit.library.entity.BookCopy;
import com.sjbit.library.entity.PurchaseRequest;
import com.sjbit.library.repository.BookRepository;
import com.sjbit.library.repository.PurchaseRequestRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the exact same demo data the frontend-only version had, so the app
 * looks identical the first time you run it. Runs only if the books table
 * is empty (won't duplicate data on every restart).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;

    public DataSeeder(BookRepository bookRepository, PurchaseRequestRepository purchaseRequestRepository) {
        this.bookRepository = bookRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
    }

    @Override
    public void run(String... args) {
        if (bookRepository.count() == 0) {
            seedBooks();
        }
        if (purchaseRequestRepository.count() == 0) {
            seedPurchaseRequests();
        }
    }

    private void seedBooks() {
        bookRepository.save(book("CS1001", "Introduction to Algorithms", "Cormen", 2009, "CSE", "CSE",
                copies("CS1001", 5, "3rd", 2009)));
        bookRepository.save(book("EC2001", "Digital Logic Design", "M. Mano", 2014, "ECE", "ECE",
                copies("EC2001", 3, "5th", 2014)));
        bookRepository.save(book("ME3001", "Fluid Mechanics", "R. K. Bansal", 2011, "MECH", "MECH",
                copies("ME3001", 2, "2nd", 2011)));
        bookRepository.save(book("CV4001", "Structural Analysis", "D. Builder", 2016, "CIVIL", "CIVIL",
                copies("CV4001", 1, "1st", 2016)));

        Book isBook = book("IS5001", "Database Systems", "Ramakrishnan", 2017, "ISE", "ISE", new ArrayList<>());
        isBook.setOnline(true);
        isBook.setOnlineUrl("#");
        BookCopy c1 = new BookCopy(); c1.setCopyId("IS5001-1"); c1.setEdition("2nd"); c1.setYear(2017); c1.setAvailable(true); c1.setBook(isBook);
        BookCopy c2 = new BookCopy(); c2.setCopyId("IS5001-2"); c2.setEdition("2nd"); c2.setYear(2017); c2.setAvailable(false); c2.setBook(isBook);
        isBook.getEditions().add(c1);
        isBook.getEditions().add(c2);
        bookRepository.save(isBook);
    }

    private Book book(String id, String title, String author, int year, String category, String branch, List<BookCopy> copies) {
        Book b = new Book();
        b.setId(id);
        b.setTitle(title);
        b.setAuthor(author);
        b.setYear(year);
        b.setCategory(category);
        b.setBranch(branch);
        b.setOnline(false);
        copies.forEach(c -> c.setBook(b));
        b.setEditions(copies);
        return b;
    }

    private List<BookCopy> copies(String bookId, int count, String edition, int year) {
        List<BookCopy> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BookCopy c = new BookCopy();
            c.setCopyId(bookId + "-" + i);
            c.setEdition(edition);
            c.setYear(year);
            c.setAvailable(true);
            list.add(c);
        }
        return list;
    }

    private void seedPurchaseRequests() {
        purchaseRequestRepository.save(req("Artificial Intelligence: A Modern Approach", "Russell & Norvig", "3rd"));
        purchaseRequestRepository.save(req("Operating System Concepts", "Silberschatz", "9th"));
        purchaseRequestRepository.save(req("Data Communications and Networking", "Behrouz A. Forouzan", "5th"));
    }

    private PurchaseRequest req(String title, String author, String edition) {
        PurchaseRequest r = new PurchaseRequest();
        r.setTitle(title);
        r.setAuthor(author);
        r.setEdition(edition);
        r.setRequestedBy("staff");
        return r;
    }
}
