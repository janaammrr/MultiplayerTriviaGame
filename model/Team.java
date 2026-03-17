package model;

import server.ClientHandler;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private final String teamName;
    private final String creatorUsername;
    private final String category;
    private final String difficulty;
    private final int questionCount;
    private final int maxPlayers;
    private final List<ClientHandler> members = new ArrayList<>();
    private boolean inGame;

    public Team(String teamName, String creatorUsername, String category, String difficulty,
                int questionCount, int maxPlayers) {
        this.teamName = teamName;
        this.creatorUsername = creatorUsername;
        this.category = category;
        this.difficulty = difficulty;
        this.questionCount = questionCount;
        this.maxPlayers = maxPlayers;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public String getCategory() {
        return category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public synchronized List<ClientHandler> getMembers() {
        return new ArrayList<>(members);
    }

    public synchronized boolean addMember(ClientHandler player) {
        if (inGame || members.size() >= maxPlayers || members.contains(player)) {
            return false;
        }
        members.add(player);
        return true;
    }

    public synchronized void removeMember(ClientHandler player) {
        members.remove(player);
    }

    public synchronized int size() {
        return members.size();
    }

    public synchronized boolean isReady() {
        return members.size() == maxPlayers;
    }

    public synchronized boolean isInGame() {
        return inGame;
    }

    public synchronized void setInGame(boolean inGame) {
        this.inGame = inGame;
    }
}
