import React, { useState } from 'react'
import {
  Card,
  Button,
  Table,
  message,
  Upload,
  Modal,
  Form,
  Input,
  InputNumber,
  Checkbox,
  Space,
  Popconfirm,
} from 'antd'
import {
  UploadOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  InboxOutlined,
} from '@ant-design/icons'
import type { UploadProps } from 'antd'
import * as XLSX from 'xlsx'
import axios from 'axios'
import './BatchGenerate.css'

const { Dragger } = Upload

interface Requirement {
  key: number
  moduleName: string
  system: string
  numCases: number
  requirementText?: string
}

/**
 * 批量测试用例生成组件
 * 参考 frontend/docs/UI原型演示.html 中的批量生成页面设计
 */
const BatchGenerate: React.FC = () => {
  const [requirements, setRequirements] = useState<Requirement[]>([])
  const [nextKey, setNextKey] = useState(1)
  const [addModalVisible, setAddModalVisible] = useState(false)
  const [editModalVisible, setEditModalVisible] = useState(false)
  const [currentRequirement, setCurrentRequirement] = useState<Requirement | null>(null)
  const [form] = Form.useForm()

  // 配置参数
  const [applyAdvanced, setApplyAdvanced] = useState(true)
  const [autoDeduplicate, setAutoDeduplicate] = useState(true)
  const [prioritySort, setPrioritySort] = useState(true)
  const [maxCases, setMaxCases] = useState(100)
  const [loading, setLoading] = useState(false)

  // 文件上传配置
  const uploadProps: UploadProps = {
    name: 'file',
    multiple: false,
    accept: '.xlsx,.xls',
    beforeUpload: (file) => {
      const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
                      file.type === 'application/vnd.ms-excel'
      if (!isExcel) {
        message.error('只能上传 .xlsx 或 .xls 格式的文件!')
        return false
      }

      // 解析Excel文件
      const reader = new FileReader()
      reader.onload = (e) => {
        try {
          const data = e.target?.result
          const workbook = XLSX.read(data, { type: 'binary' })

          // 读取第一个sheet
          const firstSheetName = workbook.SheetNames[0]
          const worksheet = workbook.Sheets[firstSheetName]

          // 转换为JSON
          const jsonData = XLSX.utils.sheet_to_json(worksheet)

          if (jsonData.length === 0) {
            message.warning('Excel文件中没有数据')
            return
          }

          // 解析数据并添加到需求列表
          const newRequirements: Requirement[] = []
          let currentKey = nextKey

          jsonData.forEach((row: any) => {
            const moduleName = row['模块名称'] || row['moduleName']
            const system = row['所属系统'] || row['system']
            const numCases = row['生成数量'] || row['numCases'] || 10

            if (moduleName && system) {
              newRequirements.push({
                key: currentKey++,
                moduleName: String(moduleName),
                system: String(system),
                numCases: Number(numCases),
              })
            }
          })

          if (newRequirements.length === 0) {
            message.warning('未能从Excel中解析出有效数据，请检查文件格式')
            return
          }

          setRequirements([...requirements, ...newRequirements])
          setNextKey(currentKey)
          message.success(`成功导入 ${newRequirements.length} 条需求`)
        } catch (error: any) {
          console.error('解析Excel文件失败:', error)
          message.error('解析Excel文件失败: ' + error.message)
        }
      }

      reader.readAsBinaryString(file)
      return false  // 阻止自动上传
    },
    onChange(info) {
      const { status } = info.file
      if (status === 'done') {
        message.success(`${info.file.name} 文件上传成功`)
      } else if (status === 'error') {
        message.error(`${info.file.name} 文件上传失败`)
      }
    },
  }

  // 下载模板
  const handleDownloadTemplate = () => {
    try {
      // 创建模板数据
      const templateData = [
        {
          '模块名称': '用户登录模块',
          '所属系统': '用户认证',
          '生成数量': 10,
        },
        {
          '模块名称': '支付功能模块',
          '所属系统': '支付系统',
          '生成数量': 15,
        },
        {
          '模块名称': '订单管理模块',
          '所属系统': '订单中心',
          '生成数量': 12,
        },
      ]

      // 创建工作簿
      const worksheet = XLSX.utils.json_to_sheet(templateData)

      // 设置列宽
      worksheet['!cols'] = [
        { wch: 20 }, // 模块名称
        { wch: 15 }, // 所属系统
        { wch: 12 }, // 生成数量
      ]

      const workbook = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(workbook, worksheet, '批量生成模板')

      // 添加说明sheet
      const instructionData = [
        { '说明': '本模板用于批量生成测试用例' },
        { '说明': '' },
        { '说明': '字段说明：' },
        { '说明': '1. 模块名称：必填，测试模块的名称，如"用户登录模块"' },
        { '说明': '2. 所属系统：必填，模块所属的系统名称，如"用户认证"' },
        { '说明': '3. 生成数量：必填，需要生成的测试用例数量（1-50）' },
        { '说明': '' },
        { '说明': '使用方法：' },
        { '说明': '1. 请参考示例数据填写您的需求' },
        { '说明': '2. 可以删除示例数据，添加您自己的需求' },
        { '说明': '3. 填写完成后保存文件' },
        { '说明': '4. 在批量生成页面上传该文件' },
      ]
      const instructionSheet = XLSX.utils.json_to_sheet(instructionData)
      instructionSheet['!cols'] = [{ wch: 60 }]
      XLSX.utils.book_append_sheet(workbook, instructionSheet, '使用说明')

      // 生成Excel文件并下载
      XLSX.writeFile(workbook, '测试用例批量生成模板.xlsx')

      message.success('模板下载成功')
    } catch (error: any) {
      console.error('模板下载失败:', error)
      message.error('模板下载失败: ' + error.message)
    }
  }

  // 使用示例
  const handleUseExample = () => {
    const examples: Requirement[] = [
      { key: nextKey, moduleName: '用户登录模块', system: '用户认证', numCases: 10 },
      { key: nextKey + 1, moduleName: '支付功能模块', system: '支付系统', numCases: 15 },
      { key: nextKey + 2, moduleName: '订单管理模块', system: '订单中心', numCases: 12 },
    ]
    setRequirements(examples)
    setNextKey(nextKey + 3)
    message.success('已加载示例数据')
  }

  // 打开添加需求弹窗
  const handleOpenAddModal = () => {
    form.resetFields()
    setAddModalVisible(true)
  }

  // 添加需求
  const handleAddRequirement = () => {
    form.validateFields().then((values) => {
      const newRequirement: Requirement = {
        key: nextKey,
        moduleName: values.moduleName,
        system: values.system,
        numCases: values.numCases || 10,
      }
      setRequirements([...requirements, newRequirement])
      setNextKey(nextKey + 1)
      setAddModalVisible(false)
      message.success('需求添加成功')
    })
  }

  // 打开编辑需求弹窗
  const handleOpenEditModal = (record: Requirement) => {
    setCurrentRequirement(record)
    form.setFieldsValue(record)
    setEditModalVisible(true)
  }

  // 编辑需求
  const handleEditRequirement = () => {
    form.validateFields().then((values) => {
      setRequirements(requirements.map(req =>
        req.key === currentRequirement?.key
          ? { ...req, ...values }
          : req
      ))
      setEditModalVisible(false)
      message.success('需求更新成功')
    })
  }

  // 删除需求
  const handleDeleteRequirement = (key: number) => {
    setRequirements(requirements.filter(req => req.key !== key))
    message.success('需求删除成功')
  }

  // 全部删除
  const handleDeleteAll = () => {
    setRequirements([])
    message.success('已清空所有需求')
  }

  // 开始批量生成
  const handleBatchGenerate = async () => {
    if (requirements.length === 0) {
      message.warning('请至少添加一个需求')
      return
    }

    setLoading(true)
    try {
      // 构建批量生成请求参数
      const batchRequest = {
        requirements: requirements.map((req) => ({
          requirement_text: req.requirementText || `${req.system} - ${req.moduleName}的功能需求`,
          module: req.moduleName,
          num_cases: req.numCases,
          include_edge_cases: true,
          optimization: applyAdvanced ? {
            deduplicate: autoDeduplicate,
            prioritize: prioritySort,
            min_priority: null,
            max_cases: maxCases,
          } : null,
        })),
      }

      console.log('批量生成请求:', batchRequest)

      // 调用后端接口
      const response = await axios.post('/api/v1/ai/testcase/generate/batch', batchRequest)

      console.log('批量生成响应:', response.data)

      // 后端返回格式: { success: true, data: { success: true, total_requests: 3, successful: 3, failed: 0, results: [...] } }
      const actualData = response.data.data || response.data

      if (actualData.success) {
        const { total_requests, successful, failed } = actualData
        message.success(
          `批量生成完成！共处理 ${total_requests} 个需求，成功 ${successful} 个，失败 ${failed} 个`
        )

        // TODO: 展示生成结果，可以跳转到结果页面或显示弹窗
        // 这里可以添加逻辑来展示每个需求的生成结果
      } else {
        message.error('批量生成失败: ' + (actualData.error || '未知错误'))
      }
    } catch (error: any) {
      console.error('批量生成失败:', error)
      message.error('批量生成失败: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }

  // 表格列配置
  const columns = [
    {
      title: '序号',
      dataIndex: 'key',
      key: 'key',
      width: 80,
      render: (_: any, __: any, index: number) => index + 1,
    },
    {
      title: '模块名称',
      dataIndex: 'moduleName',
      key: 'moduleName',
      width: 200,
    },
    {
      title: '所属系统',
      dataIndex: 'system',
      key: 'system',
      width: 150,
    },
    {
      title: '生成数量',
      dataIndex: 'numCases',
      key: 'numCases',
      width: 120,
      render: (num: number) => `${num}条`,
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: Requirement) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleOpenEditModal(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个需求吗？"
            onConfirm={() => handleDeleteRequirement(record.key)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="link"
              danger
              size="small"
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div className="batch-generate-container">
      <h2 style={{ marginBottom: 24 }}>批量测试用例生成</h2>

      {/* 上传需求区域 */}
      <Card
        title="批量上传需求"
        extra={
          <Space>
            <Button onClick={handleDownloadTemplate}>📥 下载模板</Button>
            <Button onClick={handleUseExample}>📝 使用示例</Button>
          </Space>
        }
      >
        <Dragger {...uploadProps}>
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">点击选择文件或拖拽文件到此处</p>
          <p className="ant-upload-hint">支持 .xlsx, .xls 格式</p>
        </Dragger>

        <div style={{ textAlign: 'center', margin: '24px 0', color: '#8c8c8c' }}>
          或
        </div>

        <div style={{ textAlign: 'center' }}>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleOpenAddModal}
          >
            手动添加需求
          </Button>
        </div>
      </Card>

      {/* 需求列表 */}
      <Card
        title={`需求列表 (已添加 ${requirements.length} 个需求)`}
        extra={
          requirements.length > 0 && (
            <Popconfirm
              title="确定要清空所有需求吗？"
              onConfirm={handleDeleteAll}
              okText="确定"
              cancelText="取消"
            >
              <Button danger>全部删除</Button>
            </Popconfirm>
          )
        }
        style={{ marginTop: 24 }}
      >
        <Table
          columns={columns}
          dataSource={requirements}
          pagination={false}
          locale={{ emptyText: '暂无需求，请添加需求' }}
        />

        {/* 配置参数 */}
        {requirements.length > 0 && (
          <div style={{ marginTop: 24 }}>
            <Space size="middle">
              <Checkbox
                checked={applyAdvanced}
                onChange={(e) => setApplyAdvanced(e.target.checked)}
              >
                统一应用高级配置
              </Checkbox>
              <Checkbox
                checked={autoDeduplicate}
                onChange={(e) => setAutoDeduplicate(e.target.checked)}
              >
                自动去重
              </Checkbox>
              <Checkbox
                checked={prioritySort}
                onChange={(e) => setPrioritySort(e.target.checked)}
              >
                优先级排序
              </Checkbox>
              <span>最大用例数:</span>
              <InputNumber
                min={1}
                max={1000}
                value={maxCases}
                onChange={(value) => setMaxCases(value || 100)}
                style={{ width: 80 }}
              />
            </Space>
          </div>
        )}

        {/* 操作按钮 */}
        {requirements.length > 0 && (
          <div style={{ marginTop: 24, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setRequirements([])}>取消</Button>
              <Button
                type="primary"
                size="large"
                loading={loading}
                onClick={handleBatchGenerate}
              >
                开始批量生成 ({requirements.length}个任务)
              </Button>
            </Space>
          </div>
        )}
      </Card>

      {/* 添加需求弹窗 */}
      <Modal
        title="添加需求"
        open={addModalVisible}
        onOk={handleAddRequirement}
        onCancel={() => setAddModalVisible(false)}
        okText="添加"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="模块名称"
            name="moduleName"
            rules={[{ required: true, message: '请输入模块名称' }]}
          >
            <Input placeholder="例如：用户登录模块" />
          </Form.Item>
          <Form.Item
            label="所属系统"
            name="system"
            rules={[{ required: true, message: '请输入所属系统' }]}
          >
            <Input placeholder="例如：用户认证" />
          </Form.Item>
          <Form.Item
            label="生成数量"
            name="numCases"
            initialValue={10}
            rules={[{ required: true, message: '请输入生成数量' }]}
          >
            <InputNumber
              min={1}
              max={50}
              style={{ width: '100%' }}
              placeholder="1-50"
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 编辑需求弹窗 */}
      <Modal
        title="编辑需求"
        open={editModalVisible}
        onOk={handleEditRequirement}
        onCancel={() => setEditModalVisible(false)}
        okText="保存"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="模块名称"
            name="moduleName"
            rules={[{ required: true, message: '请输入模块名称' }]}
          >
            <Input placeholder="例如：用户登录模块" />
          </Form.Item>
          <Form.Item
            label="所属系统"
            name="system"
            rules={[{ required: true, message: '请输入所属系统' }]}
          >
            <Input placeholder="例如：用户认证" />
          </Form.Item>
          <Form.Item
            label="生成数量"
            name="numCases"
            rules={[{ required: true, message: '请输入生成数量' }]}
          >
            <InputNumber
              min={1}
              max={50}
              style={{ width: '100%' }}
              placeholder="1-50"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default BatchGenerate
