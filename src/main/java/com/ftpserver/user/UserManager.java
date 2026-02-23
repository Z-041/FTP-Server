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
        return users.stream()
                .filter(u -> u.getUsername().equals(username) && 
                           verifyPassword(password, u.getPassword()) && 
                           u.isEnabled())
                .findFirst();
    }

    public Optional<User> getUser(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    public void addUser(User user) {
        // Hash the password before storing
        if (user.getPassword() != null && !user.getPassword().isEmpty() && !user.isAnonymous()) {
            if (!isPasswordHashed(user.getPassword())) {
                user.setPassword(hashPasswordWithSalt(user.getPassword()));
            }
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
            if (!isPasswordHashed(user.getPassword())) {
                user.setPassword(hashPasswordWithSalt(user.getPassword()));
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

    private boolean isPasswordHashed(String password) {
        return password != null && (password.startsWith("SHA256:") || password.startsWith("SHA256SALT:"));
    }

    private boolean verifyPassword(String plainPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isEmpty()) {
            return plainPassword == null || plainPassword.isEmpty();
        }
        if (plainPassword == null) {
            return false;
        }
        if (!isPasswordHashed(storedPassword)) {
            return plainPassword.equals(storedPassword);
        }
        if (storedPassword.startsWith("SHA256SALT:")) {
            return verifyPasswordWithSalt(plainPassword, storedPassword);
        }
        String hashedOld = hashPasswordWithoutSalt(plainPassword);
        return storedPassword.equals(hashedOld);
    }

    private String hashPasswordWithSalt(String password) {
        try {
            byte[] salt = new byte[16];
            java.security.SecureRandom.getInstanceStrong().nextBytes(salt);
            String saltHex = bytesToHex(salt);
            String saltedPassword = saltHex + password;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(saltedPassword.getBytes("UTF-8"));
            return "SHA256SALT:" + saltHex + ":" + bytesToHex(hash);
        } catch (Exception e) {
            return password;
        }
    }

    private boolean verifyPasswordWithSalt(String plainPassword, String storedPassword) {
        try {
            String[] parts = storedPassword.split(":");
            if (parts.length != 3) {
                return false;
            }
            String saltHex = parts[1];
            String storedHashHex = parts[2];
            String saltedPassword = saltHex + plainPassword;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(saltedPassword.getBytes("UTF-8"));
            String computedHashHex = bytesToHex(hash);
            return storedHashHex.equals(computedHashHex);
        } catch (Exception e) {
            return false;
        }
    }

    private String hashPasswordWithoutSalt(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            return "SHA256:" + bytesToHex(hash);
        } catch (Exception e) {
            return password;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
