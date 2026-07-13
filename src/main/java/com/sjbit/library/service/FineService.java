package com.sjbit.library.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Single source of truth for how a fine is calculated, so the same rule
 * applies whether it's computed at return-time or by the nightly scheduler.
 */
@Service
public class FineService {

    @Value("${library.fine.per-day}")
    private double perDayRate;

    @Value("${library.borrow.duration-days}")
    private int borrowDurationDays;

    public int getBorrowDurationDays() {
        return borrowDurationDays;
    }

    /** Days late as of "asOf" (0 if not late). */
    public long daysLate(LocalDate dueDate, LocalDate asOf) {
        if (dueDate == null || asOf == null) return 0;
        long diff = ChronoUnit.DAYS.between(dueDate, asOf);
        return Math.max(0, diff);
    }

    /** Fine amount for a given number of late days. */
    public double calculateFine(long daysLate) {
        return Math.round(daysLate * perDayRate * 100.0) / 100.0;
    }

    public double calculateFine(LocalDate dueDate, LocalDate asOf) {
        return calculateFine(daysLate(dueDate, asOf));
    }
}
