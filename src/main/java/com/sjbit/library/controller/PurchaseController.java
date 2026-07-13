package com.sjbit.library.controller;

import com.sjbit.library.entity.PurchaseRequest;
import com.sjbit.library.entity.PurchaseStatus;
import com.sjbit.library.service.PurchaseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase")
@CrossOrigin
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public List<PurchaseRequest> all() {
        return purchaseService.findAll();
    }

    @GetMapping("/status/{status}")
    public List<PurchaseRequest> byStatus(@PathVariable PurchaseStatus status) {
        return purchaseService.findByStatus(status);
    }

    @PostMapping
    public PurchaseRequest add(@RequestBody PurchaseRequest req) {
        return purchaseService.add(req);
    }

    @PostMapping("/{id}/approve")
    public PurchaseRequest approve(@PathVariable Long id) {
        return purchaseService.approve(id);
    }

    @DeleteMapping("/{id}")
    public void remove(@PathVariable Long id) {
        purchaseService.remove(id);
    }
}
