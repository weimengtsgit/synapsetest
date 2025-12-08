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
 * Test Environment Service
 * Frontend service for test environment API
 */
const environmentService = {
  /**
   * Get all test environments
   */
  getAllEnvironments: async (status = null) => {
    const params = status ? { status } : {}
    return apiClient.get('/test-environments', { params })
  },

  /**
   * Get environment by ID
   */
  getEnvironmentById: async (id) => {
    return apiClient.get(`/test-environments/${id}`)
  },

  /**
   * Get environment by name
   */
  getEnvironmentByName: async (name) => {
    return apiClient.get(`/test-environments/by-name/${name}`)
  },

  /**
   * Get available environments (status = AVAILABLE)
   */
  getAvailableEnvironments: async () => {
    return apiClient.get('/test-environments', { params: { status: 'AVAILABLE' } })
  },

  /**
   * Create a new test environment
   */
  createEnvironment: async (environmentData) => {
    return apiClient.post('/test-environments', environmentData)
  },

  /**
   * Update test environment
   */
  updateEnvironment: async (id, environmentData) => {
    return apiClient.put(`/test-environments/${id}`, environmentData)
  },

  /**
   * Update environment status
   */
  updateEnvironmentStatus: async (id, status) => {
    return apiClient.post(`/test-environments/${id}/status`, { status })
  },

  /**
   * Delete test environment
   */
  deleteEnvironment: async (id) => {
    return apiClient.delete(`/test-environments/${id}`)
  },
}

export default environmentService
