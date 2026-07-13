package com.sjbit.library.scheduler;

import com.sjbit.library.entity.BorrowRecord;
import com.sjbit.library.entity.BorrowStatus;
import com.sjbit.library.repository.BorrowRecordRepository;
import com.sjbit.library.service.FineService;
import com.sjbit.library.service.MailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs once a day (09:00 server time). For every book still out (APPROVED,
 * not yet returned) and past its due date, this:
 *  1. Recalculates the running fine amount so it's always up to date
 *     even if the student never opens the return page.
 *  2. Sends an overdue reminder email once per day (won't spam every hour).
 *
 * cron = "sec min hour day month weekday"
 */
@Component
public class FineScheduler {

    private final BorrowRecordRepository borrowRecordRepository;
    private final FineService fineService;
    private final MailService mailService;

    public FineScheduler(BorrowRecordRepository borrowRecordRepository, FineService fineService, MailService mailService) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.fineService = fineService;
        this.mailService = mailService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void recalculateOverdueFinesAndNotify() {
        LocalDate today = LocalDate.now();
        List<BorrowRecord> issuedBooks = borrowRecordRepository.findByStatus(BorrowStatus.APPROVED);

        for (BorrowRecord record : issuedBooks) {
            long daysLate = fineService.daysLate(record.getDueDate(), today);
            if (daysLate <= 0) continue; // not overdue yet

            double fine = fineService.calculateFine(daysLate);
            record.setFineAmount(fine);
            record.setFinePaid(false);

            boolean alreadyRemindedToday = today.equals(record.getLastReminderSentDate());
            if (!alreadyRemindedToday) {
                mailService.sendOverdueReminder(
                        record.getUser().getEmail(), record.getUser().getName(),
                        record.getBook().getTitle(), daysLate, fine);
                record.setLastReminderSentDate(today);
            }

            borrowRecordRepository.save(record);
        }
    }
}
