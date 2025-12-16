import React, { useState, useEffect } from 'react'
import {
  Card,
  Table,
  Tag,
  Button,
  message,
  Space,
  Input,
  Modal,
  Descriptions,
  Pagination,
  Spin,
} from 'antd'
import {
  EyeOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import axios from 'axios'
import type { ColumnsType } from 'antd/es/table'
import { getPriorityColor, convertPriorityToLabel } from '../../utils/priorityUtils'
import './GenerationHistory.css'

const { Search } = Input

interface HistoryRecord {
  id: string
  request_id: string
  module: string
  testcase_name: string
  type: string
  priority: string
  tags: string[]
  generated_at: string
  description: string
  steps: Array<{ step: number; action: string; expected: string }>
  preconditions: string[]
}

/**
 * 生成历史组件
 * 展示AI生成的测试用例历史记录
 */
const GenerationHistory: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [records, setRecords] = useState<HistoryRecord[]>([])
  const [total, setTotal] = useState(0)
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [moduleFilter, setModuleFilter] = useState('')
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [selectedRecord, setSelectedRecord] = useState<HistoryRecord | null>(null)

  // 加载历史记录
  const loadHistory = async (page: number = currentPage, module: string = moduleFilter) => {
    setLoading(true)
    try {
      const offset = (page - 1) * pageSize
      const params: any = {
        limit: pageSize,
        offset: offset,
      }
      if (module) {
        params.module = module
      }

      const response = await axios.get('/api/v1/ai/testcase/history', { params })

      console.log('生成历史响应:', response.data)

      // 后端返回格式: { success: true, data: { success: true, total, records: [...] } }
      const actualData = response.data.data || response.data

      if (actualData.success) {
        setRecords(actualData.records || [])
        setTotal(actualData.total || 0)
      } else {
        message.error('加载失败: ' + (actualData.error || actualData.message || '未知错误'))
      }
    } catch (error: any) {
      console.error('加载历史记录失败:', error)
      message.error('加载失败: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
    }
  }

  // 初始加载
  useEffect(() => {
    loadHistory()
  }, [])

  // 刷新
  const handleRefresh = () => {
    setCurrentPage(1)
    loadHistory(1, moduleFilter)
  }

  // 搜索
  const handleSearch = (value: string) => {
    setModuleFilter(value)
    setCurrentPage(1)
    loadHistory(1, value)
  }

  // 分页变化
  const handlePageChange = (page: number, pageSize: number) => {
    setCurrentPage(page)
    setPageSize(pageSize)
    loadHistory(page, moduleFilter)
  }

  // 查看详情
  const handleViewDetail = (record: HistoryRecord) => {
    setSelectedRecord(record)
    setDetailModalVisible(true)
  }

  // 类型颜色
  const getTypeColor = (type: string) => {
    const colorMap: Record<string, string> = {
      '功能测试': 'blue',
      '性能测试': 'orange',
      '安全测试': 'red',
      'FUNCTIONAL': 'blue',
      'PERFORMANCE': 'orange',
      'SECURITY': 'red',
    }
    return colorMap[type] || 'default'
  }

  // 表格列配置
  const columns: ColumnsType<HistoryRecord> = [
    {
      title: '用例名称',
      dataIndex: 'testcase_name',
      key: 'testcase_name',
      width: 250,
      ellipsis: true,
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
      width: 150,
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: string) => <Tag color={getTypeColor(type)}>{type}</Tag>,
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 100,
      render: (priority: string | number) => {
        // 统一转换：支持数字（0-10）和字符串（P0-P3）两种格式
        const label = typeof priority === 'number' 
          ? convertPriorityToLabel(priority) 
          : priority
        return <Tag color={getPriorityColor(priority)}>{label}</Tag>
      },
    },
    {
      title: '标签',
      dataIndex: 'tags',
      key: 'tags',
      width: 200,
      render: (tags: string[]) => (
        <>
          {tags && tags.slice(0, 2).map((tag, idx) => (
            <Tag key={idx}>{tag}</Tag>
          ))}
          {tags && tags.length > 2 && <Tag>+{tags.length - 2}</Tag>}
        </>
      ),
    },
    {
      title: '生成时间',
      dataIndex: 'generated_at',
      key: 'generated_at',
      width: 180,
      render: (date: string) => {
        if (!date) return '-'
        try {
          return new Date(date).toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false
          })
        } catch (e) {
          return date
        }
      },
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 100,
      render: (_: any, record: HistoryRecord) => (
        <Button
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => handleViewDetail(record)}
        >
          查看
        </Button>
      ),
    },
  ]

  return (
    <div className="generation-history-container">
      <h2 style={{ marginBottom: 24 }}>生成历史</h2>

      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <Space>
            <Search
              placeholder="按模块名称搜索"
              allowClear
              onSearch={handleSearch}
              style={{ width: 250 }}
              prefix={<SearchOutlined />}
            />
          </Space>
          <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
            刷新
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={records}
          rowKey="id"
          loading={loading}
          pagination={false}
          scroll={{ x: 1200 }}
        />

        <div style={{ marginTop: 16, textAlign: 'right' }}>
          <Pagination
            current={currentPage}
            pageSize={pageSize}
            total={total}
            onChange={handlePageChange}
            onShowSizeChange={handlePageChange}
            showSizeChanger
            showQuickJumper
            showTotal={(total) => `共 ${total} 条记录`}
            pageSizeOptions={['10', '20', '50', '100']}
          />
        </div>
      </Card>

      {/* 详情弹窗 */}
      <Modal
        title={`测试用例详情 - ${selectedRecord?.testcase_name}`}
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>,
        ]}
        width={800}
      >
        {selectedRecord && (
          <div>
            <Descriptions bordered column={2}>
              <Descriptions.Item label="用例ID">
                {selectedRecord.id}
              </Descriptions.Item>
              <Descriptions.Item label="请求ID">
                {selectedRecord.request_id}
              </Descriptions.Item>
              <Descriptions.Item label="模块">
                {selectedRecord.module}
              </Descriptions.Item>
              <Descriptions.Item label="类型">
                <Tag color={getTypeColor(selectedRecord.type)}>
                  {selectedRecord.type}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="优先级">
                <Tag color={getPriorityColor(selectedRecord.priority)}>
                  {typeof selectedRecord.priority === 'number' 
                    ? convertPriorityToLabel(selectedRecord.priority) 
                    : selectedRecord.priority}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="生成时间">
                {selectedRecord.generated_at
                  ? (() => {
                      try {
                        return new Date(selectedRecord.generated_at).toLocaleString('zh-CN', {
                          year: 'numeric',
                          month: '2-digit',
                          day: '2-digit',
                          hour: '2-digit',
                          minute: '2-digit',
                          second: '2-digit',
                          hour12: false
                        })
                      } catch (e) {
                        return selectedRecord.generated_at
                      }
                    })()
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="标签" span={2}>
                {selectedRecord.tags &&
                  selectedRecord.tags.map((tag, idx) => <Tag key={idx}>{tag}</Tag>)}
              </Descriptions.Item>
            </Descriptions>

            <div style={{ marginTop: 24 }}>
              <h4>用例描述:</h4>
              <p>{selectedRecord.description || '无描述'}</p>
            </div>

            {selectedRecord.preconditions && selectedRecord.preconditions.length > 0 && (
              <div style={{ marginTop: 24 }}>
                <h4>前置条件:</h4>
                <ul>
                  {selectedRecord.preconditions.map((condition, idx) => (
                    <li key={idx}>{condition}</li>
                  ))}
                </ul>
              </div>
            )}

            <div style={{ marginTop: 24 }}>
              <h4>测试步骤:</h4>
              {selectedRecord.steps && selectedRecord.steps.length > 0 ? (
                <ol style={{ paddingLeft: 20 }}>
                  {selectedRecord.steps.map((step, idx) => (
                    <li key={idx} style={{ marginBottom: 8 }}>
                      执行：{step.action}。预期结果：{step.expected}。
                    </li>
                  ))}
                </ol>
              ) : (
                <p>无测试步骤</p>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default GenerationHistory
