package com.sjbit.library.repository;

import com.sjbit.library.entity.BookCopy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookCopyRepository extends JpaRepository<BookCopy, String> {
}
