package com.vladt.lab5.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "city_objects")
public class CityObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ObjectType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ObjectCondition condition;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public CityObject() {
    }

    public Long getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public ObjectType getType() {
        return type;
    }

    public void setType(ObjectType type) {
        this.type = type;
    }

    public ObjectCondition getCondition() {
        return condition;
    }

    public void setCondition(ObjectCondition condition) {
        this.condition = condition;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
