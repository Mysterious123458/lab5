package com.vladt.lab5.controller;

import com.vladt.lab5.model.AppRole;
import com.vladt.lab5.model.AppUser;
import com.vladt.lab5.model.UserStatus;
import com.vladt.lab5.security.CurrentUser;
import com.vladt.lab5.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public List<UserResponse> list() {
        return userService.findAll().stream().map(UserController::toResponse).toList();
    }

    @GetMapping("/me")
    public UserResponse me() {
        return toResponse(currentUser.get());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public UserResponse get(@PathVariable Long id) {
        return toResponse(userService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        AppUser created = userService.create(request.fullName(), request.email(), request.password(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id, @RequestBody UserPatchRequest request) {
        return toResponse(userService.update(id, request.fullName(), request.email()));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return toResponse(userService.changeRole(id, request.role()));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse block(@PathVariable Long id) {
        return toResponse(userService.block(id));
    }

    @PostMapping("/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse unblock(@PathVariable Long id) {
        return toResponse(userService.unblock(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public static UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getRole(), user.getStatus(), user.getCreatedAt());
    }

    public record UserRequest(@NotBlank String fullName, @NotBlank @Email String email,
                              @NotBlank String password, @NotNull AppRole role) {
    }

    public record UserPatchRequest(String fullName, @Email String email) {
    }

    public record RoleRequest(@NotNull AppRole role) {
    }

    public record UserResponse(Long id, String fullName, String email, AppRole role, UserStatus status,
                               Instant createdAt) {
    }
}
