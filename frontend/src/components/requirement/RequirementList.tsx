import React, { useState, useEffect } from 'react'
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Card } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import type { Requirement, RequirementType, RequirementPriority, RequirementStatus } from '../../types/requirement'
import { getRequirements, createRequirement, updateRequirement, deleteRequirement } from '../../services/requirementService'
import './RequirementList.css'

const { TextArea } = Input
const { Option } = Select

const RequirementList: React.FC = () => {
  const [requirements, setRequirements] = useState<Requirement[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRequirement, setEditingRequirement] = useState<Requirement | null>(null)
  const [form] = Form.useForm()
  const navigate = useNavigate()

  useEffect(() => {
    loadRequirements()
  }, [])

  const loadRequirements = async () => {
    setLoading(true)
    try {
      const data = await getRequirements()
      setRequirements(data)
    } catch (error) {
      message.error('加载需求列表失败')
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = () => {
    setEditingRequirement(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record: Requirement) => {
    setEditingRequirement(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = (id: string) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个需求吗？',
      onOk: async () => {
        try {
          await deleteRequirement(id)
          message.success('删除成功')
          loadRequirements()
        } catch (error) {
          message.error('删除失败')
        }
      },
    })
  }

  const handleAIAnalysis = (record: Requirement) => {
    navigate('/requirements/ai-analysis', { state: { requirement: record } })
  }

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      if (editingRequirement) {
        await updateRequirement(editingRequirement.id, values)
        message.success('更新成功')
      } else {
        await createRequirement(values)
        message.success('创建成功')
      }

      setModalVisible(false)
      loadRequirements()
    } catch (error) {
      message.error('操作失败')
    } finally {
      setLoading(false)
    }
  }

  const getPriorityColor = (priority: RequirementPriority) => {
    const colors = { P0: 'red', P1: 'orange', P2: 'blue', P3: 'default' }
    return colors[priority]
  }

  const getStatusColor = (status: RequirementStatus) => {
    const colors = { DRAFT: 'default', APPROVED: 'green', REJECTED: 'red' }
    return colors[status]
  }

  const getStatusText = (status: RequirementStatus) => {
    const texts = { DRAFT: '草稿', APPROVED: '已批准', REJECTED: '已拒绝' }
    return texts[status]
  }

  const columns = [
    {
      title: '需求ID',
      dataIndex: 'id',
      key: 'id',
      width: 120,
    },
    {
      title: '需求标题',
      dataIndex: 'title',
      key: 'title',
      width: 200,
    },
    {
      title: '需求描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: RequirementType) => (
        <Tag color={type === 'FUNCTIONAL' ? 'blue' : 'purple'}>
          {type === 'FUNCTIONAL' ? '功能需求' : '非功能需求'}
        </Tag>
      ),
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 100,
      render: (priority: RequirementPriority) => (
        <Tag color={getPriorityColor(priority)}>{priority}</Tag>
      ),
    },
    {
      title: '关联模块',
      dataIndex: 'modules',
      key: 'modules',
      width: 150,
      render: (modules: string[]) => (
        <>
          {modules.map((module) => (
            <Tag key={module}>{module}</Tag>
          ))}
        </>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: RequirementStatus) => (
        <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: Requirement) => (
        <Space size="small">
          <Button
            type="primary"
            icon={<ThunderboltOutlined />}
            size="small"
            onClick={() => handleAIAnalysis(record)}
          >
            AI分析
          </Button>
          <Button
            icon={<EditOutlined />}
            size="small"
            onClick={() => handleEdit(record)}
          />
          <Button
            danger
            icon={<DeleteOutlined />}
            size="small"
            onClick={() => handleDelete(record.id)}
          />
        </Space>
      ),
    },
  ]

  return (
    <div className="requirement-list-container">
      <Card
        title="需求列表 (Requirement List)"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建需求
          </Button>
        }
      >
        <Table
          columns={columns}
          dataSource={requirements}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
        />
      </Card>

      <Modal
        title={editingRequirement ? '编辑需求' : '新建需求'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        width={700}
        confirmLoading={loading}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="title"
            label="需求标题"
            rules={[{ required: true, message: '请输入需求标题' }]}
          >
            <Input placeholder="请输入需求标题" />
          </Form.Item>

          <Form.Item
            name="description"
            label="需求描述"
            rules={[{ required: true, message: '请输入需求描述' }]}
          >
            <TextArea rows={4} placeholder="请详细描述需求内容" />
          </Form.Item>

          <Form.Item
            name="type"
            label="需求类型"
            rules={[{ required: true, message: '请选择需求类型' }]}
          >
            <Select placeholder="请选择需求类型">
              <Option value="FUNCTIONAL">功能需求</Option>
              <Option value="NON_FUNCTIONAL">非功能需求</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="priority"
            label="优先级"
            rules={[{ required: true, message: '请选择优先级' }]}
          >
            <Select placeholder="请选择优先级">
              <Option value="P0">P0 - 最高</Option>
              <Option value="P1">P1 - 高</Option>
              <Option value="P2">P2 - 中</Option>
              <Option value="P3">P3 - 低</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="modules"
            label="关联模块"
            rules={[{ required: true, message: '请选择关联模块' }]}
          >
            <Select mode="multiple" placeholder="请选择关联模块">
              <Option value="用户管理">用户管理</Option>
              <Option value="订单管理">订单管理</Option>
              <Option value="支付系统">支付系统</Option>
              <Option value="安全认证">安全认证</Option>
              <Option value="数据库">数据库</Option>
              <Option value="性能优化">性能优化</Option>
              <Option value="第三方集成">第三方集成</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select placeholder="请选择状态">
              <Option value="DRAFT">草稿</Option>
              <Option value="APPROVED">已批准</Option>
              <Option value="REJECTED">已拒绝</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default RequirementList
