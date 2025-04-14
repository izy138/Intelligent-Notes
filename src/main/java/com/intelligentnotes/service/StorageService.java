package com.intelligentnotes.service;
import com.intelligentnotes.model.Folder;
import com.intelligentnotes.model.Note;
import com.intelligentnotes.model.SearchResult;

import java.util.List;

public interface StorageService {
    void saveNote(Note note, Folder parent);

    void saveFolder(Folder folder, Folder parent);

    void deleteNote(Note note, Folder parent);

    void deleteFolder(Folder folder, Folder parent);

    List<Folder> getRootFolders();

    void removeRootFolder(Folder folder);

    List<SearchResult> searchNotes(String query);
}