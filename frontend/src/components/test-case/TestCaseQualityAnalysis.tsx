import React, { useState, useEffect } from 'react'
import {
  Card,
  Button,
  Space,
  message,
  Progress,
  Row,
  Col,
  Statistic,
  Descriptions,
  Alert,
} from 'antd'
import {
  BarChartOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import { Pie } from '@ant-design/plots'
import testCaseService from '../../services/testCaseService'
import './TestCaseOptimization.css'

interface QualityMetrics {
  success: boolean
  total_cases: number
  priority_distribution: {
    P0: number
    P1: number
    P2: number
    P3: number
  }
  type_distribution: {
    [key: string]: number
  }
  average_steps_per_case: number
  completeness_score: number
  quality_grade: string
}

const TestCaseQualityAnalysis: React.FC = () => {
  const [allTestCases, setAllTestCases] = useState<any[]>([])
  const [qualityMetrics, setQualityMetrics] = useState<QualityMetrics | null>(null)
  const [loading, setLoading] = useState<boolean>(false)
  const [showResults, setShowResults] = useState<boolean>(false)

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

  const handleReset = () => {
    console.log('Resetting quality analysis...')
    setQualityMetrics(null)
    setShowResults(false)
    message.info('已重置分析结果')
  }

  const handleAnalyze = async () => {
    console.log('handleAnalyze called, allTestCases length:', allTestCases.length)
    
    if (allTestCases.length === 0) {
      message.warning('没有可分析的测试用例')
      return
    }

    // 先重置状态
    setQualityMetrics(null)
    setShowResults(false)
    
    setLoading(true)
    const loadingMsg = message.loading('正在分析测试用例质量...', 0)

    try {
      const response = await testCaseService.analyzeQuality(allTestCases)

      loadingMsg()

      console.log('=== Quality Analysis Response Debug ===')
      console.log('Raw response:', JSON.stringify(response, null, 2))
      console.log('Response type:', typeof response)
      console.log('Response keys:', response ? Object.keys(response) : 'null')
      
      if (!response) {
        console.error('Response is null or undefined!')
        message.error('分析失败: 未收到响应数据')
        return
      }
      
      // 提取实际数据 - 数据在 response.data 中
      const actualData = response.data || response
      console.log('Actual data:', JSON.stringify(actualData, null, 2))
      console.log('success:', actualData.success)
      console.log('total_cases:', actualData.total_cases)
      console.log('priority_distribution:', JSON.stringify(actualData.priority_distribution))
      console.log('type_distribution:', JSON.stringify(actualData.type_distribution))
      console.log('average_steps_per_case:', actualData.average_steps_per_case)
      console.log('completeness_score:', actualData.completeness_score)
      console.log('quality_grade:', actualData.quality_grade)
      console.log('======================================')

      // 设置实际数据
      console.log('About to set quality metrics...')
      setQualityMetrics(actualData)
      console.log('Quality metrics set!')
      setShowResults(true)
      console.log('Show results set to true!')
      message.success(`分析完成！质量等级: ${actualData.quality_grade ?? 'N/A'}`)
    } catch (error: any) {
      loadingMsg()
      console.error('Quality analysis failed:', error)
      message.error('分析失败: ' + (error.message || '未知错误'))
    } finally {
      setLoading(false)
    }
  }

  const getGradeColor = (grade: string) => {
    switch (grade) {
      case 'A':
        return '#52c41a'
      case 'B':
        return '#1890ff'
      case 'C':
        return '#faad14'
      case 'D':
        return '#f5222d'
      default:
        return '#8c8c8c'
    }
  }

  const getGradeIcon = (grade: string) => {
    if (grade === 'A' || grade === 'B') {
      return <CheckCircleOutlined style={{ color: getGradeColor(grade) }} />
    }
    return <WarningOutlined style={{ color: getGradeColor(grade) }} />
  }

  // 准备优先级分布饼图数据
  const getPriorityChartData = () => {
    console.log('[getPriorityChartData] Called')
    console.log('[getPriorityChartData] qualityMetrics:', qualityMetrics)
    
    if (!qualityMetrics) {
      console.log('[getPriorityChartData] No qualityMetrics, returning empty array')
      return []
    }
    
    console.log('[getPriorityChartData] priority_distribution:', qualityMetrics.priority_distribution)
    
    if (!qualityMetrics.priority_distribution) {
      console.log('[getPriorityChartData] No priority_distribution field!')
      return []
    }
    
    const entries = Object.entries(qualityMetrics.priority_distribution)
    console.log('[getPriorityChartData] Entries:', entries)
    
    const filtered = entries.filter(([_, value]) => value > 0)
    console.log('[getPriorityChartData] Filtered (value > 0):', filtered)
    
    const data = filtered.map(([key, value]) => ({
      type: key,
      value: value,
    }))
    console.log('[getPriorityChartData] Final data:', data)
    return data
  }

  // 准备类型分布饼图数据
  const getTypeChartData = () => {
    console.log('[getTypeChartData] Called')
    console.log('[getTypeChartData] qualityMetrics:', qualityMetrics)
    
    if (!qualityMetrics) {
      console.log('[getTypeChartData] No qualityMetrics, returning empty array')
      return []
    }
    
    console.log('[getTypeChartData] type_distribution:', qualityMetrics.type_distribution)
    
    if (!qualityMetrics.type_distribution) {
      console.log('[getTypeChartData] No type_distribution field!')
      return []
    }
    
    const entries = Object.entries(qualityMetrics.type_distribution)
    console.log('[getTypeChartData] Entries:', entries)
    
    const filtered = entries.filter(([_, value]) => value > 0)
    console.log('[getTypeChartData] Filtered (value > 0):', filtered)
    
    const data = filtered.map(([key, value]) => ({
      type: key,
      value: value,
    }))
    console.log('[getTypeChartData] Final data:', data)
    return data
  }

  // 饼图基础配置
  const priorityPieConfig = {
    angleField: 'value',
    colorField: 'type',
    radius: 0.9,
    label: {
      type: 'outer',
    },
    legend: {
      position: 'bottom' as const,
    },
  }

  const typePieConfig = {
    angleField: 'value',
    colorField: 'type',
    radius: 0.9,
    label: {
      type: 'outer',
    },
    legend: {
      position: 'bottom' as const,
    },
  }

  return (
    <div>
      <Card title="质量分析 (Quality Analysis)" style={{ marginBottom: 24 }}>
        <Alert
          message="测试用例质量分析"
          description="分析测试用例的完整性、优先级分布、类型分布等质量指标，帮助提升测试用例质量"
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />

        <Space>
          <Button
            type="primary"
            icon={<BarChartOutlined />}
            onClick={handleAnalyze}
            loading={loading}
            disabled={allTestCases.length === 0}
          >
            开始分析
          </Button>
          <Button icon={<ReloadOutlined />} onClick={loadTestCases}>
            刷新用例
          </Button>
          {showResults && (
            <Button onClick={handleReset}>
              重置结果
            </Button>
          )}
          <span style={{ color: '#8c8c8c', marginLeft: 8 }}>
            当前用例数: {allTestCases.length}
          </span>
        </Space>
      </Card>

      {/* Quality Analysis Results */}
      {showResults && qualityMetrics && (
        <Card title={`分析结果 (共 ${qualityMetrics.total_cases} 条测试用例)`}>
          {/* Overall Quality Grade */}
          <Row gutter={16} style={{ marginBottom: 24 }}>
            <Col span={24}>
              <Card
                style={{
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  color: 'white',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: 48, fontWeight: 'bold', marginBottom: 8 }}>
                  {getGradeIcon(qualityMetrics.quality_grade)}{' '}
                  {qualityMetrics.quality_grade}
                </div>
                <div style={{ fontSize: 18 }}>整体质量等级</div>
                <div style={{ marginTop: 16 }}>
                  <Progress
                    percent={qualityMetrics.completeness_score != null 
                      ? Math.round(qualityMetrics.completeness_score * 100) 
                      : 0}
                    strokeColor="white"
                    trailColor="rgba(255,255,255,0.3)"
                    format={(percent) => `完整度 ${percent}%`}
                  />
                </div>
              </Card>
            </Col>
          </Row>

          {/* Key Metrics */}
          <Row gutter={16} style={{ marginBottom: 24 }}>
            <Col span={8}>
              <Card>
                <Statistic
                  title="测试用例总数"
                  value={qualityMetrics.total_cases ?? 0}
                  prefix={<BarChartOutlined />}
                  valueStyle={{ color: '#1890ff' }}
                />
              </Card>
            </Col>
            <Col span={8}>
              <Card>
                <Statistic
                  title="平均步骤数"
                  value={qualityMetrics.average_steps_per_case ?? 0}
                  precision={2}
                  valueStyle={{ color: '#52c41a' }}
                />
              </Card>
            </Col>
            <Col span={8}>
              <Card>
                <Statistic
                  title="完整性分数"
                  value={(qualityMetrics.completeness_score ?? 0) * 100}
                  precision={1}
                  suffix="%"
                  valueStyle={{
                    color:
                      (qualityMetrics.completeness_score ?? 0) >= 0.75
                        ? '#52c41a'
                        : (qualityMetrics.completeness_score ?? 0) >= 0.5
                        ? '#faad14'
                        : '#f5222d',
                  }}
                />
              </Card>
            </Col>
          </Row>

          {/* Distribution Charts */}
          <Row gutter={16} style={{ marginBottom: 24 }}>
            <Col span={12}>
              <Card title="优先级分布" bordered={false}>
                <Pie
                  {...priorityPieConfig}
                  data={getPriorityChartData()}
                  height={300}
                />
                {getPriorityChartData().length === 0 && (
                  <div style={{ textAlign: 'center', padding: '20px 0', color: '#999' }}>
                    暂无数据
                  </div>
                )}
              </Card>
            </Col>
            <Col span={12}>
              <Card title="类型分布" bordered={false}>
                <Pie
                  {...typePieConfig}
                  data={getTypeChartData()}
                  height={300}
                />
                {getTypeChartData().length === 0 && (
                  <div style={{ textAlign: 'center', padding: '20px 0', color: '#999' }}>
                    暂无数据
                  </div>
                )}
              </Card>
            </Col>
          </Row>

          {/* Detailed Metrics */}
          <Card title="详细指标" bordered={false}>
            <Descriptions column={2} bordered>
              <Descriptions.Item label="总用例数">
                {qualityMetrics.total_cases}
              </Descriptions.Item>
              <Descriptions.Item label="质量等级">
                <span style={{ color: getGradeColor(qualityMetrics.quality_grade), fontWeight: 'bold', fontSize: 18 }}>
                  {getGradeIcon(qualityMetrics.quality_grade)} {qualityMetrics.quality_grade}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="完整性分数">
                {qualityMetrics.completeness_score != null 
                  ? (qualityMetrics.completeness_score * 100).toFixed(1) + '%'
                  : 'N/A'}
              </Descriptions.Item>
              <Descriptions.Item label="平均步骤数">
                {qualityMetrics.average_steps_per_case != null
                  ? qualityMetrics.average_steps_per_case.toFixed(2)
                  : 'N/A'}
              </Descriptions.Item>
              <Descriptions.Item label="P0 高优先级">
                {qualityMetrics.priority_distribution?.P0 ?? 0} 条
              </Descriptions.Item>
              <Descriptions.Item label="P1 中高优先级">
                {qualityMetrics.priority_distribution?.P1 ?? 0} 条
              </Descriptions.Item>
              <Descriptions.Item label="P2 中优先级">
                {qualityMetrics.priority_distribution?.P2 ?? 0} 条
              </Descriptions.Item>
              <Descriptions.Item label="P3 低优先级">
                {qualityMetrics.priority_distribution?.P3 ?? 0} 条
              </Descriptions.Item>
            </Descriptions>
          </Card>

          {/* Improvement Suggestions */}
          <Card title="改进建议" bordered={false} style={{ marginTop: 16 }}>
            <Space direction="vertical" style={{ width: '100%' }}>
              {qualityMetrics.quality_grade === 'D' && (
                <Alert
                  message="质量较低，需要立即改进"
                  description={
                    <ul>
                      <li>测试用例完整性较低，建议补充测试步骤和预期结果</li>
                      <li>增加前置条件说明，确保测试可重复执行</li>
                      <li>添加标签分类，便于管理和检索</li>
                    </ul>
                  }
                  type="error"
                  showIcon
                />
              )}
              {qualityMetrics.quality_grade === 'C' && (
                <Alert
                  message="质量一般，建议优化"
                  description={
                    <ul>
                      <li>部分测试用例缺少详细步骤或预期结果</li>
                      <li>建议补充前置条件，提高可维护性</li>
                      <li>考虑增加边界值和异常场景测试</li>
                    </ul>
                  }
                  type="warning"
                  showIcon
                />
              )}
              {qualityMetrics.quality_grade === 'B' && (
                <Alert
                  message="质量良好，可继续提升"
                  description={
                    <ul>
                      <li>测试用例整体质量较好</li>
                      <li>建议定期review和更新测试用例</li>
                      <li>可以增加更多标签提升可维护性</li>
                    </ul>
                  }
                  type="info"
                  showIcon
                />
              )}
              {qualityMetrics.quality_grade === 'A' && (
                <Alert
                  message="质量优秀，请继续保持"
                  description={
                    <ul>
                      <li>测试用例质量优秀，覆盖完整</li>
                      <li>保持良好的测试用例管理习惯</li>
                      <li>可作为团队最佳实践参考</li>
                    </ul>
                  }
                  type="success"
                  showIcon
                />
              )}
            </Space>
          </Card>
        </Card>
      )}
    </div>
  )
}

export default TestCaseQualityAnalysis
