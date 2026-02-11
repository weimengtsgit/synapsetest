import React, { useState } from 'react'
import { Routes, Route, Link, useNavigate } from 'react-router-dom'
import { Layout, Menu } from 'antd'
import {
  DashboardOutlined,
  FileTextOutlined,
  PlaySquareOutlined,
  BulbOutlined,
  HomeOutlined,
  SettingOutlined,
  ThunderboltOutlined,
  BarChartOutlined,
} from '@ant-design/icons'
import './App.css'

// Import components
import CreateTestTask from './components/test-task/CreateTestTask'
import TestTaskList from './components/test-task/TestTaskList'
import TestTaskDetail from './components/test-task/TestTaskDetail'
import AITestCaseGeneration from './components/test-case/AITestCaseGeneration'
import TestCaseList from './components/test-case/TestCaseList'
import Dashboard from './components/monitoring/Dashboard'
import QualityReport from './components/report/QualityReport'
import TestVersionList from './components/test-version/TestVersionList'
import TestEnvironmentList from './components/test-environment/TestEnvironmentList'

// Import new components
import WorkbenchDashboard from './components/workbench/WorkbenchDashboard'
import BatchGenerate from './components/test-case/BatchGenerate'
import StrategyRecommendation from './components/recommendation/StrategyRecommendation'
import AppHeader from './components/layout/AppHeader'
import SmartGenerate from './components/test-case/SmartGenerate'
import GenerationHistory from './components/test-case/GenerationHistory'
import TestCaseOptimization from './components/test-case/TestCaseOptimization'
import ExecutionTracking from './components/test-case/ExecutionTracking'

const { Header, Content, Footer, Sider } = Layout

const App: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false)
  const [selectedTaskId, setSelectedTaskId] = useState<string>('')
  const navigate = useNavigate()

  const menuItems = [
    {
      key: '/',
      icon: <HomeOutlined />,
      label: <Link to="/">工作台 (Workbench)</Link>,
    },
    {
      key: 'test-cases',
      icon: <FileTextOutlined />,
      label: '用例生成 (Case Generation)',
      children: [
        {
          key: '/test-cases/smart-generate',
          label: <Link to="/test-cases/smart-generate">智能生成 (Smart Generate)</Link>,
        },
        {
          key: '/test-cases/batch-generate',
          label: <Link to="/test-cases/batch-generate">批量生成 (Batch Generate)</Link>,
        },
        {
          key: '/test-cases/history',
          label: <Link to="/test-cases/history">生成历史 (History)</Link>,
        },
      ],
    },
    {
      key: 'recommendation',
      icon: <BulbOutlined />,
      label: '智能推荐 (AI Recommendation)',
      children: [
        {
          key: '/recommendation/strategy',
          label: <Link to="/recommendation/strategy">策略推荐 (Strategy)</Link>,
        },
      ],
    },
    {
      key: 'case-management',
      icon: <BarChartOutlined />,
      label: '用例管理 (Case Management)',
      children: [
        {
          key: '/case-management/list',
          label: <Link to="/case-management/list">用例列表 (List)</Link>,
        },
        {
          key: '/case-management/optimization',
          label: <Link to="/case-management/optimization">用例优化 (Optimization)</Link>,
        },
      ],
    },
    {
      key: 'test-tasks',
      icon: <PlaySquareOutlined />,
      label: '测试任务 (Test Tasks)',
      children: [
        {
          key: '/test-tasks/create',
          label: <Link to="/test-tasks/create">创建任务 (Create)</Link>,
        },
        {
          key: '/test-tasks/list',
          label: <Link to="/test-tasks/list">任务列表 (List)</Link>,
        },
        {
          key: '/test-tasks/execution',
          label: <Link to="/test-tasks/execution">执行跟踪 (Execution)</Link>,
        },
      ],
    },
    {
      key: 'monitoring',
      icon: <DashboardOutlined />,
      label: '监控分析 (Monitoring)',
      children: [
        {
          key: '/monitoring/dashboard',
          label: <Link to="/monitoring/dashboard">实时监控 (Real-time)</Link>,
        },
        {
          key: '/reports/quality',
          label: <Link to="/reports/quality">质量报告 (Quality)</Link>,
        },
      ],
    },
    {
      key: 'config',
      icon: <SettingOutlined />,
      label: '配置管理 (Configuration)',
      children: [
        {
          key: '/config/versions',
          label: <Link to="/config/versions">测试版本 (Versions)</Link>,
        },
        {
          key: '/config/environments',
          label: <Link to="/config/environments">测试环境 (Environments)</Link>,
        },
      ],
    },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* 顶部导航栏 */}
      <AppHeader username="张三" projectName="电商系统" notificationCount={3} />

      <Layout style={{ marginTop: '64px' }}>
        <Sider
          collapsible
          collapsed={collapsed}
          onCollapse={(value) => setCollapsed(value)}
          theme="light"
        >
          <Menu
            theme="light"
            mode="inline"
            defaultSelectedKeys={['/']}
            items={menuItems}
            style={{ marginTop: '16px' }}
          />
        </Sider>
        <Layout>
          <Content style={{ margin: '0', background: '#f0f2f5' }}>
            <Routes>
              {/* Workbench Dashboard as Home */}
              <Route path="/" element={<WorkbenchDashboard />} />

              {/* Test Cases */}
              <Route path="/test-cases/smart-generate" element={<SmartGenerate />} />
              <Route path="/test-cases/generate" element={<AITestCaseGeneration />} />
              <Route path="/test-cases/batch-generate" element={<BatchGenerate />} />
              <Route path="/test-cases/history" element={<GenerationHistory />} />

              {/* AI Recommendation */}
              <Route path="/recommendation/strategy" element={<StrategyRecommendation />} />

              {/* Case Management */}
              <Route path="/case-management/list" element={<TestCaseList />} />
              <Route path="/case-management/optimization" element={<TestCaseOptimization />} />

              {/* Test Tasks */}
              <Route path="/test-tasks/create" element={<CreateTestTask />} />
              <Route path="/test-tasks/list" element={<TestTaskList />} />
              <Route path="/test-tasks/:id" element={<TestTaskDetail />} />
              <Route path="/test-tasks/execution" element={<ExecutionTracking />} />

              {/* Monitoring & Reports */}
              <Route path="/monitoring/dashboard" element={<Dashboard />} />
              <Route
                path="/reports/quality"
                element={<QualityReport taskId={selectedTaskId} />}
              />

              {/* Configuration */}
              <Route path="/config/versions" element={<TestVersionList />} />
              <Route path="/config/environments" element={<TestEnvironmentList />} />
            </Routes>
          </Content>
          <Footer style={{ textAlign: 'center', background: '#f0f2f5' }}>
            AI Test Management System ©2025 Created by SynapseTest Team
          </Footer>
        </Layout>
      </Layout>
    </Layout>
  )
}

export default App
