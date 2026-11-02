package com.weidey.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 聊天请求
 *
 * @author weidey
 */
public class ChatRequest {

    /** 本轮用户消息 */
    private String message;

    /** 历史对话（可选，按时间正序，最多保留 12 条） */
    private List<ChatMessage> history;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<ChatMessage> history) {
        this.history = history;
    }

    /** 取最近 N 条有效历史（role 只允许 user/assistant） */
    public List<ChatMessage> recentHistory(int limit) {
        List<ChatMessage> result = new ArrayList<>();
        if (history == null) {
            return result;
        }
        for (ChatMessage item : history) {
            if (item == null || item.getContent() == null || item.getContent().isBlank()) {
                continue;
            }
            String role = item.getRole();
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            result.add(item);
        }
        if (result.size() > limit) {
            return new ArrayList<>(result.subList(result.size() - limit, result.size()));
        }
        return result;
    }

    /** 单条历史消息 */
    public static class ChatMessage {

        private String role;
        private String content;

        public ChatMessage() {
        }

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
