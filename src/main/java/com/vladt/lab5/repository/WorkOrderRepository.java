package com.vladt.lab5.repository;

import com.vladt.lab5.model.OrderStatus;
import com.vladt.lab5.model.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    List<WorkOrder> findAllByOrderByIdAsc();

    List<WorkOrder> findByWorkCrew_IdOrderByIdAsc(Long crewId);

    List<WorkOrder> findByCityReport_IdOrderByIdAsc(Long reportId);

    List<WorkOrder> findByCityReport_IdAndStatusIn(Long reportId, Collection<OrderStatus> statuses);

    long countByWorkCrew_IdAndStatusIn(Long crewId, Collection<OrderStatus> statuses);

    boolean existsByCityReport_IdAndStatusIn(Long reportId, Collection<OrderStatus> statuses);

    boolean existsByCityReport_IdAndStatusInAndIdNot(Long reportId, Collection<OrderStatus> statuses, Long id);

    void deleteByCityReport_Id(Long reportId);

    void deleteByWorkCrew_Id(Long crewId);
}
