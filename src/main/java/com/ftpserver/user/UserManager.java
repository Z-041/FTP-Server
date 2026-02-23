package com.ftpserver.user;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UserManager {
    private List<User> users;
    private String usersFilePath;
    private final Gson gson;

    public UserManager(String usersFilePath) {
        this.usersFilePath = usersFilePath;
        this.gson = new Gson();
        this.users = new ArrayList<>();
        loadUsers();
    }

    public void loadUsers() {
        Path path = Paths.get(usersFilePath);
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                Type userListType = new TypeToken<List<User>>(){}.getType();
                List<User> loadedUsers = gson.fromJson(reader, userListType);
                if (loadedUsers != null) {
                    this.users = loadedUsers;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ensureAnonymousUser();
    }

    private void ensureAnonymousUser() {
        boolean hasAnonymous = users.stream().anyMatch(User::isAnonymous);
        if (!hasAnonymous) {
            User anonymous = new User();
            anonymous.setUsername("anonymous");
            anonymous.setPassword("");
            anonymous.setAnonymous(true);
            anonymous.setEnabled(true);
            anonymous.addPermission(User.Permission.READ);
            anonymous.addPermission(User.Permission.WRITE);
            anonymous.addPermission(User.Permission.LIST);
            users.add(0, anonymous);
            saveUsers();
        }
    }

    public void saveUsers() {
        try {
            if (usersFilePath != null && !usersFilePath.isEmpty()) {
                Path path = Paths.get(usersFilePath);
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }
                try (Writer writer = Files.newBufferedWriter(path)) {
                    gson.toJson(users, writer);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to save users: " + e.getMessage());
        }
    }

    public Optional<User> authenticate(String username, String password) {
        String hashedPassword = hashPassword(password);
        return users.stream()
                .filter(u -> u.getUsername().equals(username) && 
                           (u.getPassword().equals(password) || u.getPassword().equals(hashedPassword)) && 
                           u.isEnabled())
                .findFirst();
    }

    public Optional<User> getUser(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    public void addUser(User user) {
        // Hash the password before storing
        if (user.getPassword() != null && !user.getPassword().isEmpty() && !user.isAnonymous()) {
            user.setPassword(hashPassword(user.getPassword()));
        }
        users.removeIf(u -> u.getUsername().equals(user.getUsername()));
        users.add(user);
        saveUsers();
    }

    public boolean removeUser(String username) {
        Optional<User> userOpt = getUser(username);
        if (userOpt.isPresent() && userOpt.get().isAnonymous()) {
            return false;
        }
        boolean removed = users.removeIf(u -> u.getUsername().equals(username));
        if (removed) saveUsers();
        return removed;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public void updateUser(User user) {
        // Hash the password before storing (if it's not already hashed)
        if (user.getPassword() != null && !user.getPassword().isEmpty() && !user.isAnonymous()) {
            if (!user.getPassword().startsWith("SHA256:")) {
                user.setPassword(hashPassword(user.getPassword()));
            }
        }
        int index = -1;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equals(user.getUsername())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            users.set(index, user);
            saveUsers();
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return "SHA256:" + sb.toString();
        } catch (Exception e) {
            return password; // fallback to plain text if hashing fails
        }
    }
}
