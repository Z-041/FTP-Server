package com.ftpserver.ui.content;

import com.ftpserver.ui.model.ClientRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class ClientsContent extends VBox {

    private ObservableList<ClientRow> clientsData;
    private TableView<ClientRow> table;

    public ClientsContent() {
        initialize();
    }

    private void initialize() {
        setSpacing(16);
        setFillWidth(true);

        Label title = new Label("Connected Clients");
        title.getStyleClass().add("card-title");

        clientsData = FXCollections.<ClientRow>observableArrayList();
        table = new TableView<>(clientsData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("table-view");

        TableColumn<ClientRow, String> ipCol = new TableColumn<>("IP Address");
        ipCol.setCellValueFactory(cellData -> cellData.getValue().ip);

        TableColumn<ClientRow, String> portCol = new TableColumn<>("Port");
        portCol.setCellValueFactory(cellData -> cellData.getValue().port);

        TableColumn<ClientRow, String> timeCol = new TableColumn<>("Connected Since");
        timeCol.setCellValueFactory(cellData -> cellData.getValue().connectTime);

        TableColumn<ClientRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().status);

        table.getColumns().addAll(ipCol, portCol, timeCol, statusCol);

        getChildren().addAll(title, table);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
    }

    public ObservableList<ClientRow> getClientsData() {
        return clientsData;
    }

    public TableView<ClientRow> getTable() {
        return table;
    }
}