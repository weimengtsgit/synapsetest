/**
 * Priority Utility Functions
 * 
 * 统一的优先级转换工具，确保前后端优先级显示一致
 * 
 * 数据库存储格式（0-10的整数）:
 * - 10 → P0（最高优先级）
 * - 8-9 → P1
 * - 5-7 → P2
 * - 0-4 → P3（最低优先级）
 * 
 * AI服务返回格式：P0, P1, P2, P3（字符串）
 */

/**
 * 将数据库中的数字优先级转换为显示标签（P0-P3）
 * @param priority 数据库中的优先级数字（0-10）
 * @returns 优先级标签（P0, P1, P2, P3）
 */
export function convertPriorityToLabel(priority: number | undefined | null): string {
  if (priority === undefined || priority === null) {
    return 'P2' // 默认值
  }

  if (priority >= 9) {
    return 'P0' // 最高优先级
  } else if (priority >= 7) {
    return 'P1'
  } else if (priority >= 4) {
    return 'P2'
  } else {
    return 'P3' // 最低优先级
  }
}

/**
 * 将优先级标签（P0-P3）转换为数据库数字
 * @param label 优先级标签（P0, P1, P2, P3）
 * @returns 数据库中的优先级数字
 */
export function convertLabelToPriority(label: string | undefined | null): number {
  if (!label) {
    return 5 // 默认值
  }

  const upperLabel = label.toUpperCase()
  
  switch (upperLabel) {
    case 'P0':
      return 10 // 最高优先级
    case 'P1':
      return 8
    case 'P2':
      return 5
    case 'P3':
      return 3 // 最低优先级
    default:
      // 如果已经是数字，直接返回
      const num = parseInt(label)
      return isNaN(num) ? 5 : num
  }
}

/**
 * 获取优先级对应的颜色
 * @param priority 可以是数字（0-10）或标签（P0-P3）
 * @returns Ant Design Tag 颜色
 */
export function getPriorityColor(priority: number | string | undefined | null): string {
  let label: string
  
  if (typeof priority === 'number') {
    label = convertPriorityToLabel(priority)
  } else {
    label = priority as string || 'P2'
  }

  const upperLabel = label.toUpperCase()
  
  const colorMap: Record<string, string> = {
    P0: 'red',      // 最高优先级 - 红色
    P1: 'orange',   // 高优先级 - 橙色
    P2: 'blue',     // 中优先级 - 蓝色
    P3: 'default',  // 低优先级 - 灰色
  }

  return colorMap[upperLabel] || 'default'
}

/**
 * 获取优先级的数值（用于排序）
 * @param priority 可以是数字（0-10）或标签（P0-P3）
 * @returns 数字优先级
 */
export function getPriorityValue(priority: number | string | undefined | null): number {
  if (typeof priority === 'number') {
    return priority
  }
  
  return convertLabelToPriority(priority as string)
}
