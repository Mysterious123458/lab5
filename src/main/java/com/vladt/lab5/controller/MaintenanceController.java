package com.vladt.lab5.controller;

import com.vladt.lab5.model.ObjectCondition;
import com.vladt.lab5.service.MaintenanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping("/city-reports/{id}/assign")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public AssignmentResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        MaintenanceService.Assignment result = maintenanceService.assign(id, request.workCrewId(), request.dueDate());
        return new AssignmentResponse(
                CityReportController.toResponse(result.report()),
                WorkOrderController.toResponse(result.workOrder())
        );
    }

    @PostMapping("/work-orders/{id}/complete")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public CompletionResponse complete(@PathVariable Long id, @Valid @RequestBody CompleteRequest request) {
        MaintenanceService.Completion result = maintenanceService.complete(id, request.resultDescription(), request.objectCondition());
        return new CompletionResponse(
                WorkOrderController.toResponse(result.workOrder()),
                CityReportController.toResponse(result.report()),
                new CityObjectController.CityObjectResponse(
                        result.cityObject().getId(),
                        result.cityObject().getAddress(),
                        result.cityObject().getType(),
                        result.cityObject().getCondition(),
                        result.cityObject().getCreatedAt()
                )
        );
    }

    @PostMapping("/city-reports/{id}/reject")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public RejectionResponse reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
        MaintenanceService.Rejection result = maintenanceService.reject(id, request.reason());
        return new RejectionResponse(
                CityReportController.toResponse(result.report()),
                result.cancelledOrders().stream().map(WorkOrderController::toResponse).toList()
        );
    }

    @PostMapping("/work-orders/{id}/reassign")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ReassignmentResponse reassign(@PathVariable Long id, @Valid @RequestBody ReassignRequest request) {
        MaintenanceService.Reassignment result = maintenanceService.reassign(id, request.workCrewId());
        return new ReassignmentResponse(
                WorkOrderController.toResponse(result.workOrder()),
                WorkCrewController.toResponse(result.workCrew())
        );
    }

    @PostMapping("/city-objects/{id}/mark-emergency")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public EmergencyResponse markEmergency(@PathVariable Long id) {
        MaintenanceService.Emergency result = maintenanceService.markEmergency(id);
        return new EmergencyResponse(
                new CityObjectController.CityObjectResponse(
                        result.cityObject().getId(),
                        result.cityObject().getAddress(),
                        result.cityObject().getType(),
                        result.cityObject().getCondition(),
                        result.cityObject().getCreatedAt()
                ),
                result.reports().stream().map(CityReportController::toResponse).toList()
        );
    }

    public record AssignRequest(@NotNull Long workCrewId, @NotNull LocalDate dueDate) {
    }

    public record CompleteRequest(@NotBlank String resultDescription, @NotNull ObjectCondition objectCondition) {
    }

    public record RejectRequest(@NotBlank String reason) {
    }

    public record ReassignRequest(@NotNull Long workCrewId) {
    }

    public record AssignmentResponse(CityReportController.CityReportResponse report, WorkOrderController.WorkOrderResponse workOrder) {
    }

    public record CompletionResponse(
            WorkOrderController.WorkOrderResponse workOrder,
            CityReportController.CityReportResponse report,
            CityObjectController.CityObjectResponse cityObject
    ) {
    }

    public record RejectionResponse(
            CityReportController.CityReportResponse report,
            List<WorkOrderController.WorkOrderResponse> cancelledOrders
    ) {
    }

    public record ReassignmentResponse(
            WorkOrderController.WorkOrderResponse workOrder,
            WorkCrewController.WorkCrewResponse workCrew
    ) {
    }

    public record EmergencyResponse(
            CityObjectController.CityObjectResponse cityObject,
            List<CityReportController.CityReportResponse> reports
    ) {
    }
}
