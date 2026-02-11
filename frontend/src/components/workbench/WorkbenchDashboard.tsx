import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, List, Button, Space } from 'antd';
import {
  RiseOutlined,
  FallOutlined,
  ThunderboltOutlined,
  FileTextOutlined,
  BulbOutlined,
  BarChartOutlined,
  CheckCircleOutlined,
  FileSearchOutlined,
} from '@ant-design/icons';
import { Line } from '@ant-design/plots';
import { useNavigate } from 'react-router-dom';

/**
 * Workbench Dashboard
 * Main landing page with quick actions, statistics, and trends
 *
 * Features:
 * - Quick action cards (Generate, Batch, Recommendation, Analytics)
 * - Today's statistics (4 metrics)
 * - Recent tasks list
 * - Efficiency trend chart
 */
const WorkbenchDashboard: React.FC = () => {
  const navigate = useNavigate();

  const [stats, setStats] = useState({
    generatedCases: 0,
    runningTasks: 0,
    overallPassRate: 0,
    aiAccuracy: 0,
    pendingRequirements: 0,
  });

  const [recentTasks, setRecentTasks] = useState<any[]>([]);
  const [recentRequirements, setRecentRequirements] = useState<any[]>([]);
  const [trendData, setTrendData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
    // Refresh every 30 seconds
    const interval = setInterval(loadDashboardData, 30000);
    return () => clearInterval(interval);
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);

      // TODO: Replace with actual API calls
      // For now, use mock data

      // Load today's statistics
      setStats({
        generatedCases: 142,
        runningTasks: 8,
        overallPassRate: 92.5,
        aiAccuracy: 88.3,
        pendingRequirements: 12,
      });

      // Load recent tasks
      setRecentTasks([
        {
          id: 'TASK-001',
          taskId: 'TASK-001',
          environment: 'STAGING',
          status: 'RUNNING',
          passRate: 95.2,
          updatedAt: '2025-12-08 14:30',
        },
        {
          id: 'TASK-002',
          taskId: 'TASK-002',
          environment: 'PROD',
          status: 'COMPLETED',
          passRate: 98.1,
          updatedAt: '2025-12-08 13:45',
        },
        {
          id: 'TASK-003',
          taskId: 'TASK-003',
          environment: 'DEV',
          status: 'RUNNING',
          passRate: 87.3,
          updatedAt: '2025-12-08 12:20',
        },
        {
          id: 'TASK-004',
          taskId: 'TASK-004',
          environment: 'STAGING',
          status: 'COMPLETED',
          passRate: 96.8,
          updatedAt: '2025-12-08 11:10',
        },
        {
          id: 'TASK-005',
          taskId: 'TASK-005',
          environment: 'PROD',
          status: 'PENDING',
          passRate: 0,
          updatedAt: '2025-12-08 10:50',
        },
      ]);

      // Load recent requirements
      setRecentRequirements([
        {
          id: 'REQ-001',
          title: '用户登录功能',
          status: 'APPROVED',
          priority: 'P0',
          updatedAt: '2025-02-11 10:30',
        },
        {
          id: 'REQ-002',
          title: '订单管理功能',
          status: 'APPROVED',
          priority: 'P0',
          updatedAt: '2025-02-11 09:15',
        },
        {
          id: 'REQ-003',
          title: '支付集成功能',
          status: 'DRAFT',
          priority: 'P1',
          updatedAt: '2025-02-10 16:45',
        },
        {
          id: 'REQ-004',
          title: '系统性能优化',
          status: 'DRAFT',
          priority: 'P1',
          updatedAt: '2025-02-10 14:20',
        },
        {
          id: 'REQ-005',
          title: '数据安全加固',
          status: 'APPROVED',
          priority: 'P0',
          updatedAt: '2025-02-10 11:00',
        },
      ]);

      // Generate trend data for the past 7 days
      setTrendData(generateTrendData());

    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const generateTrendData = () => {
    const days = 7;
    const data: any[] = [];
    const metrics = ['生成用例数', '通过率'];

    for (let i = days - 1; i >= 0; i--) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      const dateStr = `${date.getMonth() + 1}/${date.getDate()}`;

      metrics.forEach(metric => {
        data.push({
          date: dateStr,
          type: metric,
          value: metric === '生成用例数'
            ? Math.floor(Math.random() * 50 + 100)
            : Math.floor(Math.random() * 10 + 85),
        });
      });
    }

    return data;
  };

  const quickActions = [
    {
      title: '需求分析',
      icon: <FileSearchOutlined style={{ fontSize: 32, color: '#eb2f96' }} />,
      description: 'AI智能需求分析',
      path: '/requirements/ai-analysis',
      color: '#fff0f6',
    },
    {
      title: '生成测试用例',
      icon: <FileTextOutlined style={{ fontSize: 32, color: '#1890ff' }} />,
      description: 'AI智能生成测试用例',
      path: '/test-cases/smart-generate',
      color: '#e6f7ff',
    },
    {
      title: '批量生成',
      icon: <ThunderboltOutlined style={{ fontSize: 32, color: '#52c41a' }} />,
      description: '批量导入需求生成用例',
      path: '/test-cases/batch-generate',
      color: '#f6ffed',
    },
    {
      title: '查看推荐',
      icon: <BulbOutlined style={{ fontSize: 32, color: '#faad14' }} />,
      description: '获取AI测试策略推荐',
      path: '/recommendation/strategy',
      color: '#fffbe6',
    },
    {
      title: '效能分析',
      icon: <BarChartOutlined style={{ fontSize: 32, color: '#722ed1' }} />,
      description: '查看团队测试效能',
      path: '/monitoring/dashboard',
      color: '#f9f0ff',
    },
  ];

  const trendConfig = {
    data: trendData,
    xField: 'date',
    yField: 'value',
    seriesField: 'type',
    smooth: true,
    height: 250,
    xAxis: {
      title: {
        text: '日期',
      },
    },
    yAxis: {
      title: {
        text: '数值',
      },
    },
    legend: {
      position: 'top' as const,
    },
    tooltip: {
      shared: true,
      showCrosshairs: true,
    },
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'RUNNING':
        return '#1890ff';
      case 'COMPLETED':
        return '#52c41a';
      case 'PENDING':
        return '#faad14';
      case 'FAILED':
        return '#ff4d4f';
      default:
        return '#d9d9d9';
    }
  };

  const getStatusText = (status: string) => {
    const statusMap: Record<string, string> = {
      'RUNNING': '执行中',
      'COMPLETED': '已完成',
      'PENDING': '待执行',
      'FAILED': '失败',
    };
    return statusMap[status] || status;
  };

  const getRequirementStatusColor = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return '#52c41a';
      case 'DRAFT':
        return '#faad14';
      case 'REJECTED':
        return '#ff4d4f';
      default:
        return '#d9d9d9';
    }
  };

  const getRequirementStatusText = (status: string) => {
    const statusMap: Record<string, string> = {
      'DRAFT': '草稿',
      'APPROVED': '已批准',
      'REJECTED': '已拒绝',
    };
    return statusMap[status] || status;
  };

  const getPriorityColor = (priority: string) => {
    const colors: Record<string, string> = {
      'P0': '#ff4d4f',
      'P1': '#faad14',
      'P2': '#1890ff',
      'P3': '#d9d9d9',
    };
    return colors[priority] || '#d9d9d9';
  };

  return (
    <div style={{ padding: '24px' }}>
      <h2 style={{ marginBottom: 24 }}>工作台</h2>

      {/* Quick Actions */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {quickActions.map((action, index) => (
          <Col xs={24} sm={12} md={8} lg={24 / 5} key={index}>
            <Card
              hoverable
              onClick={() => navigate(action.path)}
              style={{
                textAlign: 'center',
                backgroundColor: action.color,
                borderRadius: 8,
                height: '100%',
              }}
              bodyStyle={{ padding: '24px 16px' }}
            >
              <div style={{ marginBottom: 16 }}>
                {action.icon}
              </div>
              <h3 style={{ marginBottom: 8, fontSize: 16 }}>{action.title}</h3>
              <p style={{ color: '#666', margin: 0, fontSize: 12 }}>
                {action.description}
              </p>
            </Card>
          </Col>
        ))}
      </Row>

      {/* Today's Statistics */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6} lg={4}>
          <Card loading={loading}>
            <Statistic
              title="待分析需求"
              value={stats.pendingRequirements}
              suffix="个"
              valueStyle={{ color: '#eb2f96' }}
              prefix={<FileSearchOutlined />}
            />
            <div style={{ marginTop: 8, fontSize: 12, color: '#888' }}>
              较昨日 <span style={{ color: '#3f8600' }}>↑ 5%</span>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6} lg={4}>
          <Card loading={loading}>
            <Statistic
              title="生成用例"
              value={stats.generatedCases}
              prefix={<RiseOutlined />}
              suffix="条"
              valueStyle={{ color: '#3f8600' }}
            />
            <div style={{ marginTop: 8, fontSize: 12, color: '#888' }}>
              较昨日 <span style={{ color: '#3f8600' }}>↑ 15%</span>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6} lg={4}>
          <Card loading={loading}>
            <Statistic
              title="执行中任务"
              value={stats.runningTasks}
              suffix="个"
              valueStyle={{ color: '#1890ff' }}
            />
            <div style={{ marginTop: 8, fontSize: 12, color: '#888' }}>
              较昨日 <span style={{ color: '#cf1322' }}>↓ 8%</span>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6} lg={4}>
          <Card loading={loading}>
            <Statistic
              title="整体通过率"
              value={stats.overallPassRate}
              precision={1}
              suffix="%"
              valueStyle={{ color: '#3f8600' }}
            />
            <div style={{ marginTop: 8, fontSize: 12, color: '#888' }}>
              较昨日 <span style={{ color: '#3f8600' }}>↑ 3%</span>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} md={6} lg={4}>
          <Card loading={loading}>
            <Statistic
              title="AI准确率"
              value={stats.aiAccuracy}
              precision={1}
              suffix="%"
              valueStyle={{ color: '#722ed1' }}
            />
            <div style={{ marginTop: 8, fontSize: 12, color: '#888' }}>
              较昨日 <span style={{ color: '#3f8600' }}>↑ 2%</span>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        {/* Recent Tasks */}
        <Col xs={24} lg={12}>
          <Card
            title="最近任务"
            extra={
              <Button type="link" onClick={() => navigate('/test-tasks/list')}>
                查看全部
              </Button>
            }
            loading={loading}
          >
            <List
              dataSource={recentTasks}
              renderItem={(item: any) => (
                <List.Item
                  style={{ cursor: 'pointer' }}
                  onClick={() => navigate(`/test-tasks/${item.taskId}`)}
                >
                  <List.Item.Meta
                    avatar={
                      <CheckCircleOutlined
                        style={{
                          fontSize: 24,
                          color: getStatusColor(item.status)
                        }}
                      />
                    }
                    title={
                      <Space>
                        <span>任务 {item.taskId}</span>
                        <span
                          style={{
                            fontSize: 12,
                            color: getStatusColor(item.status),
                            backgroundColor: `${getStatusColor(item.status)}20`,
                            padding: '2px 8px',
                            borderRadius: 4,
                          }}
                        >
                          {getStatusText(item.status)}
                        </span>
                      </Space>
                    }
                    description={
                      <Space>
                        <span>{item.environment}</span>
                        <span>•</span>
                        <span>通过率: {item.passRate}%</span>
                      </Space>
                    }
                  />
                  <div style={{ fontSize: 12, color: '#888' }}>
                    {item.updatedAt}
                  </div>
                </List.Item>
              )}
            />
          </Card>
        </Col>

        {/* Recent Requirements */}
        <Col xs={24} lg={12}>
          <Card
            title="最近需求"
            extra={
              <Button type="link" onClick={() => navigate('/requirements/list')}>
                查看全部
              </Button>
            }
            loading={loading}
          >
            <List
              dataSource={recentRequirements}
              renderItem={(item: any) => (
                <List.Item
                  style={{ cursor: 'pointer' }}
                  onClick={() => navigate('/requirements/list')}
                >
                  <List.Item.Meta
                    avatar={
                      <FileSearchOutlined
                        style={{
                          fontSize: 24,
                          color: getRequirementStatusColor(item.status)
                        }}
                      />
                    }
                    title={
                      <Space>
                        <span>{item.title}</span>
                        <span
                          style={{
                            fontSize: 12,
                            color: getPriorityColor(item.priority),
                            backgroundColor: `${getPriorityColor(item.priority)}20`,
                            padding: '2px 8px',
                            borderRadius: 4,
                          }}
                        >
                          {item.priority}
                        </span>
                      </Space>
                    }
                    description={
                      <Space>
                        <span>{item.id}</span>
                        <span>•</span>
                        <span
                          style={{
                            color: getRequirementStatusColor(item.status),
                          }}
                        >
                          {getRequirementStatusText(item.status)}
                        </span>
                      </Space>
                    }
                  />
                  <div style={{ fontSize: 12, color: '#888' }}>
                    {item.updatedAt}
                  </div>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        {/* Efficiency Trend */}
        <Col xs={24}>
          <Card title="效能趋势" loading={loading}>
            <Line {...trendConfig} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default WorkbenchDashboard;
