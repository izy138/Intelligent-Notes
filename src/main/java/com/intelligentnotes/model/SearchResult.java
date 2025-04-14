package com.intelligentnotes.model;

public class SearchResult {
    private Note note;
    private Folder parentFolder;
    private String path;
    private String previewText;

    public SearchResult(Note note, Folder parentFolder, String path, String previewText) {
        this.note = note;
        this.parentFolder = parentFolder;
        this.path = path;
        this.previewText = previewText;
    }

    // Getters and Setters
    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public Folder getParentFolder() {
        return parentFolder;
    }

    public void setParentFolder(Folder parentFolder) {
        this.parentFolder = parentFolder;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPreviewText() {
        return previewText;
    }

    public void setPreviewText(String previewText) {
        this.previewText = previewText;
    }
}