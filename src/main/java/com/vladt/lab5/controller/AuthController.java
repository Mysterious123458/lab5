package com.vladt.lab5.controller;

import com.vladt.lab5.exception.RefreshReplayException;
import com.vladt.lab5.model.AppUser;
import com.vladt.lab5.service.AuthService;
import com.vladt.lab5.service.TokenPairService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;
    private final TokenPairService tokenPairService;

    public AuthController(AuthService authService, TokenPairService tokenPairService) {
        this.authService = authService;
        this.tokenPairService = tokenPairService;
    }

    @PostMapping("/auth/login")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = authService.authenticate(request.email(), request.password());
        return toResponse(tokenPairService.issueForUser(user, request.deviceId()));
    }

    @PostMapping({"/auth/register", "/api/auth/register"})
    public ResponseEntity<TokenPairResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenPairService.TokenPair pair =
                authService.registerWithTokens(request.fullName(), request.email(), request.password(), request.deviceId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(pair));
    }

    @PostMapping("/auth/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) throws RefreshReplayException {
        return toResponse(tokenPairService.refresh(request.refreshToken()));
    }

    private TokenPairResponse toResponse(TokenPairService.TokenPair pair) {
        return new TokenPairResponse(pair.accessToken(), pair.refreshToken());
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password, String deviceId) {
    }

    public record RegisterRequest(@NotBlank String fullName, @NotBlank @Email String email,
                                  @NotBlank String password, String deviceId) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenPairResponse(String accessToken, String refreshToken) {
    }
}
