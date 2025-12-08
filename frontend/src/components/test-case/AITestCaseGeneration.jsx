import React, { useState } from 'react'
import {
  Form,
  Input,
  Select,
  Button,
  Card,
  message,
  Spin,
  List,
  Tag,
  Space,
  Divider,
  Alert,
  Row,
  Col,
  Collapse,
} from 'antd'
import {
  RobotOutlined,
  CheckCircleOutlined,
  SaveOutlined,
  EditOutlined,
} from '@ant-design/icons'
import testCaseService from '../../services/testCaseService'

const { TextArea } = Input
const { Option } = Select
const { Panel } = Collapse

/**
 * AI Test Case Generation Component
 * User Story 2: AI生成测试用例 - AI测试用例生成页面
 *
 * Task: T055 [P] [US2] Create frontend components for AI测试用例生成页面
 */
const AITestCaseGeneration = () => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [generatedCases, setGeneratedCases] = useState([])
  const [confidenceScore, setConfidenceScore] = useState(null)
  const [selectedCases, setSelectedCases] = useState([])

  const handleGenerate = async (values) => {
    setLoading(true)
    try {
      const response = await testCaseService.generateTestCases(values)

      if (response.success) {
        const { testCases, confidenceScore: score, count } = response.data

        setGeneratedCases(testCases)
        setConfidenceScore(score)

        message.success(`成功生成 ${count} 个测试用例！`)
      } else {
        message.error(response.message || 'Failed to generate test cases')
      }
    } catch (error) {
      message.error(error.message || 'Failed to generate test cases')
    } finally {
      setLoading(false)
    }
  }

  const handleSaveTestCase = async (testCase) => {
    try {
      const response = await testCaseService.createTestCase(testCase)

      if (response.success) {
        message.success('测试用例已保存')
        // Remove saved case from generated list
        setGeneratedCases(generatedCases.filter((tc) => tc !== testCase))
      } else {
        message.error('保存失败')
      }
    } catch (error) {
      message.error('保存失败: ' + error.message)
    }
  }

  const handleSaveAll = async () => {
    if (generatedCases.length === 0) {
      message.warning('没有待保存的测试用例')
      return
    }

    setLoading(true)
    let successCount = 0
    let failCount = 0

    for (const testCase of generatedCases) {
      try {
        await testCaseService.createTestCase(testCase)
        successCount++
      } catch (error) {
        failCount++
      }
    }

    setLoading(false)

    if (failCount === 0) {
      message.success(`成功保存 ${successCount} 个测试用例`)
      setGeneratedCases([])
    } else {
      message.warning(`成功: ${successCount}, 失败: ${failCount}`)
    }
  }

  const getPriorityColor = (priority) => {
    if (priority >= 8) return 'red'
    if (priority >= 5) return 'orange'
    return 'blue'
  }

  const getTypeColor = (type) => {
    const colors = {
      FUNCTIONAL: 'blue',
      PERFORMANCE: 'orange',
      SECURITY: 'red',
    }
    return colors[type] || 'default'
  }

  return (
    <div style={{ padding: '24px' }}>
      <Row gutter={24}>
        {/* Input Form */}
        <Col span={10}>
          <Card
            title={
              <Space>
                <RobotOutlined />
                <span>AI 测试用例生成器</span>
              </Space>
            }
            bordered={false}
          >
            <Form form={form} layout="vertical" onFinish={handleGenerate}>
              <Form.Item
                label="需求描述 (Requirement Description)"
                name="input"
                rules={[{ required: true, message: '请输入需求描述' }]}
                tooltip="输入产品需求、用户故事或功能描述，AI将自动生成测试用例"
              >
                <TextArea
                  rows={10}
                  placeholder={`示例输入：

用户登录功能：
- 用户可以使用邮箱和密码登录
- 支持记住密码功能
- 密码输入错误3次后锁定账户30分钟
- 登录成功后跳转到首页

或者输入：
"实现一个搜索功能，用户可以输入关键词搜索产品，支持模糊匹配和过滤条件"`}
                  style={{ fontFamily: 'monospace' }}
                />
              </Form.Item>

              <Form.Item label="测试类型 (Test Type)" name="testType">
                <Select placeholder="选择测试类型（可选）">
                  <Option value="FUNCTIONAL">功能测试 (Functional)</Option>
                  <Option value="PERFORMANCE">性能测试 (Performance)</Option>
                  <Option value="SECURITY">安全测试 (Security)</Option>
                </Select>
              </Form.Item>

              <Form.Item label="标签 (Tags)" name="tags">
                <Select mode="tags" placeholder="添加标签（可选）" />
              </Form.Item>

              <Form.Item label="关联需求 (Related Requirement)" name="relatedRequirement">
                <Input placeholder="输入需求ID（可选）" />
              </Form.Item>

              <Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  icon={<RobotOutlined />}
                  size="large"
                  block
                >
                  {loading ? 'AI 生成中...' : '生成测试用例'}
                </Button>
              </Form.Item>
            </Form>

            {confidenceScore !== null && (
              <Alert
                message={`AI 置信度: ${(confidenceScore * 100).toFixed(1)}%`}
                description="基于输入质量和历史数据计算的生成置信度"
                type={confidenceScore >= 0.8 ? 'success' : 'info'}
                showIcon
                style={{ marginTop: 16 }}
              />
            )}
          </Card>
        </Col>

        {/* Generated Test Cases */}
        <Col span={14}>
          <Card
            title={`生成的测试用例 (${generatedCases.length})`}
            bordered={false}
            extra={
              generatedCases.length > 0 && (
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  onClick={handleSaveAll}
                  loading={loading}
                >
                  保存全部
                </Button>
              )
            }
          >
            {loading && (
              <div style={{ textAlign: 'center', padding: '50px' }}>
                <Spin size="large" tip="AI 正在分析需求并生成测试用例..." />
              </div>
            )}

            {!loading && generatedCases.length === 0 && (
              <div style={{ textAlign: 'center', padding: '50px', color: '#999' }}>
                <RobotOutlined style={{ fontSize: 48, marginBottom: 16 }} />
                <p>还没有生成测试用例</p>
                <p>在左侧输入需求描述，点击"生成测试用例"按钮</p>
              </div>
            )}

            {!loading && generatedCases.length > 0 && (
              <List
                dataSource={generatedCases}
                renderItem={(testCase, index) => (
                  <List.Item
                    key={index}
                    style={{
                      border: '1px solid #f0f0f0',
                      marginBottom: 16,
                      padding: 16,
                      borderRadius: 8,
                      background: '#fafafa',
                    }}
                  >
                    <div style={{ width: '100%' }}>
                      {/* Header */}
                      <div style={{ marginBottom: 12 }}>
                        <Space>
                          <Tag color={getTypeColor(testCase.type)}>{testCase.type}</Tag>
                          <Tag color={getPriorityColor(testCase.priority)}>
                            P{testCase.priority}
                          </Tag>
                          {testCase.tags &&
                            testCase.tags.map((tag) => <Tag key={tag}>{tag}</Tag>)}
                        </Space>
                      </div>

                      {/* Title */}
                      <h3 style={{ margin: '8px 0' }}>{testCase.title}</h3>

                      {/* Description */}
                      {testCase.description && (
                        <p style={{ color: '#666', margin: '8px 0' }}>
                          {testCase.description}
                        </p>
                      )}

                      <Divider />

                      {/* Test Steps */}
                      <Collapse ghost>
                        <Panel header={`测试步骤 (${testCase.steps?.length || 0} 步)`} key="1">
                          <ol style={{ paddingLeft: 20 }}>
                            {testCase.steps?.map((step, idx) => (
                              <li key={idx} style={{ marginBottom: 8 }}>
                                {step}
                              </li>
                            ))}
                          </ol>
                        </Panel>
                      </Collapse>

                      {/* Expected Results */}
                      {testCase.expectedResults && (
                        <>
                          <Divider />
                          <div>
                            <strong>预期结果:</strong>
                            <p style={{ marginTop: 8, color: '#666' }}>
                              {testCase.expectedResults}
                            </p>
                          </div>
                        </>
                      )}

                      {/* Actions */}
                      <Divider />
                      <Space>
                        <Button
                          type="primary"
                          icon={<SaveOutlined />}
                          onClick={() => handleSaveTestCase(testCase)}
                        >
                          保存
                        </Button>
                        <Button icon={<EditOutlined />}>编辑</Button>
                      </Space>
                    </div>
                  </List.Item>
                )}
              />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default AITestCaseGeneration
