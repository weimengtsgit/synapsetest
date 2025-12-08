package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.exception.ResourceNotFoundException;
import com.synapsetest.testmanagement.exception.ValidationException;
import com.synapsetest.testmanagement.mapper.ResourcePoolMapper;
import com.synapsetest.testmanagement.model.ResourcePool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ResourcePool Service (MyBatis version)
 * Business logic for resource pool management
 *
 * Task: T033 [US1] Implement ResourcePoolService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourcePoolService {

    private final ResourcePoolMapper resourcePoolMapper;

    /**
     * Get all available resource pools
     */
    public List<ResourcePool> getAvailableResourcePools() {
        return resourcePoolMapper.selectByStatus(ResourcePool.ResourceStatus.AVAILABLE.name());
    }

    /**
     * Get resource pool by ID
     */
    public ResourcePool getResourcePoolById(String id) {
        ResourcePool pool = resourcePoolMapper.selectById(id);
        if (pool == null) {
            throw new ResourceNotFoundException("ResourcePool", "id", id);
        }
        return pool;
    }

    /**
     * Get resource pools by type
     */
    public List<ResourcePool> getResourcePoolsByType(String type) {
        return resourcePoolMapper.selectByType(type);
    }

    /**
     * Create a new resource pool
     */
    public ResourcePool createResourcePool(ResourcePool resourcePool) {
        log.info("Creating resource pool: {}", resourcePool.getName());
        resourcePool.setId(UUID.randomUUID().toString());
        resourcePool.setCreatedAt(LocalDateTime.now());
        resourcePool.setUpdatedAt(LocalDateTime.now());
        resourcePoolMapper.insert(resourcePool);
        return resourcePool;
    }

    /**
     * Allocate resources from pool
     */
    public ResourcePool allocateResources(String poolId, int count) {
        ResourcePool pool = getResourcePoolById(poolId);

        if (pool.getAvailableResources() < count) {
            throw new ValidationException(
                    String.format("Not enough resources in pool %s. Available: %d, Requested: %d",
                            pool.getName(), pool.getAvailableResources(), count));
        }

        pool.setAllocated(pool.getAllocated() + count);
        pool.setUpdatedAt(LocalDateTime.now());
        resourcePoolMapper.update(pool);
        log.info("Allocated {} resources from pool {}", count, pool.getName());

        return pool;
    }

    /**
     * Release resources back to pool
     */
    public ResourcePool releaseResources(String poolId, int count) {
        ResourcePool pool = getResourcePoolById(poolId);

        if (pool.getAllocated() < count) {
            throw new ValidationException(
                    String.format("Cannot release %d resources from pool %s. Currently allocated: %d",
                            count, pool.getName(), pool.getAllocated()));
        }

        pool.setAllocated(pool.getAllocated() - count);
        pool.setUpdatedAt(LocalDateTime.now());
        resourcePoolMapper.update(pool);
        log.info("Released {} resources to pool {}", count, pool.getName());

        return pool;
    }

    /**
     * Find best available resource pool for a given type
     */
    public ResourcePool findBestAvailablePool(String type, int requiredCapacity) {
        List<ResourcePool> pools = resourcePoolMapper.selectByStatusAndType(
                ResourcePool.ResourceStatus.AVAILABLE.name(), type);

        return pools.stream()
                .filter(pool -> pool.getAvailableResources() >= requiredCapacity)
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        String.format("No available resource pool of type %s with capacity %d",
                                type, requiredCapacity)));
    }
}
