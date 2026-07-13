package com.sjbit.library.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.PrintStream;

/**
 * Central place for every outgoing email in the system.
 * If library.mail.enabled=false in application.properties, emails are just
 * logged to the console instead of actually sent (handy while you're setting
 * up Gmail app-password credentials).
 */
@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${library.mail.from}")
    private String from;

    @Value("${library.mail.enabled:true}")
    private boolean enabled;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String subject, String body) {
        sendWithAttachment(to, subject, body, null, null);
    }

    /** Same as send(), but can optionally attach a PDF (or any file) as bytes. */
    public void sendWithAttachment(String to, String subject, String body, byte[] attachmentBytes, String attachmentFileName) {
        if (to == null || to.isBlank()) return;

        if (!enabled) {
            PrintStream out = System.out;
            out.println("---- [SIMULATED EMAIL] ----");
            out.println("To: " + to);
            out.println("Subject: " + subject);
            out.println(body);
            if (attachmentFileName != null) out.println("(would attach: " + attachmentFileName + ")");
            out.println("----------------------------");
            return;
        }

        try {
            if (attachmentBytes != null && attachmentFileName != null) {
                MimeMessage mime = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, true); // true = multipart, needed for attachments
                helper.setFrom(from);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(body);
                helper.addAttachment(attachmentFileName, new ByteArrayResource(attachmentBytes));
                mailSender.send(mime);
            } else {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(from);
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(body);
                mailSender.send(msg);
            }
        } catch (Exception e) {
            // Never let a mail failure break a borrow/return transaction
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendBorrowConfirmation(String to, String studentName, String bookTitle, String copyId, String dueDate, String slipNumber, byte[] slipPdfBytes, String slipFileName) {
        String subject = "Book Borrowed: " + bookTitle;
        String body = "Hi " + studentName + ",\n\n"
                + "You have successfully borrowed \"" + bookTitle + "\" (copy: " + copyId + ") from the SJBIT Library.\n"
                + "Please return it on or before: " + dueDate + "\n"
                + "Borrow slip number: " + slipNumber + "\n\n"
                + "Your borrow slip is attached to this email as a PDF - keep it for your records.\n\n"
                + "A fine will be charged automatically for every day past the due date if the book is not returned.\n\n"
                + "- SJBIT Library Management System";
        sendWithAttachment(to, subject, body, slipPdfBytes, slipFileName);
    }

    public void sendReturnConfirmation(String to, String studentName, String bookTitle, double fineAmount) {
        String subject = "Book Returned: " + bookTitle;
        String body = "Hi " + studentName + ",\n\n"
                + "Your return of \"" + bookTitle + "\" has been recorded successfully.\n"
                + (fineAmount > 0
                    ? "A fine of Rs. " + fineAmount + " has been applied for the delay. Please clear it at the library counter.\n\n"
                    : "No fine is due. Thank you for returning it on time!\n\n")
                + "- SJBIT Library Management System";
        send(to, subject, body);
    }

    public void sendOverdueReminder(String to, String studentName, String bookTitle, long daysLate, double fineAmount) {
        String subject = "Overdue Reminder: " + bookTitle;
        String body = "Hi " + studentName + ",\n\n"
                + "\"" + bookTitle + "\" is now " + daysLate + " day(s) overdue.\n"
                + "Current fine: Rs. " + fineAmount + " (increases daily until returned)\n\n"
                + "Please return the book at the earliest.\n\n"
                + "- SJBIT Library Management System";
        send(to, subject, body);
    }
}
