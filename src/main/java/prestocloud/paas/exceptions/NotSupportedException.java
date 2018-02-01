package prestocloud.paas.exceptions;

import prestocloud.exceptions.TechnicalException;

/**
 * TechnicalException exceptions to be thrown when an operation or something is not yet supported.
 */
public class NotSupportedException extends TechnicalException {
    private static final long serialVersionUID = 1L;

    public NotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSupportedException(String message) {
        super(message);
    }
}