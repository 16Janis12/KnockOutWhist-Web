package exceptions;

public class NotHostException extends RuntimeException {
    public NotHostException(String message) {
        super(message);
    }
}
