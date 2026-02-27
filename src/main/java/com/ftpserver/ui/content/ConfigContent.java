package com.ftpserver.ui.content;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

public class ConfigContent extends VBox {

    private TextField portField;
    private TextField rootDirField;
    private Button browseBtn;
    private TextField maxConnField;
    private Button saveBtn;

    public ConfigContent() {
        initialize();
    }

    private void initialize() {
        setSpacing(16);
        setFillWidth(true);

        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24));

        Label cardTitle = new Label("Server Configuration");
        cardTitle.getStyleClass().add("card-title");

        GridPane form = createForm();
        HBox buttonBar = createButtonBar();

        card.getChildren().addAll(cardTitle, form, buttonBar);
        getChildren().add(card);
    }

    private GridPane createForm() {
        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(20);

        portField = new TextField();
        portField.getStyleClass().add("text-field");

        rootDirField = new TextField();
        rootDirField.getStyleClass().add("text-field");

        browseBtn = new Button("Browse");
        browseBtn.getStyleClass().add("btn-secondary");

        HBox rootDirBox = new HBox(12);
        rootDirBox.getChildren().addAll(rootDirField, browseBtn);
        HBox.setHgrow(rootDirField, Priority.ALWAYS);

        maxConnField = new TextField();
        maxConnField.getStyleClass().add("text-field");

        form.add(new Label("Port:"), 0, 0);
        form.add(portField, 1, 0);
        form.add(new Label("Root Directory:"), 0, 1);
        form.add(rootDirBox, 1, 1);
        form.add(new Label("Max Connections:"), 0, 2);
        form.add(maxConnField, 1, 2);

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(120);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labelCol, fieldCol);

        for (javafx.scene.Node node : form.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).getStyleClass().add("form-label");
            }
        }

        return form;
    }

    private HBox createButtonBar() {
        HBox buttonBar = new HBox();
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        saveBtn = new Button("Save Configuration");
        saveBtn.getStyleClass().add("btn-primary");

        buttonBar.getChildren().add(saveBtn);
        return buttonBar;
    }

    public TextField getPortField() {
        return portField;
    }

    public TextField getRootDirField() {
        return rootDirField;
    }

    public Button getBrowseBtn() {
        return browseBtn;
    }

    public TextField getMaxConnField() {
        return maxConnField;
    }

    public Button getSaveBtn() {
        return saveBtn;
    }
}