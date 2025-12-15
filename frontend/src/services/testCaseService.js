import axios from 'axios'

const API_BASE_URL = '/api/v1'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error.response?.data || error.message)
    return Promise.reject(error.response?.data || error)
  }
)

/**
 * Test Case Service
 * Frontend service for test case API
 *
 * Task: T057 [US2] Implement frontend service for test case API
 */
const testCaseService = {
  /**
   * AI Generate test cases from input
   */
  generateTestCases: async (data) => {
    return apiClient.post('/test-cases/generate', data)
  },

  /**
   * Create a new test case
   */
  createTestCase: async (caseData) => {
    return apiClient.post('/test-cases', caseData)
  },

  /**
   * Get all test cases
   */
  getAllTestCases: async (params = {}) => {
    return apiClient.get('/test-cases', { params })
  },

  /**
   * Get test case by ID
   */
  getTestCaseById: async (id) => {
    return apiClient.get(`/test-cases/${id}`)
  },

  /**
   * Get test cases by status
   */
  getTestCasesByStatus: async (status) => {
    return apiClient.get(`/test-cases`, { params: { status } })
  },

  /**
   * Get test cases by type
   */
  getTestCasesByType: async (type) => {
    return apiClient.get(`/test-cases`, { params: { type } })
  },

  /**
   * Update test case
   */
  updateTestCase: async (id, caseData) => {
    return apiClient.put(`/test-cases/${id}`, caseData)
  },

  /**
   * Approve test case
   */
  approveTestCase: async (id) => {
    return apiClient.post(`/test-cases/${id}/approve`)
  },

  /**
   * Delete test case
   */
  deleteTestCase: async (id) => {
    return apiClient.delete(`/test-cases/${id}`)
  },

  /**
   * Get all distinct modules
   */
  getAllModules: async () => {
    return apiClient.get('/test-cases/modules')
  },

  /**
   * Deduplicate test cases using AI (old method, kept for compatibility)
   */
  deduplicateTestCases: async (testCases) => {
    return apiClient.post('/test-cases/deduplicate', testCases)
  },

  /**
   * Analyze test cases for duplicates using AI
   * Returns detailed duplicate groups for user review
   */
  analyzeDeduplication: async (params) => {
    const { testcases, threshold, scope, modules } = params

    // Build request body based on scope
    let requestBody = { testcases, threshold }

    if (scope === 'module' && modules && modules.length > 0) {
      // Filter testcases by selected modules
      requestBody.testcases = testcases.filter(tc =>
        modules.includes(tc.module)
      )
    } else if (scope === 'selected') {
      // testcases array should already contain only selected cases
    }

    return apiClient.post('/ai/testcase/optimize/deduplicate', requestBody)
  },

  /**
   * Confirm deduplication optimization
   * Applies user's selection to remove duplicate test cases
   */
  confirmOptimization: async (selectedCaseIds) => {
    return apiClient.post('/ai/testcase/optimize/confirm', {
      selected_case_ids: selectedCaseIds
    })
  },

  /**
   * Prioritize test cases using AI
   * Returns test cases sorted by priority score with ranking
   */
  prioritizeTestCases: async (params) => {
    const { testcases, customWeights } = params

    const requestBody = {
      testcases,
      custom_weights: customWeights || null
    }

    return apiClient.post('/ai/testcase/optimize/prioritize', requestBody)
  },

  /**
   * Analyze test coverage
   */
  analyzeTestCoverage: async (testCases) => {
    return apiClient.post('/test-cases/analyze-coverage', testCases)
  },

  /**
   * Analyze test case quality using AI
   * Returns quality metrics and improvement suggestions
   */
  analyzeQuality: async (testcases) => {
    return apiClient.post('/ai/testcase/analyze/quality', {
      testcases
    })
  },
}

export default testCaseService
