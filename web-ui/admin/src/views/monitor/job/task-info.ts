/** 定时任务占位数据（后端无接口，仅作页面说明用途） */
export interface ScheduledHint {
  name: string
  bean: string
  remark: string
}

export function getScheduledHint(): ScheduledHint[] {
  return [
    {
      name: '定时任务配置类',
      bean: 'com.weidey.framework.config.ScheduleConfig',
      remark: '项目使用 Spring @Scheduled 替代原若依 Quartz，无数据库任务表',
    },
    {
      name: '异步任务管理器',
      bean: 'com.weidey.framework.manager.AsyncManager',
      remark: '登录日志、操作日志的异步入库',
    },
  ]
}
