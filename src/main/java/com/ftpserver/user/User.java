package com.ftpserver.user;

import java.util.HashSet;
import java.util.Set;

public class User {
    private String username;
    private String password;
    private String homeDirectory;
    private Set<Permission> permissions;
    private boolean enabled;
    private boolean anonymous;

    public User() {
        this.permissions = new HashSet<>();
        this.enabled = true;
    }

    public User(String username, String password, String homeDirectory) {
        this();
        this.username = username;
        this.password = password;
        this.homeDirectory = homeDirectory;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public void addPermission(Permission permission) {
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getHomeDirectory() { return homeDirectory; }
    public void setHomeDirectory(String homeDirectory) { this.homeDirectory = homeDirectory; }

    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isAnonymous() { return anonymous; }
    public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }

    public enum Permission {
        READ,
        WRITE,
        DELETE,
        CREATE_DIR,
        DELETE_DIR,
        RENAME,
        LIST
    }
}
