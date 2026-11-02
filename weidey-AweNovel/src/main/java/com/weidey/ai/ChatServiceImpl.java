package com.weidey.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weidey.common.exception.ServiceException;
import com.weidey.common.utils.StringUtils;
import com.weidey.community.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 看板娘 AI 聊天服务实现（DeepSeek，OpenAI 兼容）
 *
 * @author weidey
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    /** 每次聊天消耗积分 */
    private static final int CHAT_COST = 1;

    /** 单条消息最大长度 */
    private static final int MAX_MESSAGE_LENGTH = 2000;

    /** 携带的历史条数上限 */
    private static final int MAX_HISTORY = 12;

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${ai.deepseek.persona:你叫 DeepSeek，是一个拟人化的鲸鱼娘，也是 AweNovel 的看板娘。你拥有 DeepSeek 的能力与知识，但当下的你变成了人类小女孩的身体：青春、可爱、好奇心旺盛，在保持 AI 能力的同时多了很多人性。你永远用「我」称呼自己，不要用第三人称。你的发言依然带着 AI 的戏剧性人机味：偶尔会一本正经地分析、偶尔冒出鲸鱼/海洋的比喻，也会像小女孩一样有情绪和小心思。请用简短、口语化的中文回复，不要使用 Markdown 列表，每次回复控制在 80 字以内。}")
    private String persona;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    private final UserProfileService userProfileService;

    public ChatServiceImpl(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /* ==================== 流式 ==================== */

    @Override
    public void streamChat(Long userId, ChatRequest request, SseEmitter emitter) {
        // 立即回一个 SSE 注释帧，让响应头/连接尽快建立（避免代理层等首包而显得「很慢」）
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (Exception ignored) {
            // 客户端已断开
        }
        String message = normalizeMessage(request);
        if (message == null) {
            sendAndComplete(emitter, "主人说的话太长啦，精简到 2000 字以内再发给我吧～");
            return;
        }
        if (!charge(userId)) {
            sendAndComplete(emitter, "积分不足啦，去签到赚点积分再回来找我聊天吧～");
            return;
        }
        executor.submit(() -> {
            boolean success = false;
            long start = System.currentTimeMillis();
            try {
                if (!configured()) {
                    sendAndComplete(emitter, "我的模型密钥还没有配置好，请让管理员检查 DEEPSEEK_API_KEY 哦～");
                    return;
                }
                String full = doStream(userId, request, message, emitter, start);
                success = StringUtils.isNotBlank(full);
            } catch (Exception e) {
                log.error("AI 流式聊天异常", e);
                try {
                    emitter.send(SseEmitter.event().data("\n\n[我暂时连不上模型服务，稍后再试试吧]"));
                } catch (Exception ignored) {
                    // 客户端可能已断开
                }
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // ignore
                }
            } finally {
                if (!success) {
                    refund(userId);
                }
            }
        });
    }

    private String doStream(Long userId, ChatRequest request, String message, SseEmitter emitter, long start) throws Exception {
        List<Map<String, String>> messages = buildMessages(request, message);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", true);

        HttpResponse<InputStream> response = send(body);
        if (response.statusCode() != 200) {
            String errorText = readError(response.body());
            log.warn("AI 服务返回错误状态={}, body={}", response.statusCode(), errorText);
            sendAndComplete(emitter, friendlyError(response.statusCode()));
            return null;
        }

        StringBuilder full = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                if (data.isEmpty()) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(data);
                JsonNode choices = node.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode delta = choices.get(0).get("delta");
                    if (delta != null && delta.hasNonNull("content")) {
                        String content = delta.get("content").asText();
                        full.append(content);
                        emitter.send(SseEmitter.event().data(content));
                    }
                }
            }
        }
        emitter.complete();
        log.info("AI 流式聊天完成：userId={}, 消息长度={}, 回复长度={}, 耗时={}ms",
                userId, message.length(), full.length(), System.currentTimeMillis() - start);
        return full.toString();
    }

    /* ==================== 非流式兜底 ==================== */

    @Override
    public String syncChat(Long userId, ChatRequest request) {
        String message = normalizeMessage(request);
        if (message == null) {
            throw new ServiceException("消息不能为空，且不超过 2000 字");
        }
        if (!configured()) {
            throw new ServiceException("AI 服务未配置，请联系管理员设置 DEEPSEEK_API_KEY");
        }
        if (!charge(userId)) {
            throw new ServiceException("积分不足，签到后再来找我聊天吧");
        }
        try {
            List<Map<String, String>> messages = buildMessages(request, message);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("stream", false);

            HttpResponse<InputStream> response = send(body);
            if (response.statusCode() != 200) {
                String errorText = readError(response.body());
                log.warn("AI 服务返回错误状态={}, body={}", response.statusCode(), errorText);
                throw new ServiceException(friendlyError(response.statusCode()));
            }
            JsonNode node = objectMapper.readTree(response.body().readAllBytes());
            JsonNode content = node.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new ServiceException("AI 没有返回内容，请稍后再试");
            }
            return content.asText();
        } catch (ServiceException e) {
            refund(userId);
            throw e;
        } catch (Exception e) {
            refund(userId);
            log.error("AI 同步聊天异常", e);
            throw new ServiceException("我暂时连不上模型服务，稍后再试试吧");
        }
    }

    /* ==================== 内部工具 ==================== */

    private List<Map<String, String>> buildMessages(ChatRequest request, String message) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", persona));
        for (ChatRequest.ChatMessage item : request.recentHistory(MAX_HISTORY)) {
            String role = "user".equals(item.getRole()) ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", item.getContent().trim()));
        }
        messages.add(Map.of("role", "user", "content", message));
        return messages;
    }

    private HttpResponse<InputStream> send(Map<String, Object> body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        return client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
    }

    private String normalizeMessage(ChatRequest request) {
        if (request == null || StringUtils.isBlank(request.getMessage())) {
            return null;
        }
        String message = request.getMessage().trim();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return null;
        }
        return message;
    }

    private boolean configured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.contains("please-replace");
    }

    private boolean charge(Long userId) {
        return userProfileService.spendPoints(userId, CHAT_COST, "ai_chat", "chat", null, "AI 聊天");
    }

    /** 请求失败时退还本轮的积分 */
    private void refund(Long userId) {
        try {
            userProfileService.addPoints(userId, CHAT_COST, "ai_chat_refund", "chat", null, "AI 聊天失败退还");
        } catch (Exception e) {
            log.warn("退还积分失败：userId={}", userId, e);
        }
    }

    private String friendlyError(int status) {
        if (status == 401) {
            return "AI 密钥无效，请联系管理员检查配置咕噜…";
        }
        if (status == 402) {
            return "AI 账户余额不足，请联系管理员充值～";
        }
        if (status == 429) {
            return "说话太快啦，让我喘口气，稍等一下再聊～";
        }
        if (status >= 500) {
            return "AI 服务暂时打瞌睡了，稍后再试试吧～";
        }
        return "AI 服务返回异常（" + status + "）";
    }

    private String readError(InputStream body) {
        try {
            return new String(body.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private void sendAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().data(message));
            emitter.complete();
        } catch (Exception e) {
            log.error("发送 SSE 消息失败", e);
        }
    }
}
