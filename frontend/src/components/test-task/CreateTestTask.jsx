import React, { useState, useEffect } from 'react'
import { Form, Input, Select, Button, Card, message, Alert, Spin, InputNumber } from 'antd'
import testTaskService from '../../services/testTaskService'

const { Option } = Select
const { TextArea } = Input

/**
 * Create Test Task Component
 * User Story 1: 智能测试任务调度 - 创建测试任务页面
 *
 * Task: T040 [P] [US1] Create frontend components for 测试任务创建页面
 */
const CreateTestTask = () => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [environments, setEnvironments] = useState([])
  const [versions, setVersions] = useState([])
  const [recommendation, setRecommendation] = useState(null)

  useEffect(() => {
    loadInitialData()
  }, [])

  const loadInitialData = async () => {
    try {
      const [envResponse, versionResponse] = await Promise.all([
        testTaskService.getEnvironments(),
        testTaskService.getVersions(),
      ])

      setEnvironments(envResponse || [])
      setVersions(versionResponse || [])
    } catch (error) {
      message.error('Failed to load initial data')
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
        message.success('Test task created successfully!')

        // Display AI recommendation if available
        if (response.data.recommendation) {
          setRecommendation(response.data.recommendation)
        }

        // Reset form
        form.resetFields()
      } else {
        message.error(response.message || 'Failed to create test task')
      }
    } catch (error) {
      message.error(error.message || 'Failed to create test task')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ padding: '24px' }}>
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
              <Option value="DEV">开发环境 (DEV)</Option>
              <Option value="STAGING">预发环境 (STAGING)</Option>
              <Option value="PROD">生产环境 (PROD)</Option>
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
          >
            <Select mode="multiple" placeholder="Select modules to test">
              <Option value="user-management">用户管理 (User Management)</Option>
              <Option value="product-catalog">产品目录 (Product Catalog)</Option>
              <Option value="order-processing">订单处理 (Order Processing)</Option>
              <Option value="payment-gateway">支付网关 (Payment Gateway)</Option>
              <Option value="inventory-management">库存管理 (Inventory Management)</Option>
              <Option value="reporting">报表系统 (Reporting)</Option>
            </Select>
          </Form.Item>

          <Card title="代码变更信息 (Code Change Info)" size="small" style={{ marginBottom: 16 }}>
            <Form.Item
              label="变更文件数 (Changed Files Count)"
              name="changedFilesCount"
              rules={[{ required: true, message: 'Please input changed files count!' }]}
            >
              <InputNumber min={0} style={{ width: '100%' }} placeholder="Number of files changed" />
            </Form.Item>

            <Form.Item
              label="变更行数 (Changed Lines Count)"
              name="changedLinesCount"
              rules={[{ required: true, message: 'Please input changed lines count!' }]}
            >
              <InputNumber min={0} style={{ width: '100%' }} placeholder="Number of lines changed" />
            </Form.Item>

            <Form.Item
              label="是否热修复 (Is Hotfix)"
              name="isHotfix"
              valuePropName="checked"
            >
              <Select>
                <Option value={false}>否 (No)</Option>
                <Option value={true}>是 (Yes)</Option>
              </Select>
            </Form.Item>

            <Form.Item
              label="是否核心模块 (Is Critical Module)"
              name="isCriticalModule"
              valuePropName="checked"
            >
              <Select>
                <Option value={false}>否 (No)</Option>
                <Option value={true}>是 (Yes)</Option>
              </Select>
            </Form.Item>
          </Card>

          <Form.Item label="测试范围 (Test Scope)" name="testScope">
            <Select placeholder="Select test scope">
              <Option value="SMOKE">冒烟测试 (Smoke Test)</Option>
              <Option value="CORE">核心回归 (Core Regression)</Option>
              <Option value="FULL">全量回归 (Full Regression)</Option>
            </Select>
          </Form.Item>

          <Form.Item
            label="优先级 (Priority)"
            name="priority"
            rules={[
              { type: 'number', min: 0, max: 10, message: 'Priority must be between 0 and 10' },
            ]}
          >
            <Select placeholder="Select priority">
              {[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((p) => (
                <Option key={p} value={p}>
                  {p}
                </Option>
              ))}
            </Select>
          </Form.Item>

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
