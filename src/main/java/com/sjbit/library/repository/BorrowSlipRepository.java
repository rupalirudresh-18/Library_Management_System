package com.sjbit.library.repository;

import com.sjbit.library.entity.BorrowSlip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BorrowSlipRepository extends JpaRepository<BorrowSlip, Long> {
    List<BorrowSlip> findByStudentId(String studentId);
}
