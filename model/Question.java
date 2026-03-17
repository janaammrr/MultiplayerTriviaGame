package model;

public class Question {
    private String text, category, difficulty;
    private String[] choices;
    private char correctAnswer;

    public Question(String text, String category, String difficulty,
                    String[] choices, char correctAnswer) {
        this.text = text;
        this.category = category;
        this.difficulty = difficulty;
        this.choices = choices;
        this.correctAnswer = correctAnswer;
    }

    public String getText() { return text; }
    public String[] getChoices() { return choices; }
    public char getCorrectAnswer() { return correctAnswer; }
}