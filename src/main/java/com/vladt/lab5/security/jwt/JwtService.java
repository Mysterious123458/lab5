package com.vladt.lab5.security.jwt;

import com.vladt.lab5.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        // hmacShaKeyFor отклоняет секрет короче 32 байт, поэтому слабый ключ не запустит приложение.
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public IssuedToken createAccessToken(AppUser user, Instant now) {
        return issue(user, "access", now, properties.accessTtl());
    }

    public IssuedToken createRefreshToken(AppUser user, Instant now) {
        return issue(user, "refresh", now, properties.refreshTtl());
    }

    public Claims parseAccessToken(String token) {
        return requireType(parse(token), "access");
    }

    public Claims parseRefreshToken(String token) {
        return requireType(parse(token), "refresh");
    }

    private IssuedToken issue(AppUser user, String type, Instant now, Duration ttl) {
        Instant expiry = now.plus(ttl).truncatedTo(ChronoUnit.SECONDS);
        String token = Jwts.builder()
                .subject(user.getEmail())
                .id(UUID.randomUUID().toString())
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, expiry);
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private Claims requireType(Claims claims, String expected) {
        if (!expected.equals(claims.get("type", String.class))) {
            throw new JwtException("Неподходящий тип токена");
        }
        return claims;
    }

    public record IssuedToken(String value, Instant expiry) {
    }
}
