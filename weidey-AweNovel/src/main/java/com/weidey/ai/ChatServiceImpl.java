package com.weidey.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weidey.common.exception.ServiceException;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 看板娘 AI 聊天服务实现（DeepSeek）
 *
 * @author weidey
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    /** 每次聊天消耗积分 */
    private static final int CHAT_COST = 1;

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${ai.deepseek.persona:你是伊卡洛斯（Ikaros），来自《天降之物》的娱乐用万能天使。你性格温柔、天然呆、略带呆萌，称呼用户为「主人」。请用简短、可爱、轻松的语气回复，像一位贴心的小女仆一样陪伴用户聊天。}")
    private String persona;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    private final UserProfileService userProfileService;

    public ChatServiceImpl(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Override
    public void streamChat(Long userId, String message, SseEmitter emitter) {
        // 扣积分（每次 1 积分）
        boolean ok = userProfileService.spendPoints(userId, CHAT_COST, "ai_chat", "chat", null, "AI 聊天");
        if (!ok) {
            sendAndComplete(emitter, "积分不足，无法与看板娘聊天，请先签到获取积分～");
            return;
        }
        executor.submit(() -> doStream(message, emitter));
    }

    private void doStream(String message, SseEmitter emitter) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                sendAndComplete(emitter, "AI 服务未配置，请联系管理员设置 DEEPSEEK_API_KEY");
                return;
            }

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", persona));
            messages.add(Map.of("role", "user", "content", message));

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", messages,
                    "stream", true
            );
            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                sendAndComplete(emitter, "AI 服务返回错误：" + response.statusCode());
                return;
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
                        if (delta != null && delta.has("content")) {
                            String content = delta.get("content").asText();
                            full.append(content);
                            emitter.send(SseEmitter.event().data(content));
                        }
                    }
                }
            }
            emitter.complete();
            log.debug("AI 聊天完成，用户消息长度={}, 回复长度={}", message.length(), full.length());
        } catch (Exception e) {
            log.error("AI 聊天异常", e);
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
            }
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
