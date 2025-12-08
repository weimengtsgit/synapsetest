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
} from '@ant-design/icons'
import './App.css'

// Import components
import CreateTestTask from './components/test-task/CreateTestTask'
import TestTaskList from './components/test-task/TestTaskList'
import AITestCaseGeneration from './components/test-case/AITestCaseGeneration'
import TestCaseList from './components/test-case/TestCaseList'
import Dashboard from './components/monitoring/Dashboard'
import QualityReport from './components/report/QualityReport'
import TestVersionList from './components/test-version/TestVersionList'
import TestEnvironmentList from './components/test-environment/TestEnvironmentList'

const { Header, Content, Footer, Sider } = Layout

const App: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false)
  const [selectedTaskId, setSelectedTaskId] = useState<string>('')
  const navigate = useNavigate()

  const menuItems = [
    {
      key: '/',
      icon: <HomeOutlined />,
      label: <Link to="/">首页 (Home)</Link>,
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
      ],
    },
    {
      key: 'test-cases',
      icon: <BulbOutlined />,
      label: '测试用例 (Test Cases)',
      children: [
        {
          key: '/test-cases/generate',
          label: <Link to="/test-cases/generate">AI生成 (AI Generate)</Link>,
        },
        {
          key: '/test-cases/list',
          label: <Link to="/test-cases/list">用例列表 (List)</Link>,
        },
      ],
    },
    {
      key: 'monitoring',
      icon: <DashboardOutlined />,
      label: '监控仪表盘 (Monitoring)',
      children: [
        {
          key: '/monitoring/dashboard',
          label: <Link to="/monitoring/dashboard">实时监控 (Real-time)</Link>,
        },
      ],
    },
    {
      key: 'reports',
      icon: <FileTextOutlined />,
      label: '质量报告 (Reports)',
      children: [
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
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={(value) => setCollapsed(value)}
        theme="dark"
      >
        <div
          style={{
            height: '32px',
            margin: '16px',
            background: 'rgba(255, 255, 255, 0.2)',
            borderRadius: '6px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
            fontWeight: 'bold',
          }}
        >
          {!collapsed ? 'SynapseTest' : 'ST'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          defaultSelectedKeys={['/']}
          items={menuItems}
        />
      </Sider>
      <Layout>
        <Header style={{ padding: 0, background: '#fff' }}>
          <div
            style={{
              padding: '0 24px',
              fontSize: '20px',
              fontWeight: 'bold',
              color: '#1890ff',
            }}
          >
            AI驱动测试任务管理系统 (AI-Driven Test Management System)
          </div>
        </Header>
        <Content style={{ margin: '0', background: '#f0f2f5' }}>
          <Routes>
            <Route
              path="/"
              element={
                <div style={{ padding: '50px', textAlign: 'center' }}>
                  <h1>欢迎使用AI驱动测试任务管理系统</h1>
                  <p style={{ fontSize: '16px', color: '#666' }}>
                    Welcome to AI-Driven Test Management System
                  </p>
                  <p style={{ marginTop: '20px', fontSize: '14px' }}>
                    请从左侧菜单选择功能模块 (Please select a module from the left menu)
                  </p>
                </div>
              }
            />
            <Route path="/test-tasks/create" element={<CreateTestTask />} />
            <Route path="/test-tasks/list" element={<TestTaskList />} />
            <Route path="/test-cases/generate" element={<AITestCaseGeneration />} />
            <Route path="/test-cases/list" element={<TestCaseList />} />
            <Route path="/monitoring/dashboard" element={<Dashboard />} />
            <Route
              path="/reports/quality"
              element={<QualityReport taskId={selectedTaskId} />}
            />
            <Route path="/config/versions" element={<TestVersionList />} />
            <Route path="/config/environments" element={<TestEnvironmentList />} />
          </Routes>
        </Content>
        <Footer style={{ textAlign: 'center', background: '#f0f2f5' }}>
          AI Test Management System ©2025 Created by SynapseTest Team
        </Footer>
      </Layout>
    </Layout>
  )
}

export default App
