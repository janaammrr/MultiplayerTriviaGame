package server;

import model.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    public static class LoginResponse {
        private final User user;
        private final int statusCode;

        public LoginResponse(User user, int statusCode) {
            this.user = user;
            this.statusCode = statusCode;
        }

        public User getUser() {
            return user;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    private static final String FILE = "data/users.txt";
    private final Map<String, User> users = new HashMap<>();

    public AuthService() {
        loadUsers();
    }

    private void loadUsers() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    users.put(parts[0], new User(parts[2], parts[0], parts[1]));
                }
            }
        } catch (IOException e) {
            Server.log("Error loading users.");
        }
    }

    public synchronized boolean usernameExists(String username) {
        return users.containsKey(username);
    }

    public synchronized String register(String name, String username, String password) {
        if (users.containsKey(username)) {
            return "CUSTOM ERROR: Username already exists. Change username.";
        }

        User user = new User(name, username, password);
        users.put(username, user);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE, true))) {
            bw.write(username + "," + password + "," + name);
            bw.newLine();
        } catch (IOException e) {
            return "ERROR: Could not save user";
        }

        return "SUCCESS";
    }

    public synchronized LoginResponse login(String username, String password) {
        if (!users.containsKey(username)) {
            return new LoginResponse(null, 404);
        }

        User user = users.get(username);
        if (!user.getPassword().equals(password)) {
            return new LoginResponse(null, 401);
        }

        return new LoginResponse(user, 200);
    }
}
