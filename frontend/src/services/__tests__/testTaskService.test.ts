/**
 * Service层单元测试 - testTaskService
 *
 * 测试目标：
 * 1. 验证Service层的API调用逻辑
 * 2. 验证请求参数的构建
 * 3. 验证响应数据的处理和转换
 * 4. 验证错误处理逻辑
 *
 * 技术栈：
 * - Jest: 测试框架
 * - jest.mock: Mock axios模块
 * - TypeScript: 类型检查
 *
 * 对应User Story: US1-智能测试任务调度
 */

import testTaskService from '../../../services/testTaskService';
import axios from 'axios';

// Mock axios模块
jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('testTaskService单元测试', () => {
  // 每个测试前清除所有Mock
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('createTestTask - 创建测试任务', () => {
    it('场景1.1: 创建冒烟测试任务成功', async () => {
      // Given: 准备请求数据和Mock响应
      const requestData = {
        taskName: '冒烟测试-订单模块',
        environment: 'DEV',
        version: 'v1.2.0',
        modules: ['order', 'payment'],
        codeChangeInfo: {
          changed_files_count: 3,
          changed_lines_count: 25,
          is_hotfix: false,
        },
      };

      const mockResponse = {
        data: {
          id: 1,
          taskName: '冒烟测试-订单模块',
          status: 'PENDING',
          aiRecommendation: {
            test_scope: 'SMOKE',
            environment: 'DEV',
            confidence: 0.85,
          },
          createdAt: '2024-01-15T10:00:00Z',
        },
      };

      mockedAxios.post.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await testTaskService.createTestTask(requestData, 'zhangsan');

      // Then: 验证axios.post被正确调用
      expect(mockedAxios.post).toHaveBeenCalledTimes(1);
      expect(mockedAxios.post).toHaveBeenCalledWith(
        '/api/v1/tasks',
        requestData,
        expect.objectContaining({
          headers: expect.objectContaining({
            'X-User-Id': 'zhangsan',
          }),
        })
      );

      // 验证返回数据
      expect(result.id).toBe(1);
      expect(result.taskName).toBe('冒烟测试-订单模块');
      expect(result.status).toBe('PENDING');
      expect(result.aiRecommendation?.test_scope).toBe('SMOKE');
      expect(result.aiRecommendation?.confidence).toBe(0.85);
    });

    it('场景1.2: 创建任务失败 - 参数验证错误', async () => {
      // Given: Mock 400错误响应
      const errorResponse = {
        response: {
          status: 400,
          data: {
            error: 'Validation Error',
            message: 'taskName is required',
          },
        },
      };

      mockedAxios.post.mockRejectedValue(errorResponse);

      // When & Then: 调用应该抛出错误
      await expect(
        testTaskService.createTestTask({ environment: 'DEV' } as any, 'zhangsan')
      ).rejects.toMatchObject({
        response: expect.objectContaining({
          status: 400,
          data: expect.objectContaining({
            error: 'Validation Error',
          }),
        }),
      });
    });

    it('场景1.3: 创建任务失败 - 网络错误', async () => {
      // Given: Mock网络错误
      const networkError = new Error('Network Error');
      mockedAxios.post.mockRejectedValue(networkError);

      // When & Then: 调用应该抛出错误
      await expect(
        testTaskService.createTestTask({ taskName: 'Test' } as any, 'zhangsan')
      ).rejects.toThrow('Network Error');
    });
  });

  describe('getTestTasks - 获取任务列表', () => {
    it('场景2.1: 获取分页任务列表成功', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          content: [
            { id: 1, taskName: '任务1', status: 'PENDING' },
            { id: 2, taskName: '任务2', status: 'RUNNING' },
          ],
          totalElements: 10,
          totalPages: 2,
          currentPage: 0,
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await testTaskService.getTestTasks({ page: 0, size: 10, status: 'PENDING' });

      // Then: 验证axios.get被正确调用
      expect(mockedAxios.get).toHaveBeenCalledTimes(1);
      expect(mockedAxios.get).toHaveBeenCalledWith(
        '/api/v1/tasks',
        expect.objectContaining({
          params: { page: 0, size: 10, status: 'PENDING' },
        })
      );

      // 验证返回数据
      expect(result.content).toHaveLength(2);
      expect(result.totalElements).toBe(10);
      expect(result.totalPages).toBe(2);
    });

    it('场景2.2: 获取任务列表 - 无查询参数', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          currentPage: 0,
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法（不传参数）
      const result = await testTaskService.getTestTasks();

      // Then: 验证使用默认参数
      expect(mockedAxios.get).toHaveBeenCalledWith(
        '/api/v1/tasks',
        expect.objectContaining({
          params: {},
        })
      );

      expect(result.content).toHaveLength(0);
    });
  });

  describe('getTestTaskById - 根据ID获取任务详情', () => {
    it('场景3.1: 获取任务详情成功', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          id: 1,
          taskName: '订单模块集成测试',
          testScope: 'CORE',
          environment: 'TEST',
          version: 'v1.5.0',
          status: 'RUNNING',
          progress: 45.5,
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await testTaskService.getTestTaskById(1);

      // Then: 验证axios.get被正确调用
      expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/tasks/1');

      // 验证返回数据
      expect(result.id).toBe(1);
      expect(result.taskName).toBe('订单模块集成测试');
      expect(result.status).toBe('RUNNING');
      expect(result.progress).toBe(45.5);
    });

    it('场景3.2: 获取不存在的任务 - 返回404', async () => {
      // Given: Mock 404错误响应
      const errorResponse = {
        response: {
          status: 404,
          data: {
            error: 'Not Found',
            message: 'Task not found',
          },
        },
      };

      mockedAxios.get.mockRejectedValue(errorResponse);

      // When & Then: 调用应该抛出404错误
      await expect(testTaskService.getTestTaskById(999999)).rejects.toMatchObject({
        response: expect.objectContaining({
          status: 404,
        }),
      });
    });
  });

  describe('updateTaskStatus - 更新任务状态', () => {
    it('场景4.1: 更新任务状态成功', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          id: 1,
          taskName: '订单模块测试',
          status: 'RUNNING',
          updatedAt: '2024-01-15T11:00:00Z',
        },
      };

      mockedAxios.patch.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await testTaskService.updateTaskStatus(1, 'RUNNING', 'zhangsan');

      // Then: 验证axios.patch被正确调用
      expect(mockedAxios.patch).toHaveBeenCalledWith(
        '/api/v1/tasks/1/status',
        { status: 'RUNNING', updated_by: 'zhangsan' },
        expect.objectContaining({
          headers: expect.objectContaining({
            'X-User-Id': 'zhangsan',
          }),
        })
      );

      // 验证返回数据
      expect(result.status).toBe('RUNNING');
    });

    it('场景4.2: 更新任务状态失败 - 无效状态', async () => {
      // Given: Mock 400错误响应
      const errorResponse = {
        response: {
          status: 400,
          data: {
            error: 'Invalid Status',
            message: 'Status must be one of: PENDING, RUNNING, COMPLETED, FAILED',
          },
        },
      };

      mockedAxios.patch.mockRejectedValue(errorResponse);

      // When & Then: 调用应该抛出错误
      await expect(testTaskService.updateTaskStatus(1, 'INVALID' as any, 'zhangsan')).rejects.toMatchObject({
        response: expect.objectContaining({
          status: 400,
        }),
      });
    });
  });

  describe('deleteTestTask - 删除测试任务', () => {
    it('场景5.1: 删除任务成功', async () => {
      // Given: Mock响应（204 No Content）
      const mockResponse = {
        status: 204,
        data: null,
      };

      mockedAxios.delete.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      await testTaskService.deleteTestTask(1, 'zhangsan');

      // Then: 验证axios.delete被正确调用
      expect(mockedAxios.delete).toHaveBeenCalledWith(
        '/api/v1/tasks/1',
        expect.objectContaining({
          headers: expect.objectContaining({
            'X-User-Id': 'zhangsan',
          }),
        })
      );
    });

    it('场景5.2: 删除任务失败 - 任务不存在', async () => {
      // Given: Mock 404错误响应
      const errorResponse = {
        response: {
          status: 404,
          data: {
            error: 'Not Found',
          },
        },
      };

      mockedAxios.delete.mockRejectedValue(errorResponse);

      // When & Then: 调用应该抛出错误
      await expect(testTaskService.deleteTestTask(999, 'zhangsan')).rejects.toMatchObject({
        response: expect.objectContaining({
          status: 404,
        }),
      });
    });
  });

  describe('getAiRecommendation - 获取AI推荐策略', () => {
    it('场景6.1: 获取AI推荐成功', async () => {
      // Given: Mock响应数据
      const mockResponse = {
        data: {
          test_scope: 'SMOKE',
          environment: 'DEV',
          priority: 'HIGH',
          estimated_time_minutes: 15,
          confidence: 0.88,
          reasoning: '代码变更量小，建议执行冒烟测试',
        },
      };

      mockedAxios.post.mockResolvedValue(mockResponse);

      const context = {
        environment: 'DEV',
        version: 'v1.2.0',
        modules: ['order'],
        code_change: {
          changed_files_count: 3,
          changed_lines_count: 25,
        },
      };

      // When: 调用Service方法
      const result = await testTaskService.getAiRecommendation(context);

      // Then: 验证axios.post被正确调用
      expect(mockedAxios.post).toHaveBeenCalledWith('/api/v1/tasks/ai/recommendation', context);

      // 验证返回数据
      expect(result.test_scope).toBe('SMOKE');
      expect(result.confidence).toBe(0.88);
      expect(result.reasoning).toBeTruthy();
    });

    it('场景6.2: 获取AI推荐失败 - AI服务不可用', async () => {
      // Given: Mock 503错误响应
      const errorResponse = {
        response: {
          status: 503,
          data: {
            error: 'Service Unavailable',
            message: 'AI service is currently unavailable',
          },
        },
      };

      mockedAxios.post.mockRejectedValue(errorResponse);

      // When & Then: 调用应该抛出错误
      await expect(testTaskService.getAiRecommendation({})).rejects.toMatchObject({
        response: expect.objectContaining({
          status: 503,
        }),
      });
    });
  });

  describe('请求拦截器和响应拦截器', () => {
    it('场景7.1: 验证默认请求头设置', async () => {
      // Given: Mock响应
      const mockResponse = { data: { content: [] } };
      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用任意Service方法
      await testTaskService.getTestTasks();

      // Then: 验证Content-Type被设置
      expect(mockedAxios.get).toHaveBeenCalledWith(
        '/api/v1/tasks',
        expect.objectContaining({
          params: expect.any(Object),
        })
      );
    });

    it('场景7.2: 验证响应数据转换', async () => {
      // Given: Mock响应（驼峰命名转换）
      const mockResponse = {
        data: {
          task_name: '测试任务',
          test_scope: 'CORE',
          created_at: '2024-01-15T10:00:00Z',
        },
      };

      mockedAxios.get.mockResolvedValue(mockResponse);

      // When: 调用Service方法
      const result = await testTaskService.getTestTaskById(1);

      // Then: 验证数据被正确返回（Service层负责转换）
      expect(result).toEqual(mockResponse.data);
    });
  });
});
