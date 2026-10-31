package com.vladt.lab5.service;

import com.vladt.lab5.exception.BadRequestException;
import com.vladt.lab5.exception.NotFoundException;
import com.vladt.lab5.model.CityObject;
import com.vladt.lab5.model.ObjectCondition;
import com.vladt.lab5.model.ObjectType;
import com.vladt.lab5.repository.CityObjectRepository;
import com.vladt.lab5.repository.CityReportRepository;
import com.vladt.lab5.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class CityObjectService {

    private final CityObjectRepository cityObjectRepository;
    private final CityReportRepository cityReportRepository;
    private final WorkOrderRepository workOrderRepository;

    public CityObjectService(CityObjectRepository cityObjectRepository,
                             CityReportRepository cityReportRepository,
                             WorkOrderRepository workOrderRepository) {
        this.cityObjectRepository = cityObjectRepository;
        this.cityReportRepository = cityReportRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<CityObject> findAll() {
        return cityObjectRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public CityObject findById(Long id) {
        return cityObjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Городской объект не найден"));
    }

    public CityObject create(String address, ObjectType type, ObjectCondition condition) {
        CityObject cityObject = new CityObject();
        cityObject.setAddress(address.trim());
        cityObject.setType(type);
        cityObject.setCondition(condition);
        cityObject.setCreatedAt(Instant.now());
        return cityObjectRepository.save(cityObject);
    }

    public CityObject update(Long id, String address, ObjectType type, ObjectCondition condition) {
        CityObject current = findById(id);
        if (address != null) {
            if (address.isBlank()) {
                throw new BadRequestException("Адрес не может быть пустым");
            }
            current.setAddress(address.trim());
        }
        if (type != null) {
            current.setType(type);
        }
        if (condition != null) {
            current.setCondition(condition);
        }
        return current;
    }

    public void delete(Long id) {
        CityObject cityObject = findById(id);
        cityReportRepository.findByCityObject_IdOrderByIdAsc(id).forEach(report ->
                workOrderRepository.deleteAll(workOrderRepository.findByCityReport_IdOrderByIdAsc(report.getId())));
        cityReportRepository.deleteAll(cityReportRepository.findByCityObject_IdOrderByIdAsc(id));
        cityObjectRepository.delete(cityObject);
    }
}
