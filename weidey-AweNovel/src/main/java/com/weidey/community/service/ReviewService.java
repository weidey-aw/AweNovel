package com.weidey.community.service;

import java.util.List;
import java.util.Map;

/**
 * 内容审核服务（Flowable 工作流）
 *
 * @author weidey
 */
public interface ReviewService {

    /**
     * 启动审核流程
     *
     * @param bizType 业务类型（resource/article）
     * @param bizId   业务ID
     * @return 流程实例ID
     */
    String startReview(String bizType, Long bizId);

    /**
     * 完成审核（通过/拒绝），返回业务信息（bizType/bizId）
     */
    Map<String, Object> completeReview(String processInstanceId, boolean approved);

    /**
     * 查询待审核任务列表（admin 候选组）
     */
    List<Map<String, Object>> listPendingTasks();
}
