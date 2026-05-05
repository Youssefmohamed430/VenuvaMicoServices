package com.example.event_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.event_service.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
