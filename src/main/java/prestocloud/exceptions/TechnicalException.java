package prestocloud.exceptions;

/**
 * Base class for all Alien technical exceptions
 *
 * @author mkv
 *
 */
public abstract class TechnicalException extends RuntimeException {

    private static final long serialVersionUID = -9152473183025390161L;

    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    public TechnicalException(String message) {
        super(message);
    }
}
