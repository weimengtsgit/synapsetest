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
    // Add auth token if available
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
 * Report Service
 * Frontend service for quality reports and traceability API
 *
 * Task: T074 [US3] Implement frontend service for reporting API
 */
const reportService = {
  /**
   * Get quality report by ID
   */
  getReportById: async (id) => {
    return apiClient.get(`/reports/${id}`)
  },

  /**
   * Get quality report by task ID
   */
  getReportByTaskId: async (taskId) => {
    return apiClient.get(`/reports/task/${taskId}`)
  },

  /**
   * Get recent reports
   */
  getRecentReports: async () => {
    return apiClient.get('/reports/recent')
  },

  /**
   * Get comprehensive report
   */
  getComprehensiveReport: async (taskId) => {
    return apiClient.get(`/reports/comprehensive/${taskId}`)
  },

  /**
   * Compare two reports
   */
  compareReports: async (taskId1, taskId2) => {
    return apiClient.get('/reports/compare', {
      params: { taskId1, taskId2 },
    })
  },

  /**
   * Get HTML report
   */
  getHtmlReport: async (taskId) => {
    return apiClient.get(`/reports/html/${taskId}`, {
      responseType: 'text',
    })
  },

  /**
   * Trace requirement coverage
   */
  traceRequirementCoverage: async (requirementId) => {
    return apiClient.get(`/reports/traceability/requirement/${requirementId}`)
  },

  /**
   * Generate traceability matrix
   */
  generateTraceabilityMatrix: async (requirementIds) => {
    return apiClient.post('/reports/traceability/matrix', requirementIds)
  },

  /**
   * Check release quality gates
   */
  checkQualityGates: async (taskId) => {
    return apiClient.get(`/reports/quality-gates/${taskId}`)
  },

  /**
   * Trace defect impact
   */
  traceDefectImpact: async (defectId) => {
    return apiClient.get(`/reports/traceability/defect/${defectId}`)
  },

  /**
   * Analyze change impact
   */
  analyzeChangeImpact: async (changeId, changedFiles) => {
    return apiClient.post('/reports/traceability/change-impact', changedFiles, {
      params: { changeId },
    })
  },
}

export default reportService

