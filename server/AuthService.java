package server;

import model.User;
import java.io.*;
import java.util.*;

public class AuthService {
    private Map<String, User> users = new HashMap<>();
    private final String FILE = "data/users.txt";

    public AuthService() {
        loadUsers();
    }

    private void loadUsers() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                users.put(parts[0], new User(parts[2], parts[0], parts[1]));
            }
        } catch (IOException e) {
            System.out.println("Error loading users.");
        }
    }

    public String register(String name, String username, String password) {
        if (users.containsKey(username))
            return "ERROR: Username already exists";

        User user = new User(name, username, password);
        users.put(username, user);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE, true))) {
            bw.write(username + "," + password + "," + name);
            bw.newLine();
        } catch (IOException e) {}

        return "SUCCESS";
    }

    public User login(String username, String password) {
        if (!users.containsKey(username))
            return null;

        User user = users.get(username);
        if (!user.getPassword().equals(password))
            return new User("ERROR401", "", "");

        return user;
    }
}