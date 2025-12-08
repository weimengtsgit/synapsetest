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
 * Monitoring Service
 * Frontend service for monitoring API
 *
 * Task: T073 [US3] Implement frontend service for monitoring API
 */
const monitoringService = {
  /**
   * Get monitoring data by task ID
   */
  getMonitoringDataByTaskId: async (taskId) => {
    return apiClient.get(`/monitoring/tasks/${taskId}`)
  },

  /**
   * Get recent monitoring data
   */
  getRecentMonitoringData: async () => {
    return apiClient.get('/monitoring/recent')
  },

  /**
   * Get running tasks
   */
  getRunningTasks: async () => {
    return apiClient.get('/monitoring/running')
  },

  /**
   * Get dashboard statistics
   */
  getDashboardStatistics: async () => {
    return apiClient.get('/monitoring/dashboard/stats')
  },

  /**
   * Get monitoring data by environment
   */
  getMonitoringDataByEnvironment: async (environment) => {
    return apiClient.get(`/monitoring/environment/${environment}`)
  },

  /**
   * Update resource usage
   */
  updateResourceUsage: async (taskId, resourceUsage) => {
    return apiClient.post(`/monitoring/tasks/${taskId}/resource-usage`, resourceUsage)
  },

  /**
   * Update performance metrics
   */
  updatePerformanceMetrics: async (taskId, performanceMetrics) => {
    return apiClient.post(`/monitoring/tasks/${taskId}/performance-metrics`, performanceMetrics)
  },
}

export default monitoringService

