package com.synapsetest.testmanagement.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Base Entity
 * Common fields for all entities
 * MyBatis POJO (removed JPA annotations)
 */
@Data
public abstract class BaseEntity {

    private String id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
