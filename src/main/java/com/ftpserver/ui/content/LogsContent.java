package com.ftpserver.ui.content;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LogsContent extends VBox {

    private TextArea logsArea;
    private TextField searchField;
    private Button searchBtn;
    private Button clearBtn;

    public LogsContent() {
        initialize();
    }

    private void initialize() {
        setSpacing(16);
        setFillWidth(true);

        Label title = new Label("Server Logs");
        title.getStyleClass().add("card-title");

        logsArea = new TextArea();
        logsArea.getStyleClass().add("log-area");
        logsArea.setEditable(false);

        HBox controls = createControls();

        getChildren().addAll(title, controls, logsArea);
        VBox.setVgrow(logsArea, javafx.scene.layout.Priority.ALWAYS);
    }

    private HBox createControls() {
        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.getStyleClass().add("text-field");
        searchField.setPromptText("Search logs...");
        HBox.setHgrow(searchField, javafx.scene.layout.Priority.ALWAYS);

        searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("btn-secondary");

        clearBtn = new Button("Clear Logs");
        clearBtn.getStyleClass().add("btn-danger");

        controls.getChildren().addAll(searchField, searchBtn, clearBtn);
        return controls;
    }

    public TextArea getLogsArea() {
        return logsArea;
    }

    public TextField getSearchField() {
        return searchField;
    }

    public Button getSearchBtn() {
        return searchBtn;
    }

    public Button getClearBtn() {
        return clearBtn;
    }
}