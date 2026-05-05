package com.example.event_service.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.event_service.domain.Category;
import com.example.event_service.dto.EventDtos.CategoryDto;
import com.example.event_service.repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDto> getAll() {
        return categoryRepository.findAll().stream().map(c -> {
            CategoryDto dto = new CategoryDto();
            dto.setId(c.getId()); dto.setName(c.getName());
            return dto;
        }).collect(Collectors.toList());
    }

    public CategoryDto add(CategoryDto dto) {
        Category c = Category.builder().name(dto.getName()).build();
        categoryRepository.save(c);
        dto.setId(c.getId());
        return dto;
    }

    public boolean update(int id, CategoryDto dto) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        c.setName(dto.getName());
        categoryRepository.save(c);
        return true;
    }

    public boolean delete(int id) {
        categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        categoryRepository.deleteById(id);
        return true;
    }
}
