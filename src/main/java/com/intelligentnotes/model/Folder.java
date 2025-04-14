package com.intelligentnotes.model;

import java.util.ArrayList;
import java.util.List;

public class Folder {
    private String id;
    private String name;
    private List<Note> notes;
    private List<Folder> subFolders;
    private String summary;

    public Folder() {
        this.notes = new ArrayList<>();
        this.subFolders = new ArrayList<>();
    }

    public Folder(String id, String name) {
        this.id = id;
        this.name = name;
        this.notes = new ArrayList<>();
        this.subFolders = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }

    public List<Folder> getSubFolders() {
        return subFolders;
    }

    public void setSubFolders(List<Folder> subFolders) {
        this.subFolders = subFolders;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void addNote(Note note) {
        if (notes == null) {
            notes = new ArrayList<>();
        }
        notes.add(note);
    }

    public void removeNote(Note note) {
        if (notes != null) {
            notes.remove(note);
        }
    }

    public void addSubFolder(Folder folder) {
        if (subFolders == null) {
            subFolders = new ArrayList<>();
        }
        subFolders.add(folder);
    }

    public void removeSubFolder(Folder folder) {
        if (subFolders != null) {
            subFolders.remove(folder);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Folder folder = (Folder) o;
        return id.equals(folder.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}