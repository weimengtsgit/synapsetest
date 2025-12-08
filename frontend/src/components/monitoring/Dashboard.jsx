import React, { useState, useEffect, useCallback } from 'react'
import {
  Card,
  Row,
  Col,
  Statistic,
  Progress,
  Table,
  Tag,
  message,
  Select,
  Space,
  Spin,
} from 'antd'
import {
  SyncOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DashboardOutlined,
} from '@ant-design/icons'
import monitoringService from '../../services/monitoringService'

const { Option } = Select

/**
 * Dashboard Component
 * Real-time test monitoring dashboard
 *
 * Task: T071 [P] [US3] Create frontend components for 监控仪表盘页面
 * User Story 3: 测试结果可视化分析
 */
const Dashboard = () => {
  const [loading, setLoading] = useState(false)
  const [statistics, setStatistics] = useState({})
  const [runningTasks, setRunningTasks] = useState([])
  const [recentData, setRecentData] = useState([])
  const [selectedEnvironment, setSelectedEnvironment] = useState('ALL')
  const [autoRefresh, setAutoRefresh] = useState(true)

  // Load dashboard data
  const loadDashboardData = useCallback(async () => {
    setLoading(true)
    try {
      // Load statistics
      const statsResponse = await monitoringService.getDashboardStatistics()
      if (statsResponse.success) {
        setStatistics(statsResponse.data)
      }

      // Load running tasks
      const runningResponse = await monitoringService.getRunningTasks()
      if (runningResponse.success) {
        setRunningTasks(runningResponse.data)
      }

      // Load recent monitoring data
      let recentResponse
      if (selectedEnvironment === 'ALL') {
        recentResponse = await monitoringService.getRecentMonitoringData()
      } else {
        recentResponse = await monitoringService.getMonitoringDataByEnvironment(
          selectedEnvironment
        )
      }
      if (recentResponse.success) {
        setRecentData(recentResponse.data)
      }
    } catch (error) {
      message.error(error.message || 'Failed to load dashboard data')
    } finally {
      setLoading(false)
    }
  }, [selectedEnvironment])

  useEffect(() => {
    loadDashboardData()
  }, [loadDashboardData, selectedEnvironment])

  // Auto-refresh every 10 seconds
  useEffect(() => {
    if (!autoRefresh) return

    const interval = setInterval(() => {
      loadDashboardData()
    }, 10000) // 10 seconds

    return () => clearInterval(interval)
  }, [autoRefresh, loadDashboardData])

  const getStatusColor = (status) => {
    const colors = {
      PENDING: 'default',
      RUNNING: 'processing',
      COMPLETED: 'success',
      FAILED: 'error',
      CANCELLED: 'warning',
    }
    return colors[status] || 'default'
  }

  const getProgressStatus = (progress, status) => {
    if (status === 'FAILED') return 'exception'
    if (status === 'COMPLETED') return 'success'
    if (progress === 100) return 'success'
    return 'active'
  }

  const formatDuration = (startTime, endTime) => {
    if (!startTime) return 'N/A'
    const start = new Date(startTime)
    const end = endTime ? new Date(endTime) : new Date()
    const duration = Math.floor((end - start) / 1000) // seconds
    const hours = Math.floor(duration / 3600)
    const minutes = Math.floor((duration % 3600) / 60)
    const seconds = duration % 60
    return `${hours}h ${minutes}m ${seconds}s`
  }

  const columns = [
    {
      title: '任务ID (Task ID)',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 280,
      render: (text) => (
        <span style={{ fontFamily: 'monospace', fontSize: '12px' }}>{text}</span>
      ),
    },
    {
      title: '环境 (Environment)',
      dataIndex: 'environment',
      key: 'environment',
      width: 120,
      render: (env) => <Tag color="blue">{env}</Tag>,
    },
    {
      title: '版本 (Version)',
      dataIndex: 'version',
      key: 'version',
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
      title: '进度 (Progress)',
      dataIndex: 'progress',
      key: 'progress',
      width: 200,
      render: (progress, record) => (
        <Progress
          percent={progress || 0}
          size="small"
          status={getProgressStatus(progress, record.status)}
        />
      ),
    },
    {
      title: '用例执行 (Cases)',
      key: 'cases',
      width: 150,
      render: (_, record) => (
        <span>
          {record.executedCases || 0} / {record.totalCases || 0}
        </span>
      ),
    },
    {
      title: '通过率 (Pass Rate)',
      key: 'passRate',
      width: 120,
      render: (_, record) => {
        const passRate = record.executedCases > 0
          ? ((record.passedCases || 0) / record.executedCases * 100).toFixed(1)
          : 0
        return (
          <span style={{ color: passRate >= 90 ? '#52c41a' : passRate >= 70 ? '#faad14' : '#ff4d4f' }}>
            {passRate}%
          </span>
        )
      },
    },
    {
      title: '耗时 (Duration)',
      key: 'duration',
      width: 150,
      render: (_, record) => formatDuration(record.startTime, record.actualEndTime),
    },
    {
      title: '更新时间 (Updated)',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
      render: (date) => new Date(date).toLocaleString('zh-CN'),
    },
  ]

  return (
    <div style={{ padding: '24px', background: '#f0f2f5', minHeight: '100vh' }}>
      <Card
        title={
          <Space>
            <DashboardOutlined />
            <span>实时监控仪表盘 (Real-time Monitoring Dashboard)</span>
          </Space>
        }
        extra={
          <Space>
            <Select
              value={selectedEnvironment}
              onChange={setSelectedEnvironment}
              style={{ width: 150 }}
            >
              <Option value="ALL">全部环境 (All)</Option>
              <Option value="DEV">开发环境 (Dev)</Option>
              <Option value="STAGING">预发环境 (Staging)</Option>
              <Option value="PROD">生产环境 (Prod)</Option>
            </Select>
            <Select
              value={autoRefresh}
              onChange={setAutoRefresh}
              style={{ width: 150 }}
            >
              <Option value={true}>自动刷新 (Auto)</Option>
              <Option value={false}>手动刷新 (Manual)</Option>
            </Select>
          </Space>
        }
        bordered={false}
      >
        {/* Statistics Cards */}
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="总任务数 (Total Tasks)"
                value={statistics.totalTasks || 0}
                prefix={<DashboardOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="运行中 (Running)"
                value={statistics.runningTasks || 0}
                prefix={<SyncOutlined spin />}
                valueStyle={{ color: '#faad14' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="已完成 (Completed)"
                value={statistics.completedTasks || 0}
                prefix={<CheckCircleOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="失败任务 (Failed)"
                value={statistics.failedTasks || 0}
                prefix={<CloseCircleOutlined />}
                valueStyle={{ color: '#ff4d4f' }}
              />
            </Card>
          </Col>
        </Row>

        {/* Overall Pass Rate */}
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={24}>
            <Card title="整体通过率 (Overall Pass Rate)">
              <Progress
                percent={statistics.averagePassRate ? parseFloat(statistics.averagePassRate.toFixed(1)) : 0}
                strokeColor={{
                  '0%': '#108ee9',
                  '100%': '#87d068',
                }}
                status="active"
              />
            </Card>
          </Col>
        </Row>

        {/* Running Tasks Section */}
        {runningTasks.length > 0 && (
          <Card
            title={
              <Space>
                <SyncOutlined spin />
                <span>正在执行的任务 (Running Tasks)</span>
              </Space>
            }
            style={{ marginBottom: 24 }}
          >
            <Spin spinning={loading}>
              <Table
                columns={columns}
                dataSource={runningTasks}
                rowKey="id"
                pagination={false}
                scroll={{ x: 1600 }}
                size="small"
              />
            </Spin>
          </Card>
        )}

        {/* Recent Monitoring Data */}
        <Card
          title={
            <Space>
              <ClockCircleOutlined />
              <span>最近监控数据 (Recent Monitoring Data)</span>
            </Space>
          }
        >
          <Spin spinning={loading}>
            <Table
              columns={columns}
              dataSource={recentData}
              rowKey="id"
              scroll={{ x: 1600 }}
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                showTotal: (total) => `总计 ${total} 条记录 (Total ${total} records)`,
              }}
            />
          </Spin>
        </Card>
      </Card>
    </div>
  )
}

export default Dashboard

