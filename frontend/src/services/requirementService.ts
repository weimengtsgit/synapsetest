import type {
  Requirement,
  AIAnalysisResult,
  AnalysisType,
} from '../types/requirement'

// Mock 需求数据
const mockRequirements: Requirement[] = [
  {
    id: 'REQ-001',
    title: '用户登录功能',
    description: '实现用户通过用户名和密码登录系统的功能，支持记住密码和忘记密码功能。登录失败3次后需要输入验证码。',
    type: 'FUNCTIONAL',
    priority: 'P0',
    modules: ['用户管理', '安全认证'],
    status: 'APPROVED',
    createdAt: '2025-02-01T10:00:00Z',
    updatedAt: '2025-02-01T10:00:00Z',
  },
  {
    id: 'REQ-002',
    title: '订单管理功能',
    description: '用户可以查看、创建、修改和取消订单。订单状态包括：待支付、已支付、配送中、已完成、已取消。',
    type: 'FUNCTIONAL',
    priority: 'P0',
    modules: ['订单管理', '支付系统'],
    status: 'APPROVED',
    createdAt: '2025-02-02T10:00:00Z',
    updatedAt: '2025-02-02T10:00:00Z',
  },
  {
    id: 'REQ-003',
    title: '支付集成功能',
    description: '集成支付宝和微信支付，支持扫码支付和H5支付。需要处理支付回调和异常情况。',
    type: 'FUNCTIONAL',
    priority: 'P1',
    modules: ['支付系统', '第三方集成'],
    status: 'DRAFT',
    createdAt: '2025-02-03T10:00:00Z',
    updatedAt: '2025-02-03T10:00:00Z',
  },
  {
    id: 'REQ-004',
    title: '系统性能优化',
    description: '优化系统响应时间，页面加载时间控制在2秒以内，API响应时间控制在500ms以内。',
    type: 'NON_FUNCTIONAL',
    priority: 'P1',
    modules: ['性能优化', '数据库'],
    status: 'DRAFT',
    createdAt: '2025-02-04T10:00:00Z',
    updatedAt: '2025-02-04T10:00:00Z',
  },
  {
    id: 'REQ-005',
    title: '数据安全加固',
    description: '加强数据传输和存储的安全性，实现数据加密、SQL注入防护、XSS防护等安全措施。',
    type: 'NON_FUNCTIONAL',
    priority: 'P0',
    modules: ['安全认证', '数据库'],
    status: 'APPROVED',
    createdAt: '2025-02-05T10:00:00Z',
    updatedAt: '2025-02-05T10:00:00Z',
  },
]

// 获取所有需求
export const getRequirements = (): Promise<Requirement[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve([...mockRequirements])
    }, 300)
  })
}

// 获取单个需求
export const getRequirementById = (id: string): Promise<Requirement | null> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const requirement = mockRequirements.find((req) => req.id === id)
      resolve(requirement || null)
    }, 200)
  })
}

// 创建需求
export const createRequirement = (requirement: Omit<Requirement, 'id' | 'createdAt' | 'updatedAt'>): Promise<Requirement> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const newRequirement: Requirement = {
        ...requirement,
        id: `REQ-${String(mockRequirements.length + 1).padStart(3, '0')}`,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
      mockRequirements.push(newRequirement)
      resolve(newRequirement)
    }, 500)
  })
}

// 更新需求
export const updateRequirement = (id: string, updates: Partial<Requirement>): Promise<Requirement> => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const index = mockRequirements.findIndex((req) => req.id === id)
      if (index === -1) {
        reject(new Error('Requirement not found'))
        return
      }
      mockRequirements[index] = {
        ...mockRequirements[index],
        ...updates,
        updatedAt: new Date().toISOString(),
      }
      resolve(mockRequirements[index])
    }, 500)
  })
}

// 删除需求
export const deleteRequirement = (id: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const index = mockRequirements.findIndex((req) => req.id === id)
      if (index === -1) {
        reject(new Error('Requirement not found'))
        return
      }
      mockRequirements.splice(index, 1)
      resolve()
    }, 300)
  })
}

