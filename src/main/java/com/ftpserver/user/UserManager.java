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
            Path path = Paths.get(usersFilePath);
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                gson.toJson(users, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Optional<User> authenticate(String username, String password) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password) && u.isEnabled())
                .findFirst();
    }

    public Optional<User> getUser(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    public void addUser(User user) {
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
}
