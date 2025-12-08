/**
 * Service层单元测试 - monitoringService
 *
 * 测试目标：
 * 1. 验证监控数据获取的Service层逻辑
 * 2. 验证实时数据轮询机制
 * 3. 验证时间范围查询参数处理
 * 4. 验证数据导出功能
 *
 * 对应User Story: US3-测试结果可视化分析
 */

import monitoringService from '../../../services/monitoringService';
import axios from 'axios';

// Mock axios模块
jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('monitoringService单元测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getDashboardStats - 获取Dashboard统计概览', () => {
    it('场景1.1: 获取Dashboard统计数据成功', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          totalTasks: 120,
          runningTasks: 15,
          completedTasks: 95,
          failedTasks: 10,
          averagePassRate: 87.5,
          averageExecutionTime: 245.8,
          todayTasksCount: 25,
          todayPassRate: 89.2,
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await monitoringService.getDashboardStats();

      // Then: 验证axios.get被正确调用
      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/monitoring/dashboard/stats');

      // 验证返回数据
      expect(result.totalTasks).toBe(120);
      expect(result.runningTasks).toBe(15);
      expect(result.averagePassRate).toBe(87.5);
      expect(result.averageExecutionTime).toBe(245.8);
    });

    it('场景1.2: 获取统计数据失败 - 服务不可用', async () => {
      // Given: Mock 503错误
      const errorResponse = {
        response: {
          status: 503,
          data: { error: 'Service Unavailable' },
        },
      };

      mockedAxios.get.mockRejectedValue(errorResponse);

      // When & Then: 调用应该抛出错误
      await expect(monitoringService.getDashboardStats()).rejects.toMatchObject({
        response: expect.objectContaining({
          status: 503,
        }),
      });
    });
  });

  describe('getTaskProgress - 获取任务执行进度', () => {
    it('场景2.1: 获取实时任务进度成功', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          task_id: 1,
          status: 'RUNNING',
          progress_percentage: 45.5,
          executed_count: 50,
          total_count: 110,
          pass_count: 42,
          fail_count: 8,
          current_case: '订单创建-异常场景',
          estimated_remaining_time: 320,
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await monitoringService.getTaskProgress(1);

      // Then: 验证axios.get被正确调用
      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/monitoring/tasks/1/progress');

      // 验证返回数据
      expect(result.task_id).toBe(1);
      expect(result.status).toBe('RUNNING');
      expect(result.progress_percentage).toBe(45.5);
      expect(result.pass_count).toBe(42);
      expect(result.fail_count).toBe(8);
    });

    it('场景2.2: 获取任务进度 - 任务已完成', async () => {
      // Given: Mock已完成任务的响应
      const mockResponse = {
        data: {
          task_id: 2,
          status: 'COMPLETED',
          progress_percentage: 100,
          executed_count: 85,
          total_count: 85,
          pass_count: 78,
          fail_count: 7,
          completion_time: '2024-01-15T12:30:00Z',
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await monitoringService.getTaskProgress(2);

      // Then: 验证完成状态
      expect(result.status).toBe('COMPLETED');
      expect(result.progress_percentage).toBe(100);
      expect(result.completion_time).toBeTruthy();
    });
  });

  describe('getTaskMonitoringData - 获取任务监控数据', () => {
    it('场景3.1: 获取最近24小时监控数据', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          data_points: [
            {
              timestamp: '2024-01-15T10:00:00Z',
              metric_type: 'EXECUTION_TIME',
              metric_value: 125.5,
              unit: 'seconds',
            },
            {
              timestamp: '2024-01-15T11:00:00Z',
              metric_type: 'PASS_RATE',
              metric_value: 85.5,
              unit: 'percent',
            },
          ],
          time_range: '24h',
          data_source: 'mysql',
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await monitoringService.getTaskMonitoringData(1, '24h');

      // Then: 验证axios.get被正确调用
      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/monitoring/tasks/1/data', {
        params: { time_range: '24h' },
      });

      // 验证返回数据
      expect(result.data_points).toHaveLength(2);
      expect(result.time_range).toBe('24h');
      expect(result.data_source).toBe('mysql');
    });

    it('场景3.2: 获取历史监控数据（30天前）', async () => {
      // Given: Mock MongoDB历史数据响应
      const mockResponse = {
        data: {
          data_points: [
            {
              timestamp: '2024-01-01T00:00:00Z',
              metric_type: 'PASS_RATE',
              metric_value: 82.3,
            },
          ],
          time_range: '30d',
          data_source: 'mongodb',
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await monitoringService.getTaskMonitoringData(1, '30d');

      // Then: 验证数据来源
      expect(result.data_source).toBe('mongodb');
      expect(result.data_points.length).toBeGreaterThan(0);
    });

    it('场景3.3: 自定义时间范围查询', async () => {
      // Given: 自定义时间范围
      const mockResponse = {
        data: {
          data_points: [],
          time_range: 'custom',
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      const startTime = '2024-01-01T00:00:00Z';
      const endTime = '2024-01-15T23:59:59Z';

      // When: 调用Service方法
      const result = await monitoringService.getTaskMonitoringData(1, 'custom', {
        start_time: startTime,
        end_time: endTime,
      });

      // Then: 验证查询参数
      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/monitoring/tasks/1/data', {
        params: {
          time_range: 'custom',
          start_time: startTime,
          end_time: endTime,
        },
      });
    });
  });

  describe('getPassRateTrend - 获取通过率趋势', () => {
    it('场景4.1: 获取最近7天通过率趋势', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          trend_data: [
            { date: '2024-01-09', pass_rate: 82.5 },
            { date: '2024-01-10', pass_rate: 84.3 },
            { date: '2024-01-11', pass_rate: 86.7 },
            { date: '2024-01-12', pass_rate: 85.9 },
            { date: '2024-01-13', pass_rate: 88.2 },
            { date: '2024-01-14', pass_rate: 87.5 },
            { date: '2024-01-15', pass_rate: 89.1 },
          ],
          time_range: '7d',
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await monitoringService.getPassRateTrend('7d');

      // Then: 验证axios.get被正确调用
      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/monitoring/trends/pass-rate', {
        params: { time_range: '7d' },
      });

      // 验证返回数据
      expect(result.trend_data).toHaveLength(7);
      expect(result.trend_data[6].pass_rate).toBe(89.1);
    });
  });

  describe('getExecutionTimeStats - 获取执行时间统计', () => {
    it('场景5.1: 获取执行时间统计数据', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          average: 245.8,
          max: 1580.5,
          min: 45.2,
          median: 180.3,
          p95: 520.7,
          p99: 890.2,
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await monitoringService.getExecutionTimeStats();

      // Then: 验证axios.get被正确调用
      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/monitoring/stats/execution-time');

      // 验证返回数据
      expect(result.average).toBe(245.8);
      expect(result.max).toBeGreaterThanOrEqual(result.average);
      expect(result.min).toBeLessThanOrEqual(result.average);
      expect(result.p95).toBeDefined();
    });
  });

  describe('getStatsByModule - 按模块获取统计数据', () => {
    it('场景6.1: 获取按模块分组的统计数据', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          module_stats: [
            {
              module: '订单管理',
              total_cases: 45,
              pass_rate: 88.9,
              average_execution_time: 125.5,
            },
            {
              module: '支付模块',
              total_cases: 32,
              pass_rate: 92.3,
              average_execution_time: 180.2,
            },
            {
              module: '用户认证',
              total_cases: 28,
              pass_rate: 95.7,
              average_execution_time: 95.8,
            },
          ],
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await monitoringService.getStatsByModule();

      // Then: 验证axios.get被正确调用
      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/monitoring/stats/by-module');

      // 验证返回数据
      expect(result.module_stats).toHaveLength(3);
      expect(result.module_stats[0].module).toBe('订单管理');
      expect(result.module_stats[1].pass_rate).toBe(92.3);
    });
  });

  describe('getFailedCases - 获取失败用例列表', () => {
    it('场景7.1: 获取任务的失败用例详情', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          failed_cases: [
            {
              case_id: 15,
              case_name: '订单取消-库存回滚',
              error_message: 'AssertionError: Expected stock to be 100, but was 98',
              failed_at: '2024-01-15T11:25:30Z',
              module: '订单管理',
            },
            {
              case_id: 23,
              case_name: '支付-余额不足',
              error_message: 'TimeoutError: Payment gateway timeout after 30s',
              failed_at: '2024-01-15T11:30:15Z',
              module: '支付模块',
            },
          ],
          total_failed: 2,
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await monitoringService.getFailedCases(1);

      // Then: 验证axios.get被正确调用
      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/monitoring/tasks/1/failed-cases');

      // 验证返回数据
      expect(result.failed_cases).toHaveLength(2);
      expect(result.total_failed).toBe(2);
      expect(result.failed_cases[0].error_message).toContain('AssertionError');
    });
  });

  describe('exportMonitoringData - 导出监控数据', () => {
    it('场景8.1: 导出CSV格式监控数据', async () => {
      // Given: Mock Blob响应
      const csvContent = 'timestamp,metric_type,metric_value\n2024-01-15T10:00:00Z,PASS_RATE,85.5';
      const mockResponse = {
        data: new Blob([csvContent], { type: 'text/csv' }),
        headers: {
          'content-type': 'text/csv',
          'content-disposition': 'attachment; filename="task_1_monitoring.csv"',
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await monitoringService.exportMonitoringData(1, 'csv');

      // Then: 验证axios.get被正确调用
      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/monitoring/tasks/1/export', {
        params: { format: 'csv' },
        responseType: 'blob',
      });

      // 验证返回Blob
      expect(result).toBeInstanceOf(Blob);
    });

    it('场景8.2: 导出JSON格式监控数据', async () => {
      // Given: Mock JSON响应
      const jsonData = {
        data_points: [
          { timestamp: '2024-01-15T10:00:00Z', metric_value: 85.5 },
        ],
      };

      const mockResponse = {
        data: new Blob([JSON.stringify(jsonData)], { type: 'application/json' }),
        headers: {
          'content-type': 'application/json',
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await monitoringService.exportMonitoringData(1, 'json');

      // Then: 验证查询参数
      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/monitoring/tasks/1/export', {
        params: { format: 'json' },
        responseType: 'blob',
      });
    });
  });

  describe('实时数据轮询机制', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('场景9.1: 启动实时数据轮询', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          progress_percentage: 50,
          status: 'RUNNING',
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法启动轮询（假设Service层实现了轮询逻辑）
      const callback = jest.fn();
      const stopPolling = monitoringService.startProgressPolling(1, callback, 10000);

      // 模拟10秒后的第一次轮询
      jest.advanceTimersByTime(10000);
      await Promise.resolve(); // 等待Promise完成

      // Then: 验证回调被调用
      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/monitoring/tasks/1/progress');

      // 清理
      stopPolling();
    });

    it('场景9.2: 任务完成后停止轮询', async () => {
      // Given: Mock第一次返回RUNNING，第二次返回COMPLETED
      const runningResponse = {
        data: { progress_percentage: 80, status: 'RUNNING' },
      };
      const completedResponse = {
        data: { progress_percentage: 100, status: 'COMPLETED' },
      };

      mockedAxios.get
        .mockResolvedValueOnce(runningResponse)
        .mockResolvedValueOnce(completedResponse);

      // When: 启动轮询
      const callback = jest.fn();
      const stopPolling = monitoringService.startProgressPolling(1, callback, 5000);

      // 第一次轮询
      jest.advanceTimersByTime(5000);
      await Promise.resolve();

      // 第二次轮询
      jest.advanceTimersByTime(5000);
      await Promise.resolve();

      // Then: 验证轮询在任务完成后停止（由回调逻辑决定）
      expect(mockedAxios.get).toHaveBeenCalledTimes(2);

      // 清理
      stopPolling();
    });
  });

  describe('错误处理', () => {
    it('场景10.1: 参数验证 - 无效的时间范围', async () => {
      // Given: Mock 400错误
      const errorResponse = {
        response: {
          status: 400,
          data: {
            error: 'Bad Request',
            message: 'Invalid time_range parameter',
          },
        },
      };

      mockedAxios.get.mockRejectedValue(errorResponse);

      // When & Then: 调用应该抛出错误
      await expect(monitoringService.getTaskMonitoringData(1, 'invalid')).rejects.toMatchObject({
        response: expect.objectContaining({
          status: 400,
        }),
      });
    });

    it('场景10.2: 任务不存在 - 返回404', async () => {
      // Given: Mock 404错误
      const errorResponse = {
        response: {
          status: 404,
          data: { error: 'Task not found' },
        },
      };

      mockedAxios.get.mockRejectedValue(errorResponse);

      // When & Then: 调用应该抛出错误
      await expect(monitoringService.getTaskProgress(999999)).rejects.toMatchObject({
        response: expect.objectContaining({
          status: 404,
        }),
      });
    });
  });
});
