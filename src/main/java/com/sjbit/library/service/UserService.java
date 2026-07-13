package com.sjbit.library.service;

import com.sjbit.library.entity.Role;
import com.sjbit.library.entity.User;
import com.sjbit.library.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User signup(User u) {
        if (userRepository.existsById(u.getId())) {
            throw new IllegalArgumentException("Library ID already exists");
        }
        if (u.getRole() == Role.STUDENT && u.getMembershipExpiry() == null) {
            u.setMembershipExpiry(LocalDate.now().plusYears(1));
        }
        return userRepository.save(u);
    }

    public User login(String id, String role, String password) {
        User u = userRepository.findByIdIgnoreCase(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found. Create an account if you are new."));

        if (!u.getRole().name().equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("This account belongs to role \"" + u.getRole() + "\". Select correct role.");
        }
        if (!u.getPassword().equals(password)) {
            throw new IllegalArgumentException("Incorrect password");
        }
        return u;
    }

    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User renewMembership(String userId, LocalDate newExpiry) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        u.setMembershipExpiry(newExpiry != null ? newExpiry : u.getMembershipExpiry());
        return userRepository.save(u);
    }

    public User setFeesPaid(String userId, boolean paid) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        u.setFeesPaid(paid);
        return userRepository.save(u);
    }
}
