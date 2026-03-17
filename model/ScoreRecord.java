package model;

public class ScoreRecord {
    private final String username;
    private final String mode;
    private final int score;
    private final String summary;
    private final String timestamp;

    public ScoreRecord(String username, String mode, int score, String summary, String timestamp) {
        this.username = username;
        this.mode = mode;
        this.score = score;
        this.summary = summary;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getMode() {
        return mode;
    }

    public int getScore() {
        return score;
    }

    public String getSummary() {
        return summary;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
