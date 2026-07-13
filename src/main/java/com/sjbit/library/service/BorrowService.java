package com.sjbit.library.service;

import com.sjbit.library.entity.*;
import com.sjbit.library.repository.BorrowRecordRepository;
import com.sjbit.library.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final UserRepository userRepository;
    private final BookService bookService;
    private final MailService mailService;
    private final FineService fineService;
    private final SlipService slipService;
    private final NotificationService notificationService;

    public BorrowService(BorrowRecordRepository borrowRecordRepository, UserRepository userRepository,
                          BookService bookService, MailService mailService, FineService fineService,
                          SlipService slipService, NotificationService notificationService) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.userRepository = userRepository;
        this.bookService = bookService;
        this.mailService = mailService;
        this.fineService = fineService;
        this.slipService = slipService;
        this.notificationService = notificationService;
    }

    /** Step 1: student requests a book. Goes into PENDING, awaiting librarian approval. */
    public BorrowRecord requestBorrow(String userId, String bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        BookCopy copy = bookService.findAvailableCopy(bookId); // throws if none available

        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(copy.getBook());
        record.setCopy(copy);
        record.setRequestDate(LocalDate.now());
        record.setStatus(BorrowStatus.PENDING);
        BorrowRecord saved = borrowRecordRepository.save(record);

        notificationService.notify(userId, "Borrow request submitted for \"" + copy.getBook().getTitle() + "\", awaiting approval.");
        return saved;
    }

    /**
     * Step 2: librarian/staff approves -> book is actually issued.
     * This is where the due date is set, the copy is marked unavailable,
     * the borrow slip PDF is generated, and the confirmation email fires.
     */
    public BorrowRecord approve(Long recordId) {
        BorrowRecord record = getOrThrow(recordId);
        if (record.getStatus() != BorrowStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be approved");
        }

        record.setBorrowDate(LocalDate.now());
        record.setDueDate(LocalDate.now().plusDays(fineService.getBorrowDurationDays()));
        record.setStatus(BorrowStatus.APPROVED);

        BookCopy copy = record.getCopy();
        copy.setAvailable(false);
        bookService.saveCopy(copy);

        BorrowRecord saved = borrowRecordRepository.save(record);

        BorrowSlip slip = slipService.generateSlip(saved);
        byte[] pdfBytes = slipService.readSlipPdfBytes(slip);

        mailService.sendBorrowConfirmation(
                record.getUser().getEmail(), record.getUser().getName(),
                record.getBook().getTitle(), record.getCopy().getCopyId(),
                record.getDueDate().toString(), slip.getSlipNumber(),
                pdfBytes, slip.getPdfFileName());

        notificationService.notify(record.getUser().getId(),
                "Approved: \"" + record.getBook().getTitle() + "\" issued. Due back " + record.getDueDate());

        return saved;
    }

    public BorrowRecord reject(Long recordId) {
        BorrowRecord record = getOrThrow(recordId);
        record.setStatus(BorrowStatus.REJECTED);
        BorrowRecord saved = borrowRecordRepository.save(record);
        notificationService.notify(record.getUser().getId(),
                "Your request for \"" + record.getBook().getTitle() + "\" was rejected.");
        return saved;
    }

    /**
     * Step 3: student (or librarian on their behalf) returns the book.
     * Fine is auto-calculated from days-late right here, and a return email
     * fires immediately (including the fine amount, if any).
     */
    public BorrowRecord returnBook(Long recordId) {
        BorrowRecord record = getOrThrow(recordId);
        if (record.getStatus() != BorrowStatus.APPROVED) {
            throw new IllegalStateException("Only issued (approved) books can be returned");
        }

        LocalDate today = LocalDate.now();
        record.setReturnDate(today);
        record.setStatus(BorrowStatus.RETURNED);

        double fine = fineService.calculateFine(record.getDueDate(), today);
        record.setFineAmount(fine);
        record.setFinePaid(fine == 0);

        BookCopy copy = record.getCopy();
        copy.setAvailable(true);
        bookService.saveCopy(copy);

        BorrowRecord saved = borrowRecordRepository.save(record);

        mailService.sendReturnConfirmation(record.getUser().getEmail(), record.getUser().getName(),
                record.getBook().getTitle(), fine);

        notificationService.notify(record.getUser().getId(),
                "Returned: \"" + record.getBook().getTitle() + "\"" + (fine > 0 ? (" — fine Rs." + fine) : ""));

        return saved;
    }

    /** Extends the due date by 7 days. Only allowed if not already overdue. */
    public BorrowRecord renew(Long recordId) {
        BorrowRecord record = getOrThrow(recordId);
        if (record.getStatus() != BorrowStatus.APPROVED) {
            throw new IllegalStateException("Only issued (approved) books can be renewed");
        }
        if (record.getDueDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("This book is already overdue - please return and pay the fine before renewing");
        }
        record.setDueDate(record.getDueDate().plusDays(7));
        BorrowRecord saved = borrowRecordRepository.save(record);
        notificationService.notify(record.getUser().getId(),
                "Renewed: \"" + record.getBook().getTitle() + "\" — new due date " + record.getDueDate());
        return saved;
    }

    public BorrowRecord markFinePaid(Long recordId) {
        BorrowRecord record = getOrThrow(recordId);
        record.setFinePaid(true);
        return borrowRecordRepository.save(record);
    }

    public List<BorrowRecord> findByUser(String userId) {
        return borrowRecordRepository.findByUser_Id(userId);
    }

    public List<BorrowRecord> findByStatus(BorrowStatus status) {
        return borrowRecordRepository.findByStatus(status);
    }

    public List<BorrowRecord> findAll() {
        return borrowRecordRepository.findAll();
    }

    private BorrowRecord getOrThrow(Long id) {
        return borrowRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Borrow record not found"));
    }
}