// AI 分析需求
export const analyzeRequirement = (
  requirement: Requirement,
  analysisTypes: AnalysisType[]
): Promise<AIAnalysisResult> => {
  return new Promise((resolve) => {
    // 模拟 AI 分析耗时
    setTimeout(() => {
      const result: AIAnalysisResult = {
        requestId: `REQ-ANALYSIS-${Date.now()}`,
        timestamp: new Date().toISOString(),
      }

      // 需求摘要
      if (analysisTypes.includes('summary')) {
        result.summary = {
          title: `${requirement.title}的智能摘要`,
          keyPoints: [
            '核心功能：' + requirement.title,
            '涉及模块：' + requirement.modules.join('、'),
            '优先级：' + requirement.priority,
            '需求类型：' + (requirement.type === 'FUNCTIONAL' ? '功能需求' : '非功能需求'),
          ],
          stakeholders: ['产品经理', '开发团队', '测试团队', 'UI/UX设计师'],
          estimatedEffort: requirement.priority === 'P0' ? '10-15人天' : '5-8人天',
        }
      }

      // 质量分析
      if (analysisTypes.includes('quality')) {
        const descLength = requirement.description.length
        const hasModules = requirement.modules.length > 0
        const hasPriority = !!requirement.priority

        const completeness = (descLength > 50 ? 30 : 20) + (hasModules ? 30 : 0) + (hasPriority ? 40 : 0)
        const clarity = descLength > 100 ? 85 : descLength > 50 ? 70 : 50
        const testability = requirement.type === 'FUNCTIONAL' ? 80 : 60

        result.qualityAnalysis = {
          overallScore: Math.round((completeness + clarity + testability) / 3),
          completeness,
          clarity,
          testability,
          issues: [
            ...(descLength < 50
              ? [
                  {
                    type: 'WARNING' as const,
                    message: '需求描述过于简短',
                    suggestion: '建议补充更详细的功能说明、使用场景和边界条件',
                  },
                ]
              : []),
            ...(requirement.modules.length === 0
              ? [
                  {
                    type: 'ERROR' as const,
                    message: '未指定关联模块',
                    suggestion: '请明确该需求涉及的系统模块',
                  },
                ]
              : []),
            {
              type: 'INFO' as const,
              message: '建议补充非功能性需求',
              suggestion: '如性能要求、安全要求、可用性要求等',
            },
          ],
        }
      }

      // 任务拆分
      if (analysisTypes.includes('taskBreakdown')) {
        const tasks = generateMockTasks(requirement)
        result.taskBreakdown = {
          tasks,
          totalEstimatedHours: tasks.reduce((sum, task) => sum + task.estimatedHours, 0),
        }
      }

      // 验收标准
      if (analysisTypes.includes('acceptance')) {
        result.acceptanceCriteria = {
          criteria: [
            {
              id: 'AC-001',
              description: `${requirement.title}的核心功能正常运行`,
              type: 'FUNCTIONAL',
              priority: 'MUST',
            },
            {
              id: 'AC-002',
              description: '所有输入字段进行有效性验证',
              type: 'FUNCTIONAL',
              priority: 'MUST',
            },
            {
              id: 'AC-003',
              description: '错误提示信息清晰友好',
              type: 'USABILITY',
              priority: 'SHOULD',
            },
            {
              id: 'AC-004',
              description: '响应时间不超过2秒',
              type: 'PERFORMANCE',
              priority: 'SHOULD',
            },
            {
              id: 'AC-005',
              description: '敏感数据加密传输和存储',
              type: 'SECURITY',
              priority: 'MUST',
            },
            {
              id: 'AC-006',
              description: '支持主流浏览器（Chrome、Firefox、Safari）',
              type: 'FUNCTIONAL',
              priority: 'COULD',
            },
          ],
        }
      }

      // 相似度检测
      if (analysisTypes.includes('similarity')) {
        const similarReqs = mockRequirements
          .filter((req) => req.id !== requirement.id)
          .filter((req) => {
            const titleSimilar = req.title.includes(requirement.title.slice(0, 3))
            const moduleSimilar = req.modules.some((m) => requirement.modules.includes(m))
            return titleSimilar || moduleSimilar
          })
          .slice(0, 3)
          .map((req) => ({
            id: req.id,
            title: req.title,
            similarity: Math.random() * 0.4 + 0.5, // 0.5-0.9
            reason: '功能描述相似或涉及相同模块',
          }))

        result.similarityCheck = {
          hasSimilar: similarReqs.length > 0,
          similarRequirements: similarReqs,
        }
      }

      resolve(result)
    }, 2500) // 模拟 2.5 秒的分析时间
  })
}

// 生成 Mock 任务
function generateMockTasks(requirement: Requirement) {
  const baseTasks = [
    {
      id: 'TASK-001',
      title: '需求分析和技术方案设计',
      description: '分析需求细节，设计技术实现方案，评估技术风险',
      type: 'DESIGN' as const,
      estimatedHours: 8,
      priority: 1,
      dependencies: [],
    },
    {
      id: 'TASK-002',
      title: '数据库表结构设计',
      description: '设计相关数据库表结构，建立索引和约束',
      type: 'DATABASE' as const,
      estimatedHours: 4,
      priority: 2,
      dependencies: ['TASK-001'],
    },
    {
      id: 'TASK-003',
      title: '后端 API 开发',
      description: '实现后端业务逻辑和 RESTful API 接口',
      type: 'BACKEND' as const,
      estimatedHours: 16,
      priority: 3,
      dependencies: ['TASK-002'],
    },
    {
      id: 'TASK-004',
      title: '前端页面开发',
      description: '实现前端页面和交互逻辑',
      type: 'FRONTEND' as const,
      estimatedHours: 12,
      priority: 4,
      dependencies: ['TASK-003'],
    },
    {
      id: 'TASK-005',
      title: '单元测试和集成测试',
      description: '编写单元测试用例，进行集成测试',
      type: 'TEST' as const,
      estimatedHours: 8,
      priority: 5,
      dependencies: ['TASK-004'],
    },
  ]

  return baseTasks
}
