package com.ftpserver.ui.content;

import com.ftpserver.ui.model.UserRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class UsersContent extends VBox {

    private ObservableList<UserRow> usersData;
    private TableView<UserRow> table;
    private Button addBtn;
    private Button editBtn;
    private Button deleteBtn;

    public UsersContent() {
        initialize();
    }

    private void initialize() {
        setSpacing(16);
        setFillWidth(true);

        HBox headerBar = createHeaderBar();
        table = createUsersTable();

        getChildren().addAll(headerBar, table);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
    }

    private HBox createHeaderBar() {
        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("User Management");
        title.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        addBtn = new Button("Add User");
        addBtn.getStyleClass().add("btn-primary");

        editBtn = new Button("Edit User");
        editBtn.getStyleClass().add("btn-secondary");

        deleteBtn = new Button("Delete User");
        deleteBtn.getStyleClass().add("btn-danger");

        headerBar.getChildren().addAll(title, spacer, addBtn, editBtn, deleteBtn);
        HBox.setMargin(addBtn, new javafx.geometry.Insets(0, 12, 0, 0));
        HBox.setMargin(editBtn, new javafx.geometry.Insets(0, 12, 0, 0));

        return headerBar;
    }

    private TableView<UserRow> createUsersTable() {
        usersData = FXCollections.<UserRow>observableArrayList();
        TableView<UserRow> table = new TableView<>(usersData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("table-view");

        TableColumn<UserRow, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(cellData -> cellData.getValue().username);

        TableColumn<UserRow, String> homeCol = new TableColumn<>("Home Directory");
        homeCol.setCellValueFactory(cellData -> cellData.getValue().homeDir);

        TableColumn<UserRow, String> enabledCol = new TableColumn<>("Enabled");
        enabledCol.setCellValueFactory(cellData -> cellData.getValue().enabled);

        TableColumn<UserRow, String> permsCol = new TableColumn<>("Permissions");
        permsCol.setCellValueFactory(cellData -> cellData.getValue().permissions);

        table.getColumns().addAll(userCol, homeCol, enabledCol, permsCol);

        return table;
    }

    public ObservableList<UserRow> getUsersData() {
        return usersData;
    }

    public TableView<UserRow> getTable() {
        return table;
    }

    public Button getAddBtn() {
        return addBtn;
    }

    public Button getEditBtn() {
        return editBtn;
    }

    public Button getDeleteBtn() {
        return deleteBtn;
    }
}