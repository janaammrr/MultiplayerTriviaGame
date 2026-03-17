package server;

import model.GameConfig;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(5000);
        log("Server started on port 5000");

        ConfigService configService = new ConfigService();
        ScoreHistoryService scoreHistoryService = new ScoreHistoryService();
        GameConfig config = configService.loadConfig();
        AuthService authService = new AuthService();
        GameService gameService = new GameService(config, scoreHistoryService);

        log("Loaded config successfully.");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            log("Client connected: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
            new Thread(new ClientHandler(clientSocket, authService, gameService, config)).start();
        }
    }

    public static synchronized void log(String message) {
        System.out.println(message);
    }
}
