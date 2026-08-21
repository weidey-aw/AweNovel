package com.weidey.ai;

import com.weidey.common.utils.SecurityUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 看板娘 AI 聊天接口
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

    /**
     * 流式聊天（SSE）
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam("message") String message) {
        Long userId = SecurityUtils.getUserId();
        SseEmitter emitter = new SseEmitter(120000L);
        chatService.streamChat(userId, message, emitter);
        return emitter;
    }
}
