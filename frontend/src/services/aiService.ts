import axios from 'axios';

/**
 * AI Service
 * Service layer for all AI-powered features
 *
 * Architecture: Frontend -> Backend (/api/v1/ai/*) -> AI-Service
 */

const API_BASE_URL = '/api/v1/ai';

/**
 * AI Test Case Generation Request
 */
export interface AIGenerationRequest {
  input: string;              // Requirement text
  testType?: string;          // FUNCTIONAL, PERFORMANCE, SECURITY
  tags?: string[];            // Optional tags
  relatedRequirement?: string;// Related requirement ID
}

/**
 * Generated Test Case
 */
export interface GeneratedTestCase {
  name: string;
  description: string;
  steps: string[];
  expectedResult: string;
  priority: string;    // P0, P1, P2, P3
  type: string;        // FUNCTIONAL, PERFORMANCE, SECURITY
  tags?: string[];
}

/**
 * AI Generation Response
 */
export interface AIGenerationResponse {
  requestId: string;
  generatedCases: GeneratedTestCase[];
  generationTime: number;
  confidenceScore: number;
  success: boolean;
  message?: string;
}

/**
 * Batch Generation Request
 */
export interface BatchGenerationRequest {
  requests: AIGenerationRequest[];
}

/**
 * Deduplication Request
 */
export interface DeduplicationRequest {
  testcases: any[];
  threshold?: number;  // Similarity threshold (0-1), default 0.85
}

/**
 * Deduplication Response
 */
export interface DeduplicationResponse {
  requestId: string;
  optimizedCases: any[];
  duplicateGroups: number[][];
  reductionRate: number;
  summary: string;
}

/**
 * Prioritization Request
 */
export interface PrioritizationRequest {
  testcases: any[];
  customWeights?: Record<string, number>;
}

/**
 * Prioritized Test Case
 */
export interface PrioritizedTestCase {
  testcase: any;
  finalScore: number;
  reasoning: string;
}

/**
 * Prioritization Response
 */
export interface PrioritizationResponse {
  requestId: string;
  prioritizedCases: PrioritizedTestCase[];
  summary: string;
}

/**
 * Quality Analysis Request
 */
export interface QualityAnalysisRequest {
  testcases: any[];
}

/**
 * Quality Analysis Response
 */
export interface QualityAnalysisResponse {
  overallScore: number;
  issues: Array<{
    type: string;
    severity: string;
    message: string;
    affectedCases: number[];
  }>;
  recommendations: string[];
  message?: string;
}

/**
 * Strategy Recommendation Request
 */
export interface RecommendationRequest {
  taskId?: string;
  environmentId?: string;
  versionId?: string;
  context: {
    codeChange?: {
      changedFilesCount: number;
      changedLinesCount: number;
      changedModules: string[];
    };
    historical?: {
      recentPassRate: number;
      recentDefectCount: number;
    };
    business?: {
      moduleImportance: number;
      deadline?: string;
    };
  };
}

/**
 * Strategy Recommendation Response
 */
export interface RecommendationResponse {
  testScope: string;      // SMOKE, CORE, FULL
  environment: string;
  priority: number;       // 1-10
  estimatedDuration: number;  // minutes
  riskLevel: string;      // LOW, MEDIUM, HIGH
  reasoning: string[];
}

class AIService {

  /**
   * Generate test cases from requirement
   *
   * @param request Generation request
   * @returns Generated test cases
   */
  async generateTestCases(request: AIGenerationRequest): Promise<AIGenerationResponse> {
    try {
      const response = await axios.post(`${API_BASE_URL}/testcase/generate`, request);
      return response.data.data;
    } catch (error: any) {
      console.error('Failed to generate test cases:', error);
      throw new Error(error.response?.data?.message || 'AI generation failed');
    }
  }

  /**
   * Batch generate test cases
   *
   * @param request Batch generation request
   * @returns Batch generation results
   */
  async batchGenerate(request: BatchGenerationRequest): Promise<any> {
    try {
      const response = await axios.post(`${API_BASE_URL}/testcase/generate/batch`, request.requests);
      return response.data.data;
    } catch (error: any) {
      console.error('Failed to batch generate:', error);
      throw new Error(error.response?.data?.message || 'Batch generation failed');
    }
  }

  /**
   * Deduplicate test cases
   *
   * @param request Deduplication request
   * @returns Deduplicated test cases
   */
  async deduplicateTestCases(request: DeduplicationRequest): Promise<DeduplicationResponse> {
    try {
      const response = await axios.post(`${API_BASE_URL}/testcase/optimize/deduplicate`, request);
      return response.data.data;
    } catch (error: any) {
      console.error('Failed to deduplicate:', error);
      throw new Error(error.response?.data?.message || 'Deduplication failed');
    }
  }

  /**
   * Prioritize test cases
   *
   * @param request Prioritization request
   * @returns Prioritized test cases
   */
  async prioritizeTestCases(request: PrioritizationRequest): Promise<PrioritizationResponse> {
    try {
      const response = await axios.post(`${API_BASE_URL}/testcase/optimize/prioritize`, request);
      return response.data.data;
    } catch (error: any) {
      console.error('Failed to prioritize:', error);
      throw new Error(error.response?.data?.message || 'Prioritization failed');
    }
  }

  /**
   * Analyze test case quality
   *
   * @param request Quality analysis request
   * @returns Quality analysis result
   */
  async analyzeQuality(request: QualityAnalysisRequest): Promise<QualityAnalysisResponse> {
    try {
      const response = await axios.post(`${API_BASE_URL}/testcase/analyze/quality`, request);
      return response.data.data;
    } catch (error: any) {
      console.error('Failed to analyze quality:', error);
      throw new Error(error.response?.data?.message || 'Quality analysis failed');
    }
  }

  /**
   * Get test strategy recommendation
   *
   * @param request Recommendation request
   * @returns Strategy recommendation
   */
  async getRecommendation(request: RecommendationRequest): Promise<RecommendationResponse> {
    try {
      const response = await axios.post(`${API_BASE_URL}/recommendation/strategy`, request);
      return response.data.data;
    } catch (error: any) {
      console.error('Failed to get recommendation:', error);
      throw new Error(error.response?.data?.message || 'Recommendation failed');
    }
  }

  /**
   * Explain recommendation reasoning
   *
   * @param request Explanation request
   * @returns Explanation details
   */
  async explainRecommendation(request: any): Promise<any> {
    try {
      const response = await axios.post(`${API_BASE_URL}/recommendation/strategy/explain`, request);
      return response.data.data;
    } catch (error: any) {
      console.error('Failed to explain recommendation:', error);
      throw new Error(error.response?.data?.message || 'Explanation failed');
    }
  }

  /**
   * Check AI Service health
   *
   * @returns Health status
   */
  async checkHealth(): Promise<any> {
    try {
      const response = await axios.get(`${API_BASE_URL}/health`);
      return response.data.data;
    } catch (error: any) {
      console.error('Failed to check AI service health:', error);
      throw new Error('AI service is unavailable');
    }
  }
}

// Export singleton instance
const aiService = new AIService();
export default aiService;
