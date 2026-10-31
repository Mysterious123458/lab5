package com.vladt.lab5.service;

import com.vladt.lab5.exception.BadRequestException;
import com.vladt.lab5.exception.NotFoundException;
import com.vladt.lab5.model.AppRole;
import com.vladt.lab5.model.AppUser;
import com.vladt.lab5.model.CityObject;
import com.vladt.lab5.model.CityReport;
import com.vladt.lab5.model.ReportPriority;
import com.vladt.lab5.model.ReportStatus;
import com.vladt.lab5.repository.CityObjectRepository;
import com.vladt.lab5.repository.CityReportRepository;
import com.vladt.lab5.repository.UserRepository;
import com.vladt.lab5.repository.WorkOrderRepository;
import com.vladt.lab5.security.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class CityReportService {

    private final CityReportRepository cityReportRepository;
    private final CityObjectRepository cityObjectRepository;
    private final UserRepository userRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CurrentUser currentUser;

    public CityReportService(CityReportRepository cityReportRepository,
                             CityObjectRepository cityObjectRepository,
                             UserRepository userRepository,
                             WorkOrderRepository workOrderRepository,
                             CurrentUser currentUser) {
        this.cityReportRepository = cityReportRepository;
        this.cityObjectRepository = cityObjectRepository;
        this.userRepository = userRepository;
        this.workOrderRepository = workOrderRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<CityReport> findAll() {
        return cityReportRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public CityReport findById(Long id) {
        return cityReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Обращение не найдено"));
    }

    public CityReport create(Long cityObjectId, Long authorId, String description, ReportStatus status, ReportPriority priority) {
        AppUser current = currentUser.get();
        AppUser author;
        if (current.getRole() == AppRole.RESIDENT) {
            if (authorId != null && !authorId.equals(current.getId())) {
                throw new AccessDeniedException("Житель может создать обращение только от своего имени");
            }
            author = current;
        } else {
            author = requireAuthor(authorId);
        }
        Instant now = Instant.now();
        CityReport report = new CityReport();
        report.setCityObject(requireObject(cityObjectId));
        report.setAuthor(author);
        report.setDescription(description.trim());
        report.setStatus(status == null ? ReportStatus.NEW : status);
        report.setPriority(priority == null ? ReportPriority.NORMAL : priority);
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        rejectTerminalStatus(report.getStatus());
        return cityReportRepository.save(report);
    }

    public CityReport update(Long id, Long cityObjectId, Long authorId, String description, ReportStatus status, ReportPriority priority) {
        AppUser current = currentUser.get();
        CityReport currentReport = findById(id);
        if (current.getRole() == AppRole.RESIDENT) {
            if (!currentReport.getAuthor().getId().equals(current.getId())) {
                throw new AccessDeniedException("Житель может изменять только свои обращения");
            }
            if (authorId != null && !authorId.equals(current.getId())) {
                throw new AccessDeniedException("Житель не может менять автора обращения");
            }
        }
        if (description != null && description.isBlank()) {
            throw new BadRequestException("Описание проблемы не может быть пустым");
        }
        if (cityObjectId != null) {
            currentReport.setCityObject(requireObject(cityObjectId));
        }
        if (authorId != null && current.getRole() != AppRole.RESIDENT) {
            currentReport.setAuthor(requireAuthor(authorId));
        }
        if (description != null) {
            currentReport.setDescription(description.trim());
        }
        if (status != null) {
            rejectTerminalStatus(status);
            currentReport.setStatus(status);
        }
        if (priority != null) {
            currentReport.setPriority(priority);
        }
        currentReport.setUpdatedAt(Instant.now());
        return currentReport;
    }

    public void delete(Long id) {
        AppUser current = currentUser.get();
        CityReport report = findById(id);
        if (current.getRole() == AppRole.RESIDENT && !report.getAuthor().getId().equals(current.getId())) {
            throw new AccessDeniedException("Житель может удалять только свои обращения");
        }
        workOrderRepository.deleteAll(workOrderRepository.findByCityReport_IdOrderByIdAsc(id));
        cityReportRepository.delete(report);
    }

    private void rejectTerminalStatus(ReportStatus status) {
        if (status == ReportStatus.RESOLVED || status == ReportStatus.REJECTED) {
            throw new BadRequestException("Закрыть обращение можно только бизнес-операцией завершения или отклонения");
        }
    }

    private CityObject requireObject(Long id) {
        return cityObjectRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Городской объект не найден"));
    }

    private AppUser requireAuthor(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Автор обращения не найден"));
    }
}
