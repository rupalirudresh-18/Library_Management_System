package com.sjbit.library.entity;

public enum BorrowStatus {
    PENDING,    // student requested, awaiting librarian approval
    APPROVED,   // issued, book is out with the student
    RETURNED,   // book came back
    REJECTED,   // librarian rejected the request
    LOST,       // marked lost/damaged by librarian
    DAMAGED
}
