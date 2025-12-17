import React, { useState, useEffect, useRef } from 'react'
import { Form, Input, Select, Button, Card, message, Alert, Table, Tag, Divider } from 'antd'
import { EyeOutlined, BulbOutlined } from '@ant-design/icons'
import { useLocation, Link } from 'react-router-dom'
import testTaskService from '../../services/testTaskService'

const { Option } = Select
const { TextArea } = Input

/**
 * Create Test Task Component
 * User Story 1: 智能测试任务调度 - 创建测试任务页面
 *
 * Task: T040 [P] [US1] Create frontend components for 测试任务创建页面
 * Enhanced with test case preview functionality
 */
const CreateTestTask = () => {
  const [form] = Form.useForm()
  const location = useLocation()
  const [loading, setLoading] = useState(false)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [environments, setEnvironments] = useState([])
  const [versions, setVersions] = useState([])
  const [modules, setModules] = useState([])
  const [recommendation, setRecommendation] = useState(null)
  const [matchedTestCases, setMatchedTestCases] = useState([])
  const [fromRecommendation, setFromRecommendation] = useState(false)
  const hasFetchedData = useRef(false)

  useEffect(() => {
    // Prevent duplicate API calls in React StrictMode
    if (!hasFetchedData.current) {
      hasFetchedData.current = true
      loadInitialData()
    }
  }, [])

  // Handle data from Strategy Recommendation page
  useEffect(() => {
    if (location.state?.prefillData) {
      const prefillData = location.state.prefillData
      const aiRecommendation = location.state.recommendation

      // Set form values
      form.setFieldsValue({
        taskName: prefillData.taskName,
        environment: prefillData.environment,
        version: prefillData.version,
        modules: prefillData.modules,
        changedFilesCount: prefillData.changedFilesCount,
        changedLinesCount: prefillData.changedLinesCount,
        isHotfix: prefillData.isHotfix,
        isCriticalModule: prefillData.isCriticalModule,
      })

      // Set AI recommendation if available
      if (aiRecommendation) {
        setRecommendation(aiRecommendation)
        setFromRecommendation(true)
        message.success('已应用AI推荐的配置！')
      }

      // Clear location state to prevent re-applying on refresh
      window.history.replaceState({}, document.title)
    }
  }, [location, form])

  const loadInitialData = async () => {
    try {
      const [envResponse, versionResponse, modulesResponse] = await Promise.all([
        testTaskService.getEnvironments(),
        testTaskService.getVersions(),
        testTaskService.getModules(),
      ])

      setEnvironments(envResponse || [])
      setVersions(versionResponse || [])
      
      // Handle modules response - it returns {success: true, data: [...]}
      if (modulesResponse && modulesResponse.success && modulesResponse.data) {
        setModules(modulesResponse.data)
      } else if (Array.isArray(modulesResponse)) {
        setModules(modulesResponse)
      } else {
        setModules([])
      }
    } catch (error) {
      message.error('Failed to load initial data')
      console.error('Error loading initial data:', error)
    }
  }

  const handlePreviewTestCases = async () => {
    try {
      // Validate required fields
      await form.validateFields(['taskName', 'environment', 'version', 'modules'])
      
      const values = form.getFieldsValue()
      
      // Transform data to match backend expectations
      const requestData = {
        taskName: values.taskName,
        environment: values.environment,
        version: values.version,
        modules: values.modules || [],
        codeChangeInfo: {
          changed_files_count: values.changedFilesCount || 0,
          changed_lines_count: values.changedLinesCount || 0,
          is_hotfix: values.isHotfix || false,
          is_critical_module: values.isCriticalModule || false,
        }
      }
      
      setPreviewLoading(true)
      const response = await testTaskService.previewTestCases(requestData)
      
      if (response && response.success) {
        setMatchedTestCases(response.data || [])
        message.success(`Matched ${response.data?.length || 0} test cases`)
      } else {
        message.warning('No test cases matched')
        setMatchedTestCases([])
      }
    } catch (error) {
      if (error.errorFields) {
        message.error('Please fill in all required fields first!')
      } else {
        message.error(error.message || 'Failed to preview test cases')
        console.error('Error previewing test cases:', error)
      }
    } finally {
      setPreviewLoading(false)
    }
  }

  const handleSubmit = async (values) => {
    setLoading(true)
    try {
      // Transform frontend data to match backend expectations
      const requestData = {
        taskName: values.taskName,
        environment: values.environment,
        version: values.version,
        modules: values.modules || [],
        codeChangeInfo: {
          changed_files_count: values.changedFilesCount || 0,
          changed_lines_count: values.changedLinesCount || 0,
          is_hotfix: values.isHotfix || false,
          is_critical_module: values.isCriticalModule || false,
        }
      }

      const response = await testTaskService.createTestTask(requestData)

      if (response.success) {
        message.success(response.message || 'Test task created successfully!')

        // Display AI recommendation if available
        if (response.data && response.data.recommendation) {
          setRecommendation(response.data.recommendation)
        }
        
        // Show associated test cases count
        if (response.data && response.data.totalTestCases) {
          message.info(`Task created with ${response.data.totalTestCases} test cases`)
        }

        // Reset form and preview
        form.resetFields()
        setMatchedTestCases([])
      } else {
        message.error(response?.message || 'Failed to create test task')
      }
    } catch (error) {
      message.error(error.message || 'Failed to create test task')
    } finally {
      setLoading(false)
    }
  }

  const testCaseColumns = [
    {
      title: '用例编号',
      dataIndex: 'caseNumber',
      key: 'caseNumber',
      width: 120,
    },
    {
      title: '用例标题',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
      width: 120,
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
      render: (priority) => {
        const color = priority >= 8 ? 'red' : priority >= 5 ? 'orange' : 'blue'
        return <Tag color={color}>P{priority}</Tag>
      },
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => {
        const color = status === 'APPROVED' ? 'green' : status === 'DRAFT' ? 'orange' : 'default'
        return <Tag color={color}>{status}</Tag>
      },
    },
  ]

  return (
    <div style={{ padding: '24px' }}>
      {/* Navigation hint to Strategy Recommendation */}
      {!fromRecommendation && (
        <Alert
          type="info"
          closable
          icon={<BulbOutlined />}
          message="提示：需要详细的测试策略分析和风险评估？"
          description={
            <>
              如果您不确定该使用什么测试策略，可以先使用
              <Link to="/recommendation/strategy" style={{ marginLeft: 8, fontWeight: 'bold' }}>
                策略推荐功能 →
              </Link>
              获取AI的详细分析和建议
            </>
          }
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Show recommendation applied message */}
      {fromRecommendation && recommendation && (
        <Alert
          type="success"
          closable
          message="已应用AI推荐配置"
          description={
            <div>
              <p><strong>推荐范围:</strong> {recommendation.testScope}</p>
              <p><strong>推荐环境:</strong> {recommendation.environment}</p>
              <p><strong>置信度:</strong> {(recommendation.confidenceScore || recommendation.confidence || 0.85) * 100}%</p>
            </div>
          }
          style={{ marginBottom: 16 }}
        />
      )}

      <Card title="创建测试任务 (Create Test Task)" bordered={false}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
            changedFilesCount: 0,
            changedLinesCount: 0,
            isHotfix: false,
            isCriticalModule: false
          }}
        >
          <Form.Item
            label="任务名称 (Task Name)"
            name="taskName"
            rules={[
              { required: true, message: 'Please input task name!' },
              { max: 100, message: 'Task name must not exceed 100 characters' },
            ]}
          >
            <Input placeholder="Enter test task name" />
          </Form.Item>

          <Form.Item label="任务描述 (Description)" name="description">
            <TextArea rows={4} placeholder="Enter test task description" />
          </Form.Item>

          <Form.Item
            label="测试环境 (Environment)"
            name="environment"
            rules={[{ required: true, message: 'Please select environment!' }]}
          >
            <Select placeholder="Select test environment">
              {environments.map((env) => (
                <Option key={env.id} value={env.name}>
                  {env.name}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="测试版本 (Version)"
            name="version"
            rules={[{ required: true, message: 'Please select version!' }]}
          >
            <Select placeholder="Select test version">
              {versions.map((version) => (
                <Option key={version.id} value={version.name}>
                  {version.name} ({version.productVersion})
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="测试模块 (Modules)"
            name="modules"
            rules={[{ required: true, message: 'Please select at least one module!' }]}
            extra={
              <Button 
                type="link" 
                icon={<EyeOutlined />}
                onClick={handlePreviewTestCases}
                loading={previewLoading}
                style={{ padding: '4px 0', marginTop: '4px' }}
              >
                预览匹配的测试用例 (Preview Test Cases)
              </Button>
            }
          >
            <Select mode="multiple" placeholder="Select modules to test">
              {modules.map((module) => (
                <Option key={module} value={module}>
                  {module}
                </Option>
              ))}
            </Select>
          </Form.Item>

          {/* Test Cases Preview */}
          {matchedTestCases.length > 0 && (
            <Card 
              title={`匹配的测试用例 (Matched Test Cases: ${matchedTestCases.length})`}
              size="small"
              style={{ marginBottom: 16 }}
              type="inner"
            >
              <Table
                dataSource={matchedTestCases}
                columns={testCaseColumns}
                rowKey="id"
                size="small"
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                  showTotal: (total) => `Total ${total} cases`,
                }}
                scroll={{ x: 800 }}
              />
            </Card>
          )}

          <Card title="代码变更信息 (Code Change Info)" size="small" style={{ marginBottom: 16 }}>
            <Form.Item
              label="变更文件数 (Changed Files Count)"
              name="changedFilesCount"
              rules={[{ required: true, message: 'Please input changed files count!' }]}
            >
              <Input type="number" min={0} style={{ width: '100%' }} placeholder="Number of files changed" />
            </Form.Item>

            <Form.Item
              label="变更行数 (Changed Lines Count)"
              name="changedLinesCount"
              rules={[{ required: true, message: 'Please input changed lines count!' }]}
            >
              <Input type="number" min={0} style={{ width: '100%' }} placeholder="Number of lines changed" />
            </Form.Item>

            <Form.Item
              label="是否热修复 (Is Hotfix)"
              name="isHotfix"
            >
              <Select>
                <Option value={false}>否 (No)</Option>
                <Option value={true}>是 (Yes)</Option>
              </Select>
            </Form.Item>

            <Form.Item
              label="是否核心模块 (Is Critical Module)"
              name="isCriticalModule"
            >
              <Select>
                <Option value={false}>否 (No)</Option>
                <Option value={true}>是 (Yes)</Option>
              </Select>
            </Form.Item>
          </Card>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} size="large">
              创建任务 (Create Task)
            </Button>
          </Form.Item>
        </Form>

        {recommendation && (
          <Alert
            message="AI推荐 (AI Recommendation)"
            description={
              <div>
                <p>
                  <strong>推荐环境:</strong> {recommendation.recommendedEnvironment}
                </p>
                <p>
                  <strong>推荐版本:</strong> {recommendation.recommendedVersion}
                </p>
                <p>
                  <strong>推荐范围:</strong> {recommendation.recommendedScope}
                </p>
                <p>
                  <strong>置信度:</strong> {(recommendation.confidenceScore * 100).toFixed(1)}%
                </p>
                <p>
                  <strong>推理:</strong> <pre>{recommendation.reasoning}</pre>
                </p>
              </div>
            }
            type="info"
            showIcon
            style={{ marginTop: 24 }}
          />
        )}
      </Card>
    </div>
  )
}

export default CreateTestTask
