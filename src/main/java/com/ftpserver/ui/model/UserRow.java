package com.ftpserver.ui.model;

import javafx.beans.property.SimpleStringProperty;

public class UserRow {
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