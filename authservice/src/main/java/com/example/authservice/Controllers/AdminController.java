package com.example.authservice.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.authservice.Abstractions.Result;
import com.example.authservice.AuthDtos.*;
import com.example.authservice.Services.AdminService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@RestController
@RequestMapping("/api/admin/organizers")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<List<OrganizerDto>>> getAllOrganizers() {
        log.info("GET /api/admin/organizers - Fetching all organizers");
        Result<List<OrganizerDto>> result = adminService.getAllOrganizers();
        return result.isSuccess() 
            ? ResponseEntity.ok(result)
            : ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<OrganizerDto>> getOrganizerById(@PathVariable int userId) {
        log.info("GET /api/admin/organizers/{} - Fetching organizer by ID", userId);
        Result<OrganizerDto> result = adminService.getOrganizerById(userId);
        return result.isSuccess()
            ? ResponseEntity.ok(result)
            : ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }


    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Boolean>> updateOrganizer(
            @PathVariable int userId,
            @RequestBody UpdatedOrganzier updatedOrganzier) {
        log.info("PUT /api/admin/organizers/{} - Updating organizer", userId);
        Result<Boolean> result = adminService.updateOrganizer(userId, updatedOrganzier);
        return result.isSuccess()
            ? ResponseEntity.ok(result)
            : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }


    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Boolean>> deleteOrganizer(@PathVariable int userId) {
        log.info("DELETE /api/admin/organizers/{} - Deleting organizer", userId);
        Result<Boolean> result = adminService.deleteOrganizer(userId);
        return result.isSuccess()
            ? ResponseEntity.ok(result)
            : ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
}
