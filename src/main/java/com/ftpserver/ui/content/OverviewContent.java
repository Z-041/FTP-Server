package com.ftpserver.ui.content;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class OverviewContent extends VBox {

    private Label statPortValue;
    private Label statConnectionsValue;
    private Label statMaxConnectionsValue;
    private Label statUsersValue;
    private TextArea logTextArea;

    public OverviewContent() {
        initialize();
    }

    private void initialize() {
        setSpacing(24);
        setFillWidth(true);

        GridPane statsGrid = createStatsGrid();
        VBox recentActivity = createRecentActivity();

        getChildren().addAll(statsGrid, recentActivity);
    }

    private GridPane createStatsGrid() {
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(20);
        statsGrid.setPadding(new Insets(0));

        statPortValue = new Label();
        statConnectionsValue = new Label("0");
        statMaxConnectionsValue = new Label();
        statUsersValue = new Label();

        statsGrid.add(createStatCard("Server Port", statPortValue, "🔌"), 0, 0);
        statsGrid.add(createStatCard("Active Connections", statConnectionsValue, "👥"), 1, 0);
        statsGrid.add(createStatCard("Max Connections", statMaxConnectionsValue, "📊"), 0, 1);
        statsGrid.add(createStatCard("Total Users", statUsersValue, "👤"), 1, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        statsGrid.getColumnConstraints().addAll(col1, col2);

        return statsGrid;
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

    private VBox createRecentActivity() {
        VBox recentActivity = new VBox(16);
        Label activityTitle = new Label("Recent Activity");
        activityTitle.getStyleClass().add("card-title");

        logTextArea = new TextArea();
        logTextArea.getStyleClass().add("log-area");
        logTextArea.setEditable(false);
        logTextArea.setPrefRowCount(12);

        recentActivity.getChildren().addAll(activityTitle, logTextArea);
        return recentActivity;
    }

    public Label getStatPortValue() {
        return statPortValue;
    }

    public Label getStatConnectionsValue() {
        return statConnectionsValue;
    }

    public Label getStatMaxConnectionsValue() {
        return statMaxConnectionsValue;
    }

    public Label getStatUsersValue() {
        return statUsersValue;
    }

    public TextArea getLogTextArea() {
        return logTextArea;
    }
}