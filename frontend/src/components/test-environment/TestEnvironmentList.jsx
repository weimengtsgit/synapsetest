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
  Popconfirm,
  Form,
  Input,
} from 'antd'
import {
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  ToolOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons'
import environmentService from '../../services/environmentService'

const { Option } = Select
const { TextArea } = Input

/**
 * Test Environment List Component
 * User Story 1: 智能测试任务调度 - 测试环境列表页面
 */
const TestEnvironmentList = () => {
  const [environments, setEnvironments] = useState([])
  const [loading, setLoading] = useState(false)
  const [selectedStatus, setSelectedStatus] = useState('ALL')
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [editModalVisible, setEditModalVisible] = useState(false)
  const [selectedEnvironment, setSelectedEnvironment] = useState(null)
  const [form] = Form.useForm()

  useEffect(() => {
    loadEnvironments()
  }, [selectedStatus])

  const loadEnvironments = async () => {
    setLoading(true)
    try {
      let response
      if (selectedStatus === 'ALL') {
        response = await environmentService.getAllEnvironments()
      } else {
        response = await environmentService.getAllEnvironments(selectedStatus)
      }

      if (response.success) {
        const environmentsData = response.data.content || response.data
        setEnvironments(Array.isArray(environmentsData) ? environmentsData : [])
      } else {
        setEnvironments(Array.isArray(response) ? response : [])
      }
    } catch (error) {
      message.error(error.message || 'Failed to load test environments')
    } finally {
      setLoading(false)
    }
  }

  const handleViewDetails = (environment) => {
    setSelectedEnvironment(environment)
    setDetailModalVisible(true)
  }

  const handleEdit = (environment) => {
    setSelectedEnvironment(environment)
    form.setFieldsValue(environment)
    setEditModalVisible(true)
  }

  const handleDelete = async (environmentId) => {
    try {
      await environmentService.deleteEnvironment(environmentId)
      message.success('测试环境已删除 (Test environment deleted)')
      loadEnvironments()
    } catch (error) {
      message.error('删除失败: ' + (error.message || 'Delete failed'))
    }
  }

  const handleUpdateStatus = async (environmentId, newStatus) => {
    try {
      await environmentService.updateEnvironmentStatus(environmentId, newStatus)
      message.success('环境状态已更新 (Environment status updated)')
      loadEnvironments()
    } catch (error) {
      message.error('状态更新失败: ' + (error.message || 'Status update failed'))
    }
  }

  const handleCreate = () => {
    setSelectedEnvironment(null)
    form.resetFields()
    form.setFieldsValue({ status: 'AVAILABLE' })
    setEditModalVisible(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()

      if (selectedEnvironment) {
        await environmentService.updateEnvironment(selectedEnvironment.id, values)
        message.success('测试环境已更新 (Test environment updated)')
      } else {
        await environmentService.createEnvironment(values)
        message.success('测试环境已创建 (Test environment created)')
      }

      setEditModalVisible(false)
      loadEnvironments()
      form.resetFields()
    } catch (error) {
      if (error.errorFields) {
        message.error('请检查表单填写 (Please check the form)')
      } else {
        message.error('操作失败: ' + (error.message || 'Operation failed'))
      }
    }
  }

  const getStatusColor = (status) => {
    const colors = {
      AVAILABLE: 'success',
      MAINTENANCE: 'warning',
      UNAVAILABLE: 'error',
    }
    return colors[status] || 'default'
  }

  const getStatusIcon = (status) => {
    const icons = {
      AVAILABLE: <CheckCircleOutlined />,
      MAINTENANCE: <ToolOutlined />,
      UNAVAILABLE: <CloseCircleOutlined />,
    }
    return icons[status] || null
  }

  const columns = [
    {
      title: '环境名称 (Environment Name)',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: 'URL',
      dataIndex: 'url',
      key: 'url',
      width: 250,
      ellipsis: true,
      render: (url) => (
        url ? (
          <a href={url} target="_blank" rel="noopener noreferrer">
            {url}
          </a>
        ) : '-'
      ),
    },
    {
      title: '状态 (Status)',
      dataIndex: 'status',
      key: 'status',
      width: 150,
      render: (status) => (
        <Tag icon={getStatusIcon(status)} color={getStatusColor(status)}>
          {status}
        </Tag>
      ),
    },
    {
      title: '描述 (Description)',
      dataIndex: 'description',
      key: 'description',
      width: 250,
      ellipsis: true,
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
      width: 280,
      render: (_, record) => (
        <Space size="small" wrap>
          <Button
            type="link"
            icon={<EyeOutlined />}
            size="small"
            onClick={() => handleViewDetails(record)}
          >
            查看
          </Button>
          <Button
            type="link"
            icon={<EditOutlined />}
            size="small"
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          {record.status !== 'AVAILABLE' && (
            <Button
              type="link"
              size="small"
              onClick={() => handleUpdateStatus(record.id, 'AVAILABLE')}
            >
              标记可用
            </Button>
          )}
          {record.status === 'AVAILABLE' && (
            <Button
              type="link"
              size="small"
              onClick={() => handleUpdateStatus(record.id, 'MAINTENANCE')}
            >
              维护中
            </Button>
          )}
          <Popconfirm
            title="确定要删除这个测试环境吗？"
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
        title="测试环境列表 (Test Environment List)"
        bordered={false}
        extra={
          <Space>
            <Select
              value={selectedStatus}
              onChange={setSelectedStatus}
              style={{ width: 180 }}
            >
              <Option value="ALL">全部状态 (All)</Option>
              <Option value="AVAILABLE">可用 (Available)</Option>
              <Option value="MAINTENANCE">维护中 (Maintenance)</Option>
              <Option value="UNAVAILABLE">不可用 (Unavailable)</Option>
            </Select>
            <Button icon={<SyncOutlined />} onClick={loadEnvironments}>
              刷新 (Refresh)
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              新增环境 (New Environment)
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={environments}
          loading={loading}
          rowKey="id"
          scroll={{ x: 1400 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条测试环境 (Total ${total} environments)`,
          }}
        />
      </Card>

      {/* Detail Modal */}
      <Modal
        title={`测试环境详情 - ${selectedEnvironment?.name}`}
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭 (Close)
          </Button>,
          <Button
            key="edit"
            type="primary"
            icon={<EditOutlined />}
            onClick={() => {
              setDetailModalVisible(false)
              handleEdit(selectedEnvironment)
            }}
          >
            编辑 (Edit)
          </Button>,
        ]}
        width={800}
      >
        {selectedEnvironment && (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="环境ID (ID)" span={2}>
              {selectedEnvironment.id}
            </Descriptions.Item>
            <Descriptions.Item label="环境名称 (Name)" span={2}>
              {selectedEnvironment.name}
            </Descriptions.Item>
            <Descriptions.Item label="状态 (Status)" span={2}>
              <Tag icon={getStatusIcon(selectedEnvironment.status)} color={getStatusColor(selectedEnvironment.status)}>
                {selectedEnvironment.status}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="URL" span={2}>
              {selectedEnvironment.url ? (
                <a href={selectedEnvironment.url} target="_blank" rel="noopener noreferrer">
                  {selectedEnvironment.url}
                </a>
              ) : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="描述 (Description)" span={2}>
              {selectedEnvironment.description || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间 (Created At)">
              {selectedEnvironment.createdAt
                ? new Date(selectedEnvironment.createdAt).toLocaleString('zh-CN')
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="更新时间 (Updated At)">
              {selectedEnvironment.updatedAt
                ? new Date(selectedEnvironment.updatedAt).toLocaleString('zh-CN')
                : '-'}
            </Descriptions.Item>
            {selectedEnvironment.config && Object.keys(selectedEnvironment.config).length > 0 && (
              <Descriptions.Item label="配置信息 (Config)" span={2}>
                <pre style={{ margin: 0 }}>
                  {JSON.stringify(selectedEnvironment.config, null, 2)}
                </pre>
              </Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>

      {/* Edit/Create Modal */}
      <Modal
        title={selectedEnvironment ? '编辑测试环境 (Edit Environment)' : '新增测试环境 (New Environment)'}
        open={editModalVisible}
        onCancel={() => {
          setEditModalVisible(false)
          form.resetFields()
        }}
        onOk={handleSubmit}
        width={600}
        okText={selectedEnvironment ? '更新 (Update)' : '创建 (Create)'}
        cancelText="取消 (Cancel)"
      >
        <Form form={form} layout="vertical" style={{ marginTop: 24 }}>
          <Form.Item
            name="name"
            label="环境名称 (Environment Name)"
            rules={[{ required: true, message: '请输入环境名称 (Please enter environment name)' }]}
          >
            <Input placeholder="例如: DEV, QA, UAT, PROD" />
          </Form.Item>
          <Form.Item
            name="url"
            label="环境URL (Environment URL)"
            rules={[
              { type: 'url', message: '请输入有效的URL (Please enter a valid URL)' },
            ]}
          >
            <Input placeholder="https://example.com" />
          </Form.Item>
          <Form.Item
            name="status"
            label="状态 (Status)"
            rules={[{ required: true, message: '请选择状态 (Please select status)' }]}
          >
            <Select>
              <Option value="AVAILABLE">可用 (Available)</Option>
              <Option value="MAINTENANCE">维护中 (Maintenance)</Option>
              <Option value="UNAVAILABLE">不可用 (Unavailable)</Option>
            </Select>
          </Form.Item>
          <Form.Item name="description" label="描述 (Description)">
            <TextArea rows={4} placeholder="请输入环境描述信息" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default TestEnvironmentList
