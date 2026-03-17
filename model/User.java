package model;

public class User {
    private String name;
    private String username;
    private String password;
    private int score;

    public User(String name, String username, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.score = 0;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getName() { return name; }

    public int getScore() { return score; }
    public void addScore(int s) { score += s; }
}