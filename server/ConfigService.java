package server;

import model.GameConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigService {
    private static final String FILE = "data/config.txt";

    public GameConfig loadConfig() {
        int minTeamPlayers = 1;
        int maxTeamPlayers = 2;
        int questionDurationSeconds = 15;
        int singlePlayerQuestions = 5;
        int maxQuestionsPerGame = 10;
        List<Integer> countdownWarnings = new ArrayList<>();
        countdownWarnings.add(10);
        countdownWarnings.add(5);

        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }

                String[] parts = line.split("=", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();

                switch (key) {
                    case "minTeamPlayers":
                        minTeamPlayers = Integer.parseInt(value);
                        break;
                    case "maxTeamPlayers":
                        maxTeamPlayers = Integer.parseInt(value);
                        break;
                    case "questionDurationSeconds":
                        questionDurationSeconds = Integer.parseInt(value);
                        break;
                    case "singlePlayerQuestions":
                        singlePlayerQuestions = Integer.parseInt(value);
                        break;
                    case "maxQuestionsPerGame":
                        maxQuestionsPerGame = Integer.parseInt(value);
                        break;
                    case "countdownWarnings":
                        countdownWarnings = parseWarnings(value);
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Using default config because config file could not be fully loaded.");
        }

        if (minTeamPlayers < 1) {
            minTeamPlayers = 1;
        }
        if (maxTeamPlayers < minTeamPlayers) {
            maxTeamPlayers = minTeamPlayers;
        }
        if (questionDurationSeconds < 5) {
            questionDurationSeconds = 15;
        }
        if (singlePlayerQuestions < 1) {
            singlePlayerQuestions = 5;
        }
        if (maxQuestionsPerGame < 1) {
            maxQuestionsPerGame = 10;
        }

        return new GameConfig(minTeamPlayers, maxTeamPlayers, questionDurationSeconds,
                singlePlayerQuestions, maxQuestionsPerGame, countdownWarnings);
    }

    private List<Integer> parseWarnings(String value) {
        List<Integer> warnings = new ArrayList<>();
        for (String part : value.split(",")) {
            try {
                warnings.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (warnings.isEmpty()) {
            warnings.add(10);
            warnings.add(5);
        }
        return warnings;
    }
}
