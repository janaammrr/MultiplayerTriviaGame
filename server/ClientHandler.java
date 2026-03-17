package server;

import exceptions.InvalidInputException;
import model.GameConfig;
import model.Team;
import model.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Set;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final AuthService authService;
    private final GameService gameService;
    private final GameConfig config;

    private BufferedReader in;
    private PrintWriter out;
    private User user;
    private String teamName;
    private String lastAnswer;
    private GameRoom currentRoom;
    private boolean disconnected;
    private boolean asyncListenerStarted;

    public ClientHandler(Socket socket, AuthService authService, GameService gameService, GameConfig config) {
        this.socket = socket;
        this.authService = authService;
        this.gameService = gameService;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            if (!handleAuthentication()) {
                closeConnection();
                return;
            }

            Server.log("Client authenticated: " + user.getName() + " (" + user.getUsername() + ")");
            out.println("Welcome " + user.getName());
            gameService.printScoreHistory(user, out);

            while (!disconnected) {
                out.println("\n===== MENU =====");
                out.println("1. Single Player");
                out.println("2. Create Team");
                out.println("3. Join Team");
                out.println("4. List Teams");
                out.println("5. View Score History");
                out.println("6. Quit");
                out.println("Press '-' or a blank/space input to quit.");

                String option = in.readLine();
                if (option == null || isQuit(option)) {
                    out.println("Goodbye!");
                    break;
                }

                if ("1".equals(option.trim())) {
                    boolean completed = gameService.startSingleGame(user, in, out);
                    if (!completed) {
                        break;
                    }
                } else if ("2".equals(option.trim())) {
                    if (handleCreateTeam()) {
                        startAsyncInputListener();
                        waitForMultiplayerToEnd();
                        return;
                    }
                } else if ("3".equals(option.trim())) {
                    if (handleJoinTeam()) {
                        startAsyncInputListener();
                        waitForMultiplayerToEnd();
                        return;
                    }
                } else if ("4".equals(option.trim())) {
                    printTeams();
                } else if ("5".equals(option.trim())) {
                    gameService.printScoreHistory(user, out);
                } else if ("6".equals(option.trim())) {
                    out.println("Goodbye!");
                    break;
                } else {
                    out.println("Invalid menu option.");
                }
            }
        } catch (IOException e) {
            handleDisconnect("I/O error: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private boolean handleAuthentication() throws IOException {
        out.println("1. Login");
        out.println("2. Register");
        out.println("Press '-' or a blank/space input anytime to quit.");

        String choice = in.readLine();
        if (choice == null || isQuit(choice)) {
            return false;
        }

        if ("1".equals(choice.trim())) {
            out.println("Username:");
            String username = in.readLine();
            if (username == null || isQuit(username)) {
                return false;
            }

            out.println("Password:");
            String password = in.readLine();
            if (password == null || isQuit(password)) {
                return false;
            }

            AuthService.LoginResponse response = authService.login(username.trim(), password.trim());
            if (response.getStatusCode() == 404) {
                out.println("404 User Not Found");
                return false;
            }
            if (response.getStatusCode() == 401) {
                out.println("401 Unauthorized");
                return false;
            }
            user = response.getUser();
            return true;
        }

        if ("2".equals(choice.trim())) {
            out.println("Name:");
            String name = in.readLine();
            if (name == null || isQuit(name)) {
                return false;
            }

            out.println("Username:");
            String username = in.readLine();
            if (username == null || isQuit(username)) {
                return false;
            }

            if (authService.usernameExists(username.trim())) {
                out.println("CUSTOM ERROR: Username already exists. Change username.");
                return false;
            }

            out.println("Password:");
            String password = in.readLine();
            if (password == null || isQuit(password)) {
                return false;
            }

            out.println(authService.register(name.trim(), username.trim(), password.trim()));
            return false;
        }

        out.println("Invalid option.");
        return false;
    }

    private boolean handleCreateTeam() throws IOException {
        out.println("Enter Team Name:");
        String requestedTeamName = in.readLine();
        if (requestedTeamName == null || isQuit(requestedTeamName)) {
            out.println("Cancelled.");
            return false;
        }

        out.println("Available categories: " + gameService.getAvailableCategories());
        String category = in.readLine();
        if (category == null || isQuit(category)) {
            out.println("Cancelled.");
            return false;
        }
        if (!containsIgnoreCase(gameService.getAvailableCategories(), category)) {
            out.println("Invalid category.");
            return false;
        }

        out.println("Available difficulties: " + gameService.getAvailableDifficulties());
        String difficulty = in.readLine();
        if (difficulty == null || isQuit(difficulty)) {
            out.println("Cancelled.");
            return false;
        }
        if (!containsIgnoreCase(gameService.getAvailableDifficulties(), difficulty)) {
            out.println("Invalid difficulty.");
            return false;
        }

        out.println("Number of questions (1-" + gameService.getMaxQuestionCount() + "):");
        String questionCountInput = in.readLine();
        out.println("Players per team (" + config.getMinTeamPlayers() + "-" + config.getMaxTeamPlayers() + "):");
        String playerCountInput = in.readLine();
        if (questionCountInput == null || playerCountInput == null
                || isQuit(questionCountInput) || isQuit(playerCountInput)) {
            out.println("Cancelled.");
            return false;
        }

        try {
            int questionCount = parsePositiveInt(questionCountInput, "Invalid question count.");
            int playersPerTeam = parsePositiveInt(playerCountInput, "Invalid number of players.");

            if (questionCount < 1 || questionCount > gameService.getMaxQuestionCount()) {
                out.println("Invalid question count.");
                return false;
            }

            if (gameService.getQuestionsForGame(category.trim(), difficulty.trim(), questionCount).size() < questionCount) {
                out.println("Not enough questions available for that category/difficulty selection.");
                return false;
            }

            String result = TeamManager.createTeam(requestedTeamName.trim(), this, category.trim(),
                    difficulty.trim(), questionCount, playersPerTeam, config);
            out.println(result);
            if (!"Team created successfully".equals(result)) {
                return false;
            }

            teamName = requestedTeamName.trim();
            out.println("Team created. Waiting for players and another team with the same category, difficulty, and team size.");
            tryStartMatch();
            return true;
        } catch (InvalidInputException e) {
            out.println(e.getMessage());
            return false;
        }
    }

    private boolean handleJoinTeam() throws IOException {
        out.println("Enter Team Name to Join:");
        String requestedTeamName = in.readLine();
        if (requestedTeamName == null || isQuit(requestedTeamName)) {
            out.println("Cancelled.");
            return false;
        }

        String result = TeamManager.joinTeam(requestedTeamName.trim(), this);
        out.println(result);
        if (!"Joined Team Successfully!".equals(result)) {
            return false;
        }

        teamName = requestedTeamName.trim();
        tryStartMatch();
        return true;
    }

    private void printTeams() {
        List<Team> teams = TeamManager.listOpenTeams();
        if (teams.isEmpty()) {
            out.println("No open teams.");
            return;
        }

        out.println("Open teams:");
        for (Team team : teams) {
            out.println(team.getTeamName() + " | members " + team.size() + "/" + team.getMaxPlayers()
                    + " | category=" + team.getCategory()
                    + " | difficulty=" + team.getDifficulty()
                    + " | questions=" + team.getQuestionCount());
        }
    }

    private void startAsyncInputListener() {
        if (asyncListenerStarted) {
            return;
        }
        asyncListenerStarted = true;

        new Thread(() -> {
            try {
                String input;
                while (!socket.isClosed() && (input = in.readLine()) != null) {
                    if (isQuit(input)) {
                        sendMessage("You left the multiplayer session.");
                        handleDisconnect("Player chose to quit.");
                        closeConnection();
                        break;
                    }

                    if (currentRoom == null) {
                        sendMessage("No active question.");
                        continue;
                    }
                    if (!currentRoom.isQuestionActive()) {
                        sendMessage("Answer ignored. No active question.");
                        continue;
                    }
                    setAnswer(input);
                }
            } catch (IOException e) {
                handleDisconnect("I/O error in multiplayer listener: " + e.getMessage());
            }
        }).start();
    }

    private void waitForMultiplayerToEnd() {
        try {
            while (!disconnected && currentRoom == null) {
                Thread.sleep(500);
            }
            while (!disconnected && currentRoom != null) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void tryStartMatch() {
        GameRoom room = TeamManager.tryCreateMatch(gameService, config);
        if (room != null) {
            new Thread(room::startGame).start();
        }
    }

    public synchronized void setAnswer(String answer) {
        if (lastAnswer == null) {
            lastAnswer = answer;
        }
    }

    public synchronized String pollAnswer() {
        String result = lastAnswer;
        lastAnswer = null;
        return result;
    }

    public synchronized void prepareForNextQuestion() {
        lastAnswer = null;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public User getUser() {
        return user;
    }

    public String getTeamName() {
        return teamName;
    }

    public synchronized void setCurrentRoom(GameRoom room) {
        this.currentRoom = room;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public void closeConnection() {
        boolean wasDisconnected = disconnected;
        disconnected = true;
        String displayName = user == null ? socket.getRemoteSocketAddress().toString() : user.getName();
        if (!wasDisconnected) {
            Server.log("Client disconnected: " + displayName);
        }
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void handleDisconnect(String reason) {
        disconnected = true;
        if (teamName != null) {
            TeamManager.handleDisconnect(teamName, this);
        }
        if (currentRoom != null) {
            currentRoom.handleDisconnect(this);
        }
        if (user != null) {
            Server.log("Client disconnected: " + user.getName() + " | " + reason);
        }
    }

    private int parsePositiveInt(String value, String errorMessage) throws InvalidInputException {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidInputException(errorMessage);
        }
    }

    private boolean containsIgnoreCase(Set<String> values, String target) {
        for (String value : values) {
            if (value.equalsIgnoreCase(target.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isQuit(String value) {
        return gameService.isQuit(value);
    }
}
