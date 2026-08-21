package com.weidey.ai;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 看板娘 AI 聊天服务（DeepSeek，OpenAI 兼容，SSE 流式）
 *
 * @author weidey
 */
public interface ChatService {

    /**
     * 发起一次流式聊天（每次消耗 1 积分）
     *
     * @param userId  用户ID
     * @param message 用户消息
     * @param emitter SSE 发射器
     */
    void streamChat(Long userId, String message, SseEmitter emitter);
}
