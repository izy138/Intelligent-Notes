package com.intelligentnotes.ui;

import com.intelligentnotes.model.SearchResult;
import com.intelligentnotes.service.ClaudeAISummaryService;
import com.intelligentnotes.service.FileSystemStorageService;
import com.intelligentnotes.service.StorageService;
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

        // Main layout container
        mainLayout = new BorderPane();

        // Create components - ORDER MATTERS HERE!
        // First create the noteEditor
        noteEditor = new NoteEditorComponent(storageService, summaryService);

        // Then create folderManager and pass it references
        folderManager = new FolderManagementComponent(storageService, noteEditor);
        folderManager.setMainLayout(mainLayout); // Set mainLayout reference
        folderManager.setAiService(summaryService);

        // Create the left sidebar with the folder manager
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
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app_icon.png")));
        } catch (Exception e) {
            System.out.println("App icon not found: " + e.getMessage());
        }

        primaryStage.setScene(scene);
        primaryStage.show();

        // Verify that components are properly initialized
        System.out.println("Application initialized:");
        System.out.println("- Main Layout: " + (mainLayout != null ? "OK" : "NULL"));
        System.out.println("- Note Editor: " + (noteEditor != null ? "OK" : "NULL"));
        System.out.println("- Folder Manager: " + (folderManager != null ? "OK" : "NULL"));
        System.out.println("- Storage Service: " + (storageService != null ? "OK" : "NULL"));

        // Load folders after everything is initialized
        folderManager.loadFolders();
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
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setSpacing(10);

        // Create search icon (text-based if image not available)
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 16px;");

        try {
            ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/images/search_icon.png")));
            imageView.setFitHeight(16);
            imageView.setFitWidth(16);
            searchIcon.setGraphic(imageView);
            searchIcon.setText("");
        } catch (Exception e) {
            // Keep the text icon if image fails to load
            System.out.println("Search icon image not found, using text icon instead");
        }

        // Create search text field
        TextField searchField = new TextField();
        searchField.setPromptText("Search notes by keywords...");
        searchField.setPrefHeight(30);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // Add clear button that appears when text is entered
        Button clearButton = new Button("✕");
        clearButton.setStyle("-fx-background-color: transparent;");
        clearButton.setVisible(false);
        clearButton.setOnAction(e -> {
            searchField.clear();
            clearButton.setVisible(false);
        });

        // Search button
        Button searchButton = new Button("Search");
        searchButton.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white;");
        searchButton.setOnAction(e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                searchNotes(query);
            }
        });

        // Make the clear button appear/disappear based on text content
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            clearButton.setVisible(!newVal.isEmpty());
        });

        // Add search functionality on Enter key
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String query = searchField.getText().trim();
                if (!query.isEmpty()) {
                    searchNotes(query);
                }
            }
        });

        // Add all components to the search bar
        searchBar.getChildren().addAll(searchIcon, searchField, clearButton, searchButton);

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
        System.out.println("Searching for: " + query);

        // Show loading indicator in the center area
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(100, 100);
        VBox loadingBox = new VBox(progressIndicator, new Label("Searching..."));
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setSpacing(20);
        mainLayout.setCenter(loadingBox);

        Task<List<SearchResult>> searchTask = new Task<>() {
            @Override
            protected List<SearchResult> call() throws Exception {
                return storageService.searchNotes(query);
            }
        };

        searchTask.setOnSucceeded(e -> {
            List<SearchResult> results = searchTask.getValue();
            System.out.println("Search completed. Found " + results.size() + " results.");
            showSearchResults(results, query);
        });

        searchTask.setOnFailed(e -> {
            System.err.println("Search failed with exception: " + searchTask.getException());
            searchTask.getException().printStackTrace();

            // Show error message
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Search Error");
            alert.setHeaderText("An error occurred while searching");
            alert.setContentText(searchTask.getException().getMessage());
            alert.showAndWait();

            // Restore previous view
            if (noteEditor.isNoteLoaded()) {
                mainLayout.setCenter(noteEditor);
            } else {
                mainLayout.setCenter(createEmptyState());
            }
        });

        new Thread(searchTask).start();
    }

    private void showSearchResults(List<SearchResult> results, String query) {
        // Create search results view
        VBox resultsView = new VBox(10);
        resultsView.setPadding(new Insets(15));

        Label headerLabel = new Label("Search Results for \"" + query + "\"");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button backButton = new Button("Back");
        backButton.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white;");
        backButton.setOnAction(e -> {
            // Simply restore the layout to its default state
            // This ensures the folder selection handlers continue to work
            if (noteEditor.isNoteLoaded()) {
                mainLayout.setCenter(noteEditor);
            } else {
                mainLayout.setCenter(createEmptyState());
            }
            System.out.println("Returned to main view from search results");
        });

        HBox topBar = new HBox(10, backButton, headerLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);

        resultsView.getChildren().addAll(topBar, new Separator());

        if (results.isEmpty()) {
            Label noResultsLabel = new Label("No results found.");
            noResultsLabel.setStyle("-fx-font-size: 14px; -fx-padding: 20px 0;");
            resultsView.getChildren().add(noResultsLabel);
        } else {
            Label resultCountLabel = new Label("Found " + results.size() + " results");
            resultCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #707070; -fx-padding: 0 0 10px 0;");
            resultsView.getChildren().add(resultCountLabel);

            for (SearchResult result : results) {
                VBox resultBox = new VBox(5);
                resultBox.setPadding(new Insets(10));
                resultBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 10; -fx-margin: 5 0;");

                Label titleLabel = new Label(result.getNote().getTitle());
                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                Label pathLabel = new Label("In: " + result.getPath());
                pathLabel.setStyle("-fx-text-fill: #505050; -fx-font-size: 12px;");

                // Format preview text
                String previewText = result.getPreviewText();
                Label previewLabel = new Label(previewText);
                previewLabel.setWrapText(true);
                previewLabel.setMaxWidth(Double.MAX_VALUE);
                previewLabel.setStyle("-fx-text-fill: #303030;");

                resultBox.getChildren().addAll(titleLabel, pathLabel, previewLabel);

                // Make result clickable
                resultBox.setOnMouseClicked(event -> {
                    System.out.println("Clicked on search result: " + result.getNote().getTitle());
                    noteEditor.loadNote(result.getNote(), result.getParentFolder());
                    mainLayout.setCenter(noteEditor);
                });

                // Add hover effect
                resultBox.setOnMouseEntered(event ->
                        resultBox.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #d0d0d0; -fx-border-radius: 5; -fx-padding: 10; -fx-margin: 5 0;")
                );
                resultBox.setOnMouseExited(event ->
                        resultBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 10; -fx-margin: 5 0;")
                );

                resultsView.getChildren().add(resultBox);
            }
        }

        ScrollPane scrollPane = new ScrollPane(resultsView);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        // Replace the editor with search results
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