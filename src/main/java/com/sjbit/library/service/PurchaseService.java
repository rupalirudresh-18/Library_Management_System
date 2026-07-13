package com.sjbit.library.service;

import com.sjbit.library.entity.PurchaseRequest;
import com.sjbit.library.entity.PurchaseStatus;
import com.sjbit.library.repository.PurchaseRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PurchaseService {

    private final PurchaseRequestRepository repository;

    public PurchaseService(PurchaseRequestRepository repository) {
        this.repository = repository;
    }

    public List<PurchaseRequest> findAll() {
        return repository.findAll();
    }

    public List<PurchaseRequest> findByStatus(PurchaseStatus status) {
        return repository.findByStatus(status);
    }

    public PurchaseRequest add(PurchaseRequest req) {
        req.setStatus(PurchaseStatus.PENDING);
        return repository.save(req);
    }

    public PurchaseRequest approve(Long id) {
        PurchaseRequest req = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase request not found"));
        req.setStatus(PurchaseStatus.APPROVED);
        return repository.save(req);
    }

    public void remove(Long id) {
        repository.deleteById(id);
    }
}
