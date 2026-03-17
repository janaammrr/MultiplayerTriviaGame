package server;

import model.AnswerRecord;
import model.GameConfig;
import model.Question;
import model.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameRoom {
    private final Team firstTeam;
    private final Team secondTeam;
    private final List<Question> questions;
    private final GameService gameService;
    private final GameConfig config;
    private final Map<String, Integer> scores = new LinkedHashMap<>();
    private final Map<String, List<AnswerRecord>> answerHistory = new HashMap<>();
    private volatile boolean questionActive;

    public GameRoom(Team firstTeam, Team secondTeam, List<Question> questions,
                    GameService gameService, GameConfig config) {
        this.firstTeam = firstTeam;
        this.secondTeam = secondTeam;
        this.questions = questions;
        this.gameService = gameService;
        this.config = config;

        for (ClientHandler player : getAllPlayers()) {
            scores.put(player.getUser().getUsername(), 0);
            answerHistory.put(player.getUser().getUsername(), new ArrayList<>());
            player.setCurrentRoom(this);
        }
    }

    public void startGame() {
        try {
            broadcast("Game starting: " + firstTeam.getTeamName() + " vs " + secondTeam.getTeamName());
            broadcast("Category: " + firstTeam.getCategory() + " | Difficulty: " + firstTeam.getDifficulty()
                    + " | Questions: " + questions.size());

            for (int i = 0; i < questions.size(); i++) {
                askQuestion(i, questions.get(i));
            }

            endGame();
        } catch (Exception e) {
            broadcast("A game error occurred: " + e.getMessage());
            Server.log("Game room error: " + e.getMessage());
        } finally {
            for (ClientHandler player : getAllPlayers()) {
                player.setCurrentRoom(null);
                player.finishMultiplayerSession();
            }
            TeamManager.finishMatch(firstTeam, secondTeam);
        }
    }

    private void askQuestion(int questionIndex, Question question) {
        questionActive = true;
        for (ClientHandler player : getAllPlayers()) {
            player.prepareForNextQuestion();
        }

        broadcast("\nQuestion " + (questionIndex + 1) + "/" + questions.size());
        broadcast("Category: " + question.getCategory().getDisplayName()
                + " | Difficulty: " + question.getDifficulty().getDisplayName());
        broadcast(question.getText());

        char option = 'A';
        for (String choice : question.getChoices()) {
            broadcast(option + ". " + choice);
            option++;
        }

        broadcast("Answer now! (" + config.getQuestionDurationSeconds() + " seconds)");
        long endTime = System.currentTimeMillis() + (config.getQuestionDurationSeconds() * 1000L);
        List<Integer> milestones = buildCountdownMilestones(config.getQuestionDurationSeconds());
        int milestoneIndex = 0;

        while (System.currentTimeMillis() < endTime) {
            long remainingSeconds = Math.max(0, (long) Math.ceil((endTime - System.currentTimeMillis()) / 1000.0));
            while (milestoneIndex < milestones.size() && remainingSeconds <= milestones.get(milestoneIndex)) {
                broadcast("Time left: " + milestones.get(milestoneIndex) + " seconds");
                milestoneIndex++;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        questionActive = false;
        evaluateQuestion(question);
        showScores();
    }

    private void evaluateQuestion(Question question) {
        boolean anySubmitted = false;

        for (ClientHandler player : getAllPlayers()) {
            String username = player.getUser().getUsername();
            String answer = player.pollAnswer();

            if (answer == null || gameService.isQuit(answer)) {
                answerHistory.get(username).add(new AnswerRecord(question.getText(), "No Answer",
                        question.getCorrectAnswer(), false));
                continue;
            }

            anySubmitted = true;
            String normalized = gameService.normalizeAnswer(answer);
            boolean correct = normalized != null && normalized.charAt(0) == question.getCorrectAnswer();
            if (correct) {
                scores.put(username, scores.get(username) + 10);
                broadcast(username + " answered correctly.");
            } else {
                broadcast(username + " answered incorrectly.");
            }

            answerHistory.get(username).add(new AnswerRecord(question.getText(),
                    normalized == null ? answer : normalized, question.getCorrectAnswer(), correct));
        }

        if (!anySubmitted) {
            broadcast("No answers received before timeout.");
        }
        broadcast("Correct answer: " + question.getCorrectAnswer());
    }

    public boolean isQuestionActive() {
        return questionActive;
    }

    public synchronized void handleDisconnect(ClientHandler disconnectedPlayer) {
        String name = disconnectedPlayer.getUser() == null
                ? "Unknown"
                : disconnectedPlayer.getUser().getName();
        broadcast("Player disconnected: " + name);
        Server.log("Client disconnected during game: " + name);
    }

    private void showScores() {
        broadcast("Scores:");
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            broadcast(entry.getKey() + ": " + entry.getValue());
        }
        broadcast("Team totals:");
        broadcast(firstTeam.getTeamName() + ": " + calculateTeamScore(firstTeam));
        broadcast(secondTeam.getTeamName() + ": " + calculateTeamScore(secondTeam));
    }

    private int calculateTeamScore(Team team) {
        int total = 0;
        for (ClientHandler player : team.getMembers()) {
            total += scores.getOrDefault(player.getUser().getUsername(), 0);
        }
        return total;
    }

    private void endGame() {
        broadcast("\nGame Over!");
        showScores();

        int firstTeamScore = calculateTeamScore(firstTeam);
        int secondTeamScore = calculateTeamScore(secondTeam);
        if (firstTeamScore > secondTeamScore) {
            broadcast("Winner: " + firstTeam.getTeamName());
        } else if (secondTeamScore > firstTeamScore) {
            broadcast("Winner: " + secondTeam.getTeamName());
        } else {
            broadcast("The match ended in a draw.");
        }

        for (ClientHandler player : getAllPlayers()) {
            List<AnswerRecord> records = answerHistory.get(player.getUser().getUsername());
            player.sendMessage("Your question details:");
            for (AnswerRecord record : records) {
                player.sendMessage(record.getQuestionText());
                player.sendMessage("Your answer: " + record.getSubmittedAnswer()
                        + " | Correct: " + record.getCorrectAnswer()
                        + " | Result: " + (record.isCorrect() ? "Correct" : "Wrong"));
            }
            gameService.recordMultiplayerGame(player.getUser().getUsername(),
                    scores.getOrDefault(player.getUser().getUsername(), 0),
                    gameService.buildSummary(records));
            player.sendMessage("Match finished. Returning to main menu.");
        }
    }

    private List<ClientHandler> getAllPlayers() {
        List<ClientHandler> players = new ArrayList<>();
        players.addAll(firstTeam.getMembers());
        players.addAll(secondTeam.getMembers());
        return players;
    }

    private void broadcast(String message) {
        for (ClientHandler player : getAllPlayers()) {
            if (!player.isDisconnected()) {
                player.sendMessage(message);
            }
        }
    }

    private List<Integer> buildCountdownMilestones(int durationSeconds) {
        List<Integer> milestones = new ArrayList<>();
        int[] defaults = {15, 10, 5};
        for (int value : defaults) {
            if (durationSeconds >= value) {
                milestones.add(value);
            }
        }
        if (milestones.isEmpty() && durationSeconds > 0) {
            milestones.add(durationSeconds);
        }
        return milestones;
    }
}
