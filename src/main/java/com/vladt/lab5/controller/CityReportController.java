package com.vladt.lab5.controller;

import com.vladt.lab5.model.CityReport;
import com.vladt.lab5.model.ReportPriority;
import com.vladt.lab5.model.ReportStatus;
import com.vladt.lab5.service.CityReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/city-reports")
public class CityReportController {

    private final CityReportService cityReportService;

    public CityReportController(CityReportService cityReportService) {
        this.cityReportService = cityReportService;
    }

    @GetMapping
    public List<CityReportResponse> list() {
        return cityReportService.findAll().stream().map(CityReportController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CityReportResponse get(@PathVariable Long id) {
        return toResponse(cityReportService.findById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CityReportResponse> create(@Valid @RequestBody CityReportRequest request) {
        CityReport created = cityReportService.create(
                request.cityObjectId(), request.authorId(), request.description(), request.status(), request.priority());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CityReportResponse update(@PathVariable Long id, @RequestBody CityReportPatchRequest request) {
        return toResponse(cityReportService.update(
                id, request.cityObjectId(), request.authorId(), request.description(), request.status(), request.priority()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cityReportService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public static CityReportResponse toResponse(CityReport report) {
        return new CityReportResponse(
                report.getId(),
                report.getCityObject().getId(),
                report.getAuthor().getId(),
                report.getDescription(),
                report.getStatus(),
                report.getPriority(),
                report.getRejectionReason(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    public record CityReportRequest(
            @NotNull Long cityObjectId,
            @NotNull Long authorId,
            @NotBlank String description,
            ReportStatus status,
            ReportPriority priority
    ) {
    }

    public record CityReportPatchRequest(
            Long cityObjectId,
            Long authorId,
            String description,
            ReportStatus status,
            ReportPriority priority
    ) {
    }

    public record CityReportResponse(
            Long id,
            Long cityObjectId,
            Long authorId,
            String description,
            ReportStatus status,
            ReportPriority priority,
            String rejectionReason,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
