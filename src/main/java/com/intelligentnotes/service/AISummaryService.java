package com.intelligentnotes.service;

import com.intelligentnotes.model.Note;

import java.util.List;

public interface AISummaryService {
    String summarizeNoteContent(String content);
    String summarizeFolderContent(List<Note> notes);
}