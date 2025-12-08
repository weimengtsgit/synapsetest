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
 * Test Version Service
 * Frontend service for test version API
 */
const versionService = {
  /**
   * Get all test versions
   */
  getAllVersions: async () => {
    return apiClient.get('/test-versions')
  },

  /**
   * Get version by ID
   */
  getVersionById: async (id) => {
    return apiClient.get(`/test-versions/${id}`)
  },

  /**
   * Get versions by product version
   */
  getVersionsByProductVersion: async (productVersion) => {
    return apiClient.get(`/test-versions/by-product/${productVersion}`)
  },

  /**
   * Create a new test version
   */
  createVersion: async (versionData) => {
    return apiClient.post('/test-versions', versionData)
  },

  /**
   * Update test version
   */
  updateVersion: async (id, versionData) => {
    return apiClient.put(`/test-versions/${id}`, versionData)
  },

  /**
   * Delete test version
   */
  deleteVersion: async (id) => {
    return apiClient.delete(`/test-versions/${id}`)
  },
}

export default versionService
