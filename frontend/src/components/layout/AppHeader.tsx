import React, { useState } from 'react'
import { Badge, Dropdown, Space } from 'antd'
import type { MenuProps } from 'antd'
import {
  BellOutlined,
  QuestionCircleOutlined,
  UserOutlined,
  DownOutlined,
} from '@ant-design/icons'
import './AppHeader.css'

interface AppHeaderProps {
  username?: string
  projectName?: string
  notificationCount?: number
}

const AppHeader: React.FC<AppHeaderProps> = ({
  username = '张三',
  projectName = '电商系统',
  notificationCount = 3,
}) => {
  const [selectedProject, setSelectedProject] = useState(projectName)

  // 项目下拉菜单
  const projectMenuItems: MenuProps['items'] = [
    {
      key: 'ecommerce',
      label: '电商系统',
    },
    {
      key: 'payment',
      label: '支付系统',
    },
    {
      key: 'order',
      label: '订单系统',
    },
  ]

  const handleProjectChange: MenuProps['onClick'] = ({ key }) => {
    const project = projectMenuItems?.find((item) => item?.key === key)
    if (project && project.label) {
      setSelectedProject(project.label as string)
    }
  }

  // 通知下拉菜单
  const notificationMenuItems: MenuProps['items'] = [
    {
      key: '1',
      label: '测试任务已完成',
    },
    {
      key: '2',
      label: '新的用例生成完成',
    },
    {
      key: '3',
      label: '系统更新通知',
    },
  ]

  // 用户下拉菜单
  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      label: '个人中心',
    },
    {
      key: 'settings',
      label: '设置',
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      label: '退出登录',
    },
  ]

  // 帮助下拉菜单
  const helpMenuItems: MenuProps['items'] = [
    {
      key: 'docs',
      label: '使用文档',
    },
    {
      key: 'feedback',
      label: '反馈建议',
    },
    {
      key: 'about',
      label: '关于',
    },
  ]

  return (
    <div className="app-header">
      <div className="app-header-logo">
        <div className="app-header-logo-icon">🤖</div>
        <span className="app-header-logo-text">AI智能测试平台</span>
      </div>
      <div className="app-header-spacer" />
      <div className="app-header-user-info">
        <Dropdown
          menu={{ items: projectMenuItems, onClick: handleProjectChange }}
          trigger={['click']}
        >
          <div className="app-header-project-selector">
            <Space>
              项目: {selectedProject}
              <DownOutlined />
            </Space>
          </div>
        </Dropdown>
        <Dropdown menu={{ items: notificationMenuItems }} trigger={['click']}>
          <div className="app-header-icon-btn">
            <Badge count={notificationCount} size="small">
              <BellOutlined style={{ fontSize: '18px', color: '#fff' }} />
            </Badge>
          </div>
        </Dropdown>
        <Dropdown menu={{ items: helpMenuItems }} trigger={['click']}>
          <div className="app-header-icon-btn">
            <QuestionCircleOutlined style={{ fontSize: '18px', color: '#fff' }} />
          </div>
        </Dropdown>
        <Dropdown menu={{ items: userMenuItems }} trigger={['click']}>
          <div className="app-header-icon-btn">
            <Space>
              <UserOutlined style={{ fontSize: '18px' }} />
              {username}
              <DownOutlined />
            </Space>
          </div>
        </Dropdown>
      </div>
    </div>
  )
}

export default AppHeader
