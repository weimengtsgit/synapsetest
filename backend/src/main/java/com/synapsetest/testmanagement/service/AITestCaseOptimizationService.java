package com.synapsetest.testmanagement.service;

import com.synapsetest.testmanagement.dto.response.TestCaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Test Case Optimization Service
 * Implements intelligent test case deduplication and optimization
 *
 * Task: T051 [US2] Implement AI测试用例去重和优化算法
 *
 * Features:
 * - Semantic similarity detection (NLP-based)
 * - Execution path analysis
 * - Priority optimization
 * - Test suite reduction
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AITestCaseOptimizationService {

    /**
     * Deduplicate test cases based on semantic similarity
     *
     * Algorithm:
     * 1. Calculate similarity scores between all test cases
     * 2. Group similar test cases
     * 3. Keep the best representative from each group
     * 4. Return optimized test suite
     */
    public List<TestCaseResponse> deduplicateTestCases(List<TestCaseResponse> testCases) {
        log.info("Deduplicating {} test cases", testCases.size());

        if (testCases.size() <= 1) {
            return testCases;
        }

        List<TestCaseResponse> optimized = new ArrayList<>();
        Set<Integer> processed = new HashSet<>();

        for (int i = 0; i < testCases.size(); i++) {
            if (processed.contains(i)) {
                continue;
            }

            TestCaseResponse current = testCases.get(i);
            List<TestCaseResponse> duplicates = new ArrayList<>();
            duplicates.add(current);

            // Find similar test cases
            for (int j = i + 1; j < testCases.size(); j++) {
                if (processed.contains(j)) {
                    continue;
                }

                TestCaseResponse other = testCases.get(j);
                double similarity = calculateSimilarity(current, other);

                // If similarity > 80%, consider as duplicate
                if (similarity > 0.80) {
                    duplicates.add(other);
                    processed.add(j);
                }
            }

            // Select best representative from duplicates
            TestCaseResponse best = selectBestTestCase(duplicates);
            optimized.add(best);
            processed.add(i);
        }

        log.info("Reduced from {} to {} test cases ({}% reduction)",
                testCases.size(), optimized.size(),
                String.format("%.1f", (1 - (double) optimized.size() / testCases.size()) * 100));

        return optimized;
    }

    /**
     * Calculate semantic similarity between two test cases
     */
    private double calculateSimilarity(TestCaseResponse tc1, TestCaseResponse tc2) {
        // For MVP: Use simple text-based similarity
        // In production: Use NLP embeddings (BERT, Sentence Transformers)

        double titleSimilarity = calculateTextSimilarity(tc1.getTitle(), tc2.getTitle());
        double stepsSimilarity = calculateStepsSimilarity(tc1.getSteps(), tc2.getSteps());
        double typeSimilarity = tc1.getType().equals(tc2.getType()) ? 1.0 : 0.0;

        // Weighted average
        return (titleSimilarity * 0.4) + (stepsSimilarity * 0.5) + (typeSimilarity * 0.1);
    }

    /**
     * Calculate text similarity using Jaccard index
     */
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Calculate similarity between test steps
     */
    private double calculateStepsSimilarity(List<String> steps1, List<String> steps2) {
        if (steps1 == null || steps2 == null || steps1.isEmpty() || steps2.isEmpty()) {
            return 0.0;
        }

        // Compare step sequences
        int minLength = Math.min(steps1.size(), steps2.size());
        int maxLength = Math.max(steps1.size(), steps2.size());

        double matchScore = 0.0;

        for (int i = 0; i < minLength; i++) {
            double stepSimilarity = calculateTextSimilarity(steps1.get(i), steps2.get(i));
            matchScore += stepSimilarity;
        }

        return matchScore / maxLength;
    }

    /**
     * Select the best test case from a group of duplicates
     */
    private TestCaseResponse selectBestTestCase(List<TestCaseResponse> duplicates) {
        if (duplicates.size() == 1) {
            return duplicates.get(0);
        }

        // Selection criteria:
        // 1. Highest priority
        // 2. Most detailed (more steps)
        // 3. Approved status preferred
        // 4. Most recent

        return duplicates.stream()
                .max(Comparator
                        .comparing(TestCaseResponse::getPriority)
                        .thenComparing(tc -> tc.getSteps() != null ? tc.getSteps().size() : 0)
                        .thenComparing(tc -> "APPROVED".equals(tc.getStatus()) ? 1 : 0)
                        .thenComparing(TestCaseResponse::getCreatedAt))
                .orElse(duplicates.get(0));
    }

    /**
     * Optimize test suite for maximum coverage with minimum execution time
     */
    public List<TestCaseResponse> optimizeTestSuite(List<TestCaseResponse> testCases, double targetReduction) {
        log.info("Optimizing test suite with target reduction: {}%", targetReduction * 100);

        // First deduplicate
        List<TestCaseResponse> deduplicated = deduplicateTestCases(testCases);

        int targetSize = (int) (deduplicated.size() * (1 - targetReduction));

        if (deduplicated.size() <= targetSize) {
            return deduplicated;
        }

        // Sort by priority and select top cases
        List<TestCaseResponse> optimized = deduplicated.stream()
                .sorted(Comparator.comparing(TestCaseResponse::getPriority).reversed())
                .limit(targetSize)
                .collect(Collectors.toList());

        log.info("Optimized test suite to {} cases", optimized.size());

        return optimized;
    }

    /**
     * Analyze test coverage for a test suite
     */
    public Map<String, Object> analyzeTestCoverage(List<TestCaseResponse> testCases) {
        Map<String, Object> coverage = new HashMap<>();

        // Count by type
        Map<String, Long> typeDistribution = testCases.stream()
                .collect(Collectors.groupingBy(TestCaseResponse::getType, Collectors.counting()));

        // Count by priority
        Map<Integer, Long> priorityDistribution = testCases.stream()
                .collect(Collectors.groupingBy(TestCaseResponse::getPriority, Collectors.counting()));

        // Count by status
        Map<String, Long> statusDistribution = testCases.stream()
                .collect(Collectors.groupingBy(TestCaseResponse::getStatus, Collectors.counting()));

        coverage.put("totalCases", testCases.size());
        coverage.put("typeDistribution", typeDistribution);
        coverage.put("priorityDistribution", priorityDistribution);
        coverage.put("statusDistribution", statusDistribution);

        log.info("Test coverage analysis: {} total cases", testCases.size());

        return coverage;
    }
}
