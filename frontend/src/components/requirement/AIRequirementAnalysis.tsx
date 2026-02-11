import React, { useState, useEffect } from 'react'
import {
  Steps,
  Card,
  Form,
  Input,
  Select,
  Checkbox,
  Button,
  Progress,
  Tabs,
  Tag,
  Table,
  Alert,
  Statistic,
  Row,
  Col,
  Space,
  message,
} from 'antd'
import {
  ThunderboltOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  InfoCircleOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons'
import { useLocation, useNavigate } from 'react-router-dom'
import type { Requirement, AnalysisType, AIAnalysisResult } from '../../types/requirement'
import { analyzeRequirement } from '../../services/requirementService'
import './AIRequirementAnalysis.css'

const { TextArea } = Input
const { Option } = Select
const { Step } = Steps

const AIRequirementAnalysis: React.FC = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const [form] = Form.useForm()

  const [currentStep, setCurrentStep] = useState(0)
  const [requirement, setRequirement] = useState<Requirement | null>(null)
  const [selectedAnalysisTypes, setSelectedAnalysisTypes] = useState<AnalysisType[]>([])
  const [analyzing, setAnalyzing] = useState(false)
  const [progress, setProgress] = useState(0)
  const [analysisResult, setAnalysisResult] = useState<AIAnalysisResult | null>(null)

  useEffect(() => {
    // 从路由状态获取需求数据
    if (location.state?.requirement) {
      const req = location.state.requirement as Requirement
      setRequirement(req)
      form.setFieldsValue(req)
    }
  }, [location, form])

  const handleStepNext = async () => {
    if (currentStep === 0) {
      try {
        const values = await form.validateFields()
        setRequirement({
          ...requirement!,
          ...values,
        })
        setCurrentStep(1)
      } catch (error) {
        message.error('请完善需求信息')
      }
    } else if (currentStep === 1) {
      if (selectedAnalysisTypes.length === 0) {
        message.error('请至少选择一种分析类型')
        return
      }
      setCurrentStep(2)
      startAnalysis()
    }
  }

  const startAnalysis = async () => {
    setAnalyzing(true)
    setProgress(0)

    // 模拟进度更新
    const progressInterval = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 90) {
          clearInterval(progressInterval)
          return 90
        }
        return prev + 10
      })
    }, 250)

    try {
      const result = await analyzeRequirement(requirement!, selectedAnalysisTypes)
      setAnalysisResult(result)
      setProgress(100)
      setCurrentStep(3)
    } catch (error) {
      message.error('分析失败，请重试')
    } finally {
      clearInterval(progressInterval)
      setAnalyzing(false)
    }
  }

  const handleBack = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1)
    } else {
      navigate('/requirements/list')
    }
  }

  const handleReset = () => {
    setCurrentStep(0)
    setSelectedAnalysisTypes([])
    setAnalysisResult(null)
    setProgress(0)
  }

  const renderStep0 = () => (
    <Card title="输入需求信息" className="step-card">
      <Form form={form} layout="vertical">
        <Form.Item
          name="title"
          label="需求标题"
          rules={[{ required: true, message: '请输入需求标题' }]}
        >
          <Input placeholder="请输入需求标题" size="large" />
        </Form.Item>

        <Form.Item
          name="description"
          label="需求描述"
          rules={[{ required: true, message: '请输入需求描述' }]}
        >
          <TextArea rows={6} placeholder="请详细描述需求内容、使用场景和预期效果" />
        </Form.Item>

        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              name="type"
              label="需求类型"
              rules={[{ required: true, message: '请选择需求类型' }]}
            >
              <Select placeholder="请选择">
                <Option value="FUNCTIONAL">功能需求</Option>
                <Option value="NON_FUNCTIONAL">非功能需求</Option>
              </Select>
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              name="priority"
              label="优先级"
              rules={[{ required: true, message: '请选择优先级' }]}
            >
              <Select placeholder="请选择">
                <Option value="P0">P0 - 最高</Option>
                <Option value="P1">P1 - 高</Option>
                <Option value="P2">P2 - 中</Option>
                <Option value="P3">P3 - 低</Option>
              </Select>
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              name="status"
              label="状态"
              initialValue="DRAFT"
            >
              <Select>
                <Option value="DRAFT">草稿</Option>
                <Option value="APPROVED">已批准</Option>
                <Option value="REJECTED">已拒绝</Option>
              </Select>
            </Form.Item>
          </Col>
        </Row>

        <Form.Item
          name="modules"
          label="关联模块"
          rules={[{ required: true, message: '请选择关联模块' }]}
        >
          <Select mode="multiple" placeholder="请选择关联模块">
            <Option value="用户管理">用户管理</Option>
            <Option value="订单管理">订单管理</Option>
            <Option value="支付系统">支付系统</Option>
            <Option value="安全认证">安全认证</Option>
            <Option value="数据库">数据库</Option>
            <Option value="性能优化">性能优化</Option>
            <Option value="第三方集成">第三方集成</Option>
          </Select>
        </Form.Item>
      </Form>
    </Card>
  )

  const renderStep1 = () => (
    <Card title="选择分析类型" className="step-card">
      <Alert
        message="请选择需要进行的 AI 分析类型"
        description="可以同时选择多种分析类型，AI 将为您提供全面的需求分析报告"
        type="info"
        showIcon
        style={{ marginBottom: 24 }}
      />

      <Checkbox.Group
        style={{ width: '100%' }}
        value={selectedAnalysisTypes}
        onChange={(values) => setSelectedAnalysisTypes(values as AnalysisType[])}
      >
        <Row gutter={[16, 16]}>
          <Col span={24}>
            <Card className="analysis-type-card" hoverable>
              <Checkbox value="summary">
                <div className="analysis-type-content">
                  <h3>📝 需求摘要 (Summary)</h3>
                  <p>自动提取需求关键信息，生成简洁的需求概述，包括核心功能点、相关方和工作量评估</p>
                </div>
              </Checkbox>
            </Card>
          </Col>

          <Col span={24}>
            <Card className="analysis-type-card" hoverable>
              <Checkbox value="quality">
                <div className="analysis-type-content">
                  <h3>🎯 质量分析 (Quality Analysis)</h3>
                  <p>检查需求的完整性、清晰度和可测性，识别潜在问题并提供改进建议</p>
                </div>
              </Checkbox>
            </Card>
          </Col>

          <Col span={24}>
            <Card className="analysis-type-card" hoverable>
              <Checkbox value="taskBreakdown">
                <div className="analysis-type-content">
                  <h3>📋 任务拆分 (Task Breakdown)</h3>
                  <p>将需求拆分为可执行的开发任务，包括前端、后端、数据库、测试等，并评估工作量</p>
                </div>
              </Checkbox>
            </Card>
          </Col>

          <Col span={24}>
            <Card className="analysis-type-card" hoverable>
              <Checkbox value="acceptance">
                <div className="analysis-type-content">
                  <h3>✅ 验收补全 (Acceptance Criteria)</h3>
                  <p>自动生成验收标准，包括功能性、性能、安全性和可用性等多个维度</p>
                </div>
              </Checkbox>
            </Card>
          </Col>

          <Col span={24}>
            <Card className="analysis-type-card" hoverable>
              <Checkbox value="similarity">
                <div className="analysis-type-content">
                  <h3>🔍 相似度检测 (Similarity Check)</h3>
                  <p>检测系统中是否存在相似或重复的需求，避免重复开发</p>
                </div>
              </Checkbox>
            </Card>
          </Col>
        </Row>
      </Checkbox.Group>
    </Card>
  )

  const renderStep2 = () => (
    <Card title="AI 分析中" className="step-card analyzing-card">
      <div className="analyzing-content">
        <ThunderboltOutlined className="analyzing-icon" />
        <h2>AI 正在分析需求...</h2>
        <p>请稍候，这可能需要几秒钟时间</p>
        <Progress
          percent={progress}
          status="active"
          strokeColor={{
            '0%': '#108ee9',
            '100%': '#87d068',
          }}
          style={{ width: '60%', margin: '24px auto' }}
        />
        <div className="analyzing-tips">
          <p>💡 AI 正在分析需求的各个维度</p>
          <p>🔍 检测潜在问题和改进空间</p>
          <p>📊 生成详细的分析报告</p>
        </div>
      </div>
    </Card>
  )

  const renderStep3 = () => {
    if (!analysisResult) return null

    const tabItems = []

    // 需求摘要
    if (analysisResult.summary) {
      tabItems.push({
        key: 'summary',
        label: '📝 需求摘要',
        children: (
          <div className="result-section">
            <h3>{analysisResult.summary.title}</h3>
            <Card title="关键要点" style={{ marginTop: 16 }}>
              <ul>
                {analysisResult.summary.keyPoints.map((point, index) => (
                  <li key={index}>{point}</li>
                ))}
              </ul>
            </Card>
            <Row gutter={16} style={{ marginTop: 16 }}>
              <Col span={12}>
                <Card>
                  <Statistic
                    title="相关方"
                    value={analysisResult.summary.stakeholders.length}
                    suffix="个"
                  />
                  <div style={{ marginTop: 8 }}>
                    {analysisResult.summary.stakeholders.map((s) => (
                      <Tag key={s}>{s}</Tag>
                    ))}
                  </div>
                </Card>
              </Col>
              <Col span={12}>
                <Card>
                  <Statistic
                    title="预估工作量"
                    value={analysisResult.summary.estimatedEffort}
                  />
                </Card>
              </Col>
            </Row>
          </div>
        ),
      })
    }

    // 质量分析
    if (analysisResult.qualityAnalysis) {
      const qa = analysisResult.qualityAnalysis
      tabItems.push({
        key: 'quality',
        label: '🎯 质量分析',
        children: (
          <div className="result-section">
            <Row gutter={16}>
              <Col span={6}>
                <Card>
                  <Statistic
                    title="综合评分"
                    value={qa.overallScore}
                    suffix="/ 100"
                    valueStyle={{ color: qa.overallScore >= 80 ? '#3f8600' : qa.overallScore >= 60 ? '#faad14' : '#cf1322' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic title="完整性" value={qa.completeness} suffix="/ 100" />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic title="清晰度" value={qa.clarity} suffix="/ 100" />
                </Card>
              </Col>
              <Col span={6}>
                <Card>
                  <Statistic title="可测性" value={qa.testability} suffix="/ 100" />
                </Card>
              </Col>
            </Row>

            <Card title="问题和建议" style={{ marginTop: 16 }}>
              {qa.issues.map((issue, index) => (
                <Alert
                  key={index}
                  message={issue.message}
                  description={issue.suggestion}
                  type={issue.type === 'ERROR' ? 'error' : issue.type === 'WARNING' ? 'warning' : 'info'}
                  showIcon
                  icon={
                    issue.type === 'ERROR' ? (
                      <WarningOutlined />
                    ) : issue.type === 'WARNING' ? (
                      <WarningOutlined />
                    ) : (
                      <InfoCircleOutlined />
                    )
                  }
                  style={{ marginBottom: 12 }}
                />
              ))}
            </Card>
          </div>
        ),
      })
    }

    // 任务拆分
    if (analysisResult.taskBreakdown) {
      const tb = analysisResult.taskBreakdown
      const taskColumns = [
        {
          title: '任务ID',
          dataIndex: 'id',
          key: 'id',
          width: 100,
        },
        {
          title: '任务标题',
          dataIndex: 'title',
          key: 'title',
        },
        {
          title: '任务描述',
          dataIndex: 'description',
          key: 'description',
        },
        {
          title: '类型',
          dataIndex: 'type',
          key: 'type',
          width: 100,
          render: (type: string) => {
            const colors: Record<string, string> = {
              FRONTEND: 'blue',
              BACKEND: 'green',
              DATABASE: 'purple',
              TEST: 'orange',
              DESIGN: 'cyan',
            }
            return <Tag color={colors[type]}>{type}</Tag>
          },
        },
        {
          title: '预估工时',
          dataIndex: 'estimatedHours',
          key: 'estimatedHours',
          width: 100,
          render: (hours: number) => `${hours}h`,
        },
        {
          title: '优先级',
          dataIndex: 'priority',
          key: 'priority',
          width: 80,
          render: (priority: number) => <Tag>{priority}</Tag>,
        },
      ]

      tabItems.push({
        key: 'taskBreakdown',
        label: '📋 任务拆分',
        children: (
          <div className="result-section">
            <Card>
              <Statistic
                title="总预估工时"
                value={tb.totalEstimatedHours}
                suffix="小时"
                prefix={<CheckCircleOutlined />}
              />
            </Card>
            <Table
              columns={taskColumns}
              dataSource={tb.tasks}
              rowKey="id"
              pagination={false}
              style={{ marginTop: 16 }}
            />
          </div>
        ),
      })
    }

    // 验收标准
    if (analysisResult.acceptanceCriteria) {
      const ac = analysisResult.acceptanceCriteria
      const criteriaColumns = [
        {
          title: 'ID',
          dataIndex: 'id',
          key: 'id',
          width: 100,
        },
        {
          title: '验收标准描述',
          dataIndex: 'description',
          key: 'description',
        },
        {
          title: '类型',
          dataIndex: 'type',
          key: 'type',
          width: 120,
          render: (type: string) => {
            const colors: Record<string, string> = {
              FUNCTIONAL: 'blue',
              PERFORMANCE: 'green',
              SECURITY: 'red',
              USABILITY: 'purple',
            }
            const labels: Record<string, string> = {
              FUNCTIONAL: '功能',
              PERFORMANCE: '性能',
              SECURITY: '安全',
              USABILITY: '可用性',
            }
            return <Tag color={colors[type]}>{labels[type]}</Tag>
          },
        },
        {
          title: '优先级',
          dataIndex: 'priority',
          key: 'priority',
          width: 100,
          render: (priority: string) => {
            const colors: Record<string, string> = {
              MUST: 'red',
              SHOULD: 'orange',
              COULD: 'default',
            }
            return <Tag color={colors[priority]}>{priority}</Tag>
          },
        },
      ]

      tabItems.push({
        key: 'acceptance',
        label: '✅ 验收标准',
        children: (
          <div className="result-section">
            <Alert
              message={`共生成 ${ac.criteria.length} 条验收标准`}
              type="success"
              showIcon
              style={{ marginBottom: 16 }}
            />
            <Table
              columns={criteriaColumns}
              dataSource={ac.criteria}
              rowKey="id"
              pagination={false}
            />
          </div>
        ),
      })
    }

    // 相似度检测
    if (analysisResult.similarityCheck) {
      const sc = analysisResult.similarityCheck
      const similarColumns = [
        {
          title: '需求ID',
          dataIndex: 'id',
          key: 'id',
          width: 120,
        },
        {
          title: '需求标题',
          dataIndex: 'title',
          key: 'title',
        },
        {
          title: '相似度',
          dataIndex: 'similarity',
          key: 'similarity',
          width: 120,
          render: (similarity: number) => (
            <Progress
              percent={Math.round(similarity * 100)}
              size="small"
              status={similarity > 0.8 ? 'exception' : 'normal'}
            />
          ),
        },
        {
          title: '原因',
          dataIndex: 'reason',
          key: 'reason',
        },
      ]

      tabItems.push({
        key: 'similarity',
        label: '🔍 相似度检测',
        children: (
          <div className="result-section">
            {sc.hasSimilar ? (
              <>
                <Alert
                  message="发现相似需求"
                  description={`检测到 ${sc.similarRequirements.length} 个相似需求，请注意避免重复开发`}
                  type="warning"
                  showIcon
                  style={{ marginBottom: 16 }}
                />
                <Table
                  columns={similarColumns}
                  dataSource={sc.similarRequirements}
                  rowKey="id"
                  pagination={false}
                />
              </>
            ) : (
              <Alert
                message="未发现相似需求"
                description="该需求在系统中是唯一的，可以放心开发"
                type="success"
                showIcon
              />
            )}
          </div>
        ),
      })
    }

    return (
      <Card title="分析结果" className="step-card">
        <Alert
          message="分析完成"
          description={`分析ID: ${analysisResult.requestId} | 分析时间: ${new Date(analysisResult.timestamp).toLocaleString()}`}
          type="success"
          showIcon
          icon={<CheckCircleOutlined />}
          style={{ marginBottom: 24 }}
        />
        <Tabs items={tabItems} />
      </Card>
    )
  }

  const steps = [
    { title: '输入需求', content: renderStep0() },
    { title: '选择分析', content: renderStep1() },
    { title: 'AI 分析', content: renderStep2() },
    { title: '查看结果', content: renderStep3() },
  ]

  return (
    <div className="ai-requirement-analysis-container">
      <Card className="header-card">
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={handleBack}>
            返回
          </Button>
          <h2 style={{ margin: 0 }}>AI 需求分析 (AI Requirement Analysis)</h2>
        </Space>
      </Card>

      <Steps current={currentStep} style={{ marginBottom: 24 }}>
        {steps.map((item) => (
          <Step key={item.title} title={item.title} />
        ))}
      </Steps>

      <div className="steps-content">{steps[currentStep].content}</div>

      <div className="steps-action">
        {currentStep < 2 && (
          <Button type="primary" size="large" onClick={handleStepNext}>
            下一步
          </Button>
        )}
        {currentStep === 3 && (
          <Space>
            <Button size="large" onClick={handleReset}>
              重新分析
            </Button>
            <Button type="primary" size="large" onClick={() => navigate('/requirements/list')}>
              返回列表
            </Button>
          </Space>
        )}
      </div>
    </div>
  )
}

export default AIRequirementAnalysis
