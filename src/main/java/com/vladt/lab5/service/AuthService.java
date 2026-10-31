package com.vladt.lab5.service;

import com.vladt.lab5.exception.AuthException;
import com.vladt.lab5.exception.ConflictException;
import com.vladt.lab5.model.AppRole;
import com.vladt.lab5.model.AppUser;
import com.vladt.lab5.model.UserStatus;
import com.vladt.lab5.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final TokenPairService tokenPairService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordPolicy passwordPolicy,
                       TokenPairService tokenPairService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.tokenPairService = tokenPairService;
    }

    /**
     * Регистрация сразу с выдачей пары токенов. Одна транзакция: если сессию
     * сохранить не удалось, новая учётная запись тоже откатывается.
     */
    public TokenPairService.TokenPair registerWithTokens(String fullName, String email, String password, String deviceId) {
        AppUser user = register(fullName, email, password);
        return tokenPairService.issueForUser(user, deviceId);
    }

    public AppUser register(String fullName, String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Пользователь с таким email уже есть");
        }
        passwordPolicy.ensureValid(password);
        AppUser user = new AppUser();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        // Самостоятельная регистрация всегда создаёт жителя, роль задаёт только администратор.
        user.setRole(AppRole.RESIDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AppUser authenticate(String email, String password) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(email)).orElse(null);
        // Единый ответ для отсутствующей, заблокированной учётной записи и неверного пароля.
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())
                || !user.getStatus().isActive()) {
            throw new AuthException("Неверные учётные данные или учётная запись недоступна");
        }
        return user;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
