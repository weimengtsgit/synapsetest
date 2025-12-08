import React, { useState, useEffect } from 'react'
import {
  Table,
  Tag,
  Button,
  Space,
  message,
  Card,
  Select,
  Modal,
  Descriptions,
  Collapse,
  Popconfirm,
} from 'antd'
import {
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  SyncOutlined,
} from '@ant-design/icons'
import testCaseService from '../../services/testCaseService'

const { Option } = Select
const { Panel } = Collapse

/**
 * Test Case List Component
 * User Story 2: AI生成测试用例 - 测试用例列表页面
 *
 * Task: T056 [P] [US2] Create frontend components for 测试用例列表页面
 */
const TestCaseList = () => {
  const [testCases, setTestCases] = useState([])
  const [loading, setLoading] = useState(false)
  const [selectedType, setSelectedType] = useState('ALL')
  const [selectedStatus, setSelectedStatus] = useState('ALL')
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [selectedTestCase, setSelectedTestCase] = useState(null)

  useEffect(() => {
    loadTestCases()
  }, [selectedType, selectedStatus])

  const loadTestCases = async () => {
    setLoading(true)
    try {
      let response

      if (selectedType !== 'ALL') {
        response = await testCaseService.getTestCasesByType(selectedType)
      } else if (selectedStatus !== 'ALL') {
        response = await testCaseService.getTestCasesByStatus(selectedStatus)
      } else {
        response = await testCaseService.getAllTestCases()
      }

      if (response.success) {
        const casesData = response.data.content || response.data
        setTestCases(Array.isArray(casesData) ? casesData : [])
      } else {
        message.error('Failed to load test cases')
      }
    } catch (error) {
      message.error(error.message || 'Failed to load test cases')
    } finally {
      setLoading(false)
    }
  }

  const handleViewDetails = (testCase) => {
    setSelectedTestCase(testCase)
    setDetailModalVisible(true)
  }

  const handleApprove = async (testCaseId) => {
    try {
      const response = await testCaseService.approveTestCase(testCaseId)
      if (response.success) {
        message.success('测试用例已批准')
        loadTestCases()
      }
    } catch (error) {
      message.error('批准失败: ' + error.message)
    }
  }

  const handleDelete = async (testCaseId) => {
    try {
      const response = await testCaseService.deleteTestCase(testCaseId)
      if (response.success) {
        message.success('测试用例已删除')
        loadTestCases()
      }
    } catch (error) {
      message.error('删除失败: ' + error.message)
    }
  }

  const handleOptimize = async () => {
    if (testCases.length === 0) {
      message.warning('没有测试用例可优化')
      return
    }

    setLoading(true)
    try {
      const response = await testCaseService.deduplicateTestCases(testCases)

      if (response.success) {
        const { original, optimized, reduction } = response.data
        message.success(`优化完成！从 ${original} 个用例减少到 ${optimized} 个，减少了 ${reduction}`)
        loadTestCases()
      }
    } catch (error) {
      message.error('优化失败: ' + error.message)
    } finally {
      setLoading(false)
    }
  }

  const getStatusColor = (status) => {
    const colors = {
      DRAFT: 'default',
      APPROVED: 'success',
      DEPRECATED: 'error',
    }
    return colors[status] || 'default'
  }

  const getTypeColor = (type) => {
    const colors = {
      FUNCTIONAL: 'blue',
      PERFORMANCE: 'orange',
      SECURITY: 'red',
    }
    return colors[type] || 'default'
  }

  const getPriorityColor = (priority) => {
    if (priority >= 8) return 'red'
    if (priority >= 5) return 'orange'
    return 'blue'
  }

  const columns = [
    {
      title: '用例标题 (Title)',
      dataIndex: 'title',
      key: 'title',
      width: 300,
      ellipsis: true,
    },
    {
      title: '类型 (Type)',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type) => <Tag color={getTypeColor(type)}>{type}</Tag>,
    },
    {
      title: '优先级 (Priority)',
      dataIndex: 'priority',
      key: 'priority',
      width: 100,
      sorter: (a, b) => a.priority - b.priority,
      render: (priority) => (
        <Tag color={getPriorityColor(priority)}>P{priority}</Tag>
      ),
    },
    {
      title: '状态 (Status)',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status) => <Tag color={getStatusColor(status)}>{status}</Tag>,
    },
    {
      title: '标签 (Tags)',
      dataIndex: 'tags',
      key: 'tags',
      width: 200,
      render: (tags) => (
        <>
          {tags &&
            tags.slice(0, 3).map((tag) => (
              <Tag key={tag} style={{ marginBottom: 4 }}>
                {tag}
              </Tag>
            ))}
          {tags && tags.length > 3 && <Tag>+{tags.length - 3}</Tag>}
        </>
      ),
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
      render: (date) => (date ? new Date(date).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作 (Actions)',
      key: 'actions',
      fixed: 'right',
      width: 250,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            icon={<EyeOutlined />}
            size="small"
            onClick={() => handleViewDetails(record)}
          >
            查看
          </Button>
          <Button type="link" icon={<EditOutlined />} size="small">
            编辑
          </Button>
          {record.status === 'DRAFT' && (
            <Button
              type="link"
              icon={<CheckCircleOutlined />}
              size="small"
              onClick={() => handleApprove(record.id)}
            >
              批准
            </Button>
          )}
          <Popconfirm
            title="确定要删除这个测试用例吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />} size="small">
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: '24px' }}>
      <Card
        title="测试用例列表 (Test Case List)"
        bordered={false}
        extra={
          <Space>
            <Select
              value={selectedType}
              onChange={setSelectedType}
              style={{ width: 150 }}
            >
              <Option value="ALL">全部类型 (All)</Option>
              <Option value="FUNCTIONAL">功能测试</Option>
              <Option value="PERFORMANCE">性能测试</Option>
              <Option value="SECURITY">安全测试</Option>
            </Select>
            <Select
              value={selectedStatus}
              onChange={setSelectedStatus}
              style={{ width: 150 }}
            >
              <Option value="ALL">全部状态 (All)</Option>
              <Option value="DRAFT">草稿 (Draft)</Option>
              <Option value="APPROVED">已批准 (Approved)</Option>
              <Option value="DEPRECATED">已废弃 (Deprecated)</Option>
            </Select>
            <Button icon={<SyncOutlined />} onClick={loadTestCases}>
              刷新
            </Button>
            <Button type="primary" onClick={handleOptimize}>
              智能优化
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={testCases}
          loading={loading}
          rowKey="id"
          scroll={{ x: 1600 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条测试用例`,
          }}
        />
      </Card>

      {/* Detail Modal */}
      <Modal
        title={`测试用例详情 - ${selectedTestCase?.title}`}
        visible={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>,
          <Button key="edit" type="primary" icon={<EditOutlined />}>
            编辑
          </Button>,
        ]}
        width={800}
      >
        {selectedTestCase && (
          <div>
            <Descriptions bordered column={2}>
              <Descriptions.Item label="用例ID">
                {selectedTestCase.id}
              </Descriptions.Item>
              <Descriptions.Item label="类型">
                <Tag color={getTypeColor(selectedTestCase.type)}>
                  {selectedTestCase.type}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="优先级">
                <Tag color={getPriorityColor(selectedTestCase.priority)}>
                  P{selectedTestCase.priority}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={getStatusColor(selectedTestCase.status)}>
                  {selectedTestCase.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建者">
                {selectedTestCase.createdBy}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {selectedTestCase.createdAt
                  ? new Date(selectedTestCase.createdAt).toLocaleString('zh-CN')
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="关联需求" span={2}>
                {selectedTestCase.relatedRequirement || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="标签" span={2}>
                {selectedTestCase.tags &&
                  selectedTestCase.tags.map((tag) => <Tag key={tag}>{tag}</Tag>)}
              </Descriptions.Item>
            </Descriptions>

            <div style={{ marginTop: 24 }}>
              <h4>用例描述:</h4>
              <p>{selectedTestCase.description || '无描述'}</p>
            </div>

            <div style={{ marginTop: 24 }}>
              <h4>测试步骤:</h4>
              <ol style={{ paddingLeft: 20 }}>
                {selectedTestCase.steps?.map((step, idx) => (
                  <li key={idx} style={{ marginBottom: 8 }}>
                    {step}
                  </li>
                ))}
              </ol>
            </div>

            <div style={{ marginTop: 24 }}>
              <h4>预期结果:</h4>
              <p>{selectedTestCase.expectedResults || '无预期结果'}</p>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default TestCaseList
