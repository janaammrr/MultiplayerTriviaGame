package model;

public class User {
    private final String name;
    private final String username;
    private final String password;
    private int score;

    public User(String name, String username, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.score = 0;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int scoreToAdd) {
        score += scoreToAdd;
    }

    public void resetScore() {
        score = 0;
    }
}
