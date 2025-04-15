package com.intelligentnotes.ui;

import com.intelligentnotes.model.Folder;
import com.intelligentnotes.model.Note;
import com.intelligentnotes.service.AISummaryService;
import com.intelligentnotes.service.StorageService;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public class NoteEditorComponent extends VBox {
    private Note currentNote;
    private TextField titleField;
    private HTMLEditor contentEditor;
    private Button saveButton;
    private Button summarizeButton;
    private StorageService storageService;
    private AISummaryService aiService;
    private Folder parentFolder;
    private Runnable onTitleChangeCallback;
    private Timeline autoSaveTimer;
    private Label autoSaveStatus;
    private static final int AUTOSAVE_DELAY_MS = 2000; // saves 2 seconds after typing stops

    public NoteEditorComponent(StorageService storageService, AISummaryService aiService) {
        this.storageService = storageService;
        this.aiService = aiService;
        this.setSpacing(10);
        this.setPadding(new Insets(15));

        // Create title field
        titleField = new TextField();
        titleField.setPromptText("Note Title");
        titleField.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Create HTML editor for rich text
        contentEditor = new HTMLEditor();
        contentEditor.setPrefHeight(500);
        VBox.setVgrow(contentEditor, Priority.ALWAYS);

        // Create autosave status indicator
        autoSaveStatus = new Label("Autosave: Ready");
        autoSaveStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #707070;");

        // Create action toolbar with buttons
        HBox actionToolbar = new HBox(10);
        saveButton = new Button("Save");
        saveButton.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white;");
        summarizeButton = new Button("Summarize");
        summarizeButton.setStyle("-fx-background-color: #f0f0f0;");

        titleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (currentNote != null) {
                currentNote.setTitle(newVal);
                scheduleAutosave();

                // Call the callback when title changes
                if (onTitleChangeCallback != null) {
                    onTitleChangeCallback.run();
                }
            }
        });

        // Setup autosave for content changes - listen to key and mouse events
        contentEditor.setOnKeyReleased(e -> {
            if (currentNote != null) {
                scheduleAutosave();
            }
        });

        contentEditor.setOnMouseReleased(e -> {
            if (currentNote != null) {
                scheduleAutosave();
            }
        });

        // Add event handlers
        saveButton.setOnAction(e -> saveNote());
        summarizeButton.setOnAction(e -> summarizeNote());

        actionToolbar.getChildren().addAll(saveButton, summarizeButton);
        actionToolbar.setAlignment(Pos.CENTER_LEFT);

        // Add components to layout (removed the formattingToolbar)
        this.getChildren().addAll(titleField, contentEditor, actionToolbar);

        // Disable editor initially until a note is loaded or created
        setEditorEnabled(false);
    }

    private void scheduleAutosave() {
        // If a timer is already running, stop it
        if (autoSaveTimer != null) {
            autoSaveTimer.stop();
        }

        // Update status to show pending autosave
        autoSaveStatus.setText("Autosave: Pending...");
        autoSaveStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #f0ad4e;");

        // Create a new timer that will save after the delay
        autoSaveTimer = new Timeline(new KeyFrame(Duration.millis(AUTOSAVE_DELAY_MS), e -> saveNote()));
        autoSaveTimer.setCycleCount(1);
        autoSaveTimer.play();
    }

    // autosave when a new note is created
    public void createNewNote(Folder parent) {
    this.parentFolder = parent;
    currentNote = new Note();
    currentNote.setId(UUID.randomUUID().toString());
    currentNote.setTitle("Untitled Note");
    currentNote.setContent("");
    currentNote.setCreatedAt(LocalDateTime.now());
    currentNote.setUpdatedAt(LocalDateTime.now());

    titleField.setText(currentNote.getTitle());
    contentEditor.setHtmlText("");

    setEditorEnabled(true);
    titleField.requestFocus();

    // Save the new note immediately
    try {
        saveNote();
    } catch (Exception e) {
        System.err.println("Error creating new note: " + e.getMessage());
        e.printStackTrace();

        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("Error Creating Note");
        errorAlert.setHeaderText("Could not create new note");
        errorAlert.setContentText("An error occurred: " + e.getMessage());
        errorAlert.showAndWait();
    }
}


    public void loadNote(Note note, Folder parent) {
        this.currentNote = note;
        this.parentFolder = parent;

        titleField.setText(note.getTitle());
        contentEditor.setHtmlText(note.getContent());

        setEditorEnabled(true);

        // autosave when a note is loaded
        autoSaveStatus.setText("Autosave: Ready");
        autoSaveStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #707070;");
    }

    private void saveNote() {
        if (currentNote == null || parentFolder == null) {
            autoSaveStatus.setText("Autosave: Ready");
            autoSaveStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #707070;");
            return;
        }

        try {
            // Update note data from the UI
            currentNote.setTitle(titleField.getText());
            currentNote.setContent(contentEditor.getHtmlText());
            currentNote.setUpdatedAt(LocalDateTime.now());

            // Save to storage
            storageService.saveNote(currentNote, parentFolder);

            // Format timestamp for display
            String timestamp = currentNote.getUpdatedAt().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

            // Update the status to show successful save
            autoSaveStatus.setText("Autosave: Saved at " + timestamp);
            autoSaveStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: green;");

            System.out.println("Note autosaved: " + currentNote.getTitle());

            // Reset status after a few seconds
            PauseTransition statusReset = new PauseTransition(Duration.seconds(3));
            statusReset.setOnFinished(e -> {
                autoSaveStatus.setText("Autosave: Ready");
                autoSaveStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #707070;");
            });
            statusReset.play();

        } catch (IOException e) {
            System.err.println("Error saving note: " + e.getMessage());
            e.printStackTrace();

            // Show error in status
            autoSaveStatus.setText("Autosave: Error! " + e.getMessage());
            autoSaveStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: red;");

            // Show error dialog for critical errors
            if (e.getMessage().contains("Permission denied") ||
                    e.getMessage().contains("disk full") ||
                    e.getMessage().contains("Failed to create directory")) {

                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Save Error");
                errorAlert.setHeaderText("Could not save your note");
                errorAlert.setContentText("An error occurred: " + e.getMessage() +
                        "\n\nPlease make sure the application has write permissions and sufficient disk space.");
                errorAlert.showAndWait();
            }
        }
    }

    private void summarizeNote() {
        if (currentNote == null) return;

        String content = contentEditor.getHtmlText();

        // Show loading indicator
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(24, 24);
        Label summarizingLabel = new Label("Summarizing...");
        HBox loadingBox = new HBox(10, progress, summarizingLabel);
        loadingBox.setAlignment(Pos.CENTER_LEFT);

        // Remove any existing loading indicator
        this.getChildren().removeIf(node ->
                node instanceof HBox && ((HBox) node).getChildren().stream()
                        .anyMatch(child -> child instanceof Label &&
                                ((Label) child).getText().equals("Summarizing...")));

        this.getChildren().add(loadingBox);

        // Run summarization in background thread
        Task<String> summarizeTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return aiService.summarizeNoteContent(content);
            }
        };

        summarizeTask.setOnSucceeded(e -> {
            String summary = summarizeTask.getValue();
            currentNote.setSummary(summary);

            try {
                saveNote();

                // Show summary dialog
                Alert summaryDialog = new Alert(Alert.AlertType.INFORMATION);
                summaryDialog.setTitle("Note Summary");
                summaryDialog.setHeaderText("Summary of \"" + currentNote.getTitle() + "\"");

                TextArea textArea = new TextArea(summary);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setPrefHeight(150);

                summaryDialog.getDialogPane().setContent(textArea);
                summaryDialog.showAndWait();
            } catch (Exception ex) {
                System.err.println("Error saving summary: " + ex.getMessage());
                ex.printStackTrace();

                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Save Error");
                errorAlert.setHeaderText("Summary was generated but could not be saved");
                errorAlert.setContentText("An error occurred: " + ex.getMessage());
                errorAlert.showAndWait();
            }

            this.getChildren().remove(loadingBox);
        });

        summarizeTask.setOnFailed(e -> {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Summarization Error");
            errorAlert.setContentText("Failed to generate summary: " + summarizeTask.getException().getMessage());
            errorAlert.showAndWait();

            this.getChildren().remove(loadingBox);
        });

        new Thread(summarizeTask).start();
    }

    private void setEditorEnabled(boolean enabled) {
        titleField.setDisable(!enabled);
        contentEditor.setDisable(!enabled);
        saveButton.setDisable(!enabled);
        summarizeButton.setDisable(!enabled);
    }

    // Add a method to set the callback
    public void setOnTitleChangeCallback(Runnable callback) {
        this.onTitleChangeCallback = callback;
    }

    private HBox createFormattingToolbar() {
        HBox toolbar = new HBox(5);
        toolbar.setPadding(new Insets(5));
        toolbar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // Common formatting buttons with text labels instead of images
        Button boldBtn = new Button("B");
        boldBtn.setTooltip(new Tooltip("Bold"));
        boldBtn.setStyle("-fx-font-weight: bold;");

        Button italicBtn = new Button("I");
        italicBtn.setTooltip(new Tooltip("Italic"));
        italicBtn.setStyle("-fx-font-style: italic;");

        Button underlineBtn = new Button("U");
        underlineBtn.setTooltip(new Tooltip("Underline"));
        underlineBtn.setStyle("-fx-underline: true;");

        Separator sep1 = new Separator(Orientation.VERTICAL);

        // Alignment buttons
        Button alignLeftBtn = new Button("⫷");
        alignLeftBtn.setTooltip(new Tooltip("Align Left"));

        Button alignCenterBtn = new Button("≡");
        alignCenterBtn.setTooltip(new Tooltip("Align Center"));

        Button alignRightBtn = new Button("⫸");
        alignRightBtn.setTooltip(new Tooltip("Align Right"));

        Separator sep2 = new Separator(Orientation.VERTICAL);

        // List buttons
        Button bulletListBtn = new Button("•");
        bulletListBtn.setTooltip(new Tooltip("Bullet List"));

        Button numberedListBtn = new Button("1.");
        numberedListBtn.setTooltip(new Tooltip("Numbered List"));

        Separator sep3 = new Separator(Orientation.VERTICAL);

        // Insert link and image
        Button linkBtn = new Button("🔗");
        linkBtn.setTooltip(new Tooltip("Insert Link"));

        Button imageBtn = new Button("🖼️");
        imageBtn.setTooltip(new Tooltip("Insert Image"));

        // Style all buttons
        for (Button btn : new Button[]{boldBtn, italicBtn, underlineBtn, alignLeftBtn,
                alignCenterBtn, alignRightBtn, bulletListBtn,
                numberedListBtn, linkBtn, imageBtn}) {
            btn.setMinWidth(30);
            btn.setMaxWidth(30);
            btn.setPrefHeight(30);
        }

        // Add event handlers
        boldBtn.setOnAction(e -> executeHtmlCommand("bold"));
        italicBtn.setOnAction(e -> executeHtmlCommand("italic"));
        underlineBtn.setOnAction(e -> executeHtmlCommand("underline"));

        alignLeftBtn.setOnAction(e -> executeHtmlCommand("justifyLeft"));
        alignCenterBtn.setOnAction(e -> executeHtmlCommand("justifyCenter"));
        alignRightBtn.setOnAction(e -> executeHtmlCommand("justifyRight"));

        bulletListBtn.setOnAction(e -> executeHtmlCommand("insertUnorderedList"));
        numberedListBtn.setOnAction(e -> executeHtmlCommand("insertOrderedList"));

        linkBtn.setOnAction(e -> insertLink());
        imageBtn.setOnAction(e -> insertImage());

        toolbar.getChildren().addAll(
                boldBtn, italicBtn, underlineBtn, sep1,
                alignLeftBtn, alignCenterBtn, alignRightBtn, sep2,
                bulletListBtn, numberedListBtn, sep3,
                linkBtn, imageBtn
        );

        return toolbar;
    }
    private Button createToolbarButton(String text, String tooltip, String iconPath) {
        Button button = new Button(text);
        button.setTooltip(new Tooltip(tooltip));
        button.setMinWidth(30);
        button.setMaxWidth(30);
        button.setPrefHeight(30);
        return button;
    }
    private void executeHtmlCommand(String command) {
        // Execute HTML editing command via JavaScript
        WebView webView = getWebViewFromHtmlEditor(contentEditor);
        if (webView != null) {
            webView.getEngine().executeScript(
                    "document.execCommand('" + command + "', false, null);"
            );
        }
    }
    private WebView getWebViewFromHtmlEditor(HTMLEditor editor) {
        // Access the WebView component inside the HTMLEditor
        for (Node node : editor.lookupAll(".web-view")) {
            if (node instanceof WebView) {
                return (WebView) node;
            }
        }
        return null;
    }

    private void insertLink() {
        TextInputDialog dialog = new TextInputDialog("https://");
        dialog.setTitle("Insert Link");
        dialog.setHeaderText("Enter URL");
        dialog.setContentText("URL:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(url -> {
            // First check if there's selected text
            WebView webView = getWebViewFromHtmlEditor(contentEditor);
            if (webView != null) {
                // Create a link with the current selection, or insert a new link
                webView.getEngine().executeScript(
                        "document.execCommand('createLink', false, '" + url + "');"
                );
            }
        });
    }

    private void insertImage() {
        // Open file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(contentEditor.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Read the image file and convert to base64
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                String base64 = Base64.getEncoder().encodeToString(fileBytes);

                // Determine MIME type
                String mimeType = "image/jpeg";
                String fileName = selectedFile.getName().toLowerCase();
                if (fileName.endsWith(".png")) {
                    mimeType = "image/png";
                } else if (fileName.endsWith(".gif")) {
                    mimeType = "image/gif";
                }

                // Insert the image as base64 (embedded in the HTML)
                WebView webView = getWebViewFromHtmlEditor(contentEditor);
                if (webView != null) {
                    webView.getEngine().executeScript(
                            "document.execCommand('insertHTML', false, '<img src=\"data:" + mimeType +
                                    ";base64," + base64 + "\" style=\"max-width: 100%;\"/>');"
                    );
                }
            } catch (IOException e) {
                showErrorAlert("Error", "Could not load image", e.getMessage());
            }
        }
    }

    public boolean isNoteLoaded() {
        return currentNote != null;
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Add this method to NoteEditorComponent
    public Note getCurrentNote() {
        return currentNote;
    }

    // Add this to NoteEditorComponent
    public void setAiService(AISummaryService aiService) {
        this.aiService = aiService;
    }
}