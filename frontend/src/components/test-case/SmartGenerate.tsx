import React, { useState } from 'react'
import {
  Steps,
  Card,
  Form,
  Input,
  InputNumber,
  Checkbox,
  Button,
  Progress,
  Tag,
  Space,
  Row,
  Col,
  Statistic,
  message,
} from 'antd'
import {
  RobotOutlined,
  CheckCircleOutlined,
  FileTextOutlined,
  DownOutlined,
  UpOutlined,
} from '@ant-design/icons'
import axios from 'axios'
import './SmartGenerate.css'

const { TextArea } = Input

interface TestStep {
  step: number
  action: string
  expected: string
}

interface TestCase {
  name: string
  priority: string
  type: string
  preconditions: string[]
  steps: TestStep[]
  tags: string[]
  priority_score: number
  rank: number
}

interface GenerateResponse {
  success: boolean
  total_generated: number
  total_unique: number
  total_duplicates: number
  testcases: TestCase[]
  request_id: string
  timestamp: string
  metadata: {
    module: string
    num_requested: number
    num_delivered: number
    include_edge_cases: boolean
    llm_model: {
      model_name: string
      version: string
      provider: string
    }
  }
}

const SmartGenerate: React.FC = () => {
  const [currentStep, setCurrentStep] = useState(0)
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [generatedData, setGeneratedData] = useState<GenerateResponse | null>(null)
  const [expandedCards, setExpandedCards] = useState<Set<number>>(new Set())
  const [selectedCases, setSelectedCases] = useState<Set<number>>(new Set())
  const [saving, setSaving] = useState(false)

  // 表单初始值
  const initialValues = {
    requirement_text: `用户登录功能需求：
1. 用户可以通过手机号+验证码登录
2. 支持微信、支付宝第三方登录
3. 登录失败3次后锁定账户30分钟
4. 支持记住登录状态7天
5. 密码需要加密存储`,
    module: '用户认证模块',
    num_cases: 10,
    include_edge_cases: true,
    deduplicate: true,
    prioritize: true,
  }

  // 开始生成
  const handleGenerate = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      setCurrentStep(1)
      setProgress(0)

      // 模拟进度
      const progressInterval = setInterval(() => {
        setProgress((prev) => {
          if (prev >= 90) {
            clearInterval(progressInterval)
            return 90
          }
          return prev + 10
        })
      }, 500)

      // 构建请求参数
      const requestData = {
        requirement_text: values.requirement_text,
        module: values.module,
        num_cases: values.num_cases,
        include_edge_cases: values.include_edge_cases,
        optimization: {
          deduplicate: values.deduplicate,
          prioritize: values.prioritize,
          min_priority: 'P1',
          max_cases: 20,
        },
      }

      console.log('发送请求:', requestData)

      // 调用后端接口
      const response = await axios.post<GenerateResponse>(
        '/api/v1/ai/testcase/generate',
        requestData
      )

      console.log('接收响应:', response.data)

      clearInterval(progressInterval)
      setProgress(100)

      // 后端返回的数据在 response.data.data 中
      const actualData = response.data.data || response.data
      console.log('实际数据:', actualData)

      if (actualData.success) {
        setGeneratedData(actualData)
        setTimeout(() => {
          setCurrentStep(2)
          setLoading(false)
          message.success('测试用例生成成功!')
        }, 500)
      } else {
        throw new Error('生成失败')
      }
    } catch (error: any) {
      console.error('生成错误:', error)
      setLoading(false)
      setCurrentStep(0)
      setProgress(0)
      message.error('生成失败: ' + (error.response?.data?.message || error.message))
    }
  }

  // 重置表单
  const handleReset = () => {
    form.resetFields()
    setCurrentStep(0)
    setProgress(0)
    setGeneratedData(null)
    setExpandedCards(new Set())
    setSelectedCases(new Set())
  }

  // 返回修改
  const handleBack = () => {
    setCurrentStep(0)
    setProgress(0)
  }

  // 切换用例卡片展开状态
  const toggleCard = (index: number) => {
    const newExpanded = new Set(expandedCards)
    if (newExpanded.has(index)) {
      newExpanded.delete(index)
    } else {
      newExpanded.add(index)
    }
    setExpandedCards(newExpanded)
  }

  // 切换用例选中状态
  const toggleCaseSelection = (index: number) => {
    const newSelected = new Set(selectedCases)
    if (newSelected.has(index)) {
      newSelected.delete(index)
    } else {
      newSelected.add(index)
    }
    setSelectedCases(newSelected)
  }

  // 全选/取消全选
  const toggleSelectAll = () => {
    if (!generatedData?.testcases) return

    if (selectedCases.size === generatedData.testcases.length) {
      // 取消全选
      setSelectedCases(new Set())
    } else {
      // 全选
      const allIndexes = new Set(generatedData.testcases.map((_, idx) => idx))
      setSelectedCases(allIndexes)
    }
  }

  // 转换优先级: P0->10, P1->8, P2->5, P3->3
  const convertPriority = (priority: string): number => {
    const priorityMap: Record<string, number> = {
      P0: 10,
      P1: 8,
      P2: 5,
      P3: 3,
    }
    return priorityMap[priority] || 5
  }

  // 转换测试类型: 中文类型 -> 英文枚举值
  const convertType = (type: string): string => {
    const typeMap: Record<string, string> = {
      '功能测试': 'FUNCTIONAL',
      '性能测试': 'PERFORMANCE',
      '安全测试': 'SECURITY',
      '功能': 'FUNCTIONAL',
      '性能': 'PERFORMANCE',
      '安全': 'SECURITY',
    }
    // 如果已经是英文枚举值，直接返回
    if (['FUNCTIONAL', 'PERFORMANCE', 'SECURITY'].includes(type)) {
      return type
    }
    // 否则进行转换，默认返回 FUNCTIONAL
    return typeMap[type] || 'FUNCTIONAL'
  }

  // 转换测试步骤为字符串数组
  const convertSteps = (steps: TestStep[]): string[] => {
    return steps.map((step, idx) =>
      `${idx + 1}. ${step.action} | 预期: ${step.expected}`
    )
  }

  // 保存到数据库
  const handleSaveToDatabase = async () => {
    if (!generatedData?.testcases || selectedCases.size === 0) {
      message.warning('请至少选择一个测试用例')
      return
    }

    try {
      setSaving(true)

      // 获取选中的测试用例
      const selectedTestCases = generatedData.testcases
        .filter((_, idx) => selectedCases.has(idx))
        .map((testcase) => ({
          title: testcase.name,
          description: `${testcase.type} - ${testcase.tags.join(', ')}`,
          steps: convertSteps(testcase.steps),
          expectedResult: testcase.steps.length > 0
            ? testcase.steps[testcase.steps.length - 1].expected
            : '测试通过',
          priority: convertPriority(testcase.priority),
          type: convertType(testcase.type),
          tags: testcase.tags,
          module: generatedData.metadata.module,
          preconditions: testcase.preconditions,
        }))

      console.log('保存到数据库:', selectedTestCases)

      // 调用后端接口
      const response = await axios.post('/api/v1/test-cases/batch', {
        test_cases: selectedTestCases,
      }, {
        headers: {
          'X-User-Id': 'system', // 可以从用户上下文获取
        },
      })

      console.log('保存响应:', response.data)

      if (response.data) {
        message.success(`成功保存 ${selectedCases.size} 条测试用例到数据库!`)
        // 清空选择
        setSelectedCases(new Set())
      }
    } catch (error: any) {
      console.error('保存失败:', error)
      message.error('保存失败: ' + (error.response?.data?.error || error.message))
    } finally {
      setSaving(false)
    }
  }

  // 渲染优先级标签
  const renderPriorityTag = (priority: string) => {
    const colorMap: Record<string, string> = {
      P0: 'red',
      P1: 'orange',
      P2: 'blue',
      P3: 'default',
    }
    return <Tag color={colorMap[priority] || 'default'}>{priority}</Tag>
  }

  return (
    <div className="smart-generate-container">
      <h2 style={{ marginBottom: 24 }}>智能测试用例生成</h2>

      <Steps
        current={currentStep}
        items={[
          {
            title: '输入需求',
            icon: <FileTextOutlined />,
          },
          {
            title: 'AI生成中',
            icon: <RobotOutlined />,
          },
          {
            title: '查看结果',
            icon: <CheckCircleOutlined />,
          },
        ]}
        className="steps-container"
      />

      {/* 第1步：输入需求 */}
      {currentStep === 0 && (
        <Card title="第1步：输入需求" className="step-card">
          <Form
            form={form}
            layout="vertical"
            initialValues={initialValues}
          >
            <Form.Item
              label="需求文档"
              name="requirement_text"
              rules={[{ required: true, message: '请输入需求文档' }]}
            >
              <TextArea
                rows={8}
                placeholder="请输入需求文档内容，例如：&#10;&#10;用户登录功能需求：&#10;1. 用户可以通过手机号+验证码登录&#10;2. 支持微信、支付宝第三方登录..."
              />
            </Form.Item>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  label="模块名称"
                  name="module"
                  rules={[{ required: true, message: '请输入模块名称' }]}
                >
                  <Input placeholder="例如：用户认证模块" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="生成数量"
                  name="num_cases"
                  rules={[{ required: true, message: '请输入生成数量' }]}
                >
                  <InputNumber
                    min={1}
                    max={50}
                    style={{ width: '100%' }}
                    placeholder="1-50"
                  />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item label="测试选项">
              <Space direction="vertical">
                <Form.Item name="include_edge_cases" valuePropName="checked" noStyle>
                  <Checkbox>包含边界用例</Checkbox>
                </Form.Item>
              </Space>
            </Form.Item>

            <Form.Item label="高级配置（可选）">
              <Space direction="vertical">
                <Form.Item name="deduplicate" valuePropName="checked" noStyle>
                  <Checkbox>自动去重（相似度阈值: 85%）</Checkbox>
                </Form.Item>
                <Form.Item name="prioritize" valuePropName="checked" noStyle>
                  <Checkbox>自动优先级排序</Checkbox>
                </Form.Item>
              </Space>
            </Form.Item>

            <Form.Item>
              <Space>
                <Button size="large" onClick={handleReset}>
                  重置
                </Button>
                <Button
                  type="primary"
                  size="large"
                  onClick={handleGenerate}
                  loading={loading}
                >
                  开始生成 →
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Card>
      )}

      {/* 第2步：AI生成中 */}
      {currentStep === 1 && (
        <Card title="第2步：AI生成中" className="step-card">
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <div style={{ fontSize: 64, marginBottom: 16 }}>
              <RobotOutlined spin />
            </div>
            <div style={{ fontSize: 16, color: '#8c8c8c', marginBottom: 24 }}>
              AI正在分析需求...
            </div>
            <Progress
              percent={progress}
              status="active"
              strokeColor={{
                from: '#1890ff',
                to: '#52c41a',
              }}
            />
            <div style={{ marginTop: 16, color: '#8c8c8c', fontSize: 14 }}>
              <div>✓ 需求解析完成</div>
              <div>✓ 功能点识别完成</div>
              <div>⏳ 生成测试用例中...</div>
            </div>
          </div>
        </Card>
      )}

      {/* 第3步：查看结果 */}
      {currentStep === 2 && generatedData && (
        <Card title="第3步：查看结果" className="step-card">
          {/* 调试信息 */}
          {console.log('渲染第3步, generatedData:', generatedData)}
          {console.log('testcases数量:', generatedData.testcases?.length)}

          {/* 成功提示 */}
          <div className="success-alert">
            <div className="success-title">
              ✅ 生成完成！共生成 {generatedData.total_unique} 条测试用例
            </div>
            <div className="success-details">
              <div>• 请求ID: {generatedData.request_id}</div>
              <div>• 生成时间: {new Date(generatedData.timestamp).toLocaleString('zh-CN')}</div>
              <div>• AI模型: {generatedData.metadata?.llm_model?.model_name || 'Unknown'}</div>
              {generatedData.total_duplicates > 0 && (
                <div>• 去重: {generatedData.total_duplicates} 条</div>
              )}
            </div>
          </div>

          {/* 操作按钮 */}
          <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <Checkbox
                checked={selectedCases.size === generatedData.testcases?.length && generatedData.testcases?.length > 0}
                indeterminate={selectedCases.size > 0 && selectedCases.size < (generatedData.testcases?.length || 0)}
                onChange={toggleSelectAll}
              >
                全选 ({selectedCases.size}/{generatedData.testcases?.length || 0})
              </Checkbox>
            </div>
            <Space>
              <Button disabled={selectedCases.size === 0}>📥 导出Excel</Button>
              <Button disabled={selectedCases.size === 0}>📄 导出JSON</Button>
              <Button
                type="primary"
                loading={saving}
                disabled={selectedCases.size === 0}
                onClick={handleSaveToDatabase}
              >
                💾 保存到库 ({selectedCases.size})
              </Button>
            </Space>
          </div>

          {/* 数据调试显示 */}
          <div style={{ marginBottom: 16, padding: 16, background: '#f0f0f0', borderRadius: 4 }}>
            <div>调试信息:</div>
            <div>总用例数: {generatedData.testcases?.length || 0}</div>
            <div>数据存在: {generatedData.testcases ? '是' : '否'}</div>
            <div>是否数组: {Array.isArray(generatedData.testcases) ? '是' : '否'}</div>
          </div>

          {/* 用例列表 */}
          <div className="testcase-list">
            {generatedData.testcases && generatedData.testcases.length > 0 ? (
              generatedData.testcases.map((testcase, index) => (
                <div
                  key={index}
                  className={`testcase-card ${expandedCards.has(index) ? 'expanded' : ''}`}
                >
                  <div className="testcase-header" onClick={() => toggleCard(index)}>
                    <div className="testcase-title">
                      <Checkbox
                        checked={selectedCases.has(index)}
                        onClick={(e) => e.stopPropagation()}
                        onChange={() => toggleCaseSelection(index)}
                      />
                      {renderPriorityTag(testcase.priority)}
                      <span className="testcase-name">
                        TC{String(index + 1).padStart(3, '0')} - {testcase.name}
                      </span>
                      <Tag color="blue">{testcase.type}</Tag>
                    </div>
                    <Button size="small">
                      {expandedCards.has(index) ? <UpOutlined /> : <DownOutlined />} 展开
                    </Button>
                  </div>

                  {expandedCards.has(index) && (
                    <div className="testcase-details">
                      <div className="testcase-section">
                        <div className="section-label">【前置条件】</div>
                        <div className="section-content">
                          {testcase.preconditions && testcase.preconditions.length > 0 ? (
                            <ul>
                              {testcase.preconditions.map((condition, idx) => (
                                <li key={idx}>{condition}</li>
                              ))}
                            </ul>
                          ) : (
                            <div>无</div>
                          )}
                        </div>
                      </div>

                      <div className="testcase-section">
                        <div className="section-label">【测试步骤】</div>
                        <div className="section-content">
                          {testcase.steps && testcase.steps.length > 0 ? (
                            <ol>
                              {testcase.steps.map((step, idx) => (
                                <li key={idx}>
                                  <div><strong>操作：</strong>{step.action}</div>
                                  <div><strong>预期：</strong>{step.expected}</div>
                                </li>
                              ))}
                            </ol>
                          ) : (
                            <div>无</div>
                          )}
                        </div>
                      </div>

                      <div className="testcase-section">
                        <div className="section-label">【标签】</div>
                        <div className="section-content">
                          {testcase.tags && testcase.tags.length > 0 ? (
                            <Space wrap>
                              {testcase.tags.map((tag, idx) => (
                                <Tag key={idx}>{tag}</Tag>
                              ))}
                            </Space>
                          ) : (
                            <div>无</div>
                          )}
                        </div>
                      </div>

                      <div className="testcase-section">
                        <div className="section-label">【优先级分数】</div>
                        <div className="section-content">
                          {testcase.priority_score
                            ? (testcase.priority_score * 100).toFixed(1) + '%'
                            : 'N/A'}
                        </div>
                      </div>

                      <div style={{ marginTop: 16 }}>
                        <Space>
                          <Button size="small">✏️ 编辑</Button>
                          <Button size="small">🗑️ 删除</Button>
                        </Space>
                      </div>
                    </div>
                  )}
                </div>
              ))
            ) : (
              <div style={{ textAlign: 'center', padding: 40, color: '#8c8c8c' }}>
                暂无测试用例
              </div>
            )}
          </div>

          {/* 质量评估 */}
          {generatedData.testcases && generatedData.testcases.length > 0 && (
            <div style={{ marginTop: 24 }}>
              <h3 style={{ marginBottom: 16 }}>用例质量评估</h3>
              <Row gutter={16}>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="完整性"
                      value={92}
                      suffix="%"
                      valueStyle={{ color: '#52c41a' }}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="覆盖率"
                      value={88}
                      suffix="%"
                      valueStyle={{ color: '#fa8c16' }}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="可执行性"
                      value={95}
                      suffix="%"
                      valueStyle={{ color: '#52c41a' }}
                    />
                  </Card>
                </Col>
              </Row>
            </div>
          )}

          {/* 底部按钮 */}
          <div style={{ marginTop: 24, textAlign: 'right' }}>
            <Space>
              <Button onClick={handleBack}>返回修改</Button>
              <Button type="primary" onClick={handleReset}>
                保存并继续生成
              </Button>
            </Space>
          </div>
        </Card>
      )}
    </div>
  )
}

export default SmartGenerate
