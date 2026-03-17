package server;

import model.Question;
import model.User;

import java.io.*;
import java.util.*;

public class GameService {
    private List<Question> questions = new ArrayList<>();

    public GameService() {
        loadQuestions();
    }

    private void loadQuestions() {
        try (BufferedReader br = new BufferedReader(new FileReader("data/questions.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");

                String[] choices = {p[3], p[4], p[5], p[6]};
                questions.add(new Question(p[0], p[1], p[2], choices, p[7].charAt(0)));
            }
        } catch (IOException e) {
            System.out.println("Error loading questions.");
        }
    }

    public void startSingleGame(User user, BufferedReader in, PrintWriter out) throws IOException {
        for (Question q : questions) {
            out.println("\n" + q.getText());

            char option = 'A';
            for (String c : q.getChoices()) {
                out.println(option + ". " + c);
                option++;
            }

            out.println("Your answer:");

            long start = System.currentTimeMillis();
            String answer = in.readLine();

            if (answer == null) return;

            long end = System.currentTimeMillis();

            if ((end - start) > 15000) {
                out.println("Time out!");
                continue;
            }

            if (answer.toUpperCase().charAt(0) == q.getCorrectAnswer()) {
                out.println("Correct!");
                user.addScore(10);
            } else {
                out.println("Wrong!");
            }

            out.println("Score: " + user.getScore());
        }

        out.println("Game Over! Final Score: " + user.getScore());
    }
    public List<Question> getQuestions() {
    return questions;
}
}