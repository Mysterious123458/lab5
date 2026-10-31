package com.vladt.lab5.repository;

import com.vladt.lab5.model.WorkCrew;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkCrewRepository extends JpaRepository<WorkCrew, Long> {

    List<WorkCrew> findAllByOrderByIdAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
