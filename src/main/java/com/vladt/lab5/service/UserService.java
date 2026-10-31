package com.vladt.lab5.service;

import com.vladt.lab5.exception.BadRequestException;
import com.vladt.lab5.exception.ConflictException;
import com.vladt.lab5.exception.NotFoundException;
import com.vladt.lab5.model.AppRole;
import com.vladt.lab5.model.AppUser;
import com.vladt.lab5.model.UserStatus;
import com.vladt.lab5.repository.CityReportRepository;
import com.vladt.lab5.repository.UserRepository;
import com.vladt.lab5.repository.WorkOrderRepository;
import com.vladt.lab5.security.CurrentUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final CityReportRepository cityReportRepository;
    private final WorkOrderRepository workOrderRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final CurrentUser currentUser;

    public UserService(UserRepository userRepository,
                       CityReportRepository cityReportRepository,
                       WorkOrderRepository workOrderRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordPolicy passwordPolicy,
                       CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.cityReportRepository = cityReportRepository;
        this.workOrderRepository = workOrderRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<AppUser> findAll() {
        return userRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public AppUser findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    public AppUser create(String fullName, String email, String password, AppRole role) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Пользователь с таким email уже есть");
        }
        passwordPolicy.ensureValid(password);
        AppUser user = new AppUser();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    public AppUser update(Long id, String fullName, String email) {
        AppUser current = findById(id);
        if (fullName != null && fullName.isBlank()) {
            throw new BadRequestException("Имя пользователя не может быть пустым");
        }
        if (email != null && email.isBlank()) {
            throw new BadRequestException("Email не может быть пустым");
        }
        String nextEmail = email == null ? current.getEmail() : normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(nextEmail, id)) {
            throw new ConflictException("Пользователь с таким email уже есть");
        }
        if (fullName != null) {
            current.setFullName(fullName.trim());
        }
        current.setEmail(nextEmail);
        return current;
    }

    public AppUser changeRole(Long id, AppRole role) {
        requireNotSelf(id, "Нельзя менять роль своей учётной записи");
        AppUser user = findById(id);
        user.setRole(role);
        return user;
    }

    public AppUser block(Long id) {
        requireNotSelf(id, "Нельзя заблокировать свою учётную запись");
        AppUser user = findById(id);
        user.setStatus(UserStatus.BLOCKED);
        return user;
    }

    public AppUser unblock(Long id) {
        AppUser user = findById(id);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    public void delete(Long id) {
        requireNotSelf(id, "Нельзя удалить свою учётную запись");
        AppUser user = findById(id);
        cityReportRepository.findByAuthor_IdOrderByIdAsc(id).forEach(report ->
                workOrderRepository.deleteAll(workOrderRepository.findByCityReport_IdOrderByIdAsc(report.getId())));
        cityReportRepository.deleteAll(cityReportRepository.findByAuthor_IdOrderByIdAsc(id));
        userRepository.delete(user);
    }

    private void requireNotSelf(Long id, String message) {
        if (id.equals(currentUser.get().getId())) {
            throw new BadRequestException(message);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
