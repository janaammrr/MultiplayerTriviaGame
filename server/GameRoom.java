package server;

import model.Question;
import model.User;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class GameRoom {
    private List<ClientHandler> players = new ArrayList<>();
    private List<Question> questions;
    private Map<String, Integer> scores = new HashMap<>();

    public GameRoom(List<Question> questions) {
        this.questions = questions;
    }

    public void addPlayer(ClientHandler player) {
        players.add(player);
        scores.put(player.getUser().getUsername(), 0);
    }

    public void startGame() {
        broadcast("Game starting with " + players.size() + " players!");

        for (Question q : questions) {
            askQuestion(q);
        }

        endGame();
    }

    private void askQuestion(Question q) {
        broadcast("\n" + q.getText());

        char option = 'A';
        for (String c : q.getChoices()) {
            broadcast(option + ". " + c);
            option++;
        }

        broadcast("Answer now! (15 seconds)");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> waitForAnswer());

        try {
            String result = future.get(15, TimeUnit.SECONDS);

            if (result != null) {
                String[] parts = result.split(":");
                String username = parts[0];
                char answer = parts[1].toUpperCase().charAt(0);

                if (answer == q.getCorrectAnswer()) {
                    broadcast(username + " answered correctly!");
                    scores.put(username, scores.get(username) + 10);
                } else {
                    broadcast(username + " answered wrong!");
                }
            } else {
                broadcast("No answers received.");
            }

        } catch (TimeoutException e) {
            broadcast("Time is up!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        executor.shutdownNow();
        showScores();
    }

    private String waitForAnswer() {
        while (true) {
            for (ClientHandler p : players) {
                String ans = p.pollAnswer();
                if (ans != null) {
                    return p.getUser().getUsername() + ":" + ans;
                }
            }
        }
    }

    private void showScores() {
        broadcast("Scores:");
        for (String user : scores.keySet()) {
            broadcast(user + ": " + scores.get(user));
        }
    }

    private void endGame() {
        broadcast("\nGame Over!");
        showScores();
    }

    private void broadcast(String msg) {
        for (ClientHandler p : players) {
            p.sendMessage(msg);
        }
    }
}