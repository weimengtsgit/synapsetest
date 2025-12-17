import React, { useState, useEffect, useRef } from 'react';
import {
  Form,
  Input,
  Button,
  Card,
  Space,
  InputNumber,
  Select,
  message,
  Spin,
  Tag,
  Divider,
  Alert,
  Row,
  Col,
  Progress,
  Table,
} from 'antd';
import {
  BulbOutlined,
  ThunderboltOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  RocketOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import aiService, { RecommendationRequest } from '../../services/aiService';
import testTaskService from '../../services/testTaskService';

const { TextArea } = Input;
const { Option } = Select;

/**
 * Strategy Recommendation (Enhanced)
 * AI-powered test strategy recommendation based on code changes and context
 *
 * Features:
 * - Input task context with real data sources
 * - Get AI recommendation (test scope, environment, priority)
 * - View detailed reasoning and risk assessment
 * - Create task based on recommendation
 */
const StrategyRecommendation: React.FC = () => {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [recommendation, setRecommendation] = useState<any>(null);
  const [recommendedTestCases, setRecommendedTestCases] = useState<any[]>([]);
  const [testCasesLoading, setTestCasesLoading] = useState(false);
  const [environments, setEnvironments] = useState<any[]>([]);
  const [versions, setVersions] = useState<any[]>([]);
  const [modules, setModules] = useState<string[]>([]);
  const hasFetchedData = useRef(false);

  useEffect(() => {
    // Prevent duplicate API calls in React StrictMode
    if (!hasFetchedData.current) {
      hasFetchedData.current = true;
      loadInitialData();
    }
  }, []);

  /**
   * Load initial data from backend
   */
  const loadInitialData = async () => {
    try {
      const [envResponse, versionResponse, modulesResponse] = await Promise.all([
        testTaskService.getEnvironments(),
        testTaskService.getVersions(),
        testTaskService.getModules(),
      ]);

      setEnvironments(envResponse || []);
      setVersions(versionResponse || []);

      // Handle modules response
      if (modulesResponse && modulesResponse.success && modulesResponse.data) {
        setModules(modulesResponse.data);
      } else if (Array.isArray(modulesResponse)) {
        setModules(modulesResponse);
      } else {
        setModules([]);
      }
    } catch (error) {
      message.error('Failed to load initial data');
      console.error('Error loading initial data:', error);
    }
  };

  /**
   * Get AI recommendation
   */
  const handleGetRecommendation = async (values: any) => {
    setLoading(true);

    try {
      // Build recommendation request
      const request: RecommendationRequest = {
        taskId: values.taskId,
        environmentId: values.environment,
        versionId: values.version,
        context: {
          codeChange: {
            changedFilesCount: values.changedFilesCount || 0,
            changedLinesCount: values.changedLinesCount || 0,
            changedModules: values.modules || [],
          },
          historical: values.recentPassRate
            ? {
                recentPassRate: values.recentPassRate / 100,
                recentDefectCount: values.recentDefectCount || 0,
              }
            : undefined,
          business: values.moduleImportance
            ? {
                moduleImportance: values.moduleImportance / 100,
                deadline: values.deadline,
              }
            : undefined,
        },
      };

      const response = await aiService.getRecommendation(request);

      setRecommendation(response);

      message.success('AI推荐生成成功！');

      // Load recommended test cases based on strategy
      await loadRecommendedTestCases(values, response);
    } catch (error: any) {
      message.error(`获取推荐失败: ${error.message}`);
      console.error('Recommendation error:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Load recommended test cases based on AI strategy
   */
  const loadRecommendedTestCases = async (formValues: any, aiRecommendation: any) => {
    setTestCasesLoading(true);
    try {
      // Build request to preview test cases
      const previewRequest = {
        taskName: formValues.taskId || '策略分析',
        environment: formValues.environment,
        version: formValues.version,
        modules: formValues.modules || [],
        codeChangeInfo: {
          changed_files_count: formValues.changedFilesCount || 0,
          changed_lines_count: formValues.changedLinesCount || 0,
          is_hotfix: false,
          is_critical_module: formValues.moduleImportance >= 80,
        },
      };

      const response = await testTaskService.previewTestCases(previewRequest);

      if (response && response.success && response.data) {
        setRecommendedTestCases(response.data);
        message.info(`根据推荐策略，将执行 ${response.data.length} 个测试用例`);
      } else {
        setRecommendedTestCases([]);
      }
    } catch (error: any) {
      console.error('Failed to load recommended test cases:', error);
      // Don't show error message to user as test cases are optional
      setRecommendedTestCases([]);
    } finally {
      setTestCasesLoading(false);
    }
  };

  /**
   * Reset form and results
   */
  const handleReset = () => {
    form.resetFields();
    setRecommendation(null);
    setRecommendedTestCases([]);
  };

  /**
   * Create task based on recommendation
   */
  const handleCreateTaskWithRecommendation = () => {
    if (!recommendation) {
      message.warning('请先获取AI推荐');
      return;
    }

    const formValues = form.getFieldsValue();

    // Navigate to create task page with recommendation data
    navigate('/test-tasks/create', {
      state: {
        recommendation: recommendation,
        prefillData: {
          taskName: `${formValues.taskId || '测试任务'} - AI推荐`,
          environment: formValues.environment || recommendation.environment,
          version: formValues.version,
          modules: formValues.modules || [],
          changedFilesCount: formValues.changedFilesCount || 0,
          changedLinesCount: formValues.changedLinesCount || 0,
          isHotfix: false,
          isCriticalModule: formValues.moduleImportance >= 80,
        },
      },
    });

    message.success('正在跳转到创建任务页面...');
  };

  /**
   * Get risk level color and icon
   */
  const getRiskInfo = (level: string) => {
    const riskMap: Record<string, { color: string; icon: JSX.Element }> = {
      LOW: {
        color: 'success',
        icon: <CheckCircleOutlined />,
      },
      MEDIUM: {
        color: 'warning',
        icon: <WarningOutlined />,
      },
      HIGH: {
        color: 'error',
        icon: <WarningOutlined />,
      },
    };
    return riskMap[level] || riskMap.MEDIUM;
  };

  /**
   * Get test scope color
   */
  const getScopeColor = (scope: string) => {
    const colorMap: Record<string, string> = {
      SMOKE: 'blue',
      CORE: 'orange',
      FULL: 'red',
    };
    return colorMap[scope] || 'default';
  };

  return (
    <div style={{ padding: '24px' }}>
      <Card
        title={
          <Space>
            <BulbOutlined />
            <span>AI测试策略推荐 (AI Test Strategy Recommendation)</span>
          </Space>
        }
      >
        <Form form={form} layout="vertical" onFinish={handleGetRecommendation}>
          {/* Basic Info */}
          <Card type="inner" title="基本信息 (Basic Info)" style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item
                  label="任务标识 (Task ID)"
                  name="taskId"
                  rules={[{ required: true, message: '请输入任务标识' }]}
                >
                  <Input placeholder="例如: TASK-001 或 订单模块测试" />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item 
                  label="测试环境 (Test Environment)" 
                  name="environment"
                  rules={[{ required: true, message: '请选择测试环境' }]}
                >
                  <Select placeholder="选择测试环境">
                    {environments.map((env) => (
                      <Option key={env.id} value={env.name}>
                        {env.name}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item 
                  label="测试版本 (Test Version)" 
                  name="version"
                  rules={[{ required: true, message: '请选择测试版本' }]}
                >
                  <Select placeholder="选择测试版本">
                    {versions.map((version) => (
                      <Option key={version.id} value={version.name}>
                        {version.name} ({version.productVersion})
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
            </Row>
          </Card>

          {/* Code Change Info */}
          <Card type="inner" title="代码变更信息 (Code Change Info)" style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item 
                  label="变更文件数 (Changed Files)" 
                  name="changedFilesCount"
                  rules={[{ required: true, message: '请输入变更文件数' }]}
                >
                  <InputNumber min={0} style={{ width: '100%' }} placeholder="0" />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item 
                  label="变更行数 (Changed Lines)" 
                  name="changedLinesCount"
                  rules={[{ required: true, message: '请输入变更行数' }]}
                >
                  <InputNumber min={0} style={{ width: '100%' }} placeholder="0" />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label="变更模块 (Changed Modules)"
                  name="modules"
                  rules={[{ required: true, message: '请选择至少一个模块' }]}
                >
                  <Select 
                    mode="multiple" 
                    placeholder="选择变更的模块"
                    maxTagCount="responsive"
                  >
                    {modules.map((module) => (
                      <Option key={module} value={module}>
                        {module}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
            </Row>
          </Card>

          {/* Historical Data */}
          <Card type="inner" title="历史数据（可选 - Optional）" style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item label="最近通过率 (Recent Pass Rate %) " name="recentPassRate">
                  <InputNumber min={0} max={100} style={{ width: '100%' }} placeholder="90" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="最近缺陷数 (Recent Defect Count)" name="recentDefectCount">
                  <InputNumber min={0} style={{ width: '100%' }} placeholder="5" />
                </Form.Item>
              </Col>
            </Row>
          </Card>

          {/* Business Info */}
          <Card type="inner" title="业务信息（可选 - Optional）" style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item label="模块重要性 (Module Importance %) " name="moduleImportance">
                  <InputNumber min={0} max={100} style={{ width: '100%' }} placeholder="80" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="截止日期 (Deadline)" name="deadline">
                  <Input type="date" />
                </Form.Item>
              </Col>
            </Row>
          </Card>

          {/* Actions */}
          <Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                icon={<ThunderboltOutlined />}
                size="large"
              >
                获取AI推荐 (Get Recommendation)
              </Button>
              <Button onClick={handleReset}>重置 (Reset)</Button>
            </Space>
          </Form.Item>
        </Form>

        {/* Loading State */}
        {loading && (
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <Spin size="large" />
            <div style={{ marginTop: 16, color: '#888' }}>
              AI正在分析测试策略，请稍候...
            </div>
          </div>
        )}

        {/* Recommendation Results */}
        {recommendation && !loading && (
          <div style={{ marginTop: 32 }}>
            <Alert
              message="AI推荐结果 (AI Recommendation Result)"
              description="以下是基于您提供的信息生成的测试策略推荐"
              type="success"
              showIcon
              style={{ marginBottom: 24 }}
            />

            <Row gutter={[16, 16]}>
              {/* Test Scope */}
              <Col span={12}>
                <Card>
                  <div style={{ textAlign: 'center' }}>
                    <Tag color={getScopeColor(recommendation.testScope)} style={{ fontSize: 16, padding: '8px 16px' }}>
                      {recommendation.testScope}
                    </Tag>
                    <div style={{ marginTop: 8, color: '#888' }}>测试范围 (Test Scope)</div>
                  </div>
                </Card>
              </Col>

              {/* Environment */}
              <Col span={12}>
                <Card>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 24, fontWeight: 'bold' }}>
                      {recommendation.environment}
                    </div>
                    <div style={{ marginTop: 8, color: '#888' }}>推荐环境 (Recommended Env)</div>
                  </div>
                </Card>
              </Col>

              {/* Priority */}
              <Col span={12}>
                <Card>
                  <div style={{ textAlign: 'center' }}>
                    <Progress
                      type="circle"
                      percent={recommendation.priority * 10}
                      format={() => recommendation.priority}
                      width={80}
                    />
                    <div style={{ marginTop: 8, color: '#888' }}>优先级 (Priority 1-10)</div>
                  </div>
                </Card>
              </Col>

              {/* Duration */}
              <Col span={12}>
                <Card>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 24, fontWeight: 'bold' }}>
                      <ClockCircleOutlined style={{ marginRight: 8 }} />
                      {recommendation.estimatedDuration}
                    </div>
                    <div style={{ marginTop: 8, color: '#888' }}>预计时长 (Est. Duration min)</div>
                  </div>
                </Card>
              </Col>
            </Row>

            <Divider />

            {/* Risk Assessment */}
            <Card
              title={
                <Space>
                  <WarningOutlined />
                  <span>风险评估 (Risk Assessment)</span>
                </Space>
              }
              style={{ marginBottom: 16 }}
            >
              <div style={{ textAlign: 'center', padding: '16px' }}>
                <Tag
                  color={getRiskInfo(recommendation.riskLevel).color}
                  style={{ fontSize: 18, padding: '12px 24px' }}
                  icon={getRiskInfo(recommendation.riskLevel).icon}
                >
                  {recommendation.riskLevel} 风险
                </Tag>
              </div>
            </Card>

            {/* Reasoning */}
            {recommendation.reasoning && recommendation.reasoning.length > 0 && (
              <Card title="推荐理由 (Reasoning)" style={{ marginBottom: 16 }}>
                <ul style={{ paddingLeft: 20 }}>
                  {recommendation.reasoning.map((reason: string, index: number) => (
                    <li key={index} style={{ marginBottom: 8, fontSize: 14 }}>
                      {reason}
                    </li>
                  ))}
                </ul>
              </Card>
            )}

            {/* Recommended Test Cases */}
            <Divider orientation="left">推荐的测试用例 (Recommended Test Cases)</Divider>
            
            {testCasesLoading ? (
              <div style={{ textAlign: 'center', padding: '40px' }}>
                <Spin tip="正在加载推荐的测试用例..." />
              </div>
            ) : recommendedTestCases.length > 0 ? (
              <Card 
                title={`基于 ${recommendation.testScope || 'CORE'} 测试范围，推荐执行以下 ${recommendedTestCases.length} 个测试用例`}
                style={{ marginBottom: 16 }}
              >
                <Table
                  dataSource={recommendedTestCases}
                  rowKey="id"
                  size="small"
                  pagination={{
                    pageSize: 10,
                    showSizeChanger: true,
                    showTotal: (total) => `Total ${total} test cases`,
                  }}
                  scroll={{ x: 1000 }}
                  columns={[
                    {
                      title: '用例编号',
                      dataIndex: 'caseNumber',
                      key: 'caseNumber',
                      width: 120,
                    },
                    {
                      title: '用例标题',
                      dataIndex: 'title',
                      key: 'title',
                      ellipsis: true,
                    },
                    {
                      title: '模块',
                      dataIndex: 'module',
                      key: 'module',
                      width: 120,
                    },
                    {
                      title: '优先级',
                      dataIndex: 'priority',
                      key: 'priority',
                      width: 80,
                      render: (priority: number) => {
                        const color = priority >= 8 ? 'red' : priority >= 5 ? 'orange' : 'blue';
                        return <Tag color={color}>P{priority}</Tag>;
                      },
                    },
                    {
                      title: '类型',
                      dataIndex: 'type',
                      key: 'type',
                      width: 120,
                    },
                    {
                      title: '状态',
                      dataIndex: 'status',
                      key: 'status',
                      width: 100,
                      render: (status: string) => {
                        const color = status === 'APPROVED' ? 'green' : status === 'DRAFT' ? 'orange' : 'default';
                        return <Tag color={color}>{status}</Tag>;
                      },
                    },
                  ]}
                />
              </Card>
            ) : (
              <Alert
                type="info"
                message="未找到匹配的测试用例"
                description="请确认选择的模块中有已批准的测试用例"
                style={{ marginBottom: 16 }}
              />
            )}

            {/* Create Task Button */}
            <div style={{ textAlign: 'center', marginTop: 24 }}>
              <Button
                type="primary"
                size="large"
                icon={<RocketOutlined />}
                onClick={handleCreateTaskWithRecommendation}
              >
                基于此推荐创建任务 (Create Task with Recommendation)
              </Button>
              {recommendedTestCases.length > 0 && (
                <div style={{ marginTop: 8, color: '#666', fontSize: 12 }}>
                  将创建任务并关联 {recommendedTestCases.length} 个测试用例
                </div>
              )}
            </div>
          </div>
        )}
      </Card>
    </div>
  );
};

export default StrategyRecommendation;
