package com.ftpserver.ui;

import com.ftpserver.config.ServerConfig;
import com.ftpserver.server.FtpServer;
import com.ftpserver.ui.content.*;
import com.ftpserver.ui.model.ClientRow;
import com.ftpserver.ui.model.UserRow;
import com.ftpserver.ui.sidebar.Sidebar;
import com.ftpserver.ui.tray.SystemTrayManager;
import com.ftpserver.user.User;
import com.ftpserver.user.UserManager;
import com.ftpserver.util.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
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

    private OverviewContent overviewContent;
    private ClientsContent clientsContent;
    private UsersContent usersContent;
    private ConfigContent configContent;
    private LogsContent logsContent;
    private Sidebar sidebar;
    private SystemTrayManager systemTrayManager;

    private Timeline updateTimeline;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        // 防止最后一个窗口关闭时自动退出 JavaFX 应用程序
        Platform.setImplicitExit(false);
        loadConfig();
        setupServer();
        initUI();
        initSystemTray();
        startUpdateTimer();
    }

    private void loadConfig() {
        config = new ServerConfig();
        logger = Logger.getInstance();
        try {
            config.load(CONFIG_PATH);
        } catch (IOException e) {
            logger.error("Failed to load config: " + e.getMessage(), "UI", "-");
        }
        userManager = new UserManager(USERS_PATH);
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
        sidebar = new Sidebar();
        mainLayout.getChildren().addAll(sidebar, createContentArea());
        HBox.setHgrow(mainLayout.getChildren().get(1), Priority.ALWAYS);

        root.setCenter(mainLayout);

        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setTitle("FTP Server Console");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (systemTrayManager.isTrayInitialized()) {
                e.consume();
                systemTrayManager.hideToTray();
            } else {
                cleanup();
                if (ftpServer.isRunning()) {
                    ftpServer.stop();
                }
            }
        });
        primaryStage.show();

        setupNavigation();
    }

    private VBox createContentArea() {
        VBox contentArea = new VBox();
        contentArea.getStyleClass().add("content-area");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        HBox header = createHeader();

        contentStack = new StackPane();
        overviewContent = new OverviewContent();
        contentStack.getChildren().add(overviewContent);
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

    private void setupNavigation() {
        sidebar.getNavOverview().setOnAction(e -> {
            sidebar.setActiveNav(sidebar.getNavOverview());
            showContent("overview");
        });
        sidebar.getNavClients().setOnAction(e -> {
            sidebar.setActiveNav(sidebar.getNavClients());
            showContent("clients");
        });
        sidebar.getNavUsers().setOnAction(e -> {
            sidebar.setActiveNav(sidebar.getNavUsers());
            showContent("users");
        });
        sidebar.getNavConfig().setOnAction(e -> {
            sidebar.setActiveNav(sidebar.getNavConfig());
            showContent("config");
        });
        sidebar.getNavLogs().setOnAction(e -> {
            sidebar.setActiveNav(sidebar.getNavLogs());
            showContent("logs");
        });
    }

    private void showContent(String page) {
        contentStack.getChildren().clear();
        javafx.scene.Node content = null;
        
        switch (page) {
            case "overview":
                if (overviewContent == null) {
                    overviewContent = new OverviewContent();
                }
                content = overviewContent;
                break;
            case "clients":
                if (clientsContent == null) {
                    clientsContent = new ClientsContent();
                }
                content = clientsContent;
                break;
            case "users":
                if (usersContent == null) {
                    usersContent = new UsersContent();
                    setupUsersContent();
                }
                content = usersContent;
                break;
            case "config":
                if (configContent == null) {
                    configContent = new ConfigContent();
                    setupConfigContent();
                }
                content = configContent;
                break;
            case "logs":
                if (logsContent == null) {
                    logsContent = new LogsContent();
                    setupLogsContent();
                }
                content = logsContent;
                break;
        }
        
        if (content != null) {
            content.getStyleClass().add("slide-in");
            contentStack.getChildren().add(content);
            
            // 添加动画效果
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, new javafx.animation.KeyValue(content.opacityProperty(), 0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), new javafx.animation.KeyValue(content.opacityProperty(), 1)),
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, new javafx.animation.KeyValue(content.translateXProperty(), -20)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), new javafx.animation.KeyValue(content.translateXProperty(), 0))
            );
            timeline.play();
        }
    }

    private void setupUsersContent() {
        usersContent.getAddBtn().setOnAction(e -> showAddUserDialog());
        usersContent.getEditBtn().setOnAction(e -> {
            UserRow selected = usersContent.getTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditUserDialog(selected.username.get());
            }
        });
        usersContent.getDeleteBtn().setOnAction(e -> {
            UserRow selected = usersContent.getTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteUser(selected.username.get());
            }
        });
        refreshUsersTable();
    }

    private void setupConfigContent() {
        configContent.getPortField().setText(String.valueOf(config.getPort()));
        configContent.getRootDirField().setText(config.getRootDirectory());
        configContent.getMaxConnField().setText(String.valueOf(config.getMaxConnections()));

        configContent.getBrowseBtn().setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Select Root Directory");
            File dir = dirChooser.showDialog(primaryStage);
            if (dir != null) {
                configContent.getRootDirField().setText(dir.getAbsolutePath());
            }
        });

        configContent.getSaveBtn().setOnAction(e -> {
            try {
                config.setPort(Integer.parseInt(configContent.getPortField().getText()));
                config.setRootDirectory(configContent.getRootDirField().getText());
                config.setMaxConnections(Integer.parseInt(configContent.getMaxConnField().getText()));
                config.save(CONFIG_PATH);
                updateStats();
                showSuccessMessage("Configuration saved successfully!");
            } catch (Exception ex) {
                showErrorMessage("Failed to save: " + ex.getMessage());
            }
        });
    }

    private void setupLogsContent() {
        logger.addListener(entry -> Platform.runLater(() -> {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String line = String.format("[%s] [%s] %s%n",
                    entry.timestamp.format(fmt), entry.level, entry.message);
            logsContent.getLogsArea().appendText(line);
        }));

        logsContent.getClearBtn().setOnAction(e -> {
            logger.clearLogs();
            logsContent.getLogsArea().clear();
        });
    }

    private void toggleServer() {
        try {
            if (ftpServer.isRunning()) {
                ftpServer.stop();
            } else {
                ftpServer.start();
            }
        } catch (IOException e) {
            showErrorMessage(e.getMessage());
        }
    }

    private void updateStats() {
        Platform.runLater(() -> {
            if (overviewContent != null) {
                overviewContent.getStatPortValue().setText(String.valueOf(config.getPort()));
                int connCount = ftpServer != null ? ftpServer.getConnectionCount() : 0;
                overviewContent.getStatConnectionsValue().setText(String.valueOf(connCount));
                overviewContent.getStatMaxConnectionsValue().setText(String.valueOf(config.getMaxConnections()));
                overviewContent.getStatUsersValue().setText(String.valueOf(userManager.getAllUsers().size()));
            }
            refreshClientsTable();
        });
    }

    private void refreshClientsTable() {
        if (clientsContent != null && clientsContent.getClientsData() != null) {
            clientsContent.getClientsData().clear();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            if (ftpServer != null) {
                for (FtpServer.ClientSession session : ftpServer.getClientSessions()) {
                    clientsContent.getClientsData().add(new ClientRow(
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
        if (usersContent != null && usersContent.getUsersData() != null) {
            usersContent.getUsersData().clear();
            for (User user : userManager.getAllUsers()) {
                usersContent.getUsersData().add(new UserRow(
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
        content.getStyleClass().add("dialog-content");

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
                ((Label) node).getStyleClass().add("form-label-small");
            }
        }

        dialog.getDialogPane().setContent(content);

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
        content.getStyleClass().add("dialog-content");

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
                ((Label) node).getStyleClass().add("form-label-small");
            }
        }

        dialog.getDialogPane().setContent(content);

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
        confirm.getDialogPane().getStyleClass().add("alert");
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
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
        alert.getDialogPane().getStyleClass().add("alert");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        // 添加动画效果
        alert.getDialogPane().getStyleClass().add("fade-in");
        Platform.runLater(() -> {
            alert.getDialogPane().getStyleClass().add("visible");
        });
        
        alert.showAndWait();
    }
    
    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStyleClass().add("alert");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        alert.showAndWait();
    }
    
    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStyleClass().add("alert");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        alert.showAndWait();
    }
    
    private void showConfirmationMessage(String title, String message, Runnable onConfirm) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStyleClass().add("alert");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                onConfirm.run();
            }
        });
    }

    private void cleanup() {
        if (updateTimeline != null) {
            updateTimeline.stop();
        }
    }

    @Override
    public void onLogEntry(Logger.LogEntry entry) {
        Platform.runLater(() -> {
            if (overviewContent != null && overviewContent.getLogTextArea() != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String line = String.format("[%s] [%s] %s%n",
                        entry.timestamp.format(fmt), entry.level, entry.message);
                overviewContent.getLogTextArea().appendText(line);
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

    private void initSystemTray() {
        systemTrayManager = new SystemTrayManager(primaryStage, ftpServer, logger);
        systemTrayManager.initSystemTray();
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
        // 同步更新托盘状态，即使托盘尚未初始化也会保存状态
        if (systemTrayManager != null) {
            systemTrayManager.updateTrayServerStatus(true);
        }
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
        // 同步更新托盘状态，即使托盘尚未初始化也会保存状态
        if (systemTrayManager != null) {
            systemTrayManager.updateTrayServerStatus(false);
        }
    }
}