package com.intelligentnotes.service;

import com.intelligentnotes.model.Note;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LocalSummaryService implements AISummaryService {

    @Override
    public String summarizeNoteContent(String content) {
        // Strip HTML tags
        String plainText = stripHtml(content);

        // If content is very short, don't summarize
        if (plainText.length() < 200) {
            return plainText;
        }

        // Extract key sentences
        List<String> sentences = splitIntoSentences(plainText);
        List<String> keySentences = extractKeySentences(sentences, 5);

        return String.join(" ", keySentences);
    }

    @Override
    public String summarizeFolderContent(List<Note> notes) {
        if (notes.isEmpty()) {
            return "This folder is empty.";
        }

        // Extract titles and short previews
        StringBuilder summary = new StringBuilder("This folder contains ");
        summary.append(notes.size()).append(" notes");

        // Get top 3 most common words across all notes
        Map<String, Integer> wordFrequency = new HashMap<>();
        for (Note note : notes) {
            String plainText = stripHtml(note.getContent());
            for (String word : plainText.split("\\s+")) {
                word = word.toLowerCase().replaceAll("[^a-z]", "");
                if (word.length() > 4 && !isStopWord(word)) {
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }

        // Find top themes
        List<String> topWords = wordFrequency.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!topWords.isEmpty()) {
            summary.append(". Common themes include: ").append(String.join(", ", topWords));
        }

        // Add titles of a few notes
        if (notes.size() <= 5) {
            summary.append(". Notes: ");
            List<String> titles = notes.stream()
                    .map(Note::getTitle)
                    .collect(Collectors.toList());
            summary.append(String.join(", ", titles));
        } else {
            summary.append(". Some notes: ");
            List<String> titles = notes.subList(0, 5).stream()
                    .map(Note::getTitle)
                    .collect(Collectors.toList());
            summary.append(String.join(", ", titles))
                    .append(", and ").append(notes.size() - 5).append(" more.");
        }

        return summary.toString();
    }

    public String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = Pattern.compile("[^.!?\\s][^.!?]*(?:[.!?](?!['\"]?\\s|$)[^.!?]*)*[.!?]?['\"]?(?=\\s|$)")
                .matcher(text);

        while (matcher.find()) {
            sentences.add(matcher.group());
        }

        return sentences;
    }

    private List<String> extractKeySentences(List<String> sentences, int count) {
        if (sentences.size() <= count) {
            return new ArrayList<>(sentences);
        }

        // Simple approach: take first sentence, one from middle, and last sentence
        List<String> key = new ArrayList<>();
        key.add(sentences.get(0)); // First sentence often has context

        // Add a sentence from the middle
        if (count >= 2) {
            key.add(sentences.get(sentences.size() / 2));
        }

        // Add the last sentence if we want 3 or more
        if (count >= 3) {
            key.add(sentences.get(sentences.size() - 1));
        }

        // Add more sentences if requested
        if (count > 3) {
            int step = sentences.size() / (count - 2);
            for (int i = 1; key.size() < count && i < sentences.size() - 1; i += step) {
                if (i != sentences.size() / 2) { // Skip the middle one we already added
                    key.add(sentences.get(i));
                }
            }
        }

        return key;
    }

    private boolean isStopWord(String word) {
        // Common English stop words
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "the", "and", "that", "have", "this", "with", "from", "they", "will",
                "would", "there", "their", "what", "about", "which", "when", "were", "into"
        ));

        return stopWords.contains(word);
    }
}