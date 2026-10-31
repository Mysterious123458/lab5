package com.vladt.lab5.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "work_crews")
public class WorkCrew {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "work_crew_supported_types", joinColumns = @JoinColumn(name = "crew_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false, length = 32)
    private Set<ObjectType> supportedTypes = new LinkedHashSet<>();

    @Column(name = "active_work_limit", nullable = false)
    private int activeWorkLimit;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WorkCrew() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<ObjectType> getSupportedTypes() {
        return supportedTypes;
    }

    public int getActiveWorkLimit() {
        return activeWorkLimit;
    }

    public void setActiveWorkLimit(int activeWorkLimit) {
        this.activeWorkLimit = activeWorkLimit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
