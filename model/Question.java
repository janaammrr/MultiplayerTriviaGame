package model;

public class Question {
    private final String text;
    private final Category category;
    private final DifficultyLevel difficulty;
    private final String[] choices;
    private final char correctAnswer;

    public Question(String text, Category category, DifficultyLevel difficulty, String[] choices, char correctAnswer) {
        this.text = text;
        this.category = category;
        this.difficulty = difficulty;
        this.choices = choices;
        this.correctAnswer = Character.toUpperCase(correctAnswer);
    }

    public String getText() {
        return text;
    }

    public Category getCategory() {
        return category;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public String[] getChoices() {
        return choices;
    }

    public char getCorrectAnswer() {
        return correctAnswer;
    }
}
