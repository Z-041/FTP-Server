package com.ftpserver.user;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.mindrot.jbcrypt.BCrypt;
import com.ftpserver.util.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserManager {
    private static final int BCRYPT_ROUNDS = 12;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$");
    
    private List<User> users;
    private String usersFilePath;
    private final Gson gson;
    private final Logger logger;

    public UserManager(String usersFilePath) {
        this.usersFilePath = usersFilePath;
        this.gson = new Gson();
        this.logger = Logger.getInstance();
        this.users = new ArrayList<>();
        loadUsers();
    }

    /**
     * 加载用户列表从文件中
     */
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
                logger.error("Failed to load users: " + e.getMessage(), "UserManager", "-");  
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
            logger.error("Failed to save users: " + e.getMessage(), "UserManager", "-");
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

    public boolean validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public void addUser(User user) {
        if (!user.isAnonymous()) {
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                if (!isPasswordHashed(user.getPassword())) {
                    user.setPassword(hashPassword(user.getPassword()));
                }
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
        if (!user.isAnonymous()) {
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                if (!isPasswordHashed(user.getPassword())) {
                    user.setPassword(hashPassword(user.getPassword()));
                }
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
        return password != null && password.startsWith("$2a$");
    }

    private boolean verifyPassword(String plainPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isEmpty()) {
            return plainPassword == null || plainPassword.isEmpty();
        }
        if (plainPassword == null) {
            return false;
        }
        if (!isPasswordHashed(storedPassword)) {
            if (storedPassword.startsWith("SHA256:") || storedPassword.startsWith("SHA256SALT:")) {
                return verifyLegacyPassword(plainPassword, storedPassword);
            }
            return plainPassword.equals(storedPassword);
        }
        return BCrypt.checkpw(plainPassword, storedPassword);
    }

    private boolean verifyLegacyPassword(String plainPassword, String storedPassword) {
        if (storedPassword.startsWith("SHA256SALT:")) {
            try {
                String[] parts = storedPassword.split(":");
                if (parts.length != 3) {
                    return false;
                }
                String saltHex = parts[1];
                String storedHashHex = parts[2];
                String saltedPassword = saltHex + plainPassword;
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(saltedPassword.getBytes("UTF-8"));
                String computedHashHex = bytesToHex(hash);
                return storedHashHex.equals(computedHashHex);
            } catch (Exception e) {
                return false;
            }
        } else if (storedPassword.startsWith("SHA256:")) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(plainPassword.getBytes("UTF-8"));
                String computedHash = "SHA256:" + bytesToHex(hash);
                return storedPassword.equals(computedHash);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
