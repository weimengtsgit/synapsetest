import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Dashboard from '../Dashboard';
import monitoringService from '../../../services/monitoringService';

jest.mock('../../../services/monitoringService');
jest.useFakeTimers();

/**
 * 监控仪表盘场景 (US3)
 * TDD测试用例
 */
describe('监控仪表盘场景', () => {

  const mockDashboardStats = {
    runningTasks: 5,
    completedToday: 12,
    avgPassRate: 89.5,
    resourceUsage: { cpu: 72, memory: 65, disk: 45 }
  };

  const mockRunningTasks = [
    {
      taskId: 'task-001',
      name: '登录模块测试',
      status: 'RUNNING',
      progress: 65,
      executedCases: 65,
      totalCases: 100,
      passedCases: 60,
      failedCases: 5
    },
    {
      taskId: 'task-002',
      name: '支付流程测试',
      status: 'RUNNING',
      progress: 30,
      executedCases: 30,
      totalCases: 100,
      passedCases: 28,
      failedCases: 2
    }
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    (monitoringService.getDashboardStats as jest.Mock).mockResolvedValue(mockDashboardStats);
    (monitoringService.getRunningTasks as jest.Mock).mockResolvedValue(mockRunningTasks);
  });

  test('场景1.1: 正确渲染核心指标卡片', async () => {
    render(<Dashboard />);

    await waitFor(() => {
      expect(screen.getByText('当前运行任务')).toBeInTheDocument();
      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText('今日完成')).toBeInTheDocument();
      expect(screen.getByText('12')).toBeInTheDocument();
      expect(screen.getByText('平均通过率')).toBeInTheDocument();
      expect(screen.getByText('89.5%')).toBeInTheDocument();
    });
  });

  test('场景1.2: 显示实时任务监控列表', async () => {
    render(<Dashboard />);

    await waitFor(() => {
      expect(screen.getByText('登录模块测试')).toBeInTheDocument();
      expect(screen.getByText('支付流程测试')).toBeInTheDocument();
      expect(screen.getByText('65%')).toBeInTheDocument(); // 进度
      expect(screen.getByText('30%')).toBeInTheDocument();
    });
  });

  test('场景1.3: 显示资源使用情况', async () => {
    render(<Dashboard />);

    await waitFor(() => {
      expect(screen.getByText(/CPU: 72%/)).toBeInTheDocument();
      expect(screen.getByText(/内存: 65%/)).toBeInTheDocument();
      expect(screen.getByText(/磁盘: 45%/)).toBeInTheDocument();
    });
  });

  test('场景1.4: 数据自动刷新 (10秒间隔)', async () => {
    render(<Dashboard />);

    await waitFor(() => {
      expect(monitoringService.getDashboardStats).toHaveBeenCalledTimes(1);
    });

    // 快进10秒
    act(() => {
      jest.advanceTimersByTime(10000);
    });

    await waitFor(() => {
      expect(monitoringService.getDashboardStats).toHaveBeenCalledTimes(2);
    });

    // 再快进10秒
    act(() => {
      jest.advanceTimersByTime(10000);
    });

    await waitFor(() => {
      expect(monitoringService.getDashboardStats).toHaveBeenCalledTimes(3);
    });
  });

  test('场景1.5: 组件卸载时清除定时器', async () => {
    const { unmount } = render(<Dashboard />);

    await waitFor(() => {
      expect(monitoringService.getDashboardStats).toHaveBeenCalled();
    });

    unmount();

    // 卸载后定时器应该被清除，不再调用
    act(() => {
      jest.advanceTimersByTime(10000);
    });

    // 调用次数应该不变
    expect(monitoringService.getDashboardStats).toHaveBeenCalledTimes(1);
  });

  test('场景1.6: 点击指标卡片下钻查看详情', async () => {
    const mockOnDrillDown = jest.fn();
    render(<Dashboard onDrillDown={mockOnDrillDown} />);

    await waitFor(() => {
      expect(screen.getByText('当前运行任务')).toBeInTheDocument();
    });

    // When: 点击运行任务卡片
    await userEvent.click(screen.getByText('当前运行任务'));

    // Then: 触发下钻回调
    expect(mockOnDrillDown).toHaveBeenCalledWith('runningTasks');
  });

  test('场景1.7: 任务进度条正确显示', async () => {
    render(<Dashboard />);

    await waitFor(() => {
      const progressBars = screen.getAllByRole('progressbar');
      expect(progressBars.length).toBeGreaterThan(0);
    });
  });

  test('场景1.8: 加载失败显示错误提示', async () => {
    (monitoringService.getDashboardStats as jest.Mock).mockRejectedValue(
      new Error('网络错误')
    );

    render(<Dashboard />);

    await waitFor(() => {
      expect(screen.getByText(/加载失败/)).toBeInTheDocument();
    });
  });

  test('场景1.9: 支持自定义时间范围选择', async () => {
    render(<Dashboard />);

    await waitFor(() => {
      expect(screen.getByText('当前运行任务')).toBeInTheDocument();
    });

    // When: 选择时间范围
    const timeRangeSelector = screen.getByLabelText('时间范围');
    await userEvent.selectOptions(timeRangeSelector, '7days');

    // Then: 重新加载数据
    expect(monitoringService.getDashboardStats).toHaveBeenCalledWith({ range: '7days' });
  });
});
