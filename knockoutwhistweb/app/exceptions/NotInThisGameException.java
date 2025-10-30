package exceptions;

public class NotInThisGameException extends RuntimeException {
    public NotInThisGameException(String message) {
        super(message);
    }
}
