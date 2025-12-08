import React, { useState, useEffect } from 'react'
import {
  Table,
  Tag,
  Button,
  Space,
  message,
  Card,
  Modal,
  Descriptions,
  Popconfirm,
  Form,
  Input,
  DatePicker,
} from 'antd'
import {
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
  SyncOutlined,
} from '@ant-design/icons'
import versionService from '../../services/versionService'
import dayjs from 'dayjs'

const { TextArea } = Input

/**
 * Test Version List Component
 * User Story 1: 智能测试任务调度 - 测试版本列表页面
 */
const TestVersionList = () => {
  const [versions, setVersions] = useState([])
  const [loading, setLoading] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [editModalVisible, setEditModalVisible] = useState(false)
  const [selectedVersion, setSelectedVersion] = useState(null)
  const [form] = Form.useForm()

  useEffect(() => {
    loadVersions()
  }, [])

  const loadVersions = async () => {
    setLoading(true)
    try {
      const response = await versionService.getAllVersions()

      if (response.success) {
        const versionsData = response.data.content || response.data
        setVersions(Array.isArray(versionsData) ? versionsData : [])
      } else {
        setVersions(Array.isArray(response) ? response : [])
      }
    } catch (error) {
      message.error(error.message || 'Failed to load test versions')
    } finally {
      setLoading(false)
    }
  }

  const handleViewDetails = (version) => {
    setSelectedVersion(version)
    setDetailModalVisible(true)
  }

  const handleEdit = (version) => {
    setSelectedVersion(version)
    form.setFieldsValue({
      ...version,
      releaseDate: version.releaseDate ? dayjs(version.releaseDate) : null,
    })
    setEditModalVisible(true)
  }

  const handleDelete = async (versionId) => {
    try {
      await versionService.deleteVersion(versionId)
      message.success('测试版本已删除 (Test version deleted)')
      loadVersions()
    } catch (error) {
      message.error('删除失败: ' + (error.message || 'Delete failed'))
    }
  }

  const handleEditSubmit = async () => {
    try {
      const values = await form.validateFields()
      const submitData = {
        ...values,
        releaseDate: values.releaseDate ? values.releaseDate.format('YYYY-MM-DD') : null,
      }

      await versionService.updateVersion(selectedVersion.id, submitData)
      message.success('测试版本已更新 (Test version updated)')
      setEditModalVisible(false)
      loadVersions()
      form.resetFields()
    } catch (error) {
      if (error.errorFields) {
        message.error('请检查表单填写 (Please check the form)')
      } else {
        message.error('更新失败: ' + (error.message || 'Update failed'))
      }
    }
  }

  const handleCreate = () => {
    setSelectedVersion(null)
    form.resetFields()
    setEditModalVisible(true)
  }

  const handleCreateSubmit = async () => {
    try {
      const values = await form.validateFields()
      const submitData = {
        ...values,
        releaseDate: values.releaseDate ? values.releaseDate.format('YYYY-MM-DD') : null,
      }

      if (selectedVersion) {
        await versionService.updateVersion(selectedVersion.id, submitData)
        message.success('测试版本已更新 (Test version updated)')
      } else {
        await versionService.createVersion(submitData)
        message.success('测试版本已创建 (Test version created)')
      }

      setEditModalVisible(false)
      loadVersions()
      form.resetFields()
    } catch (error) {
      if (error.errorFields) {
        message.error('请检查表单填写 (Please check the form)')
      } else {
        message.error('操作失败: ' + (error.message || 'Operation failed'))
      }
    }
  }

  const columns = [
    {
      title: '版本名称 (Version Name)',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '产品版本 (Product Version)',
      dataIndex: 'productVersion',
      key: 'productVersion',
      width: 150,
    },
    {
      title: '发布日期 (Release Date)',
      dataIndex: 'releaseDate',
      key: 'releaseDate',
      width: 150,
      render: (date) => (date ? new Date(date).toLocaleDateString('zh-CN') : '-'),
    },
    {
      title: '描述 (Description)',
      dataIndex: 'description',
      key: 'description',
      width: 300,
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
      width: 200,
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
          <Button
            type="link"
            icon={<EditOutlined />}
            size="small"
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个测试版本吗？"
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
        title="测试版本列表 (Test Version List)"
        bordered={false}
        extra={
          <Space>
            <Button icon={<SyncOutlined />} onClick={loadVersions}>
              刷新 (Refresh)
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              新增版本 (New Version)
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={versions}
          loading={loading}
          rowKey="id"
          scroll={{ x: 1200 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条测试版本 (Total ${total} versions)`,
          }}
        />
      </Card>

      {/* Detail Modal */}
      <Modal
        title={`测试版本详情 - ${selectedVersion?.name}`}
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
              handleEdit(selectedVersion)
            }}
          >
            编辑 (Edit)
          </Button>,
        ]}
        width={800}
      >
        {selectedVersion && (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="版本ID (ID)" span={2}>
              {selectedVersion.id}
            </Descriptions.Item>
            <Descriptions.Item label="版本名称 (Name)" span={2}>
              {selectedVersion.name}
            </Descriptions.Item>
            <Descriptions.Item label="产品版本 (Product Version)" span={2}>
              {selectedVersion.productVersion}
            </Descriptions.Item>
            <Descriptions.Item label="发布日期 (Release Date)" span={2}>
              {selectedVersion.releaseDate
                ? new Date(selectedVersion.releaseDate).toLocaleDateString('zh-CN')
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="描述 (Description)" span={2}>
              {selectedVersion.description || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间 (Created At)">
              {selectedVersion.createdAt
                ? new Date(selectedVersion.createdAt).toLocaleString('zh-CN')
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="更新时间 (Updated At)">
              {selectedVersion.updatedAt
                ? new Date(selectedVersion.updatedAt).toLocaleString('zh-CN')
                : '-'}
            </Descriptions.Item>
            {selectedVersion.config && Object.keys(selectedVersion.config).length > 0 && (
              <Descriptions.Item label="配置信息 (Config)" span={2}>
                <pre style={{ margin: 0 }}>
                  {JSON.stringify(selectedVersion.config, null, 2)}
                </pre>
              </Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>

      {/* Edit/Create Modal */}
      <Modal
        title={selectedVersion ? '编辑测试版本 (Edit Version)' : '新增测试版本 (New Version)'}
        open={editModalVisible}
        onCancel={() => {
          setEditModalVisible(false)
          form.resetFields()
        }}
        onOk={handleCreateSubmit}
        width={600}
        okText={selectedVersion ? '更新 (Update)' : '创建 (Create)'}
        cancelText="取消 (Cancel)"
      >
        <Form form={form} layout="vertical" style={{ marginTop: 24 }}>
          <Form.Item
            name="name"
            label="版本名称 (Version Name)"
            rules={[{ required: true, message: '请输入版本名称 (Please enter version name)' }]}
          >
            <Input placeholder="例如: v1.0.0-test" />
          </Form.Item>
          <Form.Item
            name="productVersion"
            label="产品版本 (Product Version)"
            rules={[{ required: true, message: '请输入产品版本 (Please enter product version)' }]}
          >
            <Input placeholder="例如: v1.0.0" />
          </Form.Item>
          <Form.Item name="releaseDate" label="发布日期 (Release Date)">
            <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
          </Form.Item>
          <Form.Item name="description" label="描述 (Description)">
            <TextArea rows={4} placeholder="请输入版本描述信息" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default TestVersionList
