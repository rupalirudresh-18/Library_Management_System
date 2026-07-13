package com.sjbit.library.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.sjbit.library.entity.BorrowRecord;
import com.sjbit.library.entity.BorrowSlip;
import com.sjbit.library.repository.BorrowSlipRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Creates a real PDF borrow slip on disk (for printing / student's own record)
 * AND a permanent digital row in borrow_slips (for the college's records),
 * per the requirement that slip data must be kept digitally either way.
 */
@Service
public class SlipService {

    private final BorrowSlipRepository slipRepository;

    @Value("${library.slips.directory}")
    private String slipsDirectory;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public SlipService(BorrowSlipRepository slipRepository) {
        this.slipRepository = slipRepository;
    }

    public BorrowSlip generateSlip(BorrowRecord record) {
        // record.getId() is the DB-generated primary key -> guaranteed unique,
        // unlike an in-memory counter which would reset on every app restart.
        String slipNumber = "SLIP-" + record.getBorrowDate().format(DAY_FMT)
                + "-" + String.format("%05d", record.getId());

        File dir = new File(slipsDirectory);
        if (!dir.exists()) dir.mkdirs();
        String fileName = slipNumber + ".pdf";
        File pdfFile = new File(dir, fileName);
        System.out.println("Generating borrow slip PDF at: " + pdfFile.getAbsolutePath());

        try {
            Document document = new Document(PageSize.A5);
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph title = new Paragraph("SJB Institute of Technology\nLibrary Borrow Slip\n\n", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("Slip Number: " + slipNumber, labelFont));
            document.add(new Paragraph("Issued On: " + record.getBorrowDate(), normalFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Student ID: " + record.getUser().getId(), labelFont));
            document.add(new Paragraph("Student Name: " + record.getUser().getName(), normalFont));
            document.add(new Paragraph("Branch: " + (record.getUser().getBranch() == null ? "-" : record.getUser().getBranch()), normalFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Book Title: " + record.getBook().getTitle(), labelFont));
            document.add(new Paragraph("Author: " + record.getBook().getAuthor(), normalFont));
            document.add(new Paragraph("Copy ID: " + record.getCopy().getCopyId(), normalFont));
            document.add(new Paragraph("Edition: " + record.getCopy().getEdition(), normalFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Borrow Date: " + record.getBorrowDate(), normalFont));
            document.add(new Paragraph("Due Date: " + record.getDueDate(), labelFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Note: A fine will be automatically charged for each day the book is returned late.", normalFont));

            document.close();
        } catch (Exception e) {
            System.err.println("Failed to generate borrow slip PDF: " + e.getMessage());
            e.printStackTrace();
        }

        BorrowSlip slip = new BorrowSlip();
        slip.setSlipNumber(slipNumber);
        slip.setBorrowRecord(record);
        slip.setStudentId(record.getUser().getId());
        slip.setStudentName(record.getUser().getName());
        slip.setBookTitle(record.getBook().getTitle());
        slip.setCopyId(record.getCopy().getCopyId());
        slip.setPdfFileName(fileName);
        return slipRepository.save(slip);
    }

    /** Reads the PDF bytes back off disk so they can be attached to an email. */
    public byte[] readSlipPdfBytes(BorrowSlip slip) {
        try {
            File f = new File(slipsDirectory, slip.getPdfFileName());
            return java.nio.file.Files.readAllBytes(f.toPath());
        } catch (Exception e) {
            System.err.println("Could not read slip PDF for attachment: " + e.getMessage());
            return null;
        }
    }
}
