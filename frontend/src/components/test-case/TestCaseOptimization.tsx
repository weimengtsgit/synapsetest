import React, { useState } from 'react'
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
import './TestCaseOptimization.css'

interface DuplicateGroup {
  id: number
  similarity: number
  cases: Array<{
    id: string
    title: string
    qualityScore: number
    lastExecuted: string
    defectsFound: number
    selected: boolean
  }>
  reason: string
}

const TestCaseOptimization: React.FC = () => {
  const [scope, setScope] = useState<string>('module')
  const [selectedModules, setSelectedModules] = useState<string[]>(['用户认证', '支付系统'])
  const [similarityThreshold, setSimilarityThreshold] = useState<number>(85)
  const [analyzing, setAnalyzing] = useState<boolean>(false)
  const [showResults, setShowResults] = useState<boolean>(false)
  const [duplicateGroups, setDuplicateGroups] = useState<DuplicateGroup[]>([])

  // Mock modules
  const modules = ['用户认证', '支付系统', '订单中心', '搜索引擎']

  // Statistics
  const [stats] = useState({
    originalCount: 328,
    duplicateCount: 23,
    optimizedCount: 305,
    timeSaved: 2.5,
  })

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

    setAnalyzing(true)
    message.loading('正在分析测试用例...', 0)

    // Simulate API call
    setTimeout(() => {
      message.destroy()

      // Mock duplicate groups data
      const mockGroups: DuplicateGroup[] = [
        {
          id: 1,
          similarity: 92,
          cases: [
            {
              id: 'TC001',
              title: '手机号+验证码正常登录',
              qualityScore: 95,
              lastExecuted: '2天前',
              defectsFound: 2,
              selected: true,
            },
            {
              id: 'TC015',
              title: '测试用户登录功能',
              qualityScore: 78,
              lastExecuted: '15天前',
              defectsFound: 0,
              selected: false,
            },
          ],
          reason: '测试步骤80%相似，预期结果完全一致',
        },
        {
          id: 2,
          similarity: 88,
          cases: [
            {
              id: 'TC102',
              title: '支付订单测试',
              qualityScore: 90,
              lastExecuted: '1天前',
              defectsFound: 1,
              selected: true,
            },
            {
              id: 'TC210',
              title: '验证订单支付流程',
              qualityScore: 82,
              lastExecuted: '7天前',
              defectsFound: 0,
              selected: false,
            },
          ],
          reason: '测试步骤75%相似，覆盖场景重叠',
        },
      ]

      setDuplicateGroups(mockGroups)
      setShowResults(true)
      setAnalyzing(false)
      message.success('分析完成！')
    }, 2000)
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
            selected: c.id === caseId,
          })),
        }
      }
      return group
    })
    setDuplicateGroups(updatedGroups)
  }

  const handleConfirmOptimization = () => {
    // Calculate how many cases will be removed
    const toRemove = duplicateGroups.reduce(
      (acc, group) => acc + group.cases.filter((c) => !c.selected).length,
      0
    )

    message.success(`优化成功！已删除 ${toRemove} 个重复用例`)
    setShowResults(false)
    setDuplicateGroups([])
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
            style={{ background: '#e6f7ff', borderColor: '#91d5ff' }}
            hoverable
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
            style={{ background: '#f6ffed', borderColor: '#b7eb8f' }}
            hoverable
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
            style={{ background: '#fff7e6', borderColor: '#ffd591' }}
            hoverable
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

      {/* Smart Deduplication Form */}
      <Card title="智能去重 (Smart Deduplication)" style={{ marginBottom: 24 }}>
        <div style={{ marginBottom: 24 }}>
          <div style={{ marginBottom: 12, fontWeight: 'bold' }}>选择用例范围</div>
          <Radio.Group onChange={handleScopeChange} value={scope}>
            <Space direction="vertical">
              <Radio value="all">全部用例</Radio>
              <Radio value="module">指定模块</Radio>
              <Radio value="selected">选中的用例</Radio>
            </Space>
          </Radio.Group>
        </div>

        {scope === 'module' && (
          <div style={{ marginBottom: 24 }}>
            <div style={{ marginBottom: 12, fontWeight: 'bold' }}>模块选择</div>
            <Checkbox.Group
              options={modules}
              value={selectedModules}
              onChange={(checkedValues) => handleModuleChange(checkedValues as string[])}
            />
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

        <Button type="primary" onClick={handleAnalyze} loading={analyzing}>
          开始分析
        </Button>
      </Card>

      {/* Deduplication Results */}
      {showResults && (
        <Card title="去重结果 (Deduplication Results)">
          <Alert
            message="分析完成！"
            description={
              <div>
                <div>• 原始用例数: {stats.originalCount} 条</div>
                <div>• 重复用例数: {stats.duplicateCount} 条 ({Math.round((stats.duplicateCount / stats.originalCount) * 100)}%)</div>
                <div>• 优化后数量: {stats.optimizedCount} 条</div>
                <div>• 节省工作量: 约 {stats.timeSaved} 小时</div>
              </div>
            }
            type="success"
            showIcon
            style={{ marginBottom: 16 }}
          />

          <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ fontWeight: 'bold' }}>
              重复用例详情 (发现 {duplicateGroups.length} 组)
            </div>
            <Space>
              <Button onClick={handleKeepAll}>全部保留第一个</Button>
              <Button onClick={handleDeleteAll} danger>全部删除重复</Button>
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
                  key={testCase.id}
                  style={{
                    background: testCase.selected ? '#e6f7ff' : '#fff1f0',
                    padding: 12,
                    borderRadius: 4,
                    marginBottom: 8,
                    borderLeft: testCase.selected ? '4px solid #1890ff' : '4px solid #f5222d',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                    <Radio
                      checked={testCase.selected}
                      onChange={() => handleSelectCase(group.id, testCase.id)}
                    />
                    <span style={{ fontWeight: 'bold' }}>
                      {testCase.selected ? '●' : '○'} {testCase.id} - {testCase.title}
                    </span>
                  </div>
                  <div style={{ color: '#595959', fontSize: 12, marginBottom: 8, marginLeft: 24 }}>
                    质量分数: {testCase.qualityScore}分 | 最近执行: {testCase.lastExecuted} | 发现缺陷: {testCase.defectsFound}个
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
    </div>
  )
}

export default TestCaseOptimization
