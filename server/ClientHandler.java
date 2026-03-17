package server;

import model.User;
import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {

    private Socket socket;
    private AuthService authService;
    private GameService gameService;

    private BufferedReader in;
    private PrintWriter out;

    private User user;
    private String lastAnswer = null;

    private String teamName = null;

    public ClientHandler(Socket socket, AuthService authService, GameService gameService) {
        this.socket = socket;
        this.authService = authService;
        this.gameService = gameService;
    }

    @Override
    public void run() {
        try {

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // ================= LOGIN / REGISTER =================

            out.println("1. Login");
            out.println("2. Register");

            String choice = in.readLine();

            if (choice.equals("1")) {

                out.println("Username:");
                String u = in.readLine();

                out.println("Password:");
                String p = in.readLine();

                user = authService.login(u, p);

                if (user == null) {
                    out.println("404 User Not Found");
                    return;
                }

                if (user.getName().equals("ERROR401")) {
                    out.println("401 Unauthorized");
                    return;
                }

            } else {

                out.println("Name:");
                String name = in.readLine();

                out.println("Username:");
                String u = in.readLine();

                out.println("Password:");
                String p = in.readLine();

                String res = authService.register(name, u, p);
                out.println(res);
                return;
            }

            // ================= MAIN MENU =================

            out.println("Welcome " + user.getName());

            while (true) {

                out.println("\n===== MENU =====");
                out.println("1. Single Player");
                out.println("2. Create Team");
                out.println("3. Join Team");
                out.println("4. Quit");

                String option = in.readLine();

                // -------- SINGLE PLAYER --------
                if (option.equals("1")) {
                    gameService.startSingleGame(user, in, out);
                }

                // -------- CREATE TEAM --------
                else if (option.equals("2")) {

                    out.println("Enter Team Name:");
                    teamName = in.readLine();

                    String result = TeamManager.createTeam(teamName);

                    if (!result.equals("Team created successfully")) {
                        out.println(result);
                    } else {
                        TeamManager.getTeam(teamName).addMember(this);
                        out.println("Team Created. Waiting for teammate...");
                        Server.addToWaiting(this);
                        break;
                    }
                }

                // -------- JOIN TEAM --------
                else if (option.equals("3")) {

                    out.println("Enter Team Name to Join:");
                    teamName = in.readLine();

                    if (!TeamManager.teamExists(teamName)) {
                        out.println("Team Not Found");
                        continue;
                    }

                    TeamManager.getTeam(teamName).addMember(this);

                    out.println("Joined Team Successfully!");
                    Server.addToWaiting(this);
                    break;
                }

                // -------- QUIT --------
                else if (option.equals("4")) {
                    out.println("Goodbye!");
                    socket.close();
                    return;
                }

            }

            // ================= ANSWER LISTENER THREAD =================

            new Thread(() -> {
                try {
                    String input;
                    while ((input = in.readLine()) != null) {
                        setAnswer(input);
                    }
                } catch (IOException e) {
                    System.out.println("Client disconnected.");
                }
            }).start();

        } catch (Exception e) {
            System.out.println("Client disconnected.");
        }
    }

    // ================= MULTIPLAYER HELPERS =================

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public User getUser() {
        return user;
    }

    public String getTeamName() {
        return teamName;
    }

    public synchronized void setAnswer(String ans) {
        if (lastAnswer == null) {
            lastAnswer = ans;
        }
    }

    public synchronized String pollAnswer() {
        String temp = lastAnswer;
        lastAnswer = null;
        return temp;
    }
}