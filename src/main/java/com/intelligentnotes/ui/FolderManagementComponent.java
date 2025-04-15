package com.intelligentnotes.ui;

import com.intelligentnotes.model.Folder;
import com.intelligentnotes.model.Note;
import com.intelligentnotes.service.AISummaryService;
import com.intelligentnotes.service.StorageService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.*;

public class FolderManagementComponent extends VBox {
    private TreeView<String> folderTreeView;
    private Map<TreeItem<String>, Object> itemsMap; // Maps TreeItems to Folders or Notes
    private StorageService storageService;
    private AISummaryService aiService;
    private NoteEditorComponent noteEditor;
    private TreeItem<String> draggedItem;
    private Label emptyLabel;

    private BorderPane mainLayout;

    public FolderManagementComponent(StorageService storageService, NoteEditorComponent noteEditor) {
        this.storageService = storageService;
        this.noteEditor = noteEditor;
        this.itemsMap = new HashMap<>();
//        this.mainLayout = mainLayout;
        this.setPadding(new Insets(10));
        this.setSpacing(5);
        noteEditor.setOnTitleChangeCallback(this::updateSelectedNoteTitle);


        // Label for section
        Label foldersLabel = new Label("FOLDERS");
        foldersLabel.setStyle("-fx-font-weight: bold;");

        // Create folder tree
        TreeItem<String> rootItem = new TreeItem<>("My Notes");
        rootItem.setExpanded(true);

        folderTreeView = new TreeView<>(rootItem);
        folderTreeView.setShowRoot(false);
        folderTreeView.setCellFactory(tv -> new FolderTreeCell());
        folderTreeView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> handleSelection(newValue));
        VBox.setVgrow(folderTreeView, Priority.ALWAYS);

        // Empty state label
        emptyLabel = new Label("No folders yet. Create one to get started!");
        emptyLabel.setWrapText(true);
        emptyLabel.setStyle("-fx-text-fill: #707070;");

        // Add components to layout
        this.getChildren().addAll(foldersLabel, folderTreeView);

        // Setup drag and drop
        setupDragAndDrop();

        // Load folders from storage
        loadFolders();

//        add this code to handle keyboard events
        folderTreeView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                deleteSelected();
                event.consume();
            }
        });

        // In FolderManagementComponent constructor, add this code at the end
        // Create action buttons
        HBox actionBar = new HBox(10);
        actionBar.setPadding(new Insets(5));

        Button newFolderBtn = new Button("+📁");
        Button newNoteBtn = new Button("+📝");
        Button deleteBtn = new Button("🗑️");

        newFolderBtn.setOnAction(e -> createNewFolder());
        newNoteBtn.setOnAction(e -> createNewNote());
        deleteBtn.setOnAction(e -> deleteSelected());

        actionBar.getChildren().addAll(newFolderBtn, newNoteBtn, deleteBtn);

        // Add action bar to the bottom of the component
        this.getChildren().add(actionBar);
    }

    public void setMainLayout(BorderPane mainLayout) {
        this.mainLayout = mainLayout;
    }

    private void updateSelectedNoteTitle() {
        TreeItem<String> selectedItem = folderTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && itemsMap.get(selectedItem) instanceof Note) {
            Note note = (Note) itemsMap.get(selectedItem);
            selectedItem.setValue(note.getTitle());
        }
    }

    public void setAiService(AISummaryService aiService) {
        this.aiService = aiService;
    }

    public void loadFolders() {
        TreeItem<String> root = folderTreeView.getRoot();
        root.getChildren().clear();
        itemsMap.clear();

        List<Folder> rootFolders = storageService.getRootFolders();

        if (rootFolders.isEmpty()) {
            // Show empty state
            if (!this.getChildren().contains(emptyLabel)) {
                this.getChildren().add(emptyLabel);
            }
        } else {
            // If we had an empty label, remove it
            this.getChildren().remove(emptyLabel);

            // Populate tree with folders and notes
            for (Folder folder : rootFolders) {
                TreeItem<String> folderItem = createFolderItem(folder);
                root.getChildren().add(folderItem);
                populateFolderItem(folderItem, folder);
            }
        }
    }

    private TreeItem<String> createFolderItem(Folder folder) {
        TreeItem<String> item = new TreeItem<>(folder.getName());
        try {
            Image folderIcon = new Image(getClass().getResourceAsStream("/images/folder_icon.png"));
            item.setGraphic(new ImageView(folderIcon));
        } catch (Exception e) {
            // If image not found, use a text icon
            item.setValue("📁 " + folder.getName());
        }
        itemsMap.put(item, folder);
        return item;
    }

    private TreeItem<String> createNoteItem(Note note) {
        TreeItem<String> item = new TreeItem<>(note.getTitle());
        try {
            Image noteIcon = new Image(getClass().getResourceAsStream("/images/note_icon.png"));
            item.setGraphic(new ImageView(noteIcon));
        } catch (Exception e) {
            // If image not found, use a text icon
            item.setValue("📝 " + note.getTitle());
        }
        itemsMap.put(item, note);
        return item;
    }

    private void populateFolderItem(TreeItem<String> folderItem, Folder folder) {
        // Add subfolders
        for (Folder subFolder : folder.getSubFolders()) {
            TreeItem<String> subFolderItem = createFolderItem(subFolder);
            folderItem.getChildren().add(subFolderItem);
            populateFolderItem(subFolderItem, subFolder);
        }

        // Add notes
        for (Note note : folder.getNotes()) {
            TreeItem<String> noteItem = createNoteItem(note);
            folderItem.getChildren().add(noteItem);
        }
    }

    private void handleSelection(TreeItem<String> item) {
        if (item == null) return;

        Object selectedObj = itemsMap.get(item);
        if (selectedObj instanceof Note) {
            Note note = (Note) selectedObj;
            Folder parentFolder = findParentFolder(item);
            noteEditor.loadNote(note, parentFolder);

            // Make sure the note editor is visible in the main layout
            if (mainLayout != null) {
                System.out.println("Setting note editor as center content after selection");
                mainLayout.setCenter(noteEditor);
            } else {
                System.out.println("Main layout is null when handling selection");
            }
        } else if (selectedObj instanceof Folder) {
            // If a folder is selected, we could add folder summary view here
            // But for now, we'll leave the current view as is
            System.out.println("Folder selected: " + item.getValue());
        }
    }

    public Folder findParentFolder(TreeItem<String> item) {
        if (item == null) return null;

        TreeItem<String> parentItem = item.getParent();
        if (parentItem == null || parentItem == folderTreeView.getRoot()) {
            // This is a root-level item or has no parent
            return null;
        }

        Object obj = itemsMap.get(parentItem);
        if (obj instanceof Folder) {
            return (Folder) obj;
        }

        return null; // Shouldn't happen with proper structure
    }

    public void createNewFolder() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create a new folder");
        dialog.setContentText("Folder name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            // Create new folder
            Folder newFolder = new Folder();
            newFolder.setId(UUID.randomUUID().toString());
            newFolder.setName(name);
            newFolder.setNotes(new ArrayList<>());
            newFolder.setSubFolders(new ArrayList<>());

            // IMPORTANT: Always create at root level unless specifically requested otherwise
            Folder parentFolder = null;
            TreeItem<String> selectedItem = folderTreeView.getSelectionModel().getSelectedItem();

            // Add folder directly to root by default - ignoring any selection
            TreeItem<String> root = folderTreeView.getRoot();

            // Save folder as a root folder
            try {
                storageService.saveFolder(newFolder, null);

                // Update UI
                TreeItem<String> newFolderItem = createFolderItem(newFolder);
                root.getChildren().add(newFolderItem);

                // If this was the first folder, remove empty state
                this.getChildren().remove(emptyLabel);

                // Select the new folder
                folderTreeView.getSelectionModel().select(newFolderItem);
            } catch (IOException e) {
                showErrorAlert("Error Creating Folder",
                        "Could not create folder: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void createNewNote() {
        // Get selected folder
        TreeItem<String> selectedItem = folderTreeView.getSelectionModel().getSelectedItem();
        Folder parentFolder = null;

        if (selectedItem != null) {
            Object selectedObj = itemsMap.get(selectedItem);
            if (selectedObj instanceof Folder) {
                parentFolder = (Folder) selectedObj;
            } else if (selectedObj instanceof Note) {
                // If a note is selected, create a sibling note
                parentFolder = findParentFolder(selectedItem);
                selectedItem = selectedItem.getParent(); // Use parent item for insertion
            }
        }

        if (parentFolder == null) {
            // Must select a folder first
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Folder Selected");
            alert.setContentText("Please select a folder before creating a new note.");
            alert.showAndWait();
            return;
        }

        // Create the note in editor
        noteEditor.createNewNote(parentFolder);

        // Set the editor as the center content
        if (mainLayout != null) {
            System.out.println("Setting note editor as center content");
            mainLayout.setCenter(noteEditor);
        } else {
            System.out.println("Main layout is null");
        }

        // Create placeholder item in tree
        Note newNote = new Note();
        newNote.setId(UUID.randomUUID().toString());
        newNote.setTitle("Untitled Note");

        TreeItem<String> noteItem = createNoteItem(newNote);
        selectedItem.getChildren().add(noteItem);
        selectedItem.setExpanded(true);

        // Select the new note
        folderTreeView.getSelectionModel().select(noteItem);
    }

    public void deleteSelected() {
        TreeItem<String> selectedItem = folderTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;

        Object selectedObj = itemsMap.get(selectedItem);
        String itemType = selectedObj instanceof Folder ? "folder" : "note";
        String itemName = selectedItem.getValue();

        // Confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Deletion");
        confirmDialog.setHeaderText("Delete " + itemType + " \"" + itemName + "\"?");
        confirmDialog.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Folder parentFolder = findParentFolder(selectedItem);

            try {
                if (selectedObj instanceof Folder) {
                    Folder folder = (Folder) selectedObj;
                    storageService.deleteFolder(folder, parentFolder);
                } else if (selectedObj instanceof Note) {
                    Note note = (Note) selectedObj;
                    storageService.deleteNote(note, parentFolder);

                    // Clear the editor if the deleted note was being edited
                    if (noteEditor.isNoteLoaded() &&
                            noteEditor.getCurrentNote() != null &&
                            noteEditor.getCurrentNote().getId().equals(note.getId())) {

                        // Set empty state in main layout
                        if (mainLayout != null) {
                            VBox emptyState = createEmptyState();
                            mainLayout.setCenter(emptyState);
                        }
                    }
                }

                // Update UI
                TreeItem<String> parentItem = selectedItem.getParent();
                parentItem.getChildren().remove(selectedItem);
                itemsMap.remove(selectedItem);
            } catch (Exception e) {
                showErrorAlert("Error Deleting " + itemType,
                        "Could not delete " + itemType + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(20);
        emptyState.setAlignment(Pos.CENTER);

        Label noNotesLabel = new Label("Select a note or create a new one");
        noNotesLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #505050;");

        emptyState.getChildren().add(noNotesLabel);
        return emptyState;
    }

    private void setupDragAndDrop() {
        folderTreeView.setCellFactory(tv -> {
            FolderTreeCell cell = new FolderTreeCell();

            // Setup drag source
            cell.setOnDragDetected(event -> {
                if (cell.getItem() == null) return;

                TreeItem<String> item = cell.getTreeItem();
                if (item != null && itemsMap.containsKey(item)) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(item.getValue());
                    db.setContent(content);

                    // Store the dragged item globally
                    draggedItem = item;

                    event.consume();
                }
            });

            // Setup drag over
            cell.setOnDragOver(event -> {
                if (draggedItem == null) return;

                TreeItem<String> item = cell.getTreeItem();
                if (item != null && canDropInto(draggedItem, item)) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    cell.setStyle("-fx-background-color: #e0f0ff;");
                }

                event.consume();
            });

            // Setup drag exit
            cell.setOnDragExited(event -> {
                cell.setStyle("");
                event.consume();
            });

            // Setup drag drop
            cell.setOnDragDropped(event -> {
                if (draggedItem == null) return;

                TreeItem<String> targetItem = cell.getTreeItem();
                if (targetItem != null && canDropInto(draggedItem, targetItem)) {
                    // Handle the actual move operation
                    moveItem(draggedItem, targetItem);
                    event.setDropCompleted(true);
                } else {
                    event.setDropCompleted(false);
                }

                draggedItem = null;
                event.consume();
            });

            return cell;
        });
    }

    private boolean canDropInto(TreeItem<String> source, TreeItem<String> target) {
        // Prevent dropping into self or children
        if (source == target) return false;
        if (isAncestor(source, target)) return false;

        // Check if target is a folder
        Object targetObj = itemsMap.get(target);
        return targetObj instanceof Folder;
    }

    private boolean isAncestor(TreeItem<String> potential, TreeItem<String> target) {
        TreeItem<String> parent = target.getParent();
        while (parent != null) {
            if (parent == potential) return true;
            parent = parent.getParent();
        }
        return false;
    }

    private void moveItem(TreeItem<String> sourceItem, TreeItem<String> targetItem) {
        // Get source and target objects
        Object sourceObj = itemsMap.get(sourceItem);
        Object targetObj = itemsMap.get(targetItem);

        if (!(targetObj instanceof Folder)) return;

        Folder targetFolder = (Folder) targetObj;
        Folder sourceParent = findParentFolder(sourceItem);

        try {
            // Update data model
            if (sourceObj instanceof Folder) {
                Folder sourceFolder = (Folder) sourceObj;

                // Remove from old parent
                if (sourceParent != null) {
                    sourceParent.getSubFolders().remove(sourceFolder);
                    storageService.saveFolder(sourceParent, findParentFolder(sourceItem.getParent()));
                } else {
                    // This was a root folder, special handling
                    storageService.removeRootFolder(sourceFolder);
                }

                // Add to new parent
                targetFolder.getSubFolders().add(sourceFolder);
                storageService.saveFolder(targetFolder, findParentFolder(targetItem));

            } else if (sourceObj instanceof Note) {
                Note sourceNote = (Note) sourceObj;

                // Remove from old parent
                if (sourceParent != null) {
                    sourceParent.getNotes().remove(sourceNote);
                    storageService.saveFolder(sourceParent, findParentFolder(sourceItem.getParent()));
                }

                // Add to new parent
                targetFolder.getNotes().add(sourceNote);
                storageService.saveFolder(targetFolder, findParentFolder(targetItem));
            }

            // Update UI
            sourceItem.getParent().getChildren().remove(sourceItem);
            targetItem.getChildren().add(sourceItem);
            targetItem.setExpanded(true);
        } catch (IOException e) {
            showErrorAlert("Error Moving Item",
                    "Could not move item: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add a helper method to show error alerts
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    // Custom TreeCell for folder renaming
    private class FolderTreeCell extends TreeCell<String> {
        private TextField textField;

        public FolderTreeCell() {
            this.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !isEmpty()) {
                    Object item = itemsMap.get(getTreeItem());
                    if (item instanceof Folder) {
                        startEdit();
                    }
                }
            });
        }

        @Override
        public void startEdit() {
            super.startEdit();

            if (textField == null) {
                createTextField();
            }
            setText(null);
            setGraphic(textField);
            textField.selectAll();
            textField.requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(getTreeItem().getGraphic());
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(item);
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(item);
                    setGraphic(getTreeItem().getGraphic());

                    // Check if this cell represents a folder and apply bold style
                    TreeItem<String> treeItem = getTreeItem();
                    if (treeItem != null && itemsMap.containsKey(treeItem)) {
                        Object itemObj = itemsMap.get(treeItem);
                        if (itemObj instanceof Folder) {
                            // Make folder names bold
                            setStyle("-fx-font-weight: bold;");
                        } else {
                            // Reset style for notes
                            setStyle("");
                        }
                    }

                    // Add context menu
                    setContextMenu(createContextMenu());
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setOnKeyReleased(t -> {
                if (t.getCode() == KeyCode.ENTER) {
                    commitEdit(textField.getText());
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });

            textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    commitEdit(textField.getText());
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem();
        }
        @Override
        public void commitEdit(String newValue) {
            super.commitEdit(newValue);

            TreeItem<String> treeItem = getTreeItem();
            if (treeItem != null) {
                Object item = itemsMap.get(treeItem);

                if (item instanceof Folder) {
                    Folder folder = (Folder) item;
                    folder.setName(newValue);

                    // Update folder in storage
                    Folder parentFolder = findParentFolder(treeItem);
                    try {
                        storageService.saveFolder(folder, parentFolder);

                        // Make sure the TreeItem value is updated
                        treeItem.setValue(newValue);
                    } catch (IOException e) {
                        showErrorAlert("Error Renaming Folder",
                                "Could not save folder name change: " + e.getMessage());
                        e.printStackTrace();

                        // Revert to old name if save failed
                        treeItem.setValue(folder.getName());
                    }
                }
            }
        }

        // Inside FolderTreeCell class in FolderManagementComponent.java
        private ContextMenu createContextMenu() {
            ContextMenu menu = new ContextMenu();

            TreeItem<String> treeItem = getTreeItem();
            Object item = itemsMap.get(treeItem);

            if (item instanceof Folder) {
                MenuItem newNote = new MenuItem("New Note");
                MenuItem newFolder = new MenuItem("New Subfolder");
                MenuItem rename = new MenuItem("Rename");
                MenuItem delete = new MenuItem("Delete");
                MenuItem summarize = new MenuItem("Summarize Contents");

                newNote.setOnAction(e -> createNewNote());
                newFolder.setOnAction(e -> createNewFolder());
                rename.setOnAction(e -> startEdit());
                delete.setOnAction(e -> deleteSelected());
                summarize.setOnAction(e -> summarizeFolder((Folder) item));

                menu.getItems().addAll(newNote, newFolder, rename, delete, summarize);
            } else if (item instanceof Note) {
                MenuItem open = new MenuItem("Open");
                MenuItem rename = new MenuItem("Rename");
                MenuItem delete = new MenuItem("Delete");

                open.setOnAction(e -> {
                    folderTreeView.getSelectionModel().select(treeItem);
                    handleSelection(treeItem);
                });
                rename.setOnAction(e -> {
                    // Note renaming is handled in the editor
                    folderTreeView.getSelectionModel().select(treeItem);
                    handleSelection(treeItem);
                });
                delete.setOnAction(e -> deleteSelected());

                menu.getItems().addAll(open, rename, delete);
            }

            return menu;
        }

        private void summarizeFolder(Folder folder) {
            if (folder.getNotes().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Empty Folder");
                alert.setContentText("This folder has no notes to summarize.");
                alert.showAndWait();
                return;
            }

            if (aiService == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Summarization Error");
                alert.setContentText("AI Summary service is not configured.");
                alert.showAndWait();
                return;
            }

            // Show loading indicator
            ProgressIndicator progress = new ProgressIndicator();
            progress.setPrefSize(20, 20);
            setGraphic(new HBox(5, getTreeItem().getGraphic(), progress));

            // Run summarization in background thread
            Task<String> summarizeTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return aiService.summarizeFolderContent(folder.getNotes());
                }
            };

            summarizeTask.setOnSucceeded(e -> {
                String summary = summarizeTask.getValue();
                folder.setSummary(summary);

                // Save the updated folder
                Folder parentFolder = findParentFolder(getTreeItem());
                try {
                    storageService.saveFolder(folder, parentFolder);

                    // Reset graphic
                    setGraphic(getTreeItem().getGraphic());

                    // Show summary dialog
                    Alert summaryDialog = new Alert(Alert.AlertType.INFORMATION);
                    summaryDialog.setTitle("Folder Summary");
                    summaryDialog.setHeaderText("Summary of \"" + folder.getName() + "\"");

                    TextArea textArea = new TextArea(summary);
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.setPrefHeight(150);

                    summaryDialog.getDialogPane().setContent(textArea);
                    summaryDialog.showAndWait();
                } catch (IOException e2) {
                    setGraphic(getTreeItem().getGraphic());

                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Save Error");
                    errorAlert.setContentText("Summary was generated but could not be saved: " + e2.getMessage());
                    errorAlert.showAndWait();
                    e2.printStackTrace();
                }
            });

            summarizeTask.setOnFailed(e -> {
                setGraphic(getTreeItem().getGraphic());

                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Summarization Error");
                errorAlert.setContentText("Failed to generate summary: " + summarizeTask.getException().getMessage());
                errorAlert.showAndWait();
            });

            new Thread(summarizeTask).start();
        }
    }

}