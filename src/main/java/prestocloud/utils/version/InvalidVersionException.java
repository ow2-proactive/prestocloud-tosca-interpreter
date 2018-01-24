package prestocloud.utils.version;

import prestocloud.exceptions.TechnicalException;

public class InvalidVersionException extends TechnicalException {

    private static final long serialVersionUID = -5192834855057177252L;

    public InvalidVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidVersionException(String message) {
        super(message);
    }
}
