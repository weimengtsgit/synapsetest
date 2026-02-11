// 需求类型定义

export type RequirementType = 'FUNCTIONAL' | 'NON_FUNCTIONAL'
export type RequirementPriority = 'P0' | 'P1' | 'P2' | 'P3'
export type RequirementStatus = 'DRAFT' | 'APPROVED' | 'REJECTED'

export interface Requirement {
  id: string
  title: string
  description: string
  type: RequirementType
  priority: RequirementPriority
  modules: string[]
  status: RequirementStatus
  createdAt: string
  updatedAt: string
}

// AI 分析类型
export type AnalysisType = 'summary' | 'quality' | 'taskBreakdown' | 'acceptance' | 'similarity'

// 需求摘要
export interface RequirementSummary {
  title: string
  keyPoints: string[]
  stakeholders: string[]
  estimatedEffort: string
}

// 质量分析
export interface QualityIssue {
  type: 'ERROR' | 'WARNING' | 'INFO'
  message: string
  suggestion: string
}

export interface QualityAnalysis {
  overallScore: number
  completeness: number
  clarity: number
  testability: number
  issues: QualityIssue[]
}

// 任务拆分
export type TaskType = 'FRONTEND' | 'BACKEND' | 'DATABASE' | 'TEST' | 'DESIGN'

export interface Task {
  id: string
  title: string
  description: string
  type: TaskType
  estimatedHours: number
  priority: number
  dependencies: string[]
}

export interface TaskBreakdown {
  tasks: Task[]
  totalEstimatedHours: number
}

// 验收标准
export type AcceptanceCriteriaType = 'FUNCTIONAL' | 'PERFORMANCE' | 'SECURITY' | 'USABILITY'
export type AcceptancePriority = 'MUST' | 'SHOULD' | 'COULD'

export interface AcceptanceCriterion {
  id: string
  description: string
  type: AcceptanceCriteriaType
  priority: AcceptancePriority
}

export interface AcceptanceCriteria {
  criteria: AcceptanceCriterion[]
}

// 相似度检测
export interface SimilarRequirement {
  id: string
  title: string
  similarity: number
  reason: string
}

export interface SimilarityCheck {
  hasSimilar: boolean
  similarRequirements: SimilarRequirement[]
}

// AI 分析结果
export interface AIAnalysisResult {
  requestId: string
  timestamp: string
  summary?: RequirementSummary
  qualityAnalysis?: QualityAnalysis
  taskBreakdown?: TaskBreakdown
  acceptanceCriteria?: AcceptanceCriteria
  similarityCheck?: SimilarityCheck
}
