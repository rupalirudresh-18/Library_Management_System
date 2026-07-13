package com.sjbit.library.repository;

import com.sjbit.library.entity.PurchaseRequest;
import com.sjbit.library.entity.PurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    List<PurchaseRequest> findByStatus(PurchaseStatus status);
}
