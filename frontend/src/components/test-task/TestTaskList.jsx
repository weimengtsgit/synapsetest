import React, { useState, useEffect } from 'react'
import { Table, Tag, Button, Space, message, Card, Select } from 'antd'
import { PlayCircleOutlined, StopOutlined, EyeOutlined } from '@ant-design/icons'
import testTaskService from '../../services/testTaskService'

const { Option } = Select

/**
 * Test Task List Component
 * User Story 1: 智能测试任务调度 - 测试任务列表页面
 *
 * Task: T041 [P] [US1] Create frontend components for 测试任务列表页面
 */
const TestTaskList = () => {
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(false)
  const [selectedStatus, setSelectedStatus] = useState('ALL')

  useEffect(() => {
    loadTestTasks()
  }, [selectedStatus])

  const loadTestTasks = async () => {
    setLoading(true)
    try {
      let response
      if (selectedStatus === 'ALL') {
        response = await testTaskService.getAllTestTasks()
      } else {
        response = await testTaskService.getTestTasksByStatus(selectedStatus)
      }

      if (response.success) {
        // Handle both paginated and list responses
        const tasksData = response.data.content || response.data
        setTasks(Array.isArray(tasksData) ? tasksData : [])
      } else {
        message.error('Failed to load test tasks')
      }
    } catch (error) {
      message.error(error.message || 'Failed to load test tasks')
    } finally {
      setLoading(false)
    }
  }

  const handleStartTask = async (taskId) => {
    try {
      const response = await testTaskService.startTestTask(taskId)
      if (response.success) {
        message.success('Test task started successfully')
        loadTestTasks() // Reload tasks
      }
    } catch (error) {
      message.error(error.message || 'Failed to start test task')
    }
  }

  const handleCancelTask = async (taskId) => {
    try {
      const response = await testTaskService.cancelTestTask(taskId)
      if (response.success) {
        message.success('Test task cancelled successfully')
        loadTestTasks() // Reload tasks
      }
    } catch (error) {
      message.error(error.message || 'Failed to cancel test task')
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

  const columns = [
    {
      title: '任务名称 (Task Name)',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '环境 (Environment)',
      dataIndex: 'environment',
      key: 'environment',
      width: 120,
    },
    {
      title: '版本 (Version)',
      dataIndex: 'version',
      key: 'version',
      width: 120,
    },
    {
      title: '测试范围 (Scope)',
      dataIndex: 'testScope',
      key: 'testScope',
      width: 120,
    },
    {
      title: '状态 (Status)',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status) => <Tag color={getStatusColor(status)}>{status}</Tag>,
    },
    {
      title: '优先级 (Priority)',
      dataIndex: 'priority',
      key: 'priority',
      width: 100,
      sorter: (a, b) => a.priority - b.priority,
    },
    {
      title: '创建者 (Created By)',
      dataIndex: 'createdBy',
      key: 'createdBy',
      width: 120,
    },
    {
      title: '创建时间 (Created At)',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date) => new Date(date).toLocaleString('zh-CN'),
    },
    {
      title: '操作 (Actions)',
      key: 'actions',
      fixed: 'right',
      width: 200,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            icon={<EyeOutlined />}
            size="small"
            onClick={() => message.info(`View task: ${record.id}`)}
          >
            查看
          </Button>
          {record.status === 'PENDING' && (
            <Button
              type="link"
              icon={<PlayCircleOutlined />}
              size="small"
              onClick={() => handleStartTask(record.id)}
            >
              启动
            </Button>
          )}
          {(record.status === 'PENDING' || record.status === 'RUNNING') && (
            <Button
              type="link"
              danger
              icon={<StopOutlined />}
              size="small"
              onClick={() => handleCancelTask(record.id)}
            >
              取消
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: '24px' }}>
      <Card
        title="测试任务列表 (Test Task List)"
        bordered={false}
        extra={
          <Space>
            <Select
              value={selectedStatus}
              onChange={setSelectedStatus}
              style={{ width: 150 }}
            >
              <Option value="ALL">全部 (All)</Option>
              <Option value="PENDING">待执行 (Pending)</Option>
              <Option value="RUNNING">执行中 (Running)</Option>
              <Option value="COMPLETED">已完成 (Completed)</Option>
              <Option value="CANCELLED">已取消 (Cancelled)</Option>
            </Select>
            <Button type="primary" onClick={loadTestTasks}>
              刷新 (Refresh)
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={tasks}
          loading={loading}
          rowKey="id"
          scroll={{ x: 1500 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `Total ${total} tasks`,
          }}
        />
      </Card>
    </div>
  )
}

export default TestTaskList
