package prestocloud.tosca.parser;

import prestocloud.exceptions.TechnicalException;

/**
 * Exception thrown in case of unexpected errors while parsing tosca definitions.
 */
public class ParsingTechnicalException extends TechnicalException {
    private static final long serialVersionUID = 1L;

    /**
     * Create a technical exceptions with a single explanation message.
     * 
     * @param message The message that explain the context and cause of the exceptions.
     */
    public ParsingTechnicalException(String message) {
        super(message);
    }

    /**
     * Create a technical exceptions with an explanation message and the root cause of the error.
     * 
     * @param message The message that explain the context and cause of the exceptions.
     * @param cause The root cause.
     */
    public ParsingTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
