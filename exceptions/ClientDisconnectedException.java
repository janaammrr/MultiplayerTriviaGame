package exceptions;

public class ClientDisconnectedException extends GameException {
    public ClientDisconnectedException(String message) {
        super(message);
    }
}
