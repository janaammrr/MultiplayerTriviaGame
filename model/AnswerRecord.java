package model;

public class AnswerRecord {
    private final String questionText;
    private final String submittedAnswer;
    private final char correctAnswer;
    private final boolean correct;

    public AnswerRecord(String questionText, String submittedAnswer, char correctAnswer, boolean correct) {
        this.questionText = questionText;
        this.submittedAnswer = submittedAnswer;
        this.correctAnswer = correctAnswer;
        this.correct = correct;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getSubmittedAnswer() {
        return submittedAnswer;
    }

    public char getCorrectAnswer() {
        return correctAnswer;
    }

    public boolean isCorrect() {
        return correct;
    }
}
