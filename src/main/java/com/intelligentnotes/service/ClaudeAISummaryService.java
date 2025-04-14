package com.intelligentnotes.service;

import com.intelligentnotes.model.Note;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ClaudeAISummaryService implements AISummaryService {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private final OkHttpClient client;
    private final LocalSummaryService fallbackService;
    private final String apiKey;
    private static final String CLAUDE_MODEL = "claude-3-haiku-20240307";

    public ClaudeAISummaryService(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.fallbackService = new LocalSummaryService();
    }

    @Override
    public String summarizeNoteContent(String content) {
        // If no API key is provided, use local summarization
        if (apiKey == null || apiKey.isEmpty()) {
            return fallbackService.summarizeNoteContent(content);
        }

        // Strip HTML
        String plainText = fallbackService.stripHtml(content);

        // If text is very short, don't summarize
        if (plainText.length() < 200) {
            return plainText;
        }

        try {
            // Limit text length to avoid excessive tokens
            String textToSummarize = plainText.substring(0, Math.min(10000, plainText.length()));

            // Construct the prompt
            String prompt = "Please summarize the following text in a concise paragraph:\n\n" + textToSummarize;

            // Prepare request
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", CLAUDE_MODEL);
            requestBody.put("max_tokens", 1000);

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.put(message);

            requestBody.put("messages", messages);

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            // Execute request
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Claude API error: " + response.code() + " " + response.message());
                    System.err.println(response.body().string());
                    // Fallback to local summarization if API fails
                    return fallbackService.summarizeNoteContent(content);
                }

                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);

                // Extract the summary from Claude's response
                JSONObject contentObj = jsonResponse.getJSONObject("content");
                String summary = contentObj.getString("text");

                return summary.trim();
            }
        } catch (Exception e) {
            System.err.println("Error calling Claude API: " + e.getMessage());
            e.printStackTrace();
            // Fallback to local summarization
            return fallbackService.summarizeNoteContent(content);
        }
    }

    @Override
    public String summarizeFolderContent(List<Note> notes) {
        // If no API key is provided, use local summarization
        if (apiKey == null || apiKey.isEmpty()) {
            return fallbackService.summarizeFolderContent(notes);
        }

        if (notes.isEmpty()) {
            return "This folder is empty.";
        }

        try {
            // Prepare the content to summarize
            StringBuilder notesContent = new StringBuilder();
            for (int i = 0; i < Math.min(10, notes.size()); i++) {
                Note note = notes.get(i);
                String plainText = fallbackService.stripHtml(note.getContent());

                notesContent.append("Title: ").append(note.getTitle()).append("\n");
                notesContent.append("Content: ").append(plainText.substring(0, Math.min(500, plainText.length()))).append("\n\n");
            }

            if (notes.size() > 10) {
                notesContent.append("(and ").append(notes.size() - 10).append(" more notes)");
            }

            // Limit text length
            String textToSummarize = notesContent.toString();
            textToSummarize = textToSummarize.substring(0, Math.min(10000, textToSummarize.length()));

            // Construct the prompt
            String prompt = "Please summarize the following collection of notes. Give an overview of the main themes and topics covered:\n\n" + textToSummarize;

            // Prepare request
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", CLAUDE_MODEL);
            requestBody.put("max_tokens", 1000);

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.put(message);

            requestBody.put("messages", messages);

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            // Execute request
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Claude API error: " + response.code() + " " + response.message());
                    System.err.println(response.body().string());
                    // Fallback to local summarization
                    return fallbackService.summarizeFolderContent(notes);
                }

                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);

                // Extract the summary from Claude's response
                JSONObject contentObj = jsonResponse.getJSONObject("content");
                String summary = contentObj.getString("text");

                return summary.trim();
            }
        } catch (Exception e) {
            System.err.println("Error calling Claude API: " + e.getMessage());
            e.printStackTrace();
            // Fallback to local summarization
            return fallbackService.summarizeFolderContent(notes);
        }
    }
}