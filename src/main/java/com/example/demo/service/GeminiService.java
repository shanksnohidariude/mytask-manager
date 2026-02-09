package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        this.webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public String generateTaskPlan(String goal, int weeklyFrequency, int deadlineWeeks) {
        // 総タスク数を計算（週の頻度 × 週数）
        int totalTasks = weeklyFrequency * deadlineWeeks;
        
        // タスク数が多すぎる場合は調整（最大20個まで）
        if (totalTasks > 20) {
            totalTasks = 20;
        }
        
        // タスク数が少なすぎる場合も調整（最低5個）
        if (totalTasks < 5) {
            totalTasks = 5;
        }
        
        String prompt = String.format(
            "ユーザーの目標: %s\n\n" +
            "条件:\n" +
            "- 週に%d回取り組む予定\n" +
            "- 期限まで%d週間\n" +
            "- 合計で約%d個のタスクを生成してください\n\n" +
            "この目標を達成するための具体的なタスクを生成してください。\n" +
            "各タスクは実行可能で段階的な内容にしてください。\n" +
            "週%d回のペースに合わせて、適切な間隔でタスクを配置してください。\n\n" +
            "以下のJSON形式で出力してください（他の文章は不要です）：\n" +
            "[\n" +
            "  {\"title\": \"タスク名\", \"description\": \"詳細説明\", \"daysFromNow\": 7},\n" +
            "  {\"title\": \"タスク名2\", \"description\": \"詳細説明2\", \"daysFromNow\": 14}\n" +
            "]\n" +
            "daysFromNowは今日から何日後に期限とするかを数値で指定してください。\n" +
            "最終タスクの期限は約%d日後（%d週間後）になるようにしてください。",
            goal,
            weeklyFrequency,
            deadlineWeeks,
            totalTasks,
            weeklyFrequency,
            deadlineWeeks * 7,
            deadlineWeeks
        );

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", prompt)
                    )
                )
            )
        );

        try {
            System.out.println("=== Gemini APIリクエスト ===");
            
            String response = webClient.post()
                .uri("/v1beta/models/gemini-2.5-flash:generateContent")
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            System.out.println("=== Gemini APIレスポンス成功 ===");
            
            return extractTextFromResponse(response);
            
        } catch (WebClientResponseException e) {
            System.err.println("=== Gemini APIエラー詳細 ===");
            System.err.println("ステータスコード: " + e.getStatusCode());
            System.err.println("レスポンスボディ: " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            System.err.println("Gemini API呼び出しエラー: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String extractTextFromResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                return candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("レスポンス解析エラー: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}