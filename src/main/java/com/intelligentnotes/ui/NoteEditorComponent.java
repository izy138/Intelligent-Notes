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
        import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
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
    // In NoteEditorComponent.java, add this field
    private Runnable onTitleChangeCallback;



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

        // Create action toolbar with buttons
        HBox actionToolbar = new HBox(10);
        saveButton = new Button("Save");
        saveButton.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white;");
        summarizeButton = new Button("Summarize");
        summarizeButton.setStyle("-fx-background-color: #f0f0f0;");

        titleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (currentNote != null) {
                currentNote.setTitle(newVal);
                scheduleSave();

                // Call the callback when title changes
                if (onTitleChangeCallback != null) {
                    onTitleChangeCallback.run();
                }
            }
        });

        // Add editor change listener
        contentEditor.setOnKeyReleased(e -> {
            if (currentNote != null) {
                scheduleSave();
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

    // Auto-save functionality with debounce
    private Timeline saveTimer;
    private void scheduleSave() {
        if (saveTimer != null) {
            saveTimer.stop();
        }

        saveTimer = new Timeline(new KeyFrame(Duration.millis(1500), e -> saveNote()));

        saveTimer.play();
    }

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
    }

    public void loadNote(Note note, Folder parent) {
        this.currentNote = note;
        this.parentFolder = parent;

        titleField.setText(note.getTitle());
        contentEditor.setHtmlText(note.getContent());

        setEditorEnabled(true);
    }

    private void saveNote() {
        if (currentNote == null || parentFolder == null) return;

        currentNote.setTitle(titleField.getText());
        currentNote.setContent(contentEditor.getHtmlText());
        currentNote.setUpdatedAt(LocalDateTime.now());

        storageService.saveNote(currentNote, parentFolder);

        // Show save confirmation
        Label savedLabel = new Label("Saved");
        savedLabel.setStyle("-fx-text-fill: green;");

        // If there's already a saved label, remove it first
        this.getChildren().removeIf(node -> node instanceof Label && ((Label) node).getText().equals("Saved"));

        this.getChildren().add(savedLabel);

        // Remove the label after 2 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> this.getChildren().remove(savedLabel));
        pause.play();
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


    // In NoteEditorComponent.java, update the createFormattingToolbar() method
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
//    private Button createToolbarButton(String text, String tooltip, String iconPath) {
//        Button button = new Button();
//        if (text != null && !text.isEmpty()) {
//            button.setText(text);
//        }
//
//        if (iconPath != null) {
//            try {
//                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/images/" + iconPath)));
//                icon.setFitHeight(16);
//                icon.setFitWidth(16);
//                button.setGraphic(icon);
//            } catch (Exception e) {
//                // Fallback to text if image not found
//                if (button.getText() == null || button.getText().isEmpty()) {
//                    button.setText(tooltip.substring(0, 1));
//                }
//            }
//        }
//
//        button.setTooltip(new Tooltip(tooltip));
//        button.setMinWidth(30);
//        button.setMaxWidth(30);
//        button.setPrefHeight(30);
//
//        return button;
//    }

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
        // This is a bit hacky but necessary to access the full HTML capabilities
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