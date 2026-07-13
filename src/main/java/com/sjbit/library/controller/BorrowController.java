package com.sjbit.library.controller;

import com.sjbit.library.entity.BorrowRecord;
import com.sjbit.library.entity.BorrowStatus;
import com.sjbit.library.service.BorrowService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrow")
@CrossOrigin
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    /** Student requests a book. Body: { "userId": "s123", "bookId": "CS1001" } */
    @PostMapping("/request")
    public BorrowRecord request(@RequestBody Map<String, String> body) {
        return borrowService.requestBorrow(body.get("userId"), body.get("bookId"));
    }

    @PostMapping("/{id}/approve")
    public BorrowRecord approve(@PathVariable Long id) {
        return borrowService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public BorrowRecord reject(@PathVariable Long id) {
        return borrowService.reject(id);
    }

    @PostMapping("/{id}/return")
    public BorrowRecord returnBook(@PathVariable Long id) {
        return borrowService.returnBook(id);
    }

    @PostMapping("/{id}/renew")
    public BorrowRecord renew(@PathVariable Long id) {
        return borrowService.renew(id);
    }

    @PostMapping("/{id}/mark-fine-paid")
    public BorrowRecord markFinePaid(@PathVariable Long id) {
        return borrowService.markFinePaid(id);
    }

    @GetMapping("/user/{userId}")
    public List<BorrowRecord> byUser(@PathVariable String userId) {
        return borrowService.findByUser(userId);
    }

    @GetMapping("/status/{status}")
    public List<BorrowRecord> byStatus(@PathVariable BorrowStatus status) {
        return borrowService.findByStatus(status);
    }

    @GetMapping
    public List<BorrowRecord> all() {
        return borrowService.findAll();
    }

    /** Download the generated borrow-slip PDF, e.g. for printing or emailing again. */
    @GetMapping("/slips/{fileName}")
    public ResponseEntity<FileSystemResource> downloadSlip(@PathVariable String fileName) {
        File file = new File("slips", fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=\"" + fileName + "\"")
                .body(new FileSystemResource(file));
    }
}
