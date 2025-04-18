// Update the StorageService.java interface:

package com.intelligentnotes.service;
import com.intelligentnotes.model.Folder;
import com.intelligentnotes.model.Note;
import com.intelligentnotes.model.SearchResult;

import java.io.IOException;
import java.util.List;

public interface StorageService {

    void saveNote(Note note, Folder parent) throws IOException;

    void saveFolder(Folder folder, Folder parent) throws IOException;

    void deleteNote(Note note, Folder parent);

    void deleteFolder(Folder folder, Folder parent);

    List<Folder> getRootFolders();

    void removeRootFolder(Folder folder);

    List<SearchResult> searchNotes(String query);
}