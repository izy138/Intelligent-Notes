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
    public void saveNote(Note note, Folder parent) throws IOException {
        if (note == null) {
            throw new IllegalArgumentException("Cannot save null note");
        }

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
            File folderDir = new File(folderPath);

            // Ensure directory exists
            if (!folderDir.exists()) {
                boolean created = folderDir.mkdirs();
                if (!created) {
                    throw new IOException("Failed to create directory: " + folderPath);
                }
            }

            File noteFile = new File(folderPath + "note_" + note.getId() + ".json");
            objectMapper.writeValue(noteFile, note);

            // Update parent folder structure if this is a new note
            if (parent != null && !parent.getNotes().contains(note)) {
                parent.getNotes().add(note);
                saveFolder(parent, getParentFolder(parent));
            }
        } catch (IOException e) {
            System.err.println("Error saving note: " + e.getMessage());
            throw e; // Re-throw the exception to let the caller handle it
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

                // Log information about loaded root folders
                System.out.println("Loaded " + rootFolders.size() + " root folders from storage");

                // Ensure each folder's notes and subfolders are fully loaded
                for (Folder folder : rootFolders) {
                    ensureFolderFullyLoaded(folder);
                }
            } else {
                System.out.println("Root folders file does not exist at: " + rootFoldersFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Error loading root folders: " + e.getMessage());
            e.printStackTrace();
        }

        return rootFolders;
    }

    // Add this helper method to ensure folders are fully loaded
    private void ensureFolderFullyLoaded(Folder folder) {
        if (folder == null) return;

        // Check folder path
        String folderPath = BASE_STORAGE_PATH + "folder_" + folder.getId() + "/";
        File folderDir = new File(folderPath);

        if (!folderDir.exists()) {
            System.out.println("Warning: Folder directory does not exist for folder: " + folder.getName());
            return;
        }

        // Ensure notes list exists
        if (folder.getNotes() == null) {
            folder.setNotes(new ArrayList<>());
        }

        // Ensure subfolders list exists
        if (folder.getSubFolders() == null) {
            folder.setSubFolders(new ArrayList<>());
        }

        // Load folder metadata file to get the most up-to-date info
        try {
            File metadataFile = new File(folderPath + "metadata.json");
            if (metadataFile.exists()) {
                Folder updatedFolder = objectMapper.readValue(metadataFile, Folder.class);

                // Update folder with the latest data
                folder.setName(updatedFolder.getName());
                folder.setNotes(updatedFolder.getNotes());
                folder.setSubFolders(updatedFolder.getSubFolders());
                folder.setSummary(updatedFolder.getSummary());

                System.out.println("Loaded folder '" + folder.getName() + "' with " +
                        folder.getNotes().size() + " notes and " +
                        folder.getSubFolders().size() + " subfolders");
            }
        } catch (IOException e) {
            System.err.println("Error loading folder metadata for folder " + folder.getName() + ": " + e.getMessage());
        }

        // Recursively ensure all subfolders are loaded
        for (Folder subFolder : folder.getSubFolders()) {
            ensureFolderFullyLoaded(subFolder);
        }
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

        // First ensure we have the most up-to-date data
        List<Folder> rootFolders = getRootFolders();

        // Log the search operation
        System.out.println("Searching for \"" + query + "\" in " + rootFolders.size() + " root folders");

        if (rootFolders.isEmpty()) {
            System.out.println("No root folders found.");
            return results; // Return empty list if no folders
        }

        // Now perform the search
        searchInFolder(null, rootFolders, query, "", results);

        // Log the results
        System.out.println("Found " + results.size() + " results for \"" + query + "\"");

        return results;
    }

    private void searchInFolder(Folder parent, List<Folder> folders, String query,
                                String pathPrefix, List<SearchResult> results) {
        for (Folder folder : folders) {
            String currentPath = pathPrefix.isEmpty() ?
                    folder.getName() : pathPrefix + " > " + folder.getName();

            System.out.println("Searching in folder: " + currentPath);

            // Ensure folder notes are loaded correctly
            if (folder.getNotes() == null) {
                System.out.println("Warning: Notes list is null for folder: " + folder.getName());
                folder.setNotes(new ArrayList<>());
            } else {
                System.out.println("Folder has " + folder.getNotes().size() + " notes");

                // Search in notes
                for (Note note : folder.getNotes()) {
                    if (matchesQuery(note, query)) {
                        String previewText = generatePreview(note.getContent(), query);
                        System.out.println("Match found in note: " + note.getTitle());
                        results.add(new SearchResult(note, folder, currentPath, previewText));
                    }
                }
            }

            // Ensure subfolder list is loaded correctly
            if (folder.getSubFolders() == null) {
                System.out.println("Warning: Subfolders list is null for folder: " + folder.getName());
                folder.setSubFolders(new ArrayList<>());
            } else {
                System.out.println("Folder has " + folder.getSubFolders().size() + " subfolders");

                // Recursively search in subfolders
                searchInFolder(folder, folder.getSubFolders(), query, currentPath, results);
            }
        }
    }

    private boolean matchesQuery(Note note, String query) {
        if (note == null) {
            System.out.println("Warning: Note is null during search");
            return false;
        }

        String lowerQuery = query.toLowerCase();

        // Check title
        if (note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Check content (strip HTML for text search)
        if (note.getContent() != null) {
            String plainContent = stripHtml(note.getContent()).toLowerCase();
            return plainContent.contains(lowerQuery);
        }

        return false;
    }

    private String generatePreview(String content, String query) {
        if (content == null) {
            return "No content available";
        }

        // Generate a preview with context around the match
        String plainContent = stripHtml(content);
        String lowerContent = plainContent.toLowerCase();
        String lowerQuery = query.toLowerCase();

        int matchPos = lowerContent.indexOf(lowerQuery);
        if (matchPos == -1) {
            // If no exact match found (shouldn't happen normally), return the beginning
            return plainContent.substring(0, Math.min(100, plainContent.length())) + "...";
        }

        // Get some context around the match
        int previewStart = Math.max(0, matchPos - 50);
        int previewEnd = Math.min(plainContent.length(), matchPos + query.length() + 50);

        String preview = plainContent.substring(previewStart, previewEnd);

        // Add ellipsis if needed
        if (previewStart > 0) preview = "..." + preview;
        if (previewEnd < plainContent.length()) preview = preview + "...";

        return preview;
    }


    private String stripHtml(String html) {
        // Simple HTML stripping for search
        return html.replaceAll("<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

}