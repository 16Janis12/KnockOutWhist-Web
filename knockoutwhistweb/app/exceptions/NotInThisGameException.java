package exceptions;

public class NotInThisGameException extends GameException {
    public NotInThisGameException(String message) {
        super(message);
    }
}
