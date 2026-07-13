package com.sjbit.library.repository;

import com.sjbit.library.entity.BorrowRecord;
import com.sjbit.library.entity.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findByUser_Id(String userId);
    List<BorrowRecord> findByStatus(BorrowStatus status);
    List<BorrowRecord> findByUser_IdAndStatus(String userId, BorrowStatus status);
}
