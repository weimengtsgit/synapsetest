import React, { useState, useEffect } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  message,
  Progress,
  Tag,
  Tooltip,
  Slider,
  InputNumber,
  Row,
  Col,
} from 'antd'
import {
  SortAscendingOutlined,
  ReloadOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import testCaseService from '../../services/testCaseService'
import './TestCaseOptimization.css'

interface PrioritizedTestCase {
  id: string
  title: string
  name: string
  caseName: string
  module: string
  type: string
  priority: number
  priority_score: number
  rank: number
  score_breakdown: {
    business_value: number
    risk_level: number
    execution_cost: number
    failure_history: number
    coverage_impact: number
  }
}

interface WeightsConfig {
  business_value: number
  risk_level: number
  execution_cost: number
  failure_history: number
  coverage_impact: number
}

const TestCasePrioritization: React.FC = () => {
  const [allTestCases, setAllTestCases] = useState<any[]>([])
  const [prioritizedCases, setPrioritizedCases] = useState<PrioritizedTestCase[]>([])
  const [loading, setLoading] = useState<boolean>(false)
  const [showResults, setShowResults] = useState<boolean>(false)

  // Customizable weights
  const [weights, setWeights] = useState<WeightsConfig>({
    business_value: 30,
    risk_level: 25,
    execution_cost: 20,
    failure_history: 15,
    coverage_impact: 10,
  })

  useEffect(() => {
    loadTestCases()
  }, [])

  const loadTestCases = async () => {
    try {
      const response = await testCaseService.getAllTestCases()
      if (response.success && response.data) {
        setAllTestCases(response.data.content || response.data)
      }
    } catch (error: any) {
      console.error('Failed to load test cases:', error)
      message.error('加载测试用例失败: ' + (error.message || '未知错误'))
    }
  }

  const handlePrioritize = async () => {
    if (allTestCases.length === 0) {
      message.warning('没有可排序的测试用例')
      return
    }

    setLoading(true)
    const loadingMsg = message.loading('正在计算优先级...', 0)

    try {
      // Convert weights to decimal (0-1)
      const customWeights = {
        business_value: weights.business_value / 100,
        risk_level: weights.risk_level / 100,
        execution_cost: weights.execution_cost / 100,
        failure_history: weights.failure_history / 100,
        coverage_impact: weights.coverage_impact / 100,
      }

      const response = await testCaseService.prioritizeTestCases({
        testcases: allTestCases,
        customWeights,
      })

      loadingMsg()

      if (response.success || response.data) {
        const data = response.data || response
        setPrioritizedCases(data.prioritized_testcases || [])
        setShowResults(true)
        message.success(`排序完成！共 ${data.total_cases} 个测试用例`)
      } else {
        message.error('排序失败: ' + (response.message || '未知错误'))
      }
    } catch (error: any) {
      loadingMsg()
      console.error('Prioritization failed:', error)
      message.error('排序失败: ' + (error.message || '未知错误'))
    } finally {
      setLoading(false)
    }
  }

  const handleWeightChange = (key: keyof WeightsConfig, value: number | null) => {
    if (value !== null) {
      setWeights({
        ...weights,
        [key]: value,
      })
    }
  }

  const getTotalWeight = () => {
    return Object.values(weights).reduce((sum, val) => sum + val, 0)
  }

  const columns = [
    {
      title: '排名',
      dataIndex: 'rank',
      key: 'rank',
      width: 80,
      render: (rank: number) => (
        <Tag color={rank <= 3 ? 'red' : rank <= 10 ? 'orange' : 'default'}>
          #{rank}
        </Tag>
      ),
    },
    {
      title: '用例标题',
      dataIndex: 'title',
      key: 'title',
      width: 300,
      render: (text: string, record: PrioritizedTestCase) => (
        <div>
          <div>{text || record.name || record.caseName}</div>
          <div style={{ fontSize: 12, color: '#8c8c8c' }}>
            {record.module && `模块: ${record.module}`}
          </div>
        </div>
      ),
    },
    {
      title: (
        <span>
          优先级分数{' '}
          <Tooltip title="综合评分,分数越高优先级越高">
            <InfoCircleOutlined />
          </Tooltip>
        </span>
      ),
      dataIndex: 'priority_score',
      key: 'priority_score',
      width: 150,
      render: (score: number) => (
        <div>
          <Progress
            percent={Math.round(score * 100)}
            size="small"
            status={score > 0.8 ? 'success' : score > 0.5 ? 'normal' : 'exception'}
          />
          <div style={{ fontSize: 12, color: '#8c8c8c' }}>
            {(score * 100).toFixed(1)}分
          </div>
        </div>
      ),
    },
    {
      title: '评分明细',
      key: 'breakdown',
      width: 300,
      render: (_: any, record: PrioritizedTestCase) => {
        const breakdown = record.score_breakdown || {}
        return (
          <Space direction="vertical" size="small" style={{ width: '100%' }}>
            <div style={{ fontSize: 11 }}>
              <span style={{ color: '#1890ff' }}>业务价值: {(breakdown.business_value * 100).toFixed(0)}%</span>
            </div>
            <div style={{ fontSize: 11 }}>
              <span style={{ color: '#52c41a' }}>风险等级: {(breakdown.risk_level * 100).toFixed(0)}%</span>
            </div>
            <div style={{ fontSize: 11 }}>
              <span style={{ color: '#fa8c16' }}>执行成本: {(breakdown.execution_cost * 100).toFixed(0)}%</span>
            </div>
          </Space>
        )
      },
    },
    {
      title: '原始优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 100,
      render: (priority: number) => (
        <Tag color={priority >= 8 ? 'red' : priority >= 5 ? 'orange' : 'default'}>
          P{priority || 3}
        </Tag>
      ),
    },
  ]

  return (
    <div>
      <Card title="优先级排序 (Priority Ranking)" style={{ marginBottom: 24 }}>
        <div style={{ marginBottom: 24 }}>
          <div style={{ fontWeight: 'bold', marginBottom: 16 }}>
            权重配置 (总计: {getTotalWeight()}%)
            {getTotalWeight() !== 100 && (
              <Tag color="warning" style={{ marginLeft: 8 }}>
                建议总和为100%
              </Tag>
            )}
          </div>

          <Row gutter={[16, 16]}>
            <Col span={12}>
              <div style={{ marginBottom: 8 }}>
                <span>业务价值权重: {weights.business_value}%</span>
                <Tooltip title="用例对业务的重要程度">
                  <InfoCircleOutlined style={{ marginLeft: 4, color: '#8c8c8c' }} />
                </Tooltip>
              </div>
              <Row gutter={8}>
                <Col flex="auto">
                  <Slider
                    min={0}
                    max={100}
                    value={weights.business_value}
                    onChange={(value) => handleWeightChange('business_value', value)}
                  />
                </Col>
                <Col>
                  <InputNumber
                    min={0}
                    max={100}
                    size="small"
                    style={{ width: 60 }}
                    value={weights.business_value}
                    onChange={(value) => handleWeightChange('business_value', value)}
                  />
                </Col>
              </Row>
            </Col>

            <Col span={12}>
              <div style={{ marginBottom: 8 }}>
                <span>风险等级权重: {weights.risk_level}%</span>
                <Tooltip title="用例失败可能造成的风险">
                  <InfoCircleOutlined style={{ marginLeft: 4, color: '#8c8c8c' }} />
                </Tooltip>
              </div>
              <Row gutter={8}>
                <Col flex="auto">
                  <Slider
                    min={0}
                    max={100}
                    value={weights.risk_level}
                    onChange={(value) => handleWeightChange('risk_level', value)}
                  />
                </Col>
                <Col>
                  <InputNumber
                    min={0}
                    max={100}
                    size="small"
                    style={{ width: 60 }}
                    value={weights.risk_level}
                    onChange={(value) => handleWeightChange('risk_level', value)}
                  />
                </Col>
              </Row>
            </Col>

            <Col span={12}>
              <div style={{ marginBottom: 8 }}>
                <span>执行成本权重: {weights.execution_cost}%</span>
                <Tooltip title="用例执行所需的时间和资源">
                  <InfoCircleOutlined style={{ marginLeft: 4, color: '#8c8c8c' }} />
                </Tooltip>
              </div>
              <Row gutter={8}>
                <Col flex="auto">
                  <Slider
                    min={0}
                    max={100}
                    value={weights.execution_cost}
                    onChange={(value) => handleWeightChange('execution_cost', value)}
                  />
                </Col>
                <Col>
                  <InputNumber
                    min={0}
                    max={100}
                    size="small"
                    style={{ width: 60 }}
                    value={weights.execution_cost}
                    onChange={(value) => handleWeightChange('execution_cost', value)}
                  />
                </Col>
              </Row>
            </Col>

            <Col span={12}>
              <div style={{ marginBottom: 8 }}>
                <span>失败历史权重: {weights.failure_history}%</span>
                <Tooltip title="用例历史失败频率">
                  <InfoCircleOutlined style={{ marginLeft: 4, color: '#8c8c8c' }} />
                </Tooltip>
              </div>
              <Row gutter={8}>
                <Col flex="auto">
                  <Slider
                    min={0}
                    max={100}
                    value={weights.failure_history}
                    onChange={(value) => handleWeightChange('failure_history', value)}
                  />
                </Col>
                <Col>
                  <InputNumber
                    min={0}
                    max={100}
                    size="small"
                    style={{ width: 60 }}
                    value={weights.failure_history}
                    onChange={(value) => handleWeightChange('failure_history', value)}
                  />
                </Col>
              </Row>
            </Col>

            <Col span={12}>
              <div style={{ marginBottom: 8 }}>
                <span>覆盖率影响权重: {weights.coverage_impact}%</span>
                <Tooltip title="用例对整体覆盖率的贡献">
                  <InfoCircleOutlined style={{ marginLeft: 4, color: '#8c8c8c' }} />
                </Tooltip>
              </div>
              <Row gutter={8}>
                <Col flex="auto">
                  <Slider
                    min={0}
                    max={100}
                    value={weights.coverage_impact}
                    onChange={(value) => handleWeightChange('coverage_impact', value)}
                  />
                </Col>
                <Col>
                  <InputNumber
                    min={0}
                    max={100}
                    size="small"
                    style={{ width: 60 }}
                    value={weights.coverage_impact}
                    onChange={(value) => handleWeightChange('coverage_impact', value)}
                  />
                </Col>
              </Row>
            </Col>
          </Row>
        </div>

        <Space>
          <Button
            type="primary"
            icon={<SortAscendingOutlined />}
            onClick={handlePrioritize}
            loading={loading}
            disabled={allTestCases.length === 0}
          >
            开始排序
          </Button>
          <Button icon={<ReloadOutlined />} onClick={loadTestCases}>
            刷新用例
          </Button>
        </Space>
      </Card>

      {/* Prioritization Results */}
      {showResults && prioritizedCases.length > 0 && (
        <Card title={`排序结果 (共 ${prioritizedCases.length} 条)`}>
          <Table
            dataSource={prioritizedCases}
            columns={columns}
            rowKey={(record) => record.id}
            pagination={{
              pageSize: 20,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 条`,
            }}
          />
        </Card>
      )}
    </div>
  )
}

export default TestCasePrioritization
