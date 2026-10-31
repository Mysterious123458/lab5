package com.vladt.lab5.service;

import com.vladt.lab5.exception.BadRequestException;
import com.vladt.lab5.exception.NotFoundException;
import com.vladt.lab5.model.CityReport;
import com.vladt.lab5.model.OrderStatus;
import com.vladt.lab5.model.WorkCrew;
import com.vladt.lab5.model.WorkOrder;
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
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final CityReportRepository cityReportRepository;
    private final WorkCrewRepository workCrewRepository;
    private final CrewLoadPolicy crewLoadPolicy;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            CityReportRepository cityReportRepository,
                            WorkCrewRepository workCrewRepository,
                            CrewLoadPolicy crewLoadPolicy) {
        this.workOrderRepository = workOrderRepository;
        this.cityReportRepository = cityReportRepository;
        this.workCrewRepository = workCrewRepository;
        this.crewLoadPolicy = crewLoadPolicy;
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> findAll() {
        return workOrderRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public WorkOrder findById(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Наряд не найден"));
    }

    public WorkOrder create(Long cityReportId, Long workCrewId, LocalDate dueDate, OrderStatus status, String resultDescription) {
        OrderStatus nextStatus = status == null ? OrderStatus.ASSIGNED : status;
        rejectTerminalStatus(nextStatus);
        CityReport report = requireReport(cityReportId);
        WorkCrew crew = requireCrew(workCrewId);
        requireFutureOrToday(dueDate);
        crewLoadPolicy.ensureCanTake(crew, report.getCityObject().getType(), nextStatus, null);
        Instant now = Instant.now();
        WorkOrder order = new WorkOrder();
        order.setCityReport(report);
        order.setWorkCrew(crew);
        order.setDueDate(dueDate);
        order.setStatus(nextStatus);
        order.setResultDescription(clean(resultDescription));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return workOrderRepository.save(order);
    }

    public WorkOrder update(Long id, Long cityReportId, Long workCrewId, LocalDate dueDate, OrderStatus status, String resultDescription) {
        WorkOrder current = findById(id);
        if (status != null) {
            rejectTerminalStatus(status);
        }
        CityReport report = cityReportId == null ? current.getCityReport() : requireReport(cityReportId);
        WorkCrew crew = workCrewId == null ? current.getWorkCrew() : requireCrew(workCrewId);
        OrderStatus nextStatus = status == null ? current.getStatus() : status;
        LocalDate nextDueDate = dueDate == null ? current.getDueDate() : dueDate;
        requireFutureOrToday(nextDueDate);
        crewLoadPolicy.ensureCanTake(crew, report.getCityObject().getType(), nextStatus, id);
        current.setCityReport(report);
        current.setWorkCrew(crew);
        current.setDueDate(nextDueDate);
        current.setStatus(nextStatus);
        if (resultDescription != null) {
            current.setResultDescription(clean(resultDescription));
        }
        current.setUpdatedAt(Instant.now());
        return current;
    }

    public void delete(Long id) {
        workOrderRepository.delete(findById(id));
    }

    private void rejectTerminalStatus(OrderStatus status) {
        if (status == OrderStatus.DONE || status == OrderStatus.CANCELLED) {
            throw new BadRequestException("Завершение и отмена наряда выполняются бизнес-операциями");
        }
    }

    private void requireFutureOrToday(LocalDate dueDate) {
        if (dueDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Срок наряда не может быть в прошлом");
        }
    }

    private CityReport requireReport(Long id) {
        return cityReportRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Обращение не найдено"));
    }

    private WorkCrew requireCrew(Long id) {
        return workCrewRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Рабочая бригада не найдена"));
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
