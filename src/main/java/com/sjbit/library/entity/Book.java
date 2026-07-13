package com.sjbit.library.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
public class Book {

    @Id
    private String id; // e.g. CS1001

    private String title;
    private String author;
    private Integer year;
    private String category;
    private String branch;

    private boolean online;
    private String onlineUrl;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookCopy> editions = new ArrayList<>();
}
