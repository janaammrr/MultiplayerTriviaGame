package server;

import model.ScoreRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScoreHistoryService {
    private static final String FILE = "data/score_history.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, List<ScoreRecord>> historyByUser = new HashMap<>();

    public ScoreHistoryService() {
        loadHistory();
    }

    private void loadHistory() {
        File file = new File(FILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("Could not create score history file.");
            }
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", 5);
                if (parts.length < 5) {
                    continue;
                }
                ScoreRecord record = new ScoreRecord(parts[0], parts[1], Integer.parseInt(parts[2]), parts[3], parts[4]);
                historyByUser.computeIfAbsent(parts[0], key -> new ArrayList<>()).add(record);
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error loading score history.");
        }
    }

    public synchronized void recordGame(String username, String mode, int score, String summary) {
        String sanitizedSummary = summary.replace("|", "/");
        ScoreRecord record = new ScoreRecord(username, mode, score, sanitizedSummary, LocalDateTime.now().format(FORMATTER));
        historyByUser.computeIfAbsent(username, key -> new ArrayList<>()).add(record);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE, true))) {
            bw.write(username + "|" + mode + "|" + score + "|" + sanitizedSummary + "|" + record.getTimestamp());
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error saving score history.");
        }
    }

    public synchronized List<ScoreRecord> getLastScores(String username, int limit) {
        List<ScoreRecord> records = historyByUser.getOrDefault(username, Collections.emptyList());
        List<ScoreRecord> result = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0 && result.size() < limit; i--) {
            result.add(records.get(i));
        }
        return result;
    }
}
