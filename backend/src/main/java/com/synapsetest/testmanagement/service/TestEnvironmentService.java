package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.exception.ResourceNotFoundException;
import com.synapsetest.testmanagement.mapper.TestEnvironmentMapper;
import com.synapsetest.testmanagement.model.TestEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * TestEnvironment Service (MyBatis version)
 * Business logic for test environment management
 *
 * Task: T031 [US1] Implement TestEnvironmentService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestEnvironmentService {

    private final TestEnvironmentMapper environmentMapper;

    /**
     * Get all available environments
     */
    public List<TestEnvironment> getAvailableEnvironments() {
        return environmentMapper.selectByStatus(TestEnvironment.Status.AVAILABLE.name());
    }

    /**
     * Get environment by ID
     */
    public TestEnvironment getEnvironmentById(String id) {
        TestEnvironment environment = environmentMapper.selectById(id);
        if (environment == null) {
            throw new ResourceNotFoundException("TestEnvironment", "id", id);
        }
        return environment;
    }

    /**
     * Get environment by name
     */
    public TestEnvironment getEnvironmentByName(String name) {
        TestEnvironment environment = environmentMapper.selectByName(name);
        if (environment == null) {
            throw new ResourceNotFoundException("TestEnvironment", "name", name);
        }
        return environment;
    }

    /**
     * Create a new environment
     */
    public TestEnvironment createEnvironment(TestEnvironment environment) {
        log.info("Creating test environment: {}", environment.getName());
        environment.setId(UUID.randomUUID().toString());
        environment.setCreatedAt(LocalDateTime.now());
        environment.setUpdatedAt(LocalDateTime.now());
        environmentMapper.insert(environment);
        return environment;
    }

    /**
     * Update environment
     */
    public TestEnvironment updateEnvironment(String id, TestEnvironment environmentUpdate) {
        log.info("Updating test environment: {}", id);
        TestEnvironment existing = getEnvironmentById(id);
        
        // Update fields
        if (environmentUpdate.getName() != null) {
            existing.setName(environmentUpdate.getName());
        }
        if (environmentUpdate.getDescription() != null) {
            existing.setDescription(environmentUpdate.getDescription());
        }
        if (environmentUpdate.getUrl() != null) {
            existing.setUrl(environmentUpdate.getUrl());
        }
        if (environmentUpdate.getConfig() != null) {
            existing.setConfig(environmentUpdate.getConfig());
        }
        if (environmentUpdate.getStatus() != null) {
            existing.setStatus(environmentUpdate.getStatus());
        }
        
        existing.setUpdatedAt(LocalDateTime.now());
        environmentMapper.update(existing);
        
        log.info("Updated environment: {}", id);
        return existing;
    }

    /**
     * Update environment status
     */
    public TestEnvironment updateEnvironmentStatus(String id, String status) {
        TestEnvironment environment = getEnvironmentById(id);
        environment.setStatus(status);
        environment.setUpdatedAt(LocalDateTime.now());
        environmentMapper.update(environment);
        log.info("Updated environment {} status to {}", id, status);
        return environment;
    }

    /**
     * Delete an environment
     */
    public void deleteEnvironment(String id) {
        log.info("Deleting test environment: {}", id);
        TestEnvironment environment = getEnvironmentById(id);
        environmentMapper.deleteById(id);
        log.info("Deleted environment: {}", id);
    }

    /**
     * Get all environments (including all statuses)
     */
    public List<TestEnvironment> getAllEnvironments() {
        return environmentMapper.selectAll();
    }
}
