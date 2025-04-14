package com.intelligentnotes.ui;

import com.intelligentnotes.model.Folder;
import com.intelligentnotes.model.Note;
import com.intelligentnotes.model.SearchResult;
import com.intelligentnotes.service.ClaudeAISummaryService;
import com.intelligentnotes.service.FileSystemStorageService;
//import com.intelligentnotes.service.HybridSummaryService;
import com.intelligentnotes.service.StorageService;
import com.intelligentnotes.ui.FolderManagementComponent;
import com.intelligentnotes.ui.NoteEditorComponent;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

public class IntelligentNotesApp extends Application {
    private BorderPane mainLayout;
    private StorageService storageService;
    private ClaudeAISummaryService summaryService;
    private FolderManagementComponent folderManager;
    private NoteEditorComponent noteEditor;


    @Override
    public void start(Stage primaryStage) {
        // Initialize file system storage
        storageService = new FileSystemStorageService();

        // Load Claude API key from preferences
        Preferences prefs = Preferences.userNodeForPackage(IntelligentNotesApp.class);
        String claudeApiKey = prefs.get("summarization.claudeApiKey", "");

        // Initialize summary service with Claude (falls back to local if API key is empty)
        summaryService = new ClaudeAISummaryService(claudeApiKey);

        // Load preferences
        loadPreferences();


        // Main layout container
        mainLayout = new BorderPane();

        // Create components
        noteEditor = new NoteEditorComponent(storageService, summaryService);
        folderManager = new FolderManagementComponent(storageService, noteEditor);
        folderManager.setMainLayout(mainLayout);
        folderManager.setAiService(summaryService);

        // Create the left sidebar
        VBox leftSidebar = createLeftSidebar();
        mainLayout.setLeft(leftSidebar);

        // Initial empty state - no note selected
        VBox emptyState = createEmptyState();
        mainLayout.setCenter(emptyState);

        // Add the search bar at the top
        HBox searchBar = createSearchBar();
        mainLayout.setTop(searchBar);

        // Set scene and show stage
        Scene scene = new Scene(mainLayout, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/intelligentnotes.css").toExternalForm());

        primaryStage.setTitle("Intelligent Notes");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app_icon.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(IntelligentNotesApp.class);
        String apiKey = prefs.get("summarization.claudeApiKey", "");

        // Initialize the ClaudeAISummaryService with the API key
        summaryService = new ClaudeAISummaryService(apiKey);

        // Update references to the service if components are already created
        if (noteEditor != null) {
            noteEditor.setAiService(summaryService);
        }
        if (folderManager != null) {
            folderManager.setAiService(summaryService);
        }
    }

    private VBox createLeftSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(15));
        sidebar.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 1 0 0;");
        sidebar.setPrefWidth(250);

        // App logo/title
        HBox titleBox = new HBox(5);
        ImageView logoView;
        try {
            logoView = new ImageView(new Image(getClass().getResourceAsStream("/images/note_icon.png")));
        } catch (Exception e) {
            // Create a text-based logo as fallback
            logoView = new ImageView();
        }
        logoView.setFitHeight(24);
        logoView.setFitWidth(24);
        Label titleLabel = new Label("Intelligent Notes");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        titleBox.getChildren().addAll(logoView, titleLabel);

        // Create New button
        Button createNewBtn = new Button("Create New");
        createNewBtn.setPrefWidth(220);
        createNewBtn.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white;");
        createNewBtn.setOnAction(e -> handleCreateNew());

        // Add the folder manager component
        VBox.setVgrow(folderManager, Priority.ALWAYS);

        // User profile at bottom
        HBox userBox = new HBox(10);
        Circle userAvatar = new Circle(20, Color.web("#0078d7"));
        Text userInitial = new Text("U");
        userInitial.setFill(Color.WHITE);
        StackPane avatarPane = new StackPane(userAvatar, userInitial);

        VBox userInfo = new VBox(2);
        Label userName = new Label("User");
        Label planInfo = new Label("username@gmail.com");
        planInfo.setStyle("-fx-text-fill: #707070; -fx-font-size: 12px;");
        userInfo.getChildren().addAll(userName, planInfo);

        userBox.getChildren().addAll(avatarPane, userInfo);

        // Settings button
        Button settingsBtn = new Button("Settings");
        settingsBtn.setStyle("-fx-background-color: #f0f0f0;");
        settingsBtn.setOnAction(e -> showSettingsDialog());

        // Add all elements to sidebar
        sidebar.getChildren().addAll(titleBox, createNewBtn, new Separator(), folderManager, userBox, settingsBtn);

        return sidebar;
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(20);
        emptyState.setAlignment(Pos.CENTER);

        Label noNotesLabel = new Label("Select a note or create a new one");
        noNotesLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #505050;");

        ImageView noteIcon;
        try {
            noteIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/note_large_icon.png")));
        } catch (Exception e) {
            // Create a text-based icon as fallback
            noteIcon = new ImageView();
            noNotesLabel.setText("📝 Select a note or create a new one");
        }
        noteIcon.setFitHeight(64);
        noteIcon.setFitWidth(64);

