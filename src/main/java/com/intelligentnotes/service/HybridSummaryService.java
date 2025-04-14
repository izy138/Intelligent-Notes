//package com.intelligentnotes.service;
//
//import com.intelligentnotes.model.Note;
//
//import java.util.List;
//
//public class HybridSummaryService implements AISummaryService {
//    private final LocalSummaryService localService;
//    private FreeTierNLPService huggingfaceService;
//    private ClaudeAISummaryService claudeService;
//    private String activeServiceType = "local"; // "local", "huggingface", or "claude"
////
//    public HybridSummaryService() {
//        this.localService = new LocalSummaryService();
//        this.huggingfaceService = null;
//        this.claudeService = null;
//    }
//
//    public void configureHuggingfaceService(String apiKey) {
//        if (apiKey != null && !apiKey.isEmpty()) {
//            this.huggingfaceService = new FreeTierNLPService(apiKey);
//            this.activeServiceType = "huggingface";
//        } else {
//            this.huggingfaceService = null;
//            this.activeServiceType = "local";
//        }
//    }
//
//    public void configureClaudeService(String apiKey) {
//        if (apiKey != null && !apiKey.isEmpty()) {
//            this.claudeService = new ClaudeAISummaryService(apiKey);
//            this.activeServiceType = "claude";
//        } else {
//            this.claudeService = null;
//            if (huggingfaceService != null) {
//                this.activeServiceType = "huggingface";
//            } else {
//                this.activeServiceType = "local";
//            }
//        }
//    }
//
//    @Override
//    public String summarizeNoteContent(String content) {
//        if ("claude".equals(activeServiceType) && claudeService != null) {
//            try {
//                String apiSummary = claudeService.summarizeNoteContent(content);
//                if (apiSummary != null && !apiSummary.isEmpty()) {
//                    return apiSummary;
//                }
//            } catch (Exception e) {
//                // Fallback to next available service
//                System.err.println("Claude summarization failed: " + e.getMessage());
//            }
//        }
//
//        if ("huggingface".equals(activeServiceType) && huggingfaceService != null) {
//            try {
//                String apiSummary = huggingfaceService.summarizeNoteContent(content);
//                if (apiSummary != null && !apiSummary.isEmpty()) {
//                    return apiSummary;
//                }
//            } catch (Exception e) {
//                // Fallback to local if API fails
//                System.err.println("Huggingface summarization failed: " + e.getMessage());
//            }
//        }
//
//        // Use local summarization as default or fallback
//        return localService.summarizeNoteContent(content);
//    }
//
//    @Override
//    public String summarizeFolderContent(List<Note> notes) {
//        if ("claude".equals(activeServiceType) && claudeService != null) {
//            try {
//                String apiSummary = claudeService.summarizeFolderContent(notes);
//                if (apiSummary != null && !apiSummary.isEmpty()) {
//                    return apiSummary;
//                }
//            } catch (Exception e) {
//                // Fallback to next available service
//                System.err.println("Claude folder summarization failed: " + e.getMessage());
//            }
//        }
//
//        if ("huggingface".equals(activeServiceType) && huggingfaceService != null) {
//            try {
//                String apiSummary = huggingfaceService.summarizeFolderContent(notes);
//                if (apiSummary != null && !apiSummary.isEmpty()) {
//                    return apiSummary;
//                }
//            } catch (Exception e) {
//                // Fallback to local if API fails
//                System.err.println("Huggingface folder summarization failed: " + e.getMessage());
//            }
//        }
//
//        // Use local summarization as default or fallback
//        return localService.summarizeFolderContent(notes);
//    }
//
//    public String getActiveServiceType() {
//        return activeServiceType;
//    }
//}