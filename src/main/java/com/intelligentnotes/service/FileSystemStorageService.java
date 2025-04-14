package com.intelligentnotes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intelligentnotes.model.Folder;
import com.intelligentnotes.model.Note;
import com.intelligentnotes.model.SearchResult;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class FileSystemStorageService implements StorageService {
    private static final String BASE_STORAGE_PATH = "data/";
    private static final String ROOT_FOLDERS_FILE = "root_folders.json";
    private ObjectMapper objectMapper;

    public FileSystemStorageService() {
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Ensure storage directory exists
        File storageDir = new File(BASE_STORAGE_PATH);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
    }

    @Override
    public void saveNote(Note note, Folder parent) {
        // Generate ID if new note
        if (note.getId() == null) {
            note.setId(UUID.randomUUID().toString());
        }

        // Update timestamps
        LocalDateTime now = LocalDateTime.now();
        if (note.getCreatedAt() == null) {
            note.setCreatedAt(now);
        }
        note.setUpdatedAt(now);

        try {
            // Save note file
            String folderPath = getFolderPath(parent);
            File noteFile = new File(folderPath + "note_" + note.getId() + ".json");
            objectMapper.writeValue(noteFile, note);

            // Update parent folder structure if this is a new note
            if (parent != null && !parent.getNotes().contains(note)) {
                parent.getNotes().add(note);
                saveFolder(parent, getParentFolder(parent));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveFolder(Folder folder, Folder parent) {
        // Generate ID if new folder
        if (folder.getId() == null) {
            folder.setId(UUID.randomUUID().toString());
        }

        try {
            // Create folder directory if it doesn't exist
            String folderPath = BASE_STORAGE_PATH + "folder_" + folder.getId() + "/";
            File folderDir = new File(folderPath);
            if (!folderDir.exists()) {
                folderDir.mkdirs();
            }

            // Save folder metadata
            File metadataFile = new File(folderPath + "metadata.json");
            objectMapper.writeValue(metadataFile, folder);

            // If this is a root folder, update the root folders list
            if (parent == null) {
                List<Folder> rootFolders = getRootFolders();
                if (!rootFolders.contains(folder)) {
                    rootFolders.add(folder);
                    saveRootFolders(rootFolders);
                }
            } else {
                // Update parent folder structure if this is a new subfolder
                if (!parent.getSubFolders().contains(folder)) {
                    parent.getSubFolders().add(folder);
                    saveFolder(parent, getParentFolder(parent));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteNote(Note note, Folder parent) {
        try {
            // Delete note file
            String folderPath = getFolderPath(parent);
            File noteFile = new File(folderPath + "note_" + note.getId() + ".json");
            if (noteFile.exists()) {
                noteFile.delete();
            }

            // Update parent folder structure
            if (parent != null) {
                parent.getNotes().remove(note);
                saveFolder(parent, getParentFolder(parent));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteFolder(Folder folder, Folder parent) {
        try {
            // Delete all notes in this folder
            for (Note note : new ArrayList<>(folder.getNotes())) {
                deleteNote(note, folder);
            }

            // Delete all subfolders recursively
            for (Folder subFolder : new ArrayList<>(folder.getSubFolders())) {
                deleteFolder(subFolder, folder);
            }

            // Delete folder directory
            String folderPath = BASE_STORAGE_PATH + "folder_" + folder.getId() + "/";
            File folderDir = new File(folderPath);
            if (folderDir.exists()) {
                File[] files = folderDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                folderDir.delete();
            }

            // Update parent folder structure or root folders list
            if (parent != null) {
                parent.getSubFolders().remove(folder);
                saveFolder(parent, getParentFolder(parent));
            } else {
                List<Folder> rootFolders = getRootFolders();
                rootFolders.remove(folder);
                saveRootFolders(rootFolders);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Folder> getRootFolders() {
        List<Folder> rootFolders = new ArrayList<>();

        try {
            File rootFoldersFile = new File(BASE_STORAGE_PATH + ROOT_FOLDERS_FILE);
            if (rootFoldersFile.exists()) {
                rootFolders = objectMapper.readValue(rootFoldersFile,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Folder.class));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rootFolders;
    }

    @Override
    public void removeRootFolder(Folder folder) {
        List<Folder> rootFolders = getRootFolders();
        rootFolders.remove(folder);
        saveRootFolders(rootFolders);
    }

    private void saveRootFolders(List<Folder> rootFolders) {
        try {
            File rootFoldersFile = new File(BASE_STORAGE_PATH + ROOT_FOLDERS_FILE);
            objectMapper.writeValue(rootFoldersFile, rootFolders);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFolderPath(Folder folder) {
        if (folder == null) {
            return BASE_STORAGE_PATH;
        }
        return BASE_STORAGE_PATH + "folder_" + folder.getId() + "/";
    }

    private Folder getParentFolder(Folder folder) {
        // This is a simplified version - in a real implementation,
        // you would need to traverse the folder structure
        // or maintain parent references
        List<Folder> allFolders = getAllFolders();

        for (Folder potentialParent : allFolders) {
            if (potentialParent.getSubFolders().contains(folder)) {
                return potentialParent;
            }
        }

        return null;
    }

    private List<Folder> getAllFolders() {
        List<Folder> allFolders = new ArrayList<>(getRootFolders());

        for (Folder rootFolder : getRootFolders()) {
            collectSubfolders(rootFolder, allFolders);
        }

        return allFolders;
    }

    private void collectSubfolders(Folder folder, List<Folder> allFolders) {
        for (Folder subFolder : folder.getSubFolders()) {
            allFolders.add(subFolder);
            collectSubfolders(subFolder, allFolders);
        }
    }

    @Override
    public List<SearchResult> searchNotes(String query) {
        List<SearchResult> results = new ArrayList<>();
        searchInFolder(null, getRootFolders(), query, "", results);
        return results;
    }

    private void searchInFolder(Folder parent, List<Folder> folders, String query,
                                String pathPrefix, List<SearchResult> results) {
        for (Folder folder : folders) {
            String currentPath = pathPrefix.isEmpty() ?
                    folder.getName() : pathPrefix + " > " + folder.getName();

            // Search in notes
            for (Note note : folder.getNotes()) {
                if (matchesQuery(note, query)) {
                    String previewText = generatePreview(note.getContent(), query);
                    results.add(new SearchResult(note, folder, currentPath, previewText));
                }
            }

            // Recursively search in subfolders
            searchInFolder(folder, folder.getSubFolders(), query, currentPath, results);
        }
    }

    private boolean matchesQuery(Note note, String query) {
        String lowerQuery = query.toLowerCase();

        // Check title
        if (note.getTitle().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Check content (strip HTML for text search)
        String plainContent = stripHtml(note.getContent()).toLowerCase();
        return plainContent.contains(lowerQuery);
    }

    private String stripHtml(String html) {
        // Simple HTML stripping for search
        return html.replaceAll("<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String generatePreview(String content, String query) {
        // Generate a preview with context around the match
        String plainContent = stripHtml(content);
        String lowerContent = plainContent.toLowerCase();
        String lowerQuery = query.toLowerCase();

        int matchPos = lowerContent.indexOf(lowerQuery);
        if (matchPos == -1) return plainContent.substring(0, Math.min(100, plainContent.length())) + "...";

        // Get some context around the match
        int previewStart = Math.max(0, matchPos - 50);
        int previewEnd = Math.min(plainContent.length(), matchPos + query.length() + 50);

        String preview = plainContent.substring(previewStart, previewEnd);

        // Add ellipsis if needed
        if (previewStart > 0) preview = "..." + preview;
        if (previewEnd < plainContent.length()) preview = preview + "...";

        return preview;
    }
}