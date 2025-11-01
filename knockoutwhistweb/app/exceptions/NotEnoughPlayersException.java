package exceptions;

public class NotEnoughPlayersException extends GameException {
    public NotEnoughPlayersException(String message) {
        super(message);
    }
}
