package com.sjbit.library.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Digital, permanent record of a borrow slip (kept even if the physical/PDF
 * file is later deleted) - this is what satisfies "college record purposes".
 */
@Entity
@Table(name = "borrow_slips")
@Data
@NoArgsConstructor
public class BorrowSlip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slipNumber; // e.g. SLIP-20260711-0001

    @OneToOne
    @JoinColumn(name = "borrow_record_id")
    private BorrowRecord borrowRecord;

    private String studentId;
    private String studentName;
    private String bookTitle;
    private String copyId;
    private LocalDateTime issuedAt = LocalDateTime.now();
    private String pdfFileName; // filename inside library.slips.directory
}
