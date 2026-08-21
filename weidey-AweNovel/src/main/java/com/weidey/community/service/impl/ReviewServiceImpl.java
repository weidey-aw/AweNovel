package com.weidey.community.service.impl;

import com.weidey.common.exception.ServiceException;
import com.weidey.community.service.ReviewService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内容审核服务实现（Flowable）
 *
 * @author weidey
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final String PROCESS_KEY = "contentReview";

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public ReviewServiceImpl(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @Override
    public String startReview(String bizType, Long bizId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("bizType", bizType);
        variables.put("bizId", bizId);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY,
                bizType + ":" + bizId, variables);
        return instance.getProcessInstanceId();
    }

    @Override
    public Map<String, Object> completeReview(String processInstanceId, boolean approved) {
        String bizType = (String) runtimeService.getVariable(processInstanceId, "bizType");
        Object bizIdObj = runtimeService.getVariable(processInstanceId, "bizId");
        Long bizId = bizIdObj != null ? Long.valueOf(bizIdObj.toString()) : null;

        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        if (task == null) {
            throw new ServiceException("审核任务不存在或已处理");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        taskService.complete(task.getId(), variables);

        Map<String, Object> result = new HashMap<>();
        result.put("bizType", bizType);
        result.put("bizId", bizId);
        return result;
    }

    @Override
    public List<Map<String, Object>> listPendingTasks() {
        return taskService.createTaskQuery().taskCandidateGroup("admin").list().stream().map(task -> {
            Map<String, Object> m = new HashMap<>();
            m.put("taskId", task.getId());
            m.put("processInstanceId", task.getProcessInstanceId());
            m.put("taskName", task.getName());
            m.put("bizType", runtimeService.getVariable(task.getProcessInstanceId(), "bizType"));
            m.put("bizId", runtimeService.getVariable(task.getProcessInstanceId(), "bizId"));
            return m;
        }).collect(Collectors.toList());
    }
}
