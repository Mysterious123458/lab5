package com.vladt.lab5.controller;

import com.vladt.lab5.model.OrderStatus;
import com.vladt.lab5.model.WorkOrder;
import com.vladt.lab5.service.WorkOrderService;
import jakarta.validation.Valid;
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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public List<WorkOrderResponse> list() {
        return workOrderService.findAll().stream().map(WorkOrderController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public WorkOrderResponse get(@PathVariable Long id) {
        return toResponse(workOrderService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody WorkOrderRequest request) {
        WorkOrder created = workOrderService.create(
                request.cityReportId(), request.workCrewId(), request.dueDate(), request.status(), request.resultDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public WorkOrderResponse update(@PathVariable Long id, @RequestBody WorkOrderPatchRequest request) {
        return toResponse(workOrderService.update(
                id, request.cityReportId(), request.workCrewId(), request.dueDate(), request.status(), request.resultDescription()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public static WorkOrderResponse toResponse(WorkOrder order) {
        return new WorkOrderResponse(
                order.getId(),
                order.getCityReport().getId(),
                order.getWorkCrew().getId(),
                order.getDueDate(),
                order.getStatus(),
                order.getResultDescription(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public record WorkOrderRequest(
            @NotNull Long cityReportId,
            @NotNull Long workCrewId,
            @NotNull LocalDate dueDate,
            OrderStatus status,
            String resultDescription
    ) {
    }

    public record WorkOrderPatchRequest(
            Long cityReportId,
            Long workCrewId,
            LocalDate dueDate,
            OrderStatus status,
            String resultDescription
    ) {
    }

    public record WorkOrderResponse(
            Long id,
            Long cityReportId,
            Long workCrewId,
            LocalDate dueDate,
            OrderStatus status,
            String resultDescription,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
