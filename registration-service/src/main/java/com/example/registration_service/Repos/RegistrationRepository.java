package com.example.registration_service.Repos;

import com.example.registration_service.Models.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Integer> {

    boolean existsByUserIdAndEventId(int userId, int eventId);

    long countByEventId(int eventId);

    Registration findByEventIdAndUserId(int eventId, int userId);

    List<Registration> findByEventId(int eventId);

    List<Registration> findByUserId(int userId);
}
