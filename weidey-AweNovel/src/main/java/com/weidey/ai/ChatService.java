package com.weidey.ai;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 看板娘 AI 聊天服务（DeepSeek，OpenAI 兼容）
 *
 * @author weidey
 */
public interface ChatService {

    /**
     * 流式聊天（每轮消耗 1 积分，异常自动退还）
     *
     * @param userId  用户ID
     * @param request 消息 + 历史上下文
     * @param emitter SSE 发射器
     */
    void streamChat(Long userId, ChatRequest request, SseEmitter emitter);

    /**
     * 非流式聊天（SSE 不可用时的兜底，每轮消耗 1 积分，异常自动退还）
     *
     * @return AI 回复文本
     */
    String syncChat(Long userId, ChatRequest request);
}
