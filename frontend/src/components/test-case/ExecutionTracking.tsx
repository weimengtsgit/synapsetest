import React, { useState, useEffect } from 'react'
import {
  Card,
  Row,
  Col,
  Input,
  Select,
  Table,
  Button,
  Space,
  Tag,
  Progress,
  Checkbox,
  Typography,
  Statistic,
  Drawer,
} from 'antd'
import {
  SearchOutlined,
  RedoOutlined,
  ExportOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  FileTextOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import './ExecutionTracking.css'

const { Option } = Select
const { Text, Link } = Typography

interface ExecutionRecord {
  id: string
  testCaseId: string
  testCaseName: string
  module: string
  status: 'PASSED' | 'FAILED' | 'RUNNING' | 'PENDING'
  executor: string
  duration: string
  progress?: number
  errorMessage?: string
  errorStack?: string
  screenshot?: string
  recording?: string
}

const ExecutionTracking: React.FC = () => {
  const [searchText, setSearchText] = useState<string>('')
  const [selectedModule, setSelectedModule] = useState<string>('ALL')
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL')
  const [selectedTimeRange, setSelectedTimeRange] = useState<string>('7')
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([])
  const [loading, setLoading] = useState<boolean>(false)
  const [drawerVisible, setDrawerVisible] = useState<boolean>(false)
  const [selectedRecord, setSelectedRecord] = useState<ExecutionRecord | null>(null)

  // Mock data
  const [executionRecords, setExecutionRecords] = useState<ExecutionRecord[]>([
    {
      id: 'EX001',
      testCaseId: 'TC001',
      testCaseName: '手机号正常登录',
      module: '用户认证',
      status: 'PASSED',
      executor: '张三',
      duration: '2.3s',
    },
    {
      id: 'EX002',
      testCaseId: 'TC015',
      testCaseName: '微信第三方登录',
      module: '用户认证',
      status: 'FAILED',
      executor: '张三',
      duration: '1.8s',
      errorMessage: '微信授权接口超时',
      errorStack: 'WeChatAPI.authorize() at line 156',
      screenshot: '/screenshots/ex002.png',
      recording: '/recordings/ex002.mp4',
    },
    {
      id: 'EX003',
      testCaseId: 'TC102',
      testCaseName: '支付宝支付成功',
      module: '支付系统',
      status: 'RUNNING',
      executor: '李四',
      duration: '-',
      progress: 80,
    },
    {
      id: 'EX004',
      testCaseId: 'TC210',
      testCaseName: '订单创建成功',
      module: '订单中心',
      status: 'PENDING',
      executor: '王五',
      duration: '-',
    },
  ])

  // Statistics
  const [statistics] = useState({
    running: { value: 15, trend: '+3 新增' },
    completed: { value: 128, trend: '+25 今日' },
    passRate: { value: 92, trend: 3 },
    failed: { value: 12, trend: -5 },
  })

  const getStatusTag = (status: string) => {
    const statusConfig = {
      PASSED: { color: 'success', text: '通过' },
      FAILED: { color: 'error', text: '失败' },
      RUNNING: { color: 'processing', text: '执行中' },
      PENDING: { color: 'default', text: '待执行' },
    }
    const config = statusConfig[status as keyof typeof statusConfig]
    return <Tag color={config.color}>{config.text}</Tag>
  }

  const handleViewDetails = (record: ExecutionRecord) => {
    setSelectedRecord(record)
    setDrawerVisible(true)
  }

  const handleRetry = (record: ExecutionRecord) => {
    console.log('Retry execution:', record.id)
  }

  const handlePause = (record: ExecutionRecord) => {
    console.log('Pause execution:', record.id)
  }

  const handleStart = (record: ExecutionRecord) => {
    console.log('Start execution:', record.id)
  }

  const expandedRowRender = (record: ExecutionRecord) => {
    if (record.status === 'FAILED' && record.errorMessage) {
      return (
        <div style={{ background: '#fff7e6', padding: 12, borderLeft: '4px solid #fa8c16' }}>
          <div style={{ fontWeight: 'bold', color: '#fa8c16', marginBottom: 8 }}>
            失败详情
          </div>
          <div style={{ marginBottom: 4 }}>
            <strong>错误信息:</strong> {record.errorMessage}
          </div>
          <div style={{ marginBottom: 4 }}>
            <strong>错误堆栈:</strong> {record.errorStack}
          </div>
          <div>
            {record.screenshot && (
              <Link style={{ marginRight: 16 }}>查看截图</Link>
            )}
            {record.recording && <Link>查看录屏</Link>}
          </div>
        </div>
      )
    }

    if (record.status === 'RUNNING' && record.progress !== undefined) {
      return (
        <div style={{ background: '#e6f7ff', padding: 12 }}>
          <div style={{ marginBottom: 8 }}>执行进度:</div>
          <Progress percent={record.progress} status="active" />
        </div>
      )
    }

    return null
  }

  const columns: ColumnsType<ExecutionRecord> = [
    {
      title: '执行ID',
      dataIndex: 'id',
      key: 'id',
      width: 100,
    },
    {
      title: '用例名称',
      dataIndex: 'testCaseName',
      key: 'testCaseName',
      width: 200,
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
      width: 120,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => getStatusTag(status),
    },
    {
      title: '执行人',
      dataIndex: 'executor',
      key: 'executor',
      width: 100,
    },
    {
      title: '时长',
      dataIndex: 'duration',
      key: 'duration',
      width: 100,
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_, record) => (
        <Space size="small">
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetails(record)}
          >
            详情
          </Button>
          {record.status === 'FAILED' && (
            <>
              <Button
                size="small"
                icon={<FileTextOutlined />}
              >
                日志
              </Button>
              <Button
                size="small"
                icon={<RedoOutlined />}
                onClick={() => handleRetry(record)}
              >
                重试
              </Button>
            </>
          )}
          {record.status === 'RUNNING' && (
            <Button
              size="small"
              icon={<PauseCircleOutlined />}
              onClick={() => handlePause(record)}
            >
              暂停
            </Button>
          )}
          {record.status === 'PENDING' && (
            <Button
              size="small"
              type="primary"
              icon={<PlayCircleOutlined />}
              onClick={() => handleStart(record)}
            >
              开始
            </Button>
          )}
        </Space>
      ),
    },
  ]

  const rowSelection = {
    selectedRowKeys,
    onChange: (selectedKeys: React.Key[]) => {
      setSelectedRowKeys(selectedKeys)
    },
  }

  return (
    <div className="execution-tracking-container">
      <h2 style={{ marginBottom: 24, color: '#1890ff', fontWeight: 'bold' }}>
        测试执行跟踪 (Test Execution Tracking)
      </h2>

      {/* Filter Bar */}
      <Card style={{ marginBottom: 16 }}>
        <Space size="middle" style={{ width: '100%' }}>
          <Input
            placeholder="🔍 搜索执行记录..."
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            prefix={<SearchOutlined />}
            style={{ width: 300 }}
          />
          <Select
            value={selectedModule}
            onChange={setSelectedModule}
            style={{ width: 150 }}
          >
            <Option value="ALL">全部模块</Option>
            <Option value="用户认证">用户认证</Option>
            <Option value="支付系统">支付系统</Option>
            <Option value="订单中心">订单中心</Option>
          </Select>
          <Select
            value={selectedStatus}
            onChange={setSelectedStatus}
            style={{ width: 150 }}
          >
            <Option value="ALL">全部状态</Option>
            <Option value="PASSED">通过</Option>
            <Option value="FAILED">失败</Option>
            <Option value="RUNNING">执行中</Option>
            <Option value="PENDING">待执行</Option>
          </Select>
          <Select
            value={selectedTimeRange}
            onChange={setSelectedTimeRange}
            style={{ width: 150 }}
          >
            <Option value="7">最近7天</Option>
            <Option value="30">最近30天</Option>
          </Select>
        </Space>
      </Card>

      {/* Statistics Overview */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="执行中"
              value={statistics.running.value}
              valueStyle={{ color: '#1890ff' }}
              suffix={
                <Text type="secondary" style={{ fontSize: 14 }}>
                  {statistics.running.trend}
                </Text>
              }
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已完成"
              value={statistics.completed.value}
              valueStyle={{ color: '#52c41a' }}
              suffix={
                <Text type="secondary" style={{ fontSize: 14 }}>
                  {statistics.completed.trend}
                </Text>
              }
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="通过率"
              value={statistics.passRate.value}
              precision={0}
              valueStyle={{ color: '#52c41a' }}
              prefix={<ArrowUpOutlined />}
              suffix={
                <Text type="secondary" style={{ fontSize: 14 }}>
                  % ↑ {statistics.passRate.trend}%
                </Text>
              }
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="失败用例"
              value={statistics.failed.value}
              valueStyle={{ color: '#f5222d' }}
              prefix={<ArrowDownOutlined />}
              suffix={
                <Text type="secondary" style={{ fontSize: 14 }}>
                  ↓ {Math.abs(statistics.failed.trend)}
                </Text>
              }
            />
          </Card>
        </Col>
      </Row>

      {/* Execution List */}
      <Card
        title={`执行列表 (共 ${executionRecords.length} 条记录)`}
        extra={
          <Space>
            <Select defaultValue="batch" style={{ width: 120 }}>
              <Option value="batch">批量操作</Option>
              <Option value="retry">重新执行</Option>
              <Option value="export">导出</Option>
            </Select>
            <Button icon={<ExportOutlined />}>导出</Button>
          </Space>
        }
        style={{ marginBottom: 24 }}
      >
        <Table
          columns={columns}
          dataSource={executionRecords}
          rowKey="id"
          rowSelection={rowSelection}
          loading={loading}
          expandable={{
            expandedRowKeys,
            onExpandedRowsChange: (keys) => setExpandedRowKeys(keys as string[]),
            expandedRowRender,
            rowExpandable: (record) =>
              record.status === 'FAILED' || record.status === 'RUNNING',
          }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
        />
      </Card>

      {/* Execution Statistics */}
      <Card title="执行统计 (Execution Statistics)">
        <Row gutter={16}>
          <Col span={8}>
            <div
              style={{
                height: 200,
                background: '#fafafa',
                borderRadius: 4,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#8c8c8c',
                textAlign: 'center',
              }}
            >
              📈 执行趋势图
              <br />
              （最近7天）
            </div>
          </Col>
          <Col span={8}>
            <div
              style={{
                height: 200,
                background: '#fafafa',
                borderRadius: 4,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#8c8c8c',
                textAlign: 'center',
              }}
            >
              🥧 失败用例分布
              <br />
              （按模块）
            </div>
          </Col>
          <Col span={8}>
            <div
              style={{
                height: 200,
                background: '#fafafa',
                borderRadius: 4,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#8c8c8c',
                textAlign: 'center',
              }}
            >
              📊 执行时长分析
              <br />
              （按模块）
            </div>
          </Col>
        </Row>
      </Card>

      {/* Details Drawer */}
      <Drawer
        title={`执行详情 - ${selectedRecord?.id}`}
        placement="right"
        width={640}
        onClose={() => setDrawerVisible(false)}
        visible={drawerVisible}
      >
        {selectedRecord && (
          <div>
            <div style={{ marginBottom: 16 }}>
              <Text strong>用例名称:</Text> {selectedRecord.testCaseName}
            </div>
            <div style={{ marginBottom: 16 }}>
              <Text strong>模块:</Text> {selectedRecord.module}
            </div>
            <div style={{ marginBottom: 16 }}>
              <Text strong>执行状态:</Text> {getStatusTag(selectedRecord.status)}
            </div>
            <div style={{ marginBottom: 16 }}>
              <Text strong>执行人:</Text> {selectedRecord.executor}
            </div>
            <div style={{ marginBottom: 16 }}>
              <Text strong>执行时长:</Text> {selectedRecord.duration}
            </div>
            {selectedRecord.errorMessage && (
              <>
                <div style={{ marginBottom: 16 }}>
                  <Text strong>错误信息:</Text>
                  <div
                    style={{
                      background: '#fff1f0',
                      padding: 12,
                      borderRadius: 4,
                      marginTop: 8,
                    }}
                  >
                    {selectedRecord.errorMessage}
                  </div>
                </div>
                <div style={{ marginBottom: 16 }}>
                  <Text strong>错误堆栈:</Text>
                  <div
                    style={{
                      background: '#f5f5f5',
                      padding: 12,
                      borderRadius: 4,
                      marginTop: 8,
                      fontFamily: 'monospace',
                      fontSize: 12,
                    }}
                  >
                    {selectedRecord.errorStack}
                  </div>
                </div>
              </>
            )}
            {selectedRecord.progress !== undefined && (
              <div style={{ marginBottom: 16 }}>
                <Text strong>执行进度:</Text>
                <Progress percent={selectedRecord.progress} style={{ marginTop: 8 }} />
              </div>
            )}
          </div>
        )}
      </Drawer>
    </div>
  )
}

export default ExecutionTracking
