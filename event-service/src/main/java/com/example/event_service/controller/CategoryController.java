package com.example.event_service.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.AOP.Annotation.HandleException;
import com.example.AOP.Annotation.Loggable;
import com.example.event_service.dto.EventDtos.CategoryDto;
import com.example.event_service.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @HandleException
    @Loggable(value = "GetAllCategories", logArguments = false, logResult = false)
    public ResponseEntity<List<CategoryDto>> getAll() {
        log.info("[PUBLIC] CategoryController.getAll()");
        return ResponseEntity.ok(categoryService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @HandleException
    @Loggable(value = "AddCategory", logArguments = true, logResult = false)
    public ResponseEntity<CategoryDto> add(@RequestBody CategoryDto dto) {
        log.info("[ADMIN] CategoryController.add()");
        return ResponseEntity.ok(categoryService.add(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @HandleException
    @Loggable(value = "UpdateCategory", logArguments = true, logResult = false)
    public ResponseEntity<Boolean> update(@PathVariable int id, @RequestBody CategoryDto dto) {
        log.info("[ADMIN] CategoryController.update() — id={}", id);
        return ResponseEntity.ok(categoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @HandleException
    @Loggable(value = "DeleteCategory", logArguments = true, logResult = false)
    public ResponseEntity<Boolean> delete(@PathVariable int id) {
        log.info("[ADMIN] CategoryController.delete() — id={}", id);
        return ResponseEntity.ok(categoryService.delete(id));
    }
}
