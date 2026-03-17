package server;

import exceptions.ClientDisconnectedException;
import model.AnswerRecord;
import model.Category;
import model.DifficultyLevel;
import model.GameConfig;
import model.Question;
import model.ScoreRecord;
import model.User;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class GameService {
    private static final String TIMEOUT_MARKER = "__TIMEOUT__";
    private final List<Question> questions = new ArrayList<>();
    private final GameConfig config;
    private final ScoreHistoryService scoreHistoryService;

    public GameService(GameConfig config, ScoreHistoryService scoreHistoryService) {
        this.config = config;
        this.scoreHistoryService = scoreHistoryService;
        loadQuestions();
    }

    private void loadQuestions() {
        try (BufferedReader br = new BufferedReader(new FileReader("data/questions.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 8) {
                    continue;
                }
                String[] choices = {parts[3], parts[4], parts[5], parts[6]};
                questions.add(new Question(
                        parts[0],
                        Category.fromValue(parts[1]),
                        DifficultyLevel.fromValue(parts[2]),
                        choices,
                        parts[7].charAt(0)
                ));
            }
        } catch (IOException e) {
            Server.log("Error loading questions.");
        } catch (IllegalArgumentException e) {
            Server.log("Question bank contains invalid category or difficulty: " + e.getMessage());
        }
    }

    public synchronized List<Question> getQuestions() {
        return new ArrayList<>(questions);
    }

    public List<Question> getQuestionsForGame(String category, String difficulty, int count) {
        List<Question> filtered = new ArrayList<>();
        for (Question question : questions) {
            boolean categoryMatches = "ALL".equalsIgnoreCase(category)
                    || question.getCategory().getDisplayName().equalsIgnoreCase(category)
                    || question.getCategory().name().equalsIgnoreCase(category);
            boolean difficultyMatches = "ALL".equalsIgnoreCase(difficulty)
                    || question.getDifficulty().getDisplayName().equalsIgnoreCase(difficulty)
                    || question.getDifficulty().name().equalsIgnoreCase(difficulty);
            if (categoryMatches && difficultyMatches) {
                filtered.add(question);
            }
        }

        Collections.shuffle(filtered);
        if (filtered.size() > count) {
            return new ArrayList<>(filtered.subList(0, count));
        }
        return filtered;
    }

    public Set<String> getAvailableCategories() {
        Set<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Question question : questions) {
            categories.add(question.getCategory().getDisplayName());
        }
        return categories;
    }

    public Set<String> getAvailableDifficulties() {
        Set<String> difficulties = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Question question : questions) {
            difficulties.add(question.getDifficulty().getDisplayName());
        }
        return difficulties;
    }

    public int getMaxQuestionCount() {
        return Math.min(config.getMaxQuestionsPerGame(), questions.size());
    }

    public boolean startSingleGame(User user, BufferedReader in, PrintWriter out) throws IOException {
        user.resetScore();
        List<Question> gameQuestions = getQuestionsForGame("ALL", "ALL",
                Math.min(config.getSinglePlayerQuestions(), questions.size()));
        List<AnswerRecord> results = new ArrayList<>();

        out.println("Starting single player game with " + gameQuestions.size() + " questions.");

        for (int i = 0; i < gameQuestions.size(); i++) {
            Question question = gameQuestions.get(i);
            out.println("\nQuestion " + (i + 1) + "/" + gameQuestions.size());
            out.println("Category: " + question.getCategory().getDisplayName()
                    + " | Difficulty: " + question.getDifficulty().getDisplayName());
            out.println(question.getText());

            char option = 'A';
            for (String choice : question.getChoices()) {
                out.println(option + ". " + choice);
                option++;
            }

            String answer;
            try {
                answer = readTimedAnswer(in, out, config.getQuestionDurationSeconds());
            } catch (ClientDisconnectedException e) {
                Server.log(e.getMessage());
                return false;
            }

            if (isQuit(answer)) {
                out.println("You quit the game.");
                scoreHistoryService.recordGame(user.getUsername(), "single", user.getScore(), "Player quit early");
                return true;
            }

            if (TIMEOUT_MARKER.equals(answer)) {
                results.add(new AnswerRecord(question.getText(), "No Answer",
                        question.getCorrectAnswer(), false));
                out.println("Score: " + user.getScore());
                continue;
            }

            String normalized = normalizeAnswer(answer);
            boolean correct = normalized != null && normalized.charAt(0) == question.getCorrectAnswer();
            if (correct) {
                out.println("Correct!");
                user.addScore(10);
            } else {
                out.println("Wrong!");
            }

            results.add(new AnswerRecord(question.getText(),
                    normalized == null || normalized.isEmpty() ? "No Answer" : normalized,
                    question.getCorrectAnswer(), correct));
            out.println("Score: " + user.getScore());
        }

        out.println("Game Over! Final Score: " + user.getScore());
        printAnswerSummary(out, results);
        scoreHistoryService.recordGame(user.getUsername(), "single", user.getScore(), buildSummary(results));
        return true;
    }

    public void printScoreHistory(User user, PrintWriter out) {
        List<ScoreRecord> history = scoreHistoryService.getLastScores(user.getUsername(), 5);
        out.println("Recent score history:");
        if (history.isEmpty()) {
            out.println("No previous games found.");
            return;
        }

        for (ScoreRecord record : history) {
            out.println(record.getTimestamp() + " | " + record.getMode()
                    + " | score=" + record.getScore() + " | " + record.getSummary());
        }
    }

    public void recordMultiplayerGame(String username, int score, String summary) {
        scoreHistoryService.recordGame(username, "multiplayer", score, summary);
    }

    public String buildSummary(List<AnswerRecord> results) {
        int correct = 0;
        for (AnswerRecord result : results) {
            if (result.isCorrect()) {
                correct++;
            }
        }
        return correct + "/" + results.size() + " correct";
    }

    public void printAnswerSummary(PrintWriter out, List<AnswerRecord> results) {
        out.println("Answer details:");
        for (AnswerRecord result : results) {
            out.println(result.getQuestionText());
            out.println("Your answer: " + result.getSubmittedAnswer()
                    + " | Correct: " + result.getCorrectAnswer()
                    + " | Result: " + (result.isCorrect() ? "Correct" : "Wrong"));
        }
    }

    public String normalizeAnswer(String answer) {
        if (answer == null) {
            return null;
        }
        String trimmed = answer.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return String.valueOf(Character.toUpperCase(trimmed.charAt(0)));
    }

    private String readTimedAnswer(BufferedReader in, PrintWriter out, int durationSeconds)
            throws IOException, ClientDisconnectedException {
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        List<Integer> milestones = buildCountdownMilestones(durationSeconds);
        int milestoneIndex = 0;

        while (System.currentTimeMillis() < endTime) {
            if (in.ready()) {
                String answer = in.readLine();
                if (answer == null) {
                    throw new ClientDisconnectedException("Client disconnected during single-player game.");
                }
                return answer;
            }

            long remainingSeconds = Math.max(0, (long) Math.ceil((endTime - System.currentTimeMillis()) / 1000.0));
            while (milestoneIndex < milestones.size() && remainingSeconds <= milestones.get(milestoneIndex)) {
                out.println("Time left: " + milestones.get(milestoneIndex) + " seconds");
                milestoneIndex++;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ClientDisconnectedException("Game timer interrupted.");
            }
        }

        out.println("Time out!");
        return TIMEOUT_MARKER;
    }

    public boolean isQuit(String value) {
        return value != null && ("-".equals(value.trim()) || " ".equals(value));
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
