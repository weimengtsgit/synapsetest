import React, { useState, useEffect } from 'react'
import {
  Card,
  Row,
  Col,
  Radio,
  Checkbox,
  Slider,
  Button,
  Space,
  Alert,
  Divider,
  message,
  Tag,
  RadioChangeEvent,
  CheckboxChangeEvent,
} from 'antd'
import {
  ReloadOutlined,
  BarChartOutlined,
  LineChartOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
} from '@ant-design/icons'
import testCaseService from '../../services/testCaseService'
import TestCasePrioritization from './TestCasePrioritization'
import TestCaseQualityAnalysis from './TestCaseQualityAnalysis'
import './TestCaseOptimization.css'

interface TestCase {
  id: string
  testcase_id: string
  title: string
  name: string
  module: string
  type: string
  priority: string
  description: string
  steps: any[]
  preconditions: any[]
  expected_result: string
  quality_score: number
  last_executed: string
  defects_found: number
  selected: boolean
  similarity: number
}

interface DuplicateGroup {
  id: number
  similarity: number
  cases: TestCase[]
  reason: string
}

interface Statistics {
  original_count: number
  unique_count: number
  duplicate_count: number
  duplicate_groups: number
  time_saved_hours: number
}

const TestCaseOptimization: React.FC = () => {
  const [activeFeature, setActiveFeature] = useState<string>('dedup') // 'dedup', 'prioritize', 'quality'
  const [scope, setScope] = useState<string>('module')
  const [modules, setModules] = useState<string[]>([])
  const [selectedModules, setSelectedModules] = useState<string[]>([])
  const [similarityThreshold, setSimilarityThreshold] = useState<number>(85)
  const [analyzing, setAnalyzing] = useState<boolean>(false)
  const [showResults, setShowResults] = useState<boolean>(false)
  const [duplicateGroups, setDuplicateGroups] = useState<DuplicateGroup[]>([])
  const [stats, setStats] = useState<Statistics | null>(null)
  const [allTestCases, setAllTestCases] = useState<any[]>([])

  // Load modules and test cases on mount
  useEffect(() => {
    loadModules()
    loadTestCases()
  }, [])

  const loadModules = async () => {
    try {
      const response = await testCaseService.getAllModules()
      if (response.success && response.data) {
        setModules(response.data)
      }
    } catch (error: any) {
      console.error('Failed to load modules:', error)
      message.error('加载模块列表失败: ' + (error.message || '未知错误'))
    }
  }

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

  const handleScopeChange = (e: RadioChangeEvent) => {
    setScope(e.target.value)
  }

  const handleModuleChange = (checkedValues: string[]) => {
    setSelectedModules(checkedValues)
  }

  const handleAnalyze = async () => {
    if (scope === 'module' && selectedModules.length === 0) {
      message.warning('请至少选择一个模块')
      return
    }

    if (allTestCases.length === 0) {
      message.warning('没有可分析的测试用例')
      return
    }

    setAnalyzing(true)
    const loadingMsg = message.loading('正在分析测试用例...', 0)

    try {
      const response = await testCaseService.analyzeDeduplication({
        testcases: allTestCases,
        threshold: similarityThreshold / 100,  // Convert percentage to decimal
        scope: scope,
        modules: selectedModules
      })

      loadingMsg()

      if (response.success) {
        const data = response.data || response
        setStats(data.statistics)
        setDuplicateGroups(data.duplicate_groups || [])
        setShowResults(true)
        message.success(`分析完成！发现 ${data.statistics.duplicate_groups} 组重复用例`)
      } else {
        message.error('分析失败: ' + (response.message || '未知错误'))
      }
    } catch (error: any) {
      loadingMsg()
      console.error('Analysis failed:', error)
      message.error('分析失败: ' + (error.message || '未知错误'))
    } finally {
      setAnalyzing(false)
    }
  }

  const handleKeepAll = () => {
    const updatedGroups = duplicateGroups.map((group) => ({
      ...group,
      cases: group.cases.map((c, idx) => ({
        ...c,
        selected: idx === 0,
      })),
    }))
    setDuplicateGroups(updatedGroups)
    message.info('已全部保留第一个用例')
  }

  const handleDeleteAll = () => {
    const updatedGroups = duplicateGroups.map((group) => ({
      ...group,
      cases: group.cases.map((c, idx) => ({
        ...c,
        selected: idx === 0,
      })),
    }))
    setDuplicateGroups(updatedGroups)
    message.warning('已标记所有重复用例为删除')
  }

  const handleSelectCase = (groupId: number, caseId: string) => {
    const updatedGroups = duplicateGroups.map((group) => {
      if (group.id === groupId) {
        return {
          ...group,
          cases: group.cases.map((c) => ({
            ...c,
            selected: c.id === caseId || c.testcase_id === caseId,
          })),
        }
      }
      return group
    })
    setDuplicateGroups(updatedGroups)
  }

  const handleConfirmOptimization = async () => {
    // Collect IDs of cases to delete (not selected)
    const toDeleteIds = duplicateGroups.flatMap((group) =>
      group.cases.filter((c) => !c.selected).map((c) => c.id || c.testcase_id)
    )

    if (toDeleteIds.length === 0) {
      message.warning('没有需要删除的重复用例')
      setShowResults(false)
      setDuplicateGroups([])
      setStats(null)
      return
    }

    try {
      const loadingMsg = message.loading(`正在删除 ${toDeleteIds.length} 个重复用例...`, 0)

      // Delete test cases one by one
      let successCount = 0
      let failCount = 0

      for (const id of toDeleteIds) {
        try {
          await testCaseService.deleteTestCase(id)
          successCount++
        } catch (error) {
          console.error(`Failed to delete test case ${id}:`, error)
          failCount++
        }
      }

      loadingMsg()

      if (successCount > 0) {
        message.success(
          `优化完成！成功删除 ${successCount} 个重复用例` +
            (failCount > 0 ? `，${failCount} 个删除失败` : '')
        )
      } else {
        message.error('删除失败，请重试')
      }

      setShowResults(false)
      setDuplicateGroups([])
      setStats(null)

      // Reload test cases
      await loadTestCases()
    } catch (error: any) {
      console.error('Optimization failed:', error)
      message.error('优化失败: ' + (error.message || '未知错误'))
    }
  }

  return (
    <div className="optimization-container">
      <h2 style={{ marginBottom: 24, color: '#1890ff', fontWeight: 'bold' }}>
        测试用例优化 (Test Case Optimization)
      </h2>

      {/* Quick Action Cards */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={8}>
          <Card
            className="quick-action-card"
            style={{
              background: activeFeature === 'dedup' ? '#e6f7ff' : '#fafafa',
              borderColor: activeFeature === 'dedup' ? '#1890ff' : '#d9d9d9',
              borderWidth: activeFeature === 'dedup' ? 2 : 1,
              cursor: 'pointer'
            }}
            hoverable
            onClick={() => setActiveFeature('dedup')}
          >
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <ReloadOutlined style={{ fontSize: 32, color: '#1890ff', marginRight: 16 }} />
              <div>
                <div style={{ fontWeight: 'bold', fontSize: 16, color: '#1890ff' }}>
                  智能去重
                </div>
                <div style={{ fontSize: 12, color: '#595959' }}>
                  基于语义相似度去除重复用例
                </div>
              </div>
            </div>
          </Card>
        </Col>
        <Col span={8}>
          <Card
            className="quick-action-card"
            style={{
              background: activeFeature === 'prioritize' ? '#f6ffed' : '#fafafa',
              borderColor: activeFeature === 'prioritize' ? '#52c41a' : '#d9d9d9',
              borderWidth: activeFeature === 'prioritize' ? 2 : 1,
              cursor: 'pointer'
            }}
            hoverable
            onClick={() => setActiveFeature('prioritize')}
          >
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <BarChartOutlined style={{ fontSize: 32, color: '#52c41a', marginRight: 16 }} />
              <div>
                <div style={{ fontWeight: 'bold', fontSize: 16, color: '#52c41a' }}>
                  优先级排序
                </div>
                <div style={{ fontSize: 12, color: '#595959' }}>
                  多因素评分智能排序
                </div>
              </div>
            </div>
          </Card>
        </Col>
        <Col span={8}>
          <Card
            className="quick-action-card"
            style={{
              background: activeFeature === 'quality' ? '#fff7e6' : '#fafafa',
              borderColor: activeFeature === 'quality' ? '#fa8c16' : '#d9d9d9',
              borderWidth: activeFeature === 'quality' ? 2 : 1,
              cursor: 'pointer'
            }}
            hoverable
            onClick={() => setActiveFeature('quality')}
          >
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <LineChartOutlined style={{ fontSize: 32, color: '#fa8c16', marginRight: 16 }} />
              <div>
                <div style={{ fontWeight: 'bold', fontSize: 16, color: '#fa8c16' }}>
                  质量分析
                </div>
                <div style={{ fontSize: 12, color: '#595959' }}>
                  评估用例完整性和有效性
                </div>
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Feature Content - Smart Deduplication */}
      {activeFeature === 'dedup' && (
        <>
          {/* Smart Deduplication Form */}
          <Card title="智能去重 (Smart Deduplication)" style={{ marginBottom: 24 }}>
        <div style={{ marginBottom: 24 }}>
          <div style={{ marginBottom: 12, fontWeight: 'bold' }}>选择用例范围</div>
          <Radio.Group onChange={handleScopeChange} value={scope}>
            <Space direction="vertical">
              <Radio value="all">全部用例 ({allTestCases.length} 条)</Radio>
              <Radio value="module">指定模块</Radio>
              <Radio value="selected">选中的用例</Radio>
            </Space>
          </Radio.Group>
        </div>

        {scope === 'module' && (
          <div style={{ marginBottom: 24 }}>
            <div style={{ marginBottom: 12, fontWeight: 'bold' }}>
              模块选择 {modules.length > 0 && `(共 ${modules.length} 个模块)`}
            </div>
            {modules.length > 0 ? (
              <Checkbox.Group
                options={modules}
                value={selectedModules}
                onChange={(checkedValues) => handleModuleChange(checkedValues as string[])}
              />
            ) : (
              <div style={{ color: '#8c8c8c' }}>暂无可用模块</div>
            )}
          </div>
        )}

        <div style={{ marginBottom: 24 }}>
          <div style={{ marginBottom: 12, fontWeight: 'bold' }}>
            相似度阈值: {similarityThreshold}%
          </div>
          <Slider
            min={0}
            max={100}
            value={similarityThreshold}
            onChange={setSimilarityThreshold}
            marks={{
              0: '0%',
              50: '50%',
              100: '100%',
            }}
          />
          <div style={{ color: '#8c8c8c', fontSize: 12, marginTop: 4 }}>
            阈值越高，去重越严格
          </div>
        </div>

        <Button
          type="primary"
          onClick={handleAnalyze}
          loading={analyzing}
          disabled={allTestCases.length === 0}
        >
          开始分析
        </Button>
      </Card>

      {/* Deduplication Results */}
      {showResults && stats && (
        <Card title="去重结果 (Deduplication Results)">
          <Alert
            message="分析完成！"
            description={
              <div>
                <div>• 原始用例数: {stats.original_count} 条</div>
                <div>
                  • 重复用例数: {stats.duplicate_count} 条 (
                  {stats.original_count > 0
                    ? Math.round((stats.duplicate_count / stats.original_count) * 100)
                    : 0}
                  %)
                </div>
                <div>• 优化后数量: {stats.unique_count} 条</div>
                <div>• 节省工作量: 约 {stats.time_saved_hours} 小时</div>
              </div>
            }
            type="success"
            showIcon
            style={{ marginBottom: 16 }}
          />

          <div
            style={{
              marginBottom: 16,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <div style={{ fontWeight: 'bold' }}>
              重复用例详情 (发现 {duplicateGroups.length} 组)
            </div>
            <Space>
              <Button onClick={handleKeepAll}>全部保留第一个</Button>
              <Button onClick={handleDeleteAll} danger>
                全部删除重复
              </Button>
            </Space>
          </div>

          {duplicateGroups.map((group) => (
            <Card
              key={group.id}
              size="small"
              style={{ marginBottom: 16 }}
              title={
                <span style={{ color: '#fa8c16' }}>
                  重复组 #{group.id} (相似度: {group.similarity}%)
                </span>
              }
            >
              {group.cases.map((testCase) => (
                <div
                  key={testCase.id || testCase.testcase_id}
                  style={{
                    background: testCase.selected ? '#e6f7ff' : '#fff1f0',
                    padding: 12,
                    borderRadius: 4,
                    marginBottom: 8,
                    borderLeft: testCase.selected
                      ? '4px solid #1890ff'
                      : '4px solid #f5222d',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                    <Radio
                      checked={testCase.selected}
                      onChange={() =>
                        handleSelectCase(group.id, testCase.id || testCase.testcase_id)
                      }
                    />
                    <span style={{ fontWeight: 'bold' }}>
                      {testCase.selected ? '●' : '○'} {testCase.testcase_id || testCase.id} -{' '}
                      {testCase.title || testCase.name}
                    </span>
                  </div>
                  <div
                    style={{ color: '#595959', fontSize: 12, marginBottom: 8, marginLeft: 24 }}
                  >
                    质量分数: {testCase.quality_score}分 | 最近执行: {testCase.last_executed} |
                    发现缺陷: {testCase.defects_found}个
                  </div>
                  <div style={{ marginLeft: 24 }}>
                    {testCase.selected ? (
                      <Tag icon={<CheckCircleOutlined />} color="success">
                        保留
                      </Tag>
                    ) : (
                      <Tag icon={<DeleteOutlined />} color="error">
                        删除
                      </Tag>
                    )}
                  </div>
                </div>
              ))}
              <div
                style={{
                  background: '#fafafa',
                  padding: '8px 12px',
                  borderRadius: 4,
                  fontSize: 12,
                  color: '#8c8c8c',
                  marginTop: 8,
                }}
              >
                相似原因: {group.reason}
              </div>
            </Card>
          ))}

          <Divider />

          <div style={{ textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setShowResults(false)}>取消</Button>
              <Button type="primary" onClick={handleConfirmOptimization}>
                确认优化
              </Button>
            </Space>
          </div>
        </Card>
      )}
        </>
      )}

      {/* Feature Content - Priority Ranking */}
      {activeFeature === 'prioritize' && <TestCasePrioritization />}

      {/* Feature Content - Quality Analysis */}
      {activeFeature === 'quality' && <TestCaseQualityAnalysis />}
    </div>
  )
}

export default TestCaseOptimization
