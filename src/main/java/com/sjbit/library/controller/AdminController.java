package com.sjbit.library.controller;

import com.sjbit.library.entity.*;
import com.sjbit.library.repository.BorrowRecordRepository;
import com.sjbit.library.service.BookService;
import com.sjbit.library.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private final UserService userService;
    private final BookService bookService;
    private final BorrowRecordRepository borrowRecordRepository;

    public AdminController(UserService userService, BookService bookService, BorrowRecordRepository borrowRecordRepository) {
        this.userService = userService;
        this.bookService = bookService;
        this.borrowRecordRepository = borrowRecordRepository;
    }

    @GetMapping("/members")
    public List<User> members() {
        return userService.findAll();
    }

    @PostMapping("/members/{id}/renew")
    public User renew(@PathVariable String id, @RequestBody Map<String, String> body) {
        LocalDate newExpiry = body.get("expiry") != null ? LocalDate.parse(body.get("expiry")) : null;
        return userService.renewMembership(id, newExpiry);
    }

    @PostMapping("/members/{id}/fees")
    public User setFeesPaid(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        return userService.setFeesPaid(id, Boolean.TRUE.equals(body.get("paid")));
    }

    /** Live dashboard numbers + analytics breakdowns for the admin overview panel. */
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        List<Book> allBooks = bookService.findAll();
        List<User> allMembers = userService.findAll();
        List<BorrowRecord> allRecords = borrowRecordRepository.findAll();

        long borrowed = allRecords.stream().filter(r -> r.getStatus() == BorrowStatus.APPROVED).count();
        long pending = allRecords.stream().filter(r -> r.getStatus() == BorrowStatus.PENDING).count();
        long damagedOrLost = allRecords.stream()
                .filter(r -> r.getStatus() == BorrowStatus.DAMAGED || r.getStatus() == BorrowStatus.LOST).count();
        long unpaidFines = allRecords.stream()
                .filter(r -> r.getFineAmount() != null && r.getFineAmount() > 0 && !r.isFinePaid()).count();

        // books grouped by branch (for the branch distribution chart)
        Map<String, Long> booksByBranch = allBooks.stream()
                .collect(Collectors.groupingBy(b -> b.getBranch() == null || b.getBranch().isBlank() ? "Other" : b.getBranch(),
                        Collectors.counting()));

        // borrow records grouped by status (for the status breakdown chart)
        Map<String, Long> borrowStatusCounts = new LinkedHashMap<>();
        for (BorrowStatus s : BorrowStatus.values()) {
            long count = allRecords.stream().filter(r -> r.getStatus() == s).count();
            if (count > 0) borrowStatusCounts.put(s.name(), count);
        }

        // members grouped by role (for the member composition chart)
        Map<String, Long> membersByRole = allMembers.stream()
                .collect(Collectors.groupingBy(m -> m.getRole().name(), Collectors.counting()));

        double finesCollected = allRecords.stream()
                .filter(r -> Boolean.TRUE.equals(r.isFinePaid()) && r.getFineAmount() != null)
                .mapToDouble(BorrowRecord::getFineAmount).sum();
        double finesPending = allRecords.stream()
                .filter(r -> !r.isFinePaid() && r.getFineAmount() != null)
                .mapToDouble(BorrowRecord::getFineAmount).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalBooks", allBooks.size());
        result.put("totalMembers", allMembers.size());
        result.put("borrowed", borrowed);
        result.put("pending", pending);
        result.put("damagedOrLost", damagedOrLost);
        result.put("unpaidFinesCount", unpaidFines);
        result.put("booksByBranch", booksByBranch);
        result.put("borrowStatusCounts", borrowStatusCounts);
        result.put("membersByRole", membersByRole);
        result.put("finesCollected", Math.round(finesCollected * 100.0) / 100.0);
        result.put("finesPending", Math.round(finesPending * 100.0) / 100.0);
        return result;
    }
}
