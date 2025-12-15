package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.mapper.TestCaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test Case Number Generator
 * Generates unique case numbers in format: TC{yyyyMMdd}{seq}
 * Example: TC20231215001, TC20231215002, ...
 * 
 * Thread-safe implementation using AtomicInteger
 * Now initializes sequence from database on date change or service restart
 */
@Slf4j
@Service
public class CaseNumberGenerator {

    private static final String PREFIX = "TC";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    @Autowired
    private TestCaseMapper testCaseMapper;
    
    // Sequence counter for current date
    private final AtomicInteger sequenceCounter = new AtomicInteger(0);
    
    // Track the current date to reset counter on date change
    private volatile String currentDate = "";
    
    // Flag to track if we've initialized from database for current date
    private volatile boolean initializedFromDb = false;

    /**
     * Generate next case number
     * Format: TC{yyyyMMdd}{seq}
     * 
     * @return Generated case number (e.g., TC20231215001)
     */
    public synchronized String generateNextCaseNumber() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        
        // Reset counter if date has changed
        if (!today.equals(currentDate)) {
            currentDate = today;
            initializedFromDb = false;  // Need to re-initialize from DB for new date
            log.info("Date changed to {}, will initialize from database", today);
        }
        
        // Initialize from database if not yet done for current date
        if (!initializedFromDb) {
            initializeSequenceFromDatabase(today);
            initializedFromDb = true;
        }
        
        // Increment and get next sequence number
        int sequence = sequenceCounter.incrementAndGet();
        
        // Format: TC + yyyyMMdd + 3-digit sequence (001, 002, ...)
        String caseNumber = String.format("%s%s%03d", PREFIX, today, sequence);
        
        log.info("Generated case number: {}", caseNumber);
        return caseNumber;
    }
    
    /**
     * Initialize sequence counter from database
     * Queries for the maximum existing case number for today and sets counter accordingly
     * 
     * @param dateStr Date string in yyyyMMdd format
     */
    private void initializeSequenceFromDatabase(String dateStr) {
        try {
            String prefix = PREFIX + dateStr;
            String maxCaseNumber = testCaseMapper.findMaxCaseNumberByPrefix(prefix);
            
            if (maxCaseNumber != null && maxCaseNumber.startsWith(prefix)) {
                // Extract the sequence number from the case number
                // Format: TC20231215001 -> extract "001"
                String sequenceStr = maxCaseNumber.substring(prefix.length());
                try {
                    int maxSequence = Integer.parseInt(sequenceStr);
                    sequenceCounter.set(maxSequence);
                    log.info("Initialized sequence counter from database: date={}, max_sequence={}", dateStr, maxSequence);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse sequence from case number: {}", maxCaseNumber, e);
                    sequenceCounter.set(0);
                }
            } else {
                // No existing case numbers for today
                sequenceCounter.set(0);
                log.info("No existing case numbers found for date: {}, starting from 0", dateStr);
            }
        } catch (Exception e) {
            log.error("Error initializing sequence from database, defaulting to 0", e);
            sequenceCounter.set(0);
        }
    }

    /**
     * Generate multiple case numbers in batch
     * 
     * @param count Number of case numbers to generate
     * @return Array of generated case numbers
     */
    public synchronized String[] generateBatchCaseNumbers(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
        
        String[] caseNumbers = new String[count];
        for (int i = 0; i < count; i++) {
            caseNumbers[i] = generateNextCaseNumber();
        }
        
        log.info("Generated batch of {} case numbers: first={}, last={}", 
                count, caseNumbers[0], caseNumbers[count - 1]);
        return caseNumbers;
    }

    /**
     * Get current sequence number (for monitoring/debugging)
     * 
     * @return Current sequence number
     */
    public int getCurrentSequence() {
        return sequenceCounter.get();
    }

    /**
     * Get current date being used (for monitoring/debugging)
     * 
     * @return Current date in yyyyMMdd format
     */
    public String getCurrentDate() {
        return currentDate;
    }
}
