package com.edumind.backend.controller;

import com.edumind.backend.dto.FacultyCourseAllocationRequest;
import com.edumind.backend.dto.FacultyCourseAllocationResponse;
import com.edumind.backend.entity.FacultyCourseAllocation;
import com.edumind.backend.service.FacultyCourseAllocationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/faculty-allocations")
public class FacultyCourseAllocationController {

    private final FacultyCourseAllocationService allocationService;

    public FacultyCourseAllocationController(
            FacultyCourseAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping
    public ResponseEntity<List<FacultyCourseAllocationResponse>>
            getAllAllocations() {

        List<FacultyCourseAllocationResponse> response =
                allocationService.getAllAllocations()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultyCourseAllocationResponse>
            getAllocationById(@PathVariable Long id) {

        FacultyCourseAllocation allocation =
                allocationService.getAllocationById(id);

        return ResponseEntity.ok(toResponse(allocation));
    }

    @PostMapping
    public ResponseEntity<FacultyCourseAllocationResponse>
            createAllocation(
                    @RequestBody FacultyCourseAllocationRequest request) {

        FacultyCourseAllocation allocation =
                allocationService.createAllocation(
                        request.getFacultyId(),
                        request.getCourseId()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(allocation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAllocation(
            @PathVariable Long id) {

        allocationService.deleteAllocation(id);

        return ResponseEntity.noContent().build();
    }

    private FacultyCourseAllocationResponse toResponse(
            FacultyCourseAllocation allocation) {

        return new FacultyCourseAllocationResponse(
                allocation.getId(),

                allocation.getFaculty().getId(),
                allocation.getFaculty().getUser().getFullName(),
                allocation.getFaculty().getEmployeeId(),

                allocation.getCourse().getId(),
                allocation.getCourse().getName(),
                allocation.getCourse().getCode()
        );
    }
}