        emptyState.getChildren().addAll(noteIcon, noNotesLabel);
        return emptyState;
    }

    private HBox createSearchBar() {
        HBox searchBar = new HBox();
        searchBar.setPadding(new Insets(10, 15, 10, 15));
        searchBar.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by keywords...");
        searchField.setPrefHeight(30);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        ImageView searchIcon;
        try {
            searchIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/search_icon.png")));
        } catch (Exception e) {
            // Fallback to text if image not found
            searchIcon = new ImageView();
            searchField.setPromptText("🔍 Search by keywords...");
        }
        searchIcon.setFitHeight(16);
        searchIcon.setFitWidth(16);

        // Add search functionality
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String query = searchField.getText().trim();
                if (!query.isEmpty()) {
                    searchNotes(query);
                }
            }
        });

        searchBar.getChildren().addAll(searchIcon, searchField);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setSpacing(10);

        return searchBar;
    }

    private void handleCreateNew() {
        // Show dropdown menu for creating a new note or folder
        ContextMenu createMenu = new ContextMenu();
        MenuItem newNote = new MenuItem("New Note");
        MenuItem newFolder = new MenuItem("New Folder");

        newNote.setOnAction(e -> folderManager.createNewNote());
        newFolder.setOnAction(e -> folderManager.createNewFolder());

        createMenu.getItems().addAll(newNote, newFolder);

        // Find the create button in the sidebar
        Node createButton = null;
        for (Node node : ((VBox) mainLayout.getLeft()).getChildren()) {
            if (node instanceof Button && ((Button) node).getText().equals("Create New")) {
                createButton = node;
                break;
            }
        }

        if (createButton != null) {
            createMenu.show(createButton, Side.BOTTOM, 0, 0);
        }
    }

    private void searchNotes(String query) {
        Task<List<SearchResult>> searchTask = new Task<>() {
            @Override
            protected List<SearchResult> call() throws Exception {
                return storageService.searchNotes(query);
            }
        };

        searchTask.setOnSucceeded(e -> {
            List<SearchResult> results = searchTask.getValue();
            showSearchResults(results, query);
        });

        new Thread(searchTask).start();
    }

    private void showSearchResults(List<SearchResult> results, String query) {
        // Create search results view
        VBox resultsView = new VBox(10);
        resultsView.setPadding(new Insets(15));

        Label headerLabel = new Label("Search Results for \"" + query + "\"");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button backButton = new Button("Back to Editor");
        backButton.setOnAction(e -> {
            // Replace folderTreeView with a check on the noteEditor's state
            if (noteEditor.isNoteLoaded()) {
                mainLayout.setCenter(noteEditor);
            } else {
                mainLayout.setCenter(createEmptyState());
            }
        });

        resultsView.getChildren().addAll(headerLabel, backButton, new Separator());

        if (results.isEmpty()) {
            Label noResultsLabel = new Label("No results found.");
            resultsView.getChildren().add(noResultsLabel);
        } else {
            for (SearchResult result : results) {
                VBox resultBox = new VBox(5);
                resultBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-padding: 5 0;");

                Label titleLabel = new Label(result.getNote().getTitle());
                titleLabel.setStyle("-fx-font-weight: bold;");

                Label pathLabel = new Label("In: " + result.getPath());
                pathLabel.setStyle("-fx-text-fill: #707070; -fx-font-size: 12px;");

                Label previewLabel = new Label(result.getPreviewText());
                previewLabel.setWrapText(true);

                resultBox.getChildren().addAll(titleLabel, pathLabel, previewLabel);

                // Make result clickable
                resultBox.setOnMouseClicked(event -> {
                    noteEditor.loadNote(result.getNote(), result.getParentFolder());
                    mainLayout.setCenter(noteEditor);
                });

                resultsView.getChildren().add(resultBox);
            }
        }

        ScrollPane scrollPane = new ScrollPane(resultsView);
        scrollPane.setFitToWidth(true);

        // Replace the editor with search results temporarily
        mainLayout.setCenter(scrollPane);
    }

    private void showSettingsDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Intelligent Notes Settings");
        dialog.setHeaderText("AI Summary Service Configuration");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the content layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField apiKeyField = new TextField();
        apiKeyField.setPromptText("Enter Claude API key");

        // Load current value
        Preferences prefs = Preferences.userNodeForPackage(IntelligentNotesApp.class);
        apiKeyField.setText(prefs.get("summarization.claudeApiKey", ""));

        grid.add(new Label("Claude API Key:"), 0, 0);
        grid.add(apiKeyField, 1, 0);

        Label infoLabel = new Label("Leave empty to use the basic local summarization.");
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #707070;");
        grid.add(infoLabel, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Convert the result when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return apiKeyField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(apiKey -> {
            // Save the API key
            Preferences prefs2 = Preferences.userNodeForPackage(IntelligentNotesApp.class);
            prefs2.put("summarization.claudeApiKey", apiKey);

            // Create a new summary service with the API key (or empty for local only)
            summaryService = new ClaudeAISummaryService(apiKey);

            // Update references to the service
            noteEditor.setAiService(summaryService);
            folderManager.setAiService(summaryService);

            String serviceType = apiKey.isEmpty() ? "local basic" : "Claude AI";
            showMessageDialog("Settings Saved",
                    "Your settings have been saved successfully. Using " + serviceType + " for summarization.",
                    Alert.AlertType.INFORMATION);
        });
    }

    private void showMessageDialog(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}