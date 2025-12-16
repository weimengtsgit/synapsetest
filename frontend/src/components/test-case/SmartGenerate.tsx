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
  Rate,
  Select,
} from 'antd'
import {
  RobotOutlined,
  CheckCircleOutlined,
  FileTextOutlined,
  DownOutlined,
  UpOutlined,
  StarOutlined,
} from '@ant-design/icons'
import axios from 'axios'
import * as XLSX from 'xlsx'
import testCaseService from '../../services/testCaseService'
import { getPriorityColor, convertPriorityToLabel } from '../../utils/priorityUtils'
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
  total_after_optimization?: number  // 优化后的数量
  total_filtered?: number            // 被过滤掉的数量
  testcases: TestCase[]
  request_id: string
  timestamp: string
  optimized?: boolean
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

  // 反馈相关状态
  const [feedbackRating, setFeedbackRating] = useState(0)
  const [feedbackComments, setFeedbackComments] = useState('')
  const [feedbackSubmitted, setFeedbackSubmitted] = useState(false)
  const [submittingFeedback, setSubmittingFeedback] = useState(false)

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
    min_priority: 'P1',
    max_cases: 20,
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
          min_priority: values.min_priority || 'P1',
          max_cases: values.max_cases || 20,
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
    // 重置反馈状态
    setFeedbackRating(0)
    setFeedbackComments('')
    setFeedbackSubmitted(false)
  }

  // 提交反馈
  const handleSubmitFeedback = async () => {
    if (feedbackRating === 0) {
      message.warning('请选择评分')
      return
    }

    if (!generatedData?.request_id) {
      message.error('无法提交反馈：缺少请求ID')
      return
    }

    try {
      setSubmittingFeedback(true)

      // 获取选中和未选中的用例名称
      const acceptedCases: string[] = []
      const rejectedCases: string[] = []

      generatedData.testcases.forEach((testcase, index) => {
        if (selectedCases.has(index)) {
          acceptedCases.push(testcase.name)
        } else {
          rejectedCases.push(testcase.name)
        }
      })

      const feedbackData = {
        request_id: generatedData.request_id,
        rating: feedbackRating,
        comments: feedbackComments || undefined,
        accepted_cases: acceptedCases.length > 0 ? acceptedCases : undefined,
        rejected_cases: rejectedCases.length > 0 ? rejectedCases : undefined,
      }

      console.log('提交反馈:', feedbackData)

      const response = await testCaseService.submitFeedback(feedbackData)

      console.log('反馈响应:', response)

      if (response.success || response.data) {
        setFeedbackSubmitted(true)
        message.success('反馈提交成功！感谢您的宝贵意见')
      } else {
        throw new Error('提交失败')
      }
    } catch (error: any) {
      console.error('提交反馈失败:', error)
      message.error('提交反馈失败: ' + (error.response?.data?.message || error.message))
    } finally {
      setSubmittingFeedback(false)
    }
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

  // 转换测试步骤为JSON对象字符串数组
  // 每个步骤转换为JSON字符串，后端的StepsTypeHandler会将其组合成JSON数组
  // 最终数据库存储格式: [{"step": 1, "action": "...", "expected": "..."}, ...]
  const convertSteps = (steps: TestStep[]): string[] => {
    return steps.map(step => JSON.stringify(step))
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
          steps: convertSteps(testcase.steps),  // JSON字符串格式
          expectedResult: testcase.steps.length > 0
            ? testcase.steps[testcase.steps.length - 1].expected
            : '测试通过',
          priority: testcase.priority,  // 保持字符串格式 (P0, P1, P2, P3)
          type: convertType(testcase.type),
          tags: testcase.tags,
          module: generatedData.metadata.module,
          preconditions: testcase.preconditions,  // JSON数组格式
          quality_score: testcase.priority_score || 0.0,  // AI质量评分
          ai_generated: true,  // 标记为AI生成
          ai_confidence: testcase.priority_score || 0.85,  // AI置信度，使用priority_score作为置信度
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
        const savedCases = response.data.saved_cases || []
        const caseNumbers = savedCases.map((c: any) => c.case_number).join(', ')
        message.success(
          `成功保存 ${selectedCases.size} 条测试用例到数据库！\n` +
          `用例编号: ${caseNumbers}`
        )
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

  // 导出为 Excel
  const handleExportExcel = () => {
    if (!generatedData?.testcases || selectedCases.size === 0) {
      message.warning('请至少选择一个测试用例')
      return
    }

    try {
      // 获取选中的测试用例
      const selectedTestCases = generatedData.testcases
        .filter((_, idx) => selectedCases.has(idx))

      // 准备 Excel 数据
      const excelData = selectedTestCases.map((testcase, index) => ({
        '序号': index + 1,
        '用例名称': testcase.name,
        '优先级': testcase.priority,
        '类型': testcase.type,
        '前置条件': testcase.preconditions.join('; '),
        '测试步骤': testcase.steps.map(s => `${s.step}. ${s.action}`).join('\n'),
        '预期结果': testcase.steps.map(s => `${s.step}. ${s.expected}`).join('\n'),
        '标签': testcase.tags.join(', '),
        '优先级分数': testcase.priority_score,
        '排名': testcase.rank || index + 1,
      }))

      // 创建工作表
      const worksheet = XLSX.utils.json_to_sheet(excelData)
      
      // 设置列宽
      const colWidths = [
        { wch: 6 },  // 序号
        { wch: 30 }, // 用例名称
        { wch: 10 }, // 优先级
        { wch: 12 }, // 类型
        { wch: 30 }, // 前置条件
        { wch: 40 }, // 测试步骤
        { wch: 40 }, // 预期结果
        { wch: 20 }, // 标签
        { wch: 12 }, // 优先级分数
        { wch: 8 },  // 排名
      ]
      worksheet['!cols'] = colWidths

      // 创建工作簿
      const workbook = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(workbook, worksheet, '测试用例')

      // 生成文件名
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19)
      const fileName = `测试用例_${generatedData.metadata.module}_${timestamp}.xlsx`

      // 导出文件
      XLSX.writeFile(workbook, fileName)

      message.success(`成功导出 ${selectedCases.size} 条测试用例到 Excel!`)
    } catch (error: any) {
      console.error('导出 Excel 失败:', error)
      message.error('导出失败: ' + error.message)
    }
  }

  // 导出为 JSON
  const handleExportJSON = () => {
    if (!generatedData?.testcases || selectedCases.size === 0) {
      message.warning('请至少选择一个测试用例')
      return
    }

    try {
      // 获取选中的测试用例
      const selectedTestCases = generatedData.testcases
        .filter((_, idx) => selectedCases.has(idx))

      // 准备导出数据
      const exportData = {
        metadata: {
          export_time: new Date().toISOString(),
          module: generatedData.metadata.module,
          total_cases: selectedTestCases.length,
          llm_model: generatedData.metadata.llm_model,
        },
        testcases: selectedTestCases.map((testcase, index) => ({
          id: index + 1,
          name: testcase.name,
          priority: testcase.priority,
          type: testcase.type,
          preconditions: testcase.preconditions,
          steps: testcase.steps,
          tags: testcase.tags,
          priority_score: testcase.priority_score,
          rank: testcase.rank || index + 1,
        })),
      }

      // 转换为 JSON 字符串
      const jsonString = JSON.stringify(exportData, null, 2)

      // 创建 Blob 对象
      const blob = new Blob([jsonString], { type: 'application/json' })

      // 创建下载链接
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url

      // 生成文件名
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19)
      link.download = `测试用例_${generatedData.metadata.module}_${timestamp}.json`

      // 触发下载
      document.body.appendChild(link)
      link.click()

      // 清理
      document.body.removeChild(link)
      URL.revokeObjectURL(url)

      message.success(`成功导出 ${selectedCases.size} 条测试用例到 JSON!`)
    } catch (error: any) {
      console.error('导出 JSON 失败:', error)
      message.error('导出失败: ' + error.message)
    }
  }

  // 渲染优先级标签 - 统一转换：支持数字（0-10）和字符串（P0-P3）两种格式
  const renderPriorityTag = (priority: string | number) => {
    const label = typeof priority === 'number' 
      ? convertPriorityToLabel(priority) 
      : priority
    return <Tag color={getPriorityColor(priority)}>{label}</Tag>
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
              <Space direction="vertical" style={{ width: '100%' }}>
                <Form.Item name="deduplicate" valuePropName="checked" noStyle>
                  <Checkbox>自动去重（相似度阈值: 85%）</Checkbox>
                </Form.Item>
                <Form.Item name="prioritize" valuePropName="checked" noStyle>
                  <Checkbox>自动优先级排序</Checkbox>
                </Form.Item>

                <Row gutter={16} style={{ width: '100%', marginTop: 8 }}>
                  <Col span={12}>
                    <Form.Item
                      name="min_priority"
                      label="最低优先级"
                      tooltip="只保留此优先级及以上的用例"
                      style={{ marginBottom: 0 }}
                    >
                      <Select placeholder="选择最低优先级">
                        <Select.Option value="P0">P0（最高）</Select.Option>
                        <Select.Option value="P1">P1（高）</Select.Option>
                        <Select.Option value="P2">P2（中）</Select.Option>
                        <Select.Option value="P3">P3（低）</Select.Option>
                      </Select>
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item
                      name="max_cases"
                      label="最大用例数"
                      tooltip="优化后返回的最大用例数量"
                      style={{ marginBottom: 0 }}
                    >
                      <InputNumber
                        min={1}
                        max={100}
                        style={{ width: '100%' }}
                        placeholder="默认20"
                      />
                    </Form.Item>
                  </Col>
                </Row>
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
          {/* 成功提示 */}
          <div className="success-alert">
            <div className="success-title">
              ✅ 生成完成！处理流程如下
            </div>
            <div className="success-details">
              <div>• 初始生成: {generatedData.total_generated} 条测试用例</div>
              {generatedData.total_duplicates > 0 && (
                <div>• 去重后: {generatedData.total_unique} 条（去除 {generatedData.total_duplicates} 条重复）</div>
              )}
              {generatedData.optimized && generatedData.total_filtered !== undefined && generatedData.total_filtered > 0 && (
                <div>• 优先级过滤后: {generatedData.total_after_optimization} 条（过滤 {generatedData.total_filtered} 条低优先级用例）</div>
              )}
              <div>• 最终展示: {generatedData.testcases?.length} 条</div>
              <div style={{ marginTop: '8px', paddingTop: '8px', borderTop: '1px solid #d9d9d9' }}>
                <div>• 请求ID: {generatedData.request_id}</div>
                <div>• 生成时间: {new Date(generatedData.timestamp).toLocaleString('zh-CN')}</div>
                <div>• AI模型: {generatedData.metadata?.llm_model?.model_name || 'Unknown'}</div>
              </div>
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
              <Button
                disabled={selectedCases.size === 0}
                onClick={handleExportExcel}
              >
                📥 导出Excel
              </Button>
              <Button
                disabled={selectedCases.size === 0}
                onClick={handleExportJSON}
              >
                📄 导出JSON
              </Button>
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
                                  执行：{step.action}。预期结果：{step.expected}。
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

          {/* 反馈 */}
          {generatedData.testcases && generatedData.testcases.length > 0 && (
            <div style={{ marginTop: 24 }}>
              <Card
                title={
                  <Space>
                    <StarOutlined style={{ color: '#faad14' }} />
                    <span>反馈</span>
                    {feedbackSubmitted && (
                      <Tag color="success" icon={<CheckCircleOutlined />}>
                        已提交
                      </Tag>
                    )}
                  </Space>
                }
                style={{ background: '#fafafa' }}
              >
                {!feedbackSubmitted ? (
                  <>
                    <div style={{ marginBottom: 16 }}>
                      <div style={{ marginBottom: 8, fontSize: 14 }}>
                        这次生成的用例质量如何？
                      </div>
                      <Rate
                        value={feedbackRating}
                        onChange={setFeedbackRating}
                        style={{ fontSize: 28 }}
                        character={<StarOutlined />}
                      />
                      {feedbackRating > 0 && (
                        <span style={{ marginLeft: 16, color: '#8c8c8c' }}>
                          {feedbackRating === 5 && '非常满意'}
                          {feedbackRating === 4 && '满意'}
                          {feedbackRating === 3 && '一般'}
                          {feedbackRating === 2 && '不满意'}
                          {feedbackRating === 1 && '非常不满意'}
                        </span>
                      )}
                    </div>
                    <div style={{ marginBottom: 16 }}>
                      <TextArea
                        rows={3}
                        placeholder="可选填写评论，帮助我们改进AI生成质量..."
                        value={feedbackComments}
                        onChange={(e) => setFeedbackComments(e.target.value)}
                        maxLength={500}
                        showCount
                      />
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <Button
                        type="primary"
                        icon={<StarOutlined />}
                        onClick={handleSubmitFeedback}
                        loading={submittingFeedback}
                        disabled={feedbackRating === 0}
                      >
                        提交反馈
                      </Button>
                    </div>
                  </>
                ) : (
                  <div style={{ textAlign: 'center', padding: '20px 0' }}>
                    <CheckCircleOutlined style={{ fontSize: 48, color: '#52c41a', marginBottom: 16 }} />
                    <div style={{ fontSize: 16, color: '#52c41a' }}>
                      感谢您的反馈！您的评价将帮助我们改进AI生成质量
                    </div>
                    <div style={{ marginTop: 16, color: '#8c8c8c' }}>
                      评分: <Rate disabled value={feedbackRating} style={{ fontSize: 16 }} />
                    </div>
                    {feedbackComments && (
                      <div style={{ marginTop: 8, color: '#8c8c8c', fontSize: 14 }}>
                        评论: {feedbackComments}
                      </div>
                    )}
                  </div>
                )}
              </Card>
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
