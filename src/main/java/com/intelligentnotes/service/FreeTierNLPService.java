//package com.intelligentnotes.service;
//
//
//import com.intelligentnotes.model.Note;
//import okhttp3.*;
//import org.json.JSONObject;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//public class FreeTierNLPService implements AISummaryService {
//    private static final String API_URL = "https://api-inference.huggingface.co/models/facebook/bart-large-cnn";
//    private final OkHttpClient client;
//    private final LocalSummaryService fallbackService;
//    private final String apiToken;
//
//    public FreeTierNLPService(String apiToken) {
//        this.apiToken = apiToken;
//        this.client = new OkHttpClient.Builder()
//                .connectTimeout(30, TimeUnit.SECONDS)
//                .readTimeout(30, TimeUnit.SECONDS)
//                .build();
//        this.fallbackService = new LocalSummaryService();
//    }
//
//    @Override
//    public String summarizeNoteContent(String content) {
//        // Strip HTML
//        String plainText = fallbackService.stripHtml(content);
//
//        // If text is very short, don't summarize
//        if (plainText.length() < 200) {
//            return plainText;
//        }
//
//        try {
//            // Limit text length for API
//            String textToSummarize = plainText.substring(0, Math.min(1024, plainText.length()));
//
//            // Prepare request
//            JSONObject requestBody = new JSONObject();
//            requestBody.put("inputs", textToSummarize);
//            requestBody.put("parameters", new JSONObject()
//                    .put("max_length", 100)
//                    .put("min_length", 30)
//            );
//
//            RequestBody body = RequestBody.create(
//                    requestBody.toString(),
//                    MediaType.parse("application/json")
//            );
//
//            Request request = new Request.Builder()
//                    .url(API_URL)
//                    .addHeader("Authorization", "Bearer " + apiToken)
//                    .addHeader("Content-Type", "application/json")
//                    .post(body)
//                    .build();
//
//            // Execute request
//            try (Response response = client.newCall(request).execute()) {
//                if (!response.isSuccessful()) {
//                    // Fallback to local summarization if API fails
//                    return fallbackService.summarizeNoteContent(content);
//                }
//
//                String responseBody = response.body().string();
//                JSONObject jsonResponse = new JSONObject(responseBody);
//
//                // Extract summary from response
//                return jsonResponse.getJSONArray("summary_text").getString(0);
//            }
//        } catch (Exception e) {
//            // Fallback to local summarization
//            return fallbackService.summarizeNoteContent(content);
//        }
//    }
//    @Override
//    public String summarizeFolderContent(List<Note> notes) {
//        if (notes.isEmpty()) {
//            return "This folder is empty.";
//        }
//
//        try {
//            // Prepare the content to summarize
//            StringBuilder notesContent = new StringBuilder();
//            for (int i = 0; i < Math.min(5, notes.size()); i++) {
//                Note note = notes.get(i);
//                String plainText = fallbackService.stripHtml(note.getContent());
//
//                notesContent.append("Title: ").append(note.getTitle()).append("\n");
//                notesContent.append("Content: ").append(plainText.substring(0, Math.min(200, plainText.length()))).append("\n\n");
//            }
//
//            if (notes.size() > 5) {
//                notesContent.append("(and ").append(notes.size() - 5).append(" more notes)");
//            }
//
//            // Limit text length for API
//            String textToSummarize = notesContent.toString();
//            textToSummarize = textToSummarize.substring(0, Math.min(1024, textToSummarize.length()));
//
//            // Prepare request
//            JSONObject requestBody = new JSONObject();
//            requestBody.put("inputs", textToSummarize);
//            requestBody.put("parameters", new JSONObject()
//                    .put("max_length", 150)
//                    .put("min_length", 40)
//            );
//
//            RequestBody body = RequestBody.create(
//                    requestBody.toString(),
//                    MediaType.parse("application/json")
//            );
//
//            Request request = new Request.Builder()
//                    .url(API_URL)
//                    .addHeader("Authorization", "Bearer " + apiToken)
//                    .addHeader("Content-Type", "application/json")
//                    .post(body)
//                    .build();
//
//            // Execute request
//            try (Response response = client.newCall(request).execute()) {
//                if (!response.isSuccessful()) {
//                    // Fallback to local summarization
//                    return fallbackService.summarizeFolderContent(notes);
//                }
//
//                String responseBody = response.body().string();
//                JSONObject jsonResponse = new JSONObject(responseBody);
//
//                // Extract summary from response
//                try {
//                    return jsonResponse.getJSONArray("summary_text").getString(0);
//                } catch (Exception e) {
//                    // Different response format, try alternative parsing
//                    return jsonResponse.getString("summary_text");
//                }
//            }
//        } catch (Exception e) {
//            // Fallback to local summarization
//            return fallbackService.summarizeFolderContent(notes);
//        }
//    }
//}