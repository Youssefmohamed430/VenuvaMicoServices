package com.example.authservice.Repos;
import com.example.authservice.Models.UserDetails.*;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface UserRepository extends JpaRepository<User , Integer> {
    Optional<User> findByEmail(String email);

    default Optional<List<User>> findAll(Predicate<User> predicate) {
        return Optional.of(findAll().stream()
                .filter(predicate)
                .collect(Collectors.toList()));
    }
    List<User> findByRole(Roles role);
}