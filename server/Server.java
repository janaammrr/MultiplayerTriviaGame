package server;

import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Server {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Server started...");

        AuthService authService = new AuthService();
        GameService gameService = new GameService();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected");
            new Thread(new ClientHandler(clientSocket, authService, gameService)).start();
        }
    }
    static List<ClientHandler> waitingPlayers = new ArrayList<>();

public static synchronized void addToWaiting(ClientHandler player) {
    waitingPlayers.add(player);

    if (waitingPlayers.size() >= 2) {
        GameRoom room = new GameRoom(new GameService().getQuestions());

        for (ClientHandler p : waitingPlayers) {
            room.addPlayer(p);
        }

        waitingPlayers.clear();

        new Thread(() -> room.startGame()).start();
    }
}
}
