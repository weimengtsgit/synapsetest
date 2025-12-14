## 项目更新记录

### 2025-12-14 - 新增顶部导航栏
- 新增 `AppHeader` 组件 (frontend/src/components/layout/AppHeader.tsx)
- 顶部导航栏包含以下功能:
  - Logo 和平台名称展示
  - 项目选择器 (支持切换不同项目)
  - 通知中心 (带消息数量徽章)
  - 帮助中心
  - 用户信息下拉菜单
- 参考 `frontend/docs/UI原型演示.html` 的设计风格
- 采用深色背景 (#001529) 与侧边栏统一

## 开发规范

## 运行方式

### 以独立项目运行

```
// 配置node版本
nvm use 22
node --version

// 初始化项目
npm install 

// 开发模式
yarn run dev 或者 yarn dev 或者 npm run dev

<!-- 本地联调注意 -->
修改vite.config.ts 和 nginx.conf 文件中对应的地址