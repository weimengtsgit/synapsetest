import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import CreateTestTask from '../CreateTestTask';
import { store } from '../../../store';
import testTaskService from '../../../services/testTaskService';

jest.mock('../../../services/testTaskService');

const renderComponent = () => {
  render(
    <Provider store={store}>
      <BrowserRouter>
        <CreateTestTask />
      </BrowserRouter>
    </Provider>
  );
};

/**
 * 创建冒烟测试任务场景 (US1)
 * TDD测试用例
 */
describe('创建冒烟测试任务场景', () => {

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('场景1.1: 表单初始状态 - 提交按钮禁用', () => {
    // Given & When
    renderComponent();

    // Then
    expect(screen.getByRole('button', { name: '创建任务' })).toBeDisabled();
    expect(screen.getByLabelText('任务名称')).toHaveValue('');
  });

  test('场景1.2: 填写必填字段后提交按钮启用', async () => {
    // Given
    renderComponent();

    // When: 填写所有必填字段
    await userEvent.type(screen.getByLabelText('任务名称'), '登录模块冒烟测试');
    await userEvent.selectOptions(screen.getByLabelText('环境'), 'DEV');
    await userEvent.selectOptions(screen.getByLabelText('版本'), 'v2.1.0');

    // Then
    expect(screen.getByRole('button', { name: '创建任务' })).toBeEnabled();
  });

  test('场景1.3: 选择环境后显示AI推荐策略', async () => {
    // Given
    const mockRecommendation = {
      recommendedScope: 'SMOKE',
      estimatedTimeMinutes: 10,
      suggestedEnvironment: 'DEV',
      confidence: 0.85
    };
    (testTaskService.getRecommendation as jest.Mock).mockResolvedValue(mockRecommendation);

    renderComponent();

    // When
    await userEvent.type(screen.getByLabelText('任务名称'), '登录模块冒烟测试');
    await userEvent.selectOptions(screen.getByLabelText('环境'), 'DEV');

    // Then
    await waitFor(() => {
      expect(screen.getByText(/推荐测试范围: SMOKE/)).toBeInTheDocument();
      expect(screen.getByText(/预估时间: 10分钟/)).toBeInTheDocument();
      expect(screen.getByText(/置信度: 85%/)).toBeInTheDocument();
    });
  });

  test('场景1.4: 提交任务创建成功', async () => {
    // Given
    const mockResponse = {
      id: 'task-001',
      name: '登录模块冒烟测试',
      status: 'PENDING',
      createdBy: 'zhangsan'
    };
    (testTaskService.createTask as jest.Mock).mockResolvedValue(mockResponse);

    renderComponent();

    // When
    await userEvent.type(screen.getByLabelText('任务名称'), '登录模块冒烟测试');
    await userEvent.selectOptions(screen.getByLabelText('环境'), 'DEV');
    await userEvent.selectOptions(screen.getByLabelText('版本'), 'v2.1.0');
    await userEvent.click(screen.getByRole('button', { name: '创建任务' }));

    // Then
    await waitFor(() => {
      expect(screen.getByText('任务创建成功')).toBeInTheDocument();
    });
    expect(testTaskService.createTask).toHaveBeenCalledWith({
      name: '登录模块冒烟测试',
      environment: 'DEV',
      version: 'v2.1.0'
    });
  });

  test('场景1.5: 提交失败显示错误消息', async () => {
    // Given
    (testTaskService.createTask as jest.Mock).mockRejectedValue(
      new Error('服务器内部错误')
    );

    renderComponent();

    // When
    await userEvent.type(screen.getByLabelText('任务名称'), '测试任务');
    await userEvent.selectOptions(screen.getByLabelText('环境'), 'DEV');
    await userEvent.selectOptions(screen.getByLabelText('版本'), 'v2.1.0');
    await userEvent.click(screen.getByRole('button', { name: '创建任务' }));

    // Then
    await waitFor(() => {
      expect(screen.getByText(/创建失败/)).toBeInTheDocument();
    });
  });

  test('场景1.6: 名称为空时显示验证错误', async () => {
    // Given
    renderComponent();

    // When: 点击名称输入框后离开（触发验证）
    const nameInput = screen.getByLabelText('任务名称');
    await userEvent.click(nameInput);
    await userEvent.tab();

    // Then
    await waitFor(() => {
      expect(screen.getByText('任务名称不能为空')).toBeInTheDocument();
    });
  });
});
