package com.vladt.lab5.service;

import com.vladt.lab5.exception.AuthException;
import com.vladt.lab5.exception.RefreshReplayException;
import com.vladt.lab5.model.AppUser;
import com.vladt.lab5.model.SessionStatus;
import com.vladt.lab5.model.UserSession;
import com.vladt.lab5.repository.UserRepository;
import com.vladt.lab5.repository.UserSessionRepository;
import com.vladt.lab5.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenPairService {

    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public TokenPairService(UserSessionRepository sessionRepository,
                            UserRepository userRepository,
                            JwtService jwtService) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }

    /**
     * Выдаёт пару токенов и сохраняет сессию ACTIVE. Метод вызывается из
     * транзакции регистрации, поэтому неудачное сохранение сессии откатывает и её.
     */
    @Transactional
    public TokenPair issueForUser(AppUser user, String deviceId) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtService.IssuedToken access = jwtService.createAccessToken(user, now);
        JwtService.IssuedToken refresh = jwtService.createRefreshToken(user, now);
        UserSession session = new UserSession();
        session.setUsername(user.getEmail());
        session.setDeviceId(deviceId);
        session.setAccessToken(access.value());
        session.setRefreshToken(refresh.value());
        session.setAccessTokenExpiry(access.expiry());
        session.setRefreshTokenExpiry(refresh.expiry());
        session.setStatus(SessionStatus.ACTIVE);
        sessionRepository.save(session);
        return new TokenPair(access.value(), refresh.value());
    }

    /**
     * Обменивает действующий refresh на новую пару. Поиск с блокировкой строки,
     * перевод старой сессии в USED и сохранение новой пары идут в одной транзакции:
     * техническая ошибка сохранения откатывает всё, включая USED.
     */
    @Transactional
    public TokenPair refresh(String refreshToken) throws RefreshReplayException {
        Claims claims;
        try {
            claims = jwtService.parseRefreshToken(refreshToken);
        } catch (JwtException exception) {
            throw new AuthException("Неверный, просроченный или неподходящий по типу refresh-токен");
        }
        UserSession session = sessionRepository.findByRefreshTokenForUpdate(refreshToken)
                .orElseThrow(() -> new AuthException("Неизвестная refresh-сессия"));
        if (session.getStatus() == SessionStatus.USED) {
            // Повторное использование: сессия переводится в REVOKED и это изменение
            // сохраняется, хотя ответом будет 401 без новой пары.
            session.setStatus(SessionStatus.REVOKED);
            throw new RefreshReplayException();
        }
        if (session.getStatus() == SessionStatus.REVOKED) {
            throw new AuthException("Refresh-сессия отозвана");
        }
        AppUser user = userRepository.findByEmailIgnoreCase(claims.getSubject())
                .orElseThrow(() -> new AuthException("Неизвестная refresh-сессия"));
        if (!claims.getSubject().equals(session.getUsername())) {
            throw new AuthException("Refresh-сессия принадлежит другому пользователю");
        }
        if (!user.getStatus().isActive()) {
            throw new AuthException("Учётная запись недоступна");
        }
        if (!session.getRefreshTokenExpiry().isAfter(Instant.now())) {
            throw new AuthException("Срок действия refresh-сессии истёк");
        }
        session.setStatus(SessionStatus.USED);
        return issueForUser(user, session.getDeviceId());
    }
}
