package com.weidey.ai;

import com.weidey.common.core.domain.AjaxResult;
import com.weidey.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 看板娘 AI 聊天接口
 *
 * <p>推荐使用 `POST /ai/chat`（支持历史上下文与长消息）；SSE 不可用时前端可降级到
 * `POST /ai/chat/sync`。旧的 `GET /ai/chat?message=` 保留兼容。</p>
 *
 * @author weidey
 */
@RestController
@RequestMapping("/ai")
public class AiChatController {

    private final ChatService chatService;

    public AiChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** 流式聊天（SSE） */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request, HttpServletResponse response) {
        return startStream(request, response);
    }

    /** 非流式聊天（SSE 被代理缓冲/阻断时的兜底） */
    @PostMapping("/chat/sync")
    public AjaxResult chatSync(@RequestBody ChatRequest request) {
        Long userId = SecurityUtils.getUserId();
        long start = System.currentTimeMillis();
        String reply = chatService.syncChat(userId, request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reply", reply);
        data.put("elapsedMs", System.currentTimeMillis() - start);
        return AjaxResult.success(data);
    }

    /** 兼容旧版：GET 单轮聊天 */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatLegacy(@RequestParam("message") String message, HttpServletResponse response) {
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        return startStream(request, response);
    }

    /** 统一的 SSE 初始化：关闭代理缓冲并尽早 flush 响应头，避免「迟迟不出字」 */
    private SseEmitter startStream(ChatRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        Long userId = SecurityUtils.getUserId();
        SseEmitter emitter = new SseEmitter(180000L);
        chatService.streamChat(userId, request, emitter);
        return emitter;
    }
}
