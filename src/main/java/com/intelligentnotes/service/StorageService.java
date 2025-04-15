// Update the StorageService.java interface:

package com.intelligentnotes.service;
import com.intelligentnotes.model.Folder;
import com.intelligentnotes.model.Note;
import com.intelligentnotes.model.SearchResult;

import java.io.IOException;
import java.util.List;

public interface StorageService {
    /**
     * Saves a note to storage
     * @param note The note to save
     * @param parent The parent folder
     * @throws IOException If saving fails
     */
    void saveNote(Note note, Folder parent) throws IOException;

    /**
     * Saves a folder to storage
     * @param folder The folder to save
     * @param parent The parent folder (null if this is a root folder)
     * @throws IOException If saving fails
     */
    void saveFolder(Folder folder, Folder parent) throws IOException;

    void deleteNote(Note note, Folder parent);

    void deleteFolder(Folder folder, Folder parent);

    List<Folder> getRootFolders();

    void removeRootFolder(Folder folder);

    List<SearchResult> searchNotes(String query);
}