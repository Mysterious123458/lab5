package com.vladt.lab5.service;

import com.vladt.lab5.exception.BadRequestException;
import com.vladt.lab5.exception.NotFoundException;
import com.vladt.lab5.model.CityObject;
import com.vladt.lab5.model.CityReport;
import com.vladt.lab5.model.ObjectCondition;
import com.vladt.lab5.model.OrderStatus;
import com.vladt.lab5.model.ReportPriority;
import com.vladt.lab5.model.ReportStatus;
import com.vladt.lab5.model.WorkCrew;
import com.vladt.lab5.model.WorkOrder;
import com.vladt.lab5.repository.CityObjectRepository;
import com.vladt.lab5.repository.CityReportRepository;
import com.vladt.lab5.repository.WorkCrewRepository;
import com.vladt.lab5.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class MaintenanceService {

    private final CityReportRepository cityReportRepository;
    private final CityObjectRepository cityObjectRepository;
    private final WorkCrewRepository workCrewRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CrewLoadPolicy crewLoadPolicy;

    public MaintenanceService(CityReportRepository cityReportRepository,
                              CityObjectRepository cityObjectRepository,
                              WorkCrewRepository workCrewRepository,
                              WorkOrderRepository workOrderRepository,
                              CrewLoadPolicy crewLoadPolicy) {
        this.cityReportRepository = cityReportRepository;
        this.cityObjectRepository = cityObjectRepository;
        this.workCrewRepository = workCrewRepository;
        this.workOrderRepository = workOrderRepository;
        this.crewLoadPolicy = crewLoadPolicy;
    }

    public Assignment assign(Long reportId, Long crewId, LocalDate dueDate) {
        if (dueDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Срок наряда не может быть в прошлом");
        }
        CityReport report = requireReport(reportId);
        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.REJECTED) {
            throw new BadRequestException("Нельзя назначить бригаду на закрытое обращение");
        }
        if (workOrderRepository.existsByCityReport_IdAndStatusIn(reportId, CrewLoadPolicy.ACTIVE)) {
            throw new BadRequestException("По обращению уже есть активный наряд");
        }
        WorkCrew crew = requireCrew(crewId);
        crewLoadPolicy.ensureCanTake(crew, report.getCityObject().getType(), OrderStatus.ASSIGNED, null);
        Instant now = Instant.now();
        WorkOrder order = new WorkOrder();
        order.setCityReport(report);
        order.setWorkCrew(crew);
        order.setDueDate(dueDate);
        order.setStatus(OrderStatus.ASSIGNED);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        report.setStatus(ReportStatus.IN_PROGRESS);
        report.setUpdatedAt(now);
        return new Assignment(report, workOrderRepository.save(order));
    }

    public Completion complete(Long orderId, String resultDescription, ObjectCondition objectCondition) {
        if (resultDescription == null || resultDescription.isBlank()) {
            throw new BadRequestException("Нужно описать результат работы");
        }
        WorkOrder order = requireOrder(orderId);
        if (!order.getStatus().isActive()) {
            throw new BadRequestException("Завершить можно только активный наряд");
        }
        Instant now = Instant.now();
        order.setStatus(OrderStatus.DONE);
        order.setResultDescription(resultDescription.trim());
        order.setUpdatedAt(now);
        CityReport report = order.getCityReport();
        CityObject cityObject = report.getCityObject();
        cityObject.setCondition(objectCondition);
        boolean otherActive = workOrderRepository.existsByCityReport_IdAndStatusInAndIdNot(
                report.getId(), CrewLoadPolicy.ACTIVE, order.getId());
        if (!otherActive) {
            report.setStatus(ReportStatus.RESOLVED);
            report.setUpdatedAt(now);
        }
        return new Completion(order, report, cityObject);
    }

    public Rejection reject(Long reportId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Нужно указать причину отклонения");
        }
        CityReport report = requireReport(reportId);
        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw new BadRequestException("Нельзя отклонить уже выполненное обращение");
        }
        if (report.getStatus() == ReportStatus.REJECTED) {
            throw new BadRequestException("Обращение уже отклонено");
        }
        Instant now = Instant.now();
        String trimmedReason = reason.trim();
        List<WorkOrder> cancelled = workOrderRepository.findByCityReport_IdAndStatusIn(reportId, CrewLoadPolicy.ACTIVE);
        for (WorkOrder order : cancelled) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setResultDescription("Отменено: " + trimmedReason);
            order.setUpdatedAt(now);
        }
        report.setStatus(ReportStatus.REJECTED);
        report.setRejectionReason(trimmedReason);
        report.setUpdatedAt(now);
        return new Rejection(report, cancelled);
    }

    public Reassignment reassign(Long orderId, Long crewId) {
        WorkOrder order = requireOrder(orderId);
        if (!order.getStatus().isActive()) {
            throw new BadRequestException("Передать можно только активный наряд");
        }
        if (order.getWorkCrew().getId().equals(crewId)) {
            throw new BadRequestException("Наряд уже назначен этой бригаде");
        }
        WorkCrew crew = requireCrew(crewId);
        crewLoadPolicy.ensureCanTake(crew, order.getCityReport().getCityObject().getType(), order.getStatus(), order.getId());
        order.setWorkCrew(crew);
        order.setUpdatedAt(Instant.now());
        return new Reassignment(order, crew);
    }

    public Emergency markEmergency(Long cityObjectId) {
        CityObject cityObject = cityObjectRepository.findById(cityObjectId)
                .orElseThrow(() -> new NotFoundException("Городской объект не найден"));
        if (cityObject.getCondition() == ObjectCondition.EMERGENCY) {
            throw new BadRequestException("Объект уже отмечен как аварийный");
        }
        List<CityReport> openReports = cityReportRepository.findByCityObject_IdAndStatusInOrderByIdAsc(
                cityObjectId, List.of(ReportStatus.NEW, ReportStatus.IN_PROGRESS));
        if (openReports.isEmpty()) {
            throw new BadRequestException("Нет открытых обращений, аварийный статус не меняет работу служб");
        }
        Instant now = Instant.now();
        cityObject.setCondition(ObjectCondition.EMERGENCY);
        for (CityReport report : openReports) {
            report.setPriority(ReportPriority.HIGH);
            report.setUpdatedAt(now);
        }
        return new Emergency(cityObject, openReports);
    }

    private CityReport requireReport(Long id) {
        return cityReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Обращение не найдено"));
    }

    private WorkCrew requireCrew(Long id) {
        return workCrewRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Рабочая бригада не найдена"));
    }

    private WorkOrder requireOrder(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Наряд не найден"));
    }

    public record Assignment(CityReport report, WorkOrder workOrder) {
    }

    public record Completion(WorkOrder workOrder, CityReport report, CityObject cityObject) {
    }

    public record Rejection(CityReport report, List<WorkOrder> cancelledOrders) {
    }

    public record Reassignment(WorkOrder workOrder, WorkCrew workCrew) {
    }

    public record Emergency(CityObject cityObject, List<CityReport> reports) {
    }
}
