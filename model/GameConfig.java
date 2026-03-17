package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameConfig {
    private final int minTeamPlayers;
    private final int maxTeamPlayers;
    private final int questionDurationSeconds;
    private final int singlePlayerQuestions;
    private final int maxQuestionsPerGame;
    private final List<Integer> countdownWarnings;

    public GameConfig(int minTeamPlayers, int maxTeamPlayers, int questionDurationSeconds,
                      int singlePlayerQuestions, int maxQuestionsPerGame, List<Integer> countdownWarnings) {
        this.minTeamPlayers = minTeamPlayers;
        this.maxTeamPlayers = maxTeamPlayers;
        this.questionDurationSeconds = questionDurationSeconds;
        this.singlePlayerQuestions = singlePlayerQuestions;
        this.maxQuestionsPerGame = maxQuestionsPerGame;
        this.countdownWarnings = new ArrayList<>(countdownWarnings);
        this.countdownWarnings.sort(Collections.reverseOrder());
    }

    public int getMinTeamPlayers() {
        return minTeamPlayers;
    }

    public int getMaxTeamPlayers() {
        return maxTeamPlayers;
    }

    public int getQuestionDurationSeconds() {
        return questionDurationSeconds;
    }

    public int getSinglePlayerQuestions() {
        return singlePlayerQuestions;
    }

    public int getMaxQuestionsPerGame() {
        return maxQuestionsPerGame;
    }

    public List<Integer> getCountdownWarnings() {
        return Collections.unmodifiableList(countdownWarnings);
    }
}
