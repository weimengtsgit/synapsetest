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
 * Test Task Service
 * Frontend service for test task API
 *
 * Task: T042 [US1] Implement frontend service for test task API
 */
const testTaskService = {
  /**
   * Create a new test task
   */
  createTestTask: async (taskData) => {
    return apiClient.post('/test-tasks', taskData)
  },

  /**
   * Get all test tasks
   */
  getAllTestTasks: async (params = {}) => {
    return apiClient.get('/test-tasks', { params })
  },

  /**
   * Get test task by ID
   */
  getTestTaskById: async (id) => {
    return apiClient.get(`/test-tasks/${id}`)
  },

  /**
   * Get test tasks by status
   */
  getTestTasksByStatus: async (status) => {
    return apiClient.get(`/test-tasks`, { params: { status } })
  },

  /**
   * Start a test task
   */
  startTestTask: async (id) => {
    return apiClient.post(`/test-tasks/${id}/start`)
  },

  /**
   * Cancel a test task
   */
  cancelTestTask: async (id) => {
    return apiClient.post(`/test-tasks/${id}/cancel`)
  },

  /**
   * Get available test environments
   */
  getEnvironments: async () => {
    return apiClient.get('/test-environments')
  },

  /**
   * Get all test versions
   */
  getVersions: async () => {
    return apiClient.get('/test-versions')
  },
}

export default testTaskService
