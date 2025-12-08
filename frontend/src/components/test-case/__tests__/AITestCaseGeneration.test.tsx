import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AITestCaseGeneration from '../AITestCaseGeneration';
import testCaseService from '../../../services/testCaseService';

jest.mock('../../../services/testCaseService');

/**
 * AI生成测试用例场景 (US2)
 * TDD测试用例
 */
describe('AI生成测试用例场景', () => {

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('场景1.1: 初始状态 - 生成按钮禁用', () => {
    // Given & When
    render(<AITestCaseGeneration />);

    // Then
    expect(screen.getByRole('button', { name: '生成用例' })).toBeDisabled();
    expect(screen.getByPlaceholderText(/输入需求描述/)).toHaveValue('');
  });

  test('场景1.2: 输入需求文本后生成按钮启用', async () => {
    // Given
    render(<AITestCaseGeneration />);

    // When: 输入足够长的需求描述
    const textarea = screen.getByPlaceholderText(/输入需求描述/);
    await userEvent.type(textarea, '用户登录功能：用户输入用户名和密码进行身份验证，验证成功后跳转首页...');

    // Then
    expect(screen.getByRole('button', { name: '生成用例' })).toBeEnabled();
    expect(screen.getByText(/字符数:/)).toBeInTheDocument();
  });

  test('场景1.3: 点击生成按钮显示加载状态', async () => {
    // Given
    (testCaseService.generateTestCases as jest.Mock).mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 1000))
    );

    render(<AITestCaseGeneration />);
    await userEvent.type(
      screen.getByPlaceholderText(/输入需求描述/),
      '用户登录功能需求描述...'
    );

    // When
    await userEvent.click(screen.getByRole('button', { name: '生成用例' }));

    // Then
    expect(screen.getByText(/AI正在分析需求/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '生成用例' })).toBeDisabled();
  });

  test('场景1.4: AI生成成功后显示用例列表', async () => {
    // Given
    const mockGeneratedCases = [
      {
        id: 'gen-001',
        title: '正常登录验证',
        steps: ['输入用户名', '输入密码', '点击登录'],
        expectedResults: '登录成功',
        priority: 8
      },
      {
        id: 'gen-002',
        title: '密码错误验证',
        steps: ['输入用户名', '输入错误密码', '点击登录'],
        expectedResults: '显示错误提示',
        priority: 7
      }
    ];

    (testCaseService.generateTestCases as jest.Mock).mockResolvedValue(mockGeneratedCases);

    render(<AITestCaseGeneration />);
    await userEvent.type(
      screen.getByPlaceholderText(/输入需求描述/),
      '用户登录功能需求...'
    );

    // When
    await userEvent.click(screen.getByRole('button', { name: '生成用例' }));

    // Then
    await waitFor(() => {
      expect(screen.getByText('正常登录验证')).toBeInTheDocument();
      expect(screen.getByText('密码错误验证')).toBeInTheDocument();
      expect(screen.getByText(/共生成 2 个用例/)).toBeInTheDocument();
    });
  });

  test('场景1.5: 点击用例可展开查看详情', async () => {
    // Given
    const mockCase = {
      id: 'gen-001',
      title: '正常登录验证',
      steps: ['步骤1', '步骤2'],
      expectedResults: '预期结果'
    };
    (testCaseService.generateTestCases as jest.Mock).mockResolvedValue([mockCase]);

    render(<AITestCaseGeneration />);
    await userEvent.type(screen.getByPlaceholderText(/输入需求描述/), '需求...');
    await userEvent.click(screen.getByRole('button', { name: '生成用例' }));

    // When: 点击展开
    await waitFor(() => {
      expect(screen.getByText('正常登录验证')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('正常登录验证'));

    // Then
    await waitFor(() => {
      expect(screen.getByText('步骤1')).toBeInTheDocument();
      expect(screen.getByText('预期结果')).toBeInTheDocument();
    });
  });

  test('场景1.6: 编辑生成的用例', async () => {
    // Given
    const mockCase = {
      id: 'gen-001',
      title: '原标题',
      steps: ['步骤1'],
      expectedResults: '预期结果'
    };
    (testCaseService.generateTestCases as jest.Mock).mockResolvedValue([mockCase]);

    render(<AITestCaseGeneration />);
    await userEvent.type(screen.getByPlaceholderText(/输入需求描述/), '需求...');
    await userEvent.click(screen.getByRole('button', { name: '生成用例' }));

    await waitFor(() => {
      expect(screen.getByText('原标题')).toBeInTheDocument();
    });

    // When: 点击编辑按钮
    await userEvent.click(screen.getByRole('button', { name: '编辑' }));

    // Then: 显示编辑对话框
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByDisplayValue('原标题')).toBeInTheDocument();
    });
  });

  test('场景1.7: 批量保存选中的用例', async () => {
    // Given
    const mockCases = [
      { id: 'gen-001', title: '用例1', steps: ['步骤'], expectedResults: '结果' },
      { id: 'gen-002', title: '用例2', steps: ['步骤'], expectedResults: '结果' }
    ];
    (testCaseService.generateTestCases as jest.Mock).mockResolvedValue(mockCases);
    (testCaseService.batchSave as jest.Mock).mockResolvedValue({ saved: 2 });

    render(<AITestCaseGeneration />);
    await userEvent.type(screen.getByPlaceholderText(/输入需求描述/), '需求...');
    await userEvent.click(screen.getByRole('button', { name: '生成用例' }));

    await waitFor(() => {
      expect(screen.getByText('用例1')).toBeInTheDocument();
    });

    // When: 全选并保存
    await userEvent.click(screen.getByRole('checkbox', { name: '全选' }));
    await userEvent.click(screen.getByRole('button', { name: '保存选中' }));

    // Then
    await waitFor(() => {
      expect(screen.getByText(/成功保存 2 个用例/)).toBeInTheDocument();
    });
    expect(testCaseService.batchSave).toHaveBeenCalledWith(mockCases);
  });

  test('场景1.8: AI服务失败显示错误消息', async () => {
    // Given
    (testCaseService.generateTestCases as jest.Mock).mockRejectedValue(
      new Error('AI服务暂不可用')
    );

    render(<AITestCaseGeneration />);
    await userEvent.type(screen.getByPlaceholderText(/输入需求描述/), '需求...');

    // When
    await userEvent.click(screen.getByRole('button', { name: '生成用例' }));

    // Then
    await waitFor(() => {
      expect(screen.getByText(/生成失败/)).toBeInTheDocument();
    });
  });

  test('场景1.9: 需求文本太短时显示提示', async () => {
    // Given
    render(<AITestCaseGeneration />);

    // When: 输入太短的文本
    await userEvent.type(screen.getByPlaceholderText(/输入需求描述/), '短文本');

    // Then
    expect(screen.getByText(/请输入更详细的需求描述/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '生成用例' })).toBeDisabled();
  });
});
