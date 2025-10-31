package exceptions;

public class CantPlayCardException extends GameException {
    public CantPlayCardException(String message) {
        super(message);
    }
}
