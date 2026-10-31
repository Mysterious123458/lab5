package com.vladt.lab5.controller;

import com.vladt.lab5.model.ObjectType;
import com.vladt.lab5.model.WorkCrew;
import com.vladt.lab5.service.WorkCrewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/work-crews")
public class WorkCrewController {

    private final WorkCrewService workCrewService;

    public WorkCrewController(WorkCrewService workCrewService) {
        this.workCrewService = workCrewService;
    }

    @GetMapping
    public List<WorkCrewResponse> list() {
        return workCrewService.findAll().stream().map(WorkCrewController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public WorkCrewResponse get(@PathVariable Long id) {
        return toResponse(workCrewService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<WorkCrewResponse> create(@Valid @RequestBody WorkCrewRequest request) {
        WorkCrew created = workCrewService.create(request.name(), request.supportedTypes(), request.activeWorkLimit());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public WorkCrewResponse update(@PathVariable Long id, @RequestBody WorkCrewPatchRequest request) {
        return toResponse(workCrewService.update(id, request.name(), request.supportedTypes(), request.activeWorkLimit()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workCrewService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public static WorkCrewResponse toResponse(WorkCrew crew) {
        return new WorkCrewResponse(crew.getId(), crew.getName(), List.copyOf(crew.getSupportedTypes()),
                crew.getActiveWorkLimit(), crew.getCreatedAt());
    }

    public record WorkCrewRequest(
            @NotBlank String name,
            @NotEmpty List<@NotNull ObjectType> supportedTypes,
            @NotNull @Min(1) Integer activeWorkLimit
    ) {
    }

    public record WorkCrewPatchRequest(String name, List<ObjectType> supportedTypes, @Min(1) Integer activeWorkLimit) {
    }

    public record WorkCrewResponse(Long id, String name, List<ObjectType> supportedTypes, int activeWorkLimit, Instant createdAt) {
    }
}
