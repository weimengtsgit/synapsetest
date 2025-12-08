import React, { useState } from 'react';
import {
  Form,
  Input,
  Button,
  Upload,
  Table,
  message,
  Card,
  InputNumber,
  Space,
  Checkbox,
  Tag,
  Modal,
  Spin,
} from 'antd';
import {
  UploadOutlined,
  PlusOutlined,
  DeleteOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import aiService, { AIGenerationRequest } from '../../services/aiService';

const { TextArea } = Input;

interface Requirement {
  id: number;
  module: string;
  requirement: string;
}

/**
 * Batch Generate Test Cases
 * Allows users to generate test cases for multiple requirements at once
 *
 * Features:
 * - Upload requirements from Excel/CSV
 * - Manually add requirements
 * - Configure generation settings
 * - View and manage results
 */
const BatchGenerate: React.FC = () => {
  const [form] = Form.useForm();
  const [requirements, setRequirements] = useState<Requirement[]>([]);
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<any>(null);
  const [nextId, setNextId] = useState(1);

  /**
   * Handle file upload
   */
  const handleUpload = (file: File) => {
    message.info('文件上传功能正在开发中，请先手动添加需求');
    // TODO: Parse Excel/CSV file and extract requirements
    return false;  // Prevent automatic upload
  };

  /**
   * Add a new empty requirement
   */
  const handleAddRequirement = () => {
    setRequirements([
      ...requirements,
      {
        id: nextId,
        module: '',
        requirement: '',
      },
    ]);
    setNextId(nextId + 1);
  };

  /**
   * Update requirement field
   */
  const handleUpdateRequirement = (id: number, field: keyof Requirement, value: string) => {
    setRequirements(requirements.map(req =>
      req.id === id ? { ...req, [field]: value } : req
    ));
  };

  /**
   * Remove requirement
   */
  const handleRemoveRequirement = (id: number) => {
    setRequirements(requirements.filter(req => req.id !== id));
  };

  /**
   * Batch generate test cases
   */
  const handleBatchGenerate = async (values: any) => {
    if (requirements.length === 0) {
      message.warning('请先添加需求');
      return;
    }

    // Validate all requirements have content
    const invalidReqs = requirements.filter(req => !req.requirement.trim());
    if (invalidReqs.length > 0) {
      message.error('所有需求必须填写内容');
      return;
    }

    setLoading(true);

    try {
      // Build batch request
      const requests: AIGenerationRequest[] = requirements.map(req => ({
        input: req.requirement,
        testType: 'FUNCTIONAL',
        tags: req.module ? [req.module] : [],
      }));

      const response = await aiService.batchGenerate({ requests });

      setResults(response);

      message.success(
        `批量生成完成！共生成 ${response.total_cases || response.results?.length || 0} 条用例`
      );

    } catch (error: any) {
      message.error(`批量生成失败: ${error.message}`);
      console.error('Batch generation error:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Clear all requirements
   */
  const handleClear = () => {
    Modal.confirm({
      title: '确认清空',
      content: '确定要清空所有需求吗？',
      onOk: () => {
        setRequirements([]);
        setResults(null);
      },
    });
  };

  /**
   * Requirement table columns
   */
  const requirementColumns = [
    {
      title: '序号',
      key: 'index',
      width: 60,
      render: (_: any, __: any, index: number) => index + 1,
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
      width: 150,
      render: (text: string, record: Requirement) => (
        <Input
          value={text}
          placeholder="输入模块名称"
          onChange={(e) => handleUpdateRequirement(record.id, 'module', e.target.value)}
        />
      ),
    },
    {
      title: '需求描述',
      dataIndex: 'requirement',
      key: 'requirement',
      render: (text: string, record: Requirement) => (
        <TextArea
          value={text}
          placeholder="输入需求描述..."
          autoSize={{ minRows: 2, maxRows: 4 }}
          onChange={(e) => handleUpdateRequirement(record.id, 'requirement', e.target.value)}
        />
      ),
    },
    {
      title: '操作',
      key: 'actions',
      width: 100,
      render: (_: any, record: Requirement) => (
        <Button
          type="link"
          danger
          icon={<DeleteOutlined />}
          onClick={() => handleRemoveRequirement(record.id)}
        >
          删除
        </Button>
      ),
    },
  ];

  /**
   * Result table columns
   */
  const resultColumns = [
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
    },
    {
      title: '生成数量',
      dataIndex: 'count',
      key: 'count',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'success' ? 'green' : 'red'}>
          {status === 'success' ? '成功' : '失败'}
        </Tag>
      ),
    },
    {
      title: '生成时间',
      dataIndex: 'generation_time',
      key: 'generation_time',
      render: (time: number) => `${time?.toFixed(1)}s`,
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Card
        title={
          <Space>
            <ThunderboltOutlined />
            <span>批量生成测试用例</span>
          </Space>
        }
      >
        <Form form={form} layout="vertical" onFinish={handleBatchGenerate}>
          {/* File Upload Section */}
          <Form.Item
            label="导入需求文件"
            help="支持Excel (.xlsx, .xls) 或 CSV文件"
          >
            <Upload
              beforeUpload={handleUpload}
              accept=".xlsx,.xls,.csv"
              maxCount={1}
              showUploadList={false}
            >
              <Button icon={<UploadOutlined />}>上传Excel文件</Button>
            </Upload>
          </Form.Item>

          {/* Manual Add Section */}
          <Form.Item label="需求列表">
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              <Space>
                <Button
                  type="dashed"
                  icon={<PlusOutlined />}
                  onClick={handleAddRequirement}
                >
                  手动添加需求
                </Button>
                {requirements.length > 0 && (
                  <Button danger onClick={handleClear}>
                    清空列表
                  </Button>
                )}
              </Space>

              {requirements.length > 0 && (
                <Table
                  dataSource={requirements}
                  columns={requirementColumns}
                  rowKey="id"
                  pagination={false}
                  size="small"
                />
              )}
            </Space>
          </Form.Item>

          {/* Generation Settings */}
          {requirements.length > 0 && (
            <Card type="inner" title="生成配置" style={{ marginBottom: 16 }}>
              <Form.Item
                label="每个需求生成用例数"
                name="numCases"
                initialValue={5}
              >
                <InputNumber min={1} max={20} />
              </Form.Item>

              <Form.Item name="includeEdgeCases" valuePropName="checked" initialValue={true}>
                <Checkbox>包含边界场景</Checkbox>
              </Form.Item>

              <Form.Item name="enableDedup" valuePropName="checked" initialValue={true}>
                <Checkbox>自动去重</Checkbox>
              </Form.Item>

              <Form.Item name="enablePrioritize" valuePropName="checked" initialValue={true}>
                <Checkbox>自动优先级排序</Checkbox>
              </Form.Item>
            </Card>
          )}

          {/* Submit Button */}
          {requirements.length > 0 && (
            <Form.Item>
              <Space>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  icon={<ThunderboltOutlined />}
                  size="large"
                >
                  批量生成 ({requirements.length} 个需求)
                </Button>
              </Space>
            </Form.Item>
          )}
        </Form>

        {/* Results Section */}
        {loading && (
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <Spin size="large" />
            <div style={{ marginTop: 16, color: '#888' }}>
              AI正在批量生成测试用例，请稍候...
            </div>
          </div>
        )}

        {results && !loading && (
          <div style={{ marginTop: 32 }}>
            <Card title="生成结果" type="inner">
              <div style={{ marginBottom: 16 }}>
                <Space size="large">
                  <Statistic title="总需求数" value={results.total_requests || 0} />
                  <Statistic title="总用例数" value={results.total_cases || 0} />
                  <Statistic
                    title="成功率"
                    value={
                      results.results
                        ? (results.results.filter((r: any) => r.success).length /
                            results.results.length) *
                          100
                        : 0
                    }
                    precision={1}
                    suffix="%"
                  />
                  <Statistic
                    title="总耗时"
                    value={results.total_time?.toFixed(1) || 0}
                    suffix="s"
                  />
                </Space>
              </div>

              {results.results && (
                <Table
                  dataSource={results.results}
                  columns={resultColumns}
                  rowKey={(record, index) => index}
                  pagination={false}
                  size="small"
                />
              )}
            </Card>
          </div>
        )}
      </Card>
    </div>
  );
};

// Add Statistic component
const Statistic = ({ title, value, suffix, precision }: any) => (
  <div>
    <div style={{ fontSize: 12, color: '#888', marginBottom: 4 }}>{title}</div>
    <div style={{ fontSize: 24, fontWeight: 'bold' }}>
      {typeof value === 'number' && precision
        ? value.toFixed(precision)
        : value}
      {suffix && <span style={{ fontSize: 14, marginLeft: 4 }}>{suffix}</span>}
    </div>
  </div>
);

export default BatchGenerate;
