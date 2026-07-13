package com.sjbit.library.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "book_copies")
@Data
@NoArgsConstructor
public class BookCopy {

    @Id
    private String copyId; // e.g. CS1001-1

    private String edition;
    private Integer year;
    private boolean available = true;

    @ManyToOne
    @JoinColumn(name = "book_id")
    @JsonIgnore // avoid circular JSON book->editions->book
    private Book book;
}
