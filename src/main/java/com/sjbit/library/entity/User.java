package com.sjbit.library.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * A member of the library: student / librarian / staff / admin.
 * The "id" is the human login id (e.g. "s123"), matching the original
 * frontend's "Library ID" field, NOT an auto-increment surrogate key.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    private String id; // e.g. s123

    private String name;

    @Column(unique = true)
    private String email;

    // never serialized back out to the frontend
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String branch;
    private String year; // only meaningful for students
    private String semester; // only meaningful for students, e.g. "5th"
    private String contactNumber;
    private Boolean feesPaid = true; // only meaningful for students

    private LocalDate membershipExpiry;
}
