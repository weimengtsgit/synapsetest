import React, { useState, useEffect } from 'react'
import { Card, Descriptions, Table, Tag, message, Spin, Divider, Button } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import testTaskService from '../../services/testTaskService'

/**
 * Test Task Detail Component
 * Displays task details and associated test cases
 * 
 * Enhanced with task-testcase association display
 */
const TestTaskDetail = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const [task, setTask] = useState(null)
  const [testCases, setTestCases] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (id) {
      loadTaskDetail()
    }
  }, [id])

  const loadTaskDetail = async () => {
    setLoading(true)
    try {
      const taskResponse = await testTaskService.getTestTaskById(id)
      
      if (taskResponse && taskResponse.success) {
        setTask(taskResponse.data)
        
        // Test cases are included in the response
        if (taskResponse.data.testCases) {
          setTestCases(taskResponse.data.testCases)
        } else {
          // Fallback: fetch separately if not included
          const casesResponse = await testTaskService.getTestCasesByTaskId(id)
          if (casesResponse && casesResponse.success) {
            setTestCases(casesResponse.data || [])
          }
        }
      } else {
        message.error('Failed to load task detail')
      }
    } catch (error) {
      message.error(error.message || 'Failed to load task detail')
      console.error('Error loading task detail:', error)
    } finally {
      setLoading(false)
    }
  }

  const getStatusColor = (status) => {
    const colors = {
      PENDING: 'default',
      RUNNING: 'processing',
      COMPLETED: 'success',
      CANCELLED: 'error',
    }
    return colors[status] || 'default'
  }

  const getPriorityColor = (priority) => {
    if (priority >= 8) return 'red'
    if (priority >= 5) return 'orange'
    return 'blue'
  }

  const testCaseColumns = [
    {
      title: '执行顺序',
      dataIndex: 'executionOrder',
      key: 'executionOrder',
      width: 80,
      sorter: (a, b) => (a.executionOrder || 0) - (b.executionOrder || 0),
    },
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
      render: (priority) => (
        <Tag color={getPriorityColor(priority)}>P{priority}</Tag>
      ),
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

  if (loading) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <Spin size="large" tip="Loading task detail..." />
      </div>
    )
  }

  if (!task) {
    return (
      <div style={{ padding: '24px' }}>
        <Card>
          <p>Task not found</p>
          <Button onClick={() => navigate('/test-tasks/list')}>
            Back to Task List
          </Button>
        </Card>
      </div>
    )
  }

  return (
    <div style={{ padding: '24px' }}>
      <Button 
        icon={<ArrowLeftOutlined />} 
        onClick={() => navigate('/test-tasks/list')}
        style={{ marginBottom: 16 }}
      >
        返回列表 (Back to List)
      </Button>

      <Card 
        title={`测试任务详情 (Test Task Detail) - ${task.name}`}
        bordered={false}
      >
        <Descriptions bordered column={2}>
          <Descriptions.Item label="任务ID">{task.id}</Descriptions.Item>
          <Descriptions.Item label="任务名称">{task.name}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={getStatusColor(task.status)}>{task.status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="优先级">
            <Tag color={getPriorityColor(task.priority)}>P{task.priority}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="测试环境">{task.environment}</Descriptions.Item>
          <Descriptions.Item label="测试版本">{task.version}</Descriptions.Item>
          <Descriptions.Item label="测试范围">{task.testScope}</Descriptions.Item>
          <Descriptions.Item label="用例总数">
            <Tag color="blue">{task.totalTestCases || testCases.length}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="创建人">{task.createdBy}</Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {task.createdAt ? new Date(task.createdAt).toLocaleString('zh-CN') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="更新时间">
            {task.updatedAt ? new Date(task.updatedAt).toLocaleString('zh-CN') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="任务描述" span={2}>
            {task.description || '-'}
          </Descriptions.Item>
        </Descriptions>

        {task.recommendation && (
          <>
            <Divider orientation="left">AI 推荐 (AI Recommendation)</Divider>
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="推荐环境">
                {task.recommendation.recommendedEnvironment}
              </Descriptions.Item>
              <Descriptions.Item label="推荐版本">
                {task.recommendation.recommendedVersion}
              </Descriptions.Item>
              <Descriptions.Item label="推荐范围">
                {task.recommendation.recommendedScope}
              </Descriptions.Item>
              <Descriptions.Item label="置信度">
                {(task.recommendation.confidenceScore * 100).toFixed(1)}%
              </Descriptions.Item>
              <Descriptions.Item label="推理说明" span={2}>
                {task.recommendation.reasoning}
              </Descriptions.Item>
            </Descriptions>
          </>
        )}

        <Divider orientation="left">
          关联的测试用例 (Associated Test Cases: {testCases.length})
        </Divider>

        {testCases.length > 0 ? (
          <Table
            dataSource={testCases}
            columns={testCaseColumns}
            rowKey="id"
            size="small"
            pagination={{
              pageSize: 20,
              showSizeChanger: true,
              showTotal: (total) => `Total ${total} test cases`,
            }}
            scroll={{ x: 1000 }}
          />
        ) : (
          <Card size="small">
            <p style={{ textAlign: 'center', color: '#999' }}>
              No test cases associated with this task
            </p>
          </Card>
        )}
      </Card>
    </div>
  )
}

export default TestTaskDetail
