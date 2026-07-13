package com.sjbit.library.repository;

import com.sjbit.library.entity.Role;
import com.sjbit.library.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByIdIgnoreCase(String id);
    List<User> findByRole(Role role);
}
