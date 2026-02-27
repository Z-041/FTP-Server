package com.ftpserver.ui.model;

public class UserRow {
    public final String username;
    public final String homeDir;
    public final String enabled;
    public final String permissions;

    public UserRow(String username, String homeDir, String enabled, String permissions) {
        this.username = username;
        this.homeDir = homeDir;
        this.enabled = enabled;
        this.permissions = permissions;
    }

    public String get() {
        return username;
    }
}
