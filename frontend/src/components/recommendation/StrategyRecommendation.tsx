import React, { useState } from 'react';
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
} from 'antd';
import {
  BulbOutlined,
  ThunderboltOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import aiService, { RecommendationRequest } from '../../services/aiService';

const { TextArea } = Input;
const { Option } = Select;

/**
 * Strategy Recommendation
 * AI-powered test strategy recommendation based on code changes and context
 *
 * Features:
 * - Input task context (code changes, historical data, business info)
 * - Get AI recommendation (test scope, environment, priority)
 * - View detailed reasoning
 * - Risk assessment
 */
const StrategyRecommendation: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [recommendation, setRecommendation] = useState<any>(null);

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
            changedModules: values.changedModules
              ? values.changedModules.split(',').map((m: string) => m.trim())
              : [],
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
    } catch (error: any) {
      message.error(`获取推荐失败: ${error.message}`);
      console.error('Recommendation error:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Reset form and results
   */
  const handleReset = () => {
    form.resetFields();
    setRecommendation(null);
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
            <span>AI测试策略推荐</span>
          </Space>
        }
      >
        <Form form={form} layout="vertical" onFinish={handleGetRecommendation}>
          {/* Basic Info */}
          <Card type="inner" title="基本信息" style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item
                  label="任务ID"
                  name="taskId"
                  rules={[{ required: true, message: '请输入任务ID' }]}
                >
                  <Input placeholder="例如: TASK-001" />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item label="目标环境" name="environment">
                  <Select placeholder="选择环境">
                    <Option value="DEV">开发环境 (DEV)</Option>
                    <Option value="STAGING">预发环境 (STAGING)</Option>
                    <Option value="PROD">生产环境 (PROD)</Option>
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item label="版本号" name="version">
                  <Input placeholder="例如: v1.2.0" />
                </Form.Item>
              </Col>
            </Row>
          </Card>

          {/* Code Change Info */}
          <Card type="inner" title="代码变更信息" style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item label="变更文件数" name="changedFilesCount">
                  <InputNumber min={0} style={{ width: '100%' }} placeholder="0" />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item label="变更行数" name="changedLinesCount">
                  <InputNumber min={0} style={{ width: '100%' }} placeholder="0" />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label="变更模块"
                  name="changedModules"
                  help="多个模块用逗号分隔"
                >
                  <Input placeholder="例如: user-service, order-service" />
                </Form.Item>
              </Col>
            </Row>
          </Card>

          {/* Historical Data */}
          <Card type="inner" title="历史数据（可选）" style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item label="最近通过率 (%)" name="recentPassRate">
                  <InputNumber min={0} max={100} style={{ width: '100%' }} placeholder="90" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="最近缺陷数" name="recentDefectCount">
                  <InputNumber min={0} style={{ width: '100%' }} placeholder="5" />
                </Form.Item>
              </Col>
            </Row>
          </Card>

          {/* Business Info */}
          <Card type="inner" title="业务信息（可选）" style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item label="模块重要性 (%)" name="moduleImportance">
                  <InputNumber min={0} max={100} style={{ width: '100%' }} placeholder="80" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="截止日期" name="deadline">
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
                获取AI推荐
              </Button>
              <Button onClick={handleReset}>重置</Button>
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
              message="AI推荐结果"
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
                    <div style={{ marginTop: 8, color: '#888' }}>测试范围</div>
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
                    <div style={{ marginTop: 8, color: '#888' }}>推荐环境</div>
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
                    <div style={{ marginTop: 8, color: '#888' }}>优先级 (1-10)</div>
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
                    <div style={{ marginTop: 8, color: '#888' }}>预计时长 (分钟)</div>
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
                  <span>风险评估</span>
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
              <Card title="推荐理由" style={{ marginBottom: 16 }}>
                <ul style={{ paddingLeft: 20 }}>
                  {recommendation.reasoning.map((reason: string, index: number) => (
                    <li key={index} style={{ marginBottom: 8, fontSize: 14 }}>
                      {reason}
                    </li>
                  ))}
                </ul>
              </Card>
            )}
          </div>
        )}
      </Card>
    </div>
  );
};

export default StrategyRecommendation;
