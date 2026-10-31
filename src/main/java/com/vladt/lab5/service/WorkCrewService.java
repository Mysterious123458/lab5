package com.vladt.lab5.service;

import com.vladt.lab5.exception.BadRequestException;
import com.vladt.lab5.exception.NotFoundException;
import com.vladt.lab5.model.ObjectType;
import com.vladt.lab5.model.WorkCrew;
import com.vladt.lab5.model.WorkOrder;
import com.vladt.lab5.repository.WorkCrewRepository;
import com.vladt.lab5.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class WorkCrewService {

    private final WorkCrewRepository workCrewRepository;
    private final WorkOrderRepository workOrderRepository;

    public WorkCrewService(WorkCrewRepository workCrewRepository, WorkOrderRepository workOrderRepository) {
        this.workCrewRepository = workCrewRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkCrew> findAll() {
        return workCrewRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public WorkCrew findById(Long id) {
        return workCrewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Рабочая бригада не найдена"));
    }

    public WorkCrew create(String name, List<ObjectType> supportedTypes, int activeWorkLimit) {
        String trimmedName = name.trim();
        if (workCrewRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BadRequestException("Бригада с таким названием уже есть");
        }
        WorkCrew crew = new WorkCrew();
        crew.setName(trimmedName);
        crew.getSupportedTypes().addAll(distinctTypes(supportedTypes));
        crew.setActiveWorkLimit(activeWorkLimit);
        crew.setCreatedAt(Instant.now());
        return workCrewRepository.save(crew);
    }

    public WorkCrew update(Long id, String name, List<ObjectType> supportedTypes, Integer activeWorkLimit) {
        WorkCrew current = findById(id);
        if (name != null) {
            if (name.isBlank()) {
                throw new BadRequestException("Название бригады не может быть пустым");
            }
            String trimmedName = name.trim();
            if (workCrewRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
                throw new BadRequestException("Бригада с таким названием уже есть");
            }
            current.setName(trimmedName);
        }
        Set<ObjectType> nextTypes = supportedTypes == null
                ? current.getSupportedTypes()
                : distinctTypes(supportedTypes);
        int nextLimit = activeWorkLimit == null ? current.getActiveWorkLimit() : activeWorkLimit;
        if (nextLimit < 1) {
            throw new BadRequestException("Лимит активных работ должен быть больше нуля");
        }
        List<WorkOrder> orders = workOrderRepository.findByWorkCrew_IdOrderByIdAsc(id);
        long activeOrders = orders.stream().filter(order -> order.getStatus().isActive()).count();
        if (activeOrders > nextLimit) {
            throw new BadRequestException("Лимит меньше числа уже назначенных активных нарядов");
        }
        boolean lostType = orders.stream().anyMatch(order -> !nextTypes.contains(order.getCityReport().getCityObject().getType()));
        if (lostType) {
            throw new BadRequestException("Нельзя убрать тип объекта, пока по нему есть наряды этой бригады");
        }
        if (supportedTypes != null) {
            current.getSupportedTypes().clear();
            current.getSupportedTypes().addAll(nextTypes);
        }
        current.setActiveWorkLimit(nextLimit);
        return current;
    }

    public void delete(Long id) {
        WorkCrew crew = findById(id);
        workOrderRepository.deleteAll(workOrderRepository.findByWorkCrew_IdOrderByIdAsc(id));
        workCrewRepository.delete(crew);
    }

    private Set<ObjectType> distinctTypes(List<ObjectType> supportedTypes) {
        if (supportedTypes == null || supportedTypes.isEmpty()) {
            throw new BadRequestException("Нужно указать хотя бы один тип объекта");
        }
        return new LinkedHashSet<>(supportedTypes);
    }
}
