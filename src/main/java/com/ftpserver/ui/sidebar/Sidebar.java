package com.ftpserver.ui.sidebar;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class Sidebar extends VBox {

    private Button navOverview;
    private Button navClients;
    private Button navUsers;
    private Button navConfig;
    private Button navLogs;
    private VBox navItems;

    public Sidebar() {
        initialize();
    }

    private void initialize() {
        getStyleClass().add("sidebar");
        setPrefWidth(260);

        javafx.scene.control.Label title = new javafx.scene.control.Label("FTP Server");
        title.getStyleClass().add("sidebar-title");

        javafx.scene.control.Label subtitle = new javafx.scene.control.Label("Advanced File Transfer");
        subtitle.getStyleClass().add("sidebar-subtitle");

        navItems = new VBox(0);
        navItems.setFillWidth(true);

        navOverview = createNavItem("Overview", true);
        navClients = createNavItem("Clients", false);
        navUsers = createNavItem("Users", false);
        navConfig = createNavItem("Configuration", false);
        navLogs = createNavItem("Logs", false);

        navItems.getChildren().addAll(navOverview, navClients, navUsers, navConfig, navLogs);

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        getChildren().addAll(title, subtitle, navItems, spacer);
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

    public void setActiveNav(Button active) {
        for (javafx.scene.Node node : navItems.getChildren()) {
            node.getStyleClass().remove("active");
        }
        active.getStyleClass().add("active");
    }

    public Button getNavOverview() {
        return navOverview;
    }

    public Button getNavClients() {
        return navClients;
    }

    public Button getNavUsers() {
        return navUsers;
    }

    public Button getNavConfig() {
        return navConfig;
    }

    public Button getNavLogs() {
        return navLogs;
    }
}