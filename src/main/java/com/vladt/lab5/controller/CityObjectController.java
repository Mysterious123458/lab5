package com.vladt.lab5.controller;

import com.vladt.lab5.model.CityObject;
import com.vladt.lab5.model.ObjectCondition;
import com.vladt.lab5.model.ObjectType;
import com.vladt.lab5.service.CityObjectService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/city-objects")
public class CityObjectController {

    private final CityObjectService cityObjectService;

    public CityObjectController(CityObjectService cityObjectService) {
        this.cityObjectService = cityObjectService;
    }

    @GetMapping
    public List<CityObjectResponse> list() {
        return cityObjectService.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CityObjectResponse get(@PathVariable Long id) {
        return toResponse(cityObjectService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<CityObjectResponse> create(@Valid @RequestBody CityObjectRequest request) {
        CityObject created = cityObjectService.create(request.address(), request.type(), request.condition());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public CityObjectResponse update(@PathVariable Long id, @RequestBody CityObjectPatchRequest request) {
        return toResponse(cityObjectService.update(id, request.address(), request.type(), request.condition()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cityObjectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private CityObjectResponse toResponse(CityObject cityObject) {
        return new CityObjectResponse(cityObject.getId(), cityObject.getAddress(), cityObject.getType(),
                cityObject.getCondition(), cityObject.getCreatedAt());
    }

    public record CityObjectRequest(@NotBlank String address, @NotNull ObjectType type, @NotNull ObjectCondition condition) {
    }

    public record CityObjectPatchRequest(String address, ObjectType type, ObjectCondition condition) {
    }

    public record CityObjectResponse(Long id, String address, ObjectType type, ObjectCondition condition, Instant createdAt) {
    }
}
