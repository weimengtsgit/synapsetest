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
   * Deduplicate test cases
   */
  deduplicateTestCases: async (testCases) => {
    return apiClient.post('/test-cases/deduplicate', testCases)
  },

  /**
   * Analyze test coverage
   */
  analyzeTestCoverage: async (testCases) => {
    return apiClient.post('/test-cases/analyze-coverage', testCases)
  },
}

export default testCaseService
