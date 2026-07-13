package com.sjbit.library.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "borrow_records")
@Data
@NoArgsConstructor
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne
    @JoinColumn(name = "copy_id")
    private BookCopy copy;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate requestDate;
    private LocalDate borrowDate;   // set when approved/issued
    private LocalDate dueDate;      // borrowDate + library.borrow.duration-days
    private LocalDate returnDate;   // set when returned

    @Enumerated(EnumType.STRING)
    private BorrowStatus status = BorrowStatus.PENDING;

    private Double fineAmount = 0.0;
    private boolean finePaid = false;

    // has the daily overdue-reminder already been sent today? avoids duplicate mails
    private LocalDate lastReminderSentDate;
}
