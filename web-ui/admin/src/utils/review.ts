import { getReviewTasks } from '@gal/shared'
import type { ReviewTask } from '@gal/shared'

/** 按业务类型与业务 id 查找对应的审核任务（用于资源/文章页的快捷审核） */
export async function findReviewTask(bizType: string, bizId: number): Promise<ReviewTask | undefined> {
  try {
    const res = (await getReviewTasks()) as unknown as { data?: ReviewTask[] } | ReviewTask[]
    const tasks = (Array.isArray(res) ? res : res?.data) ?? []
    return tasks.find((t) => t.bizType === bizType && Number(t.bizId) === Number(bizId))
  } catch {
    return undefined
  }
}
