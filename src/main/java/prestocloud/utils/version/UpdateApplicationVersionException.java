package prestocloud.utils.version;

import prestocloud.exceptions.TechnicalException;

public class UpdateApplicationVersionException extends TechnicalException {

    private static final long serialVersionUID = -5192834855057177252L;

    public UpdateApplicationVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public UpdateApplicationVersionException(String message) {
        super(message);
    }
}
