package com.ftpserver.ui;

import com.ftpserver.config.ServerConfig;
import com.ftpserver.server.FtpServer;
import com.ftpserver.user.User;
import com.ftpserver.user.UserManager;
import com.ftpserver.util.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ModernMainWindow extends Application implements FtpServer.ServerListener, Logger.LogListener {

    private static final String CONFIG_PATH = "config" + File.separator + "server.properties";
    private static final String USERS_PATH = "config" + File.separator + "users.json";

    private FtpServer ftpServer;
    private ServerConfig config;
    private UserManager userManager;
    private Logger logger;

    private Stage primaryStage;
    private StackPane contentStack;
    private Label statusIndicator;
    private Label statusText;
    private Button startStopButton;

    private Label statPortValue;
    private Label statConnectionsValue;
    private Label statMaxConnectionsValue;
    private Label statUsersValue;

    private ObservableList<ClientRow> clientsData;
    private ObservableList<UserRow> usersData;

    private Timeline updateTimeline;
    private TextArea logTextArea;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        loadConfig();
        initUI();
        setupServer();
        startUpdateTimer();
    }

    private void loadConfig() {
        config = new ServerConfig();
        try {
            config.load(CONFIG_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
        userManager = new UserManager(USERS_PATH);
        logger = Logger.getInstance();
        logger.setLogDirectory(config.getLogDirectory());
        logger.addListener(this);
    }

    private void setupServer() {
        ftpServer = new FtpServer(config, userManager);
        ftpServer.addListener(this);
    }

    private void startUpdateTimer() {
        updateTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> updateStats()));
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
        updateTimeline.play();
    }

    private void initUI() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        HBox mainLayout = new HBox();
        mainLayout.getChildren().addAll(createSidebar(), createContentArea());
        HBox.setHgrow(mainLayout.getChildren().get(1), Priority.ALWAYS);

        root.setCenter(mainLayout);

        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setTitle("FTP Server Console");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            cleanup();
            if (ftpServer.isRunning()) {
                ftpServer.stop();
            }
        });
        primaryStage.show();
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(260);

        Label title = new Label("FTP Server");
        title.getStyleClass().add("sidebar-title");

        Label subtitle = new Label("Advanced File Transfer");
        subtitle.getStyleClass().add("sidebar-subtitle");

        VBox navItems = new VBox(0);
        navItems.setFillWidth(true);

        Button navOverview = createNavItem("Overview", true);
        Button navClients = createNavItem("Clients", false);
        Button navUsers = createNavItem("Users", false);
        Button navConfig = createNavItem("Configuration", false);
        Button navLogs = createNavItem("Logs", false);

        navItems.getChildren().addAll(navOverview, navClients, navUsers, navConfig, navLogs);

        navOverview.setOnAction(e -> {
            setActiveNav(navItems, navOverview);
            showContent("overview");
        });
        navClients.setOnAction(e -> {
            setActiveNav(navItems, navClients);
            showContent("clients");
        });
        navUsers.setOnAction(e -> {
            setActiveNav(navItems, navUsers);
            showContent("users");
        });
        navConfig.setOnAction(e -> {
            setActiveNav(navItems, navConfig);
            showContent("config");
        });
        navLogs.setOnAction(e -> {
            setActiveNav(navItems, navLogs);
            showContent("logs");
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        sidebar.getChildren().addAll(title, subtitle, navItems, spacer);
        return sidebar;
    }

    private Button createNavItem(String text, boolean active) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        if (active) {
            btn.getStyleClass().add("active");
        }
        return btn;
    }

    private void setActiveNav(VBox navItems, Button active) {
        for (javafx.scene.Node node : navItems.getChildren()) {
            node.getStyleClass().remove("active");
        }
        active.getStyleClass().add("active");
    }

    private VBox createContentArea() {
        VBox contentArea = new VBox();
        contentArea.getStyleClass().add("content-area");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        HBox header = createHeader();

        contentStack = new StackPane();
        contentStack.getChildren().add(createOverviewContent());
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        contentArea.getChildren().addAll(header, contentStack);
        VBox.setMargin(header, new Insets(0, 0, 24, 0));
        return contentArea;
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header, Priority.ALWAYS);

        Label headerTitle = new Label("Dashboard");
        headerTitle.getStyleClass().add("header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusBadge = new HBox(8);
        statusBadge.getStyleClass().add("status-badge");
        statusBadge.setAlignment(Pos.CENTER_LEFT);

        statusIndicator = new Label("●");
        statusIndicator.getStyleClass().addAll("status-indicator", "offline");

        statusText = new Label("Server Offline");
        statusText.getStyleClass().add("status-text");

        statusBadge.getChildren().addAll(statusIndicator, statusText);

        startStopButton = new Button("Start Server");
        startStopButton.getStyleClass().add("btn-primary");
        startStopButton.setOnAction(e -> toggleServer());

        header.getChildren().addAll(headerTitle, spacer, statusBadge, startStopButton);
        HBox.setMargin(statusBadge, new Insets(0, 16, 0, 0));
        return header;
    }

    private VBox createOverviewContent() {
        VBox content = new VBox(24);
        content.setFillWidth(true);

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(20);
        statsGrid.setPadding(new Insets(0));

        statPortValue = new Label(String.valueOf(config.getPort()));
        statConnectionsValue = new Label("0");
        statMaxConnectionsValue = new Label(String.valueOf(config.getMaxConnections()));
        statUsersValue = new Label(String.valueOf(userManager.getAllUsers().size()));

        statsGrid.add(createStatCard("Server Port", statPortValue, "🔌"), 0, 0);
        statsGrid.add(createStatCard("Active Connections", statConnectionsValue, "👥"), 1, 0);
        statsGrid.add(createStatCard("Max Connections", statMaxConnectionsValue, "📊"), 0, 1);
        statsGrid.add(createStatCard("Total Users", statUsersValue, "👤"), 1, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        statsGrid.getColumnConstraints().addAll(col1, col2);

        VBox recentActivity = new VBox(16);
        Label activityTitle = new Label("Recent Activity");
        activityTitle.getStyleClass().add("card-title");

        logTextArea = new TextArea();
        logTextArea.getStyleClass().add("log-area");
        logTextArea.setEditable(false);
        logTextArea.setPrefRowCount(12);

        recentActivity.getChildren().addAll(activityTitle, logTextArea);

        content.getChildren().addAll(statsGrid, recentActivity);
        return content;
    }

    private VBox createStatCard(String label, Label valueLabel, String icon) {
        VBox card = new VBox(12);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(24));

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stat-icon");

        valueLabel.getStyleClass().add("stat-value");

        Label labelText = new Label(label);
        labelText.getStyleClass().add("stat-label");

        card.getChildren().addAll(iconLabel, valueLabel, labelText);
        return card;
    }

    private VBox createClientsContent() {
        VBox content = new VBox(16);
        content.setFillWidth(true);

        Label title = new Label("Connected Clients");
        title.getStyleClass().add("card-title");

        clientsData = FXCollections.observableArrayList();
        TableView<ClientRow> table = new TableView<>(clientsData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("table-view");

        TableColumn<ClientRow, String> ipCol = new TableColumn<>("IP Address");
        ipCol.setCellValueFactory(c -> c.getValue().ip);

        TableColumn<ClientRow, String> portCol = new TableColumn<>("Port");
        portCol.setCellValueFactory(c -> c.getValue().port);

        TableColumn<ClientRow, String> timeCol = new TableColumn<>("Connected Since");
        timeCol.setCellValueFactory(c -> c.getValue().connectTime);

        TableColumn<ClientRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> c.getValue().status);

        table.getColumns().addAll(ipCol, portCol, timeCol, statusCol);

        content.getChildren().addAll(title, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private VBox createUsersContent() {
        VBox content = new VBox(16);
        content.setFillWidth(true);

        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("User Management");
        title.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("Add User");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddUserDialog());

        Button editBtn = new Button("Edit User");
        editBtn.getStyleClass().add("btn-secondary");

        Button deleteBtn = new Button("Delete User");
        deleteBtn.getStyleClass().add("btn-danger");

        headerBar.getChildren().addAll(title, spacer, addBtn, editBtn, deleteBtn);
        HBox.setMargin(addBtn, new Insets(0, 12, 0, 0));
        HBox.setMargin(editBtn, new Insets(0, 12, 0, 0));

        usersData = FXCollections.observableArrayList();
        TableView<UserRow> table = new TableView<>(usersData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("table-view");

        TableColumn<UserRow, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(c -> c.getValue().username);

        TableColumn<UserRow, String> homeCol = new TableColumn<>("Home Directory");
        homeCol.setCellValueFactory(c -> c.getValue().homeDir);

        TableColumn<UserRow, String> enabledCol = new TableColumn<>("Enabled");
        enabledCol.setCellValueFactory(c -> c.getValue().enabled);

        TableColumn<UserRow, String> permsCol = new TableColumn<>("Permissions");
        permsCol.setCellValueFactory(c -> c.getValue().permissions);

        table.getColumns().addAll(userCol, homeCol, enabledCol, permsCol);

        editBtn.setOnAction(e -> {
            UserRow selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditUserDialog(selected.username.get());
            }
        });

        deleteBtn.setOnAction(e -> {
            UserRow selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteUser(selected.username.get());
            }
        });

        refreshUsersTable();

        content.getChildren().addAll(headerBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private VBox createConfigContent() {
        VBox content = new VBox(16);
        content.setFillWidth(true);

        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24));

        Label cardTitle = new Label("Server Configuration");
        cardTitle.getStyleClass().add("card-title");

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(20);

        TextField portField = new TextField(String.valueOf(config.getPort()));
        portField.getStyleClass().add("text-field");

        TextField rootDirField = new TextField(config.getRootDirectory());
        rootDirField.getStyleClass().add("text-field");

        Button browseBtn = new Button("Browse");
        browseBtn.getStyleClass().add("btn-secondary");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Select Root Directory");
            File dir = dirChooser.showDialog(primaryStage);
            if (dir != null) {
                rootDirField.setText(dir.getAbsolutePath());
            }
        });

        HBox rootDirBox = new HBox(12);
        rootDirBox.getChildren().addAll(rootDirField, browseBtn);
        HBox.setHgrow(rootDirField, Priority.ALWAYS);

        TextField maxConnField = new TextField(String.valueOf(config.getMaxConnections()));
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
                ((Label) node).setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px;");
            }
        }

        HBox buttonBar = new HBox();
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("Save Configuration");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                config.setPort(Integer.parseInt(portField.getText()));
                config.setRootDirectory(rootDirField.getText());
                config.setMaxConnections(Integer.parseInt(maxConnField.getText()));
                config.save(CONFIG_PATH);
                updateStats();
                showAlert("Success", "Configuration saved successfully!");
            } catch (Exception ex) {
                showAlert("Error", "Failed to save: " + ex.getMessage());
            }
        });

        buttonBar.getChildren().add(saveBtn);

        card.getChildren().addAll(cardTitle, form, buttonBar);

        content.getChildren().add(card);
        return content;
    }

    private VBox createLogsContent() {
        VBox content = new VBox(16);
        content.setFillWidth(true);

        Label title = new Label("Server Logs");
        title.getStyleClass().add("card-title");

        TextArea logsArea = new TextArea();
        logsArea.getStyleClass().add("log-area");
        logsArea.setEditable(false);

        logger.addListener(entry -> Platform.runLater(() -> {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String line = String.format("[%s] [%s] %s%n",
                    entry.timestamp.format(fmt), entry.level, entry.message);
            logsArea.appendText(line);
        }));

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.getStyleClass().add("text-field");
        searchField.setPromptText("Search logs...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("btn-secondary");

        Button clearBtn = new Button("Clear Logs");
        clearBtn.getStyleClass().add("btn-danger");
        clearBtn.setOnAction(e -> {
            logger.clearLogs();
            logsArea.clear();
        });

        controls.getChildren().addAll(searchField, searchBtn, clearBtn);

        content.getChildren().addAll(title, controls, logsArea);
        VBox.setVgrow(logsArea, Priority.ALWAYS);
        return content;
    }

    private void showContent(String page) {
        contentStack.getChildren().clear();
        switch (page) {
            case "overview":
                contentStack.getChildren().add(createOverviewContent());
                break;
            case "clients":
                contentStack.getChildren().add(createClientsContent());
                break;
            case "users":
                contentStack.getChildren().add(createUsersContent());
                break;
            case "config":
                contentStack.getChildren().add(createConfigContent());
                break;
            case "logs":
                contentStack.getChildren().add(createLogsContent());
                break;
        }
    }

    private void toggleServer() {
        try {
            if (ftpServer.isRunning()) {
                ftpServer.stop();
            } else {
                ftpServer.start();
            }
        } catch (IOException e) {
            showAlert("Error", e.getMessage());
        }
    }

    private void updateStats() {
        Platform.runLater(() -> {
            statPortValue.setText(String.valueOf(config.getPort()));
            int connCount = ftpServer != null ? ftpServer.getConnectionCount() : 0;
            statConnectionsValue.setText(String.valueOf(connCount));
            statMaxConnectionsValue.setText(String.valueOf(config.getMaxConnections()));
            statUsersValue.setText(String.valueOf(userManager.getAllUsers().size()));
            refreshClientsTable();
        });
    }

    private void refreshClientsTable() {
        if (clientsData != null) {
            clientsData.clear();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            if (ftpServer != null) {
                for (FtpServer.ClientSession session : ftpServer.getClientSessions()) {
                    clientsData.add(new ClientRow(
                            session.getClientAddress(),
                            String.valueOf(session.getClientPort()),
                            session.connectTime.format(fmt),
                            session.active ? "Active" : "Idle"
                    ));
                }
            }
        }
    }

    private void refreshUsersTable() {
        if (usersData != null) {
            usersData.clear();
            for (User user : userManager.getAllUsers()) {
                usersData.add(new UserRow(
                        user.getUsername(),
                        user.getHomeDirectory() != null ? user.getHomeDirectory() : "Default",
                        user.isEnabled() ? "Yes" : "No",
                        user.getPermissions().toString()
                ));
            }
        }
    }

    private void showAddUserDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add User");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1e293b;");

        TextField userField = new TextField();
        userField.getStyleClass().add("text-field");
        userField.setPromptText("Username");

        PasswordField passField = new PasswordField();
        passField.getStyleClass().add("text-field");
        passField.setPromptText("Password");

        TextField homeField = new TextField();
        homeField.getStyleClass().add("text-field");
        homeField.setPromptText("Home Directory (optional)");

        CheckBox enabledBox = new CheckBox("Enabled");
        enabledBox.setSelected(true);
        enabledBox.getStyleClass().add("check-box");

        content.getChildren().addAll(new Label("Username:"), userField, new Label("Password:"), passField,
                new Label("Home Directory:"), homeField, enabledBox);

        for (javafx.scene.Node node : content.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px;");
            }
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #1e293b;");

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                User user = new User();
                user.setUsername(userField.getText());
                user.setPassword(passField.getText());
                user.setHomeDirectory(homeField.getText().isEmpty() ? null : homeField.getText());
                user.setEnabled(enabledBox.isSelected());
                user.addPermission(User.Permission.READ);
                user.addPermission(User.Permission.WRITE);
                user.addPermission(User.Permission.DELETE);
                user.addPermission(User.Permission.CREATE_DIR);
                user.addPermission(User.Permission.DELETE_DIR);
                user.addPermission(User.Permission.RENAME);
                user.addPermission(User.Permission.LIST);
                userManager.addUser(user);
                refreshUsersTable();
                updateStats();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showEditUserDialog(String username) {
        User user = userManager.getUser(username).orElse(null);
        if (user == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1e293b;");

        TextField userField = new TextField(user.getUsername());
        userField.getStyleClass().add("text-field");
        userField.setEditable(false);

        PasswordField passField = new PasswordField();
        passField.getStyleClass().add("text-field");
        passField.setText(user.getPassword());

        TextField homeField = new TextField(user.getHomeDirectory() != null ? user.getHomeDirectory() : "");
        homeField.getStyleClass().add("text-field");

        CheckBox enabledBox = new CheckBox("Enabled");
        enabledBox.setSelected(user.isEnabled());
        enabledBox.getStyleClass().add("check-box");

        content.getChildren().addAll(new Label("Username:"), userField, new Label("Password:"), passField,
                new Label("Home Directory:"), homeField, enabledBox);

        for (javafx.scene.Node node : content.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px;");
            }
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #1e293b;");

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                user.setPassword(passField.getText());
                user.setHomeDirectory(homeField.getText().isEmpty() ? null : homeField.getText());
                user.setEnabled(enabledBox.isSelected());
                refreshUsersTable();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void deleteUser(String username) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete User");
        confirm.setContentText("Are you sure you want to delete user '" + username + "'?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                userManager.removeUser(username);
                refreshUsersTable();
                updateStats();
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void cleanup() {
        if (updateTimeline != null) {
            updateTimeline.stop();
        }
    }

    @Override
    public void onServerStarted() {
        Platform.runLater(() -> {
            statusIndicator.getStyleClass().remove("offline");
            statusIndicator.getStyleClass().add("online");
            statusText.setText("Running on port " + config.getPort());
            startStopButton.setText("Stop Server");
            startStopButton.getStyleClass().remove("btn-primary");
            startStopButton.getStyleClass().add("btn-danger");
        });
    }

    @Override
    public void onServerStopped() {
        Platform.runLater(() -> {
            statusIndicator.getStyleClass().remove("online");
            statusIndicator.getStyleClass().add("offline");
            statusText.setText("Server Offline");
            startStopButton.setText("Start Server");
            startStopButton.getStyleClass().remove("btn-danger");
            startStopButton.getStyleClass().add("btn-primary");
        });
    }

    @Override
    public void onLogEntry(Logger.LogEntry entry) {
        Platform.runLater(() -> {
            if (logTextArea != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String line = String.format("[%s] [%s] %s%n",
                        entry.timestamp.format(fmt), entry.level, entry.message);
                logTextArea.appendText(line);
            }
        });
    }

    @Override
    public void onClientConnected(FtpServer.ClientSession session) {
        updateStats();
    }

    @Override
    public void onClientDisconnected(FtpServer.ClientSession session) {
        updateStats();
    }

    public static class ClientRow {
        public final SimpleStringProperty ip;
        public final SimpleStringProperty port;
        public final SimpleStringProperty connectTime;
        public final SimpleStringProperty status;

        public ClientRow(String ip, String port, String connectTime, String status) {
            this.ip = new SimpleStringProperty(ip);
            this.port = new SimpleStringProperty(port);
            this.connectTime = new SimpleStringProperty(connectTime);
            this.status = new SimpleStringProperty(status);
        }
    }

    public static class UserRow {
        public final SimpleStringProperty username;
        public final SimpleStringProperty homeDir;
        public final SimpleStringProperty enabled;
        public final SimpleStringProperty permissions;

        public UserRow(String username, String homeDir, String enabled, String permissions) {
            this.username = new SimpleStringProperty(username);
            this.homeDir = new SimpleStringProperty(homeDir);
            this.enabled = new SimpleStringProperty(enabled);
            this.permissions = new SimpleStringProperty(permissions);
        }
    }
}
