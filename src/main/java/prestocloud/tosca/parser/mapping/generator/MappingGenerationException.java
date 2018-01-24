package prestocloud.tosca.parser.mapping.generator;

import prestocloud.exceptions.TechnicalException;

/**
 * Exception thrown when an error cause Alien to fail loading and generating the mapping.
 */
public class MappingGenerationException extends TechnicalException {
    public MappingGenerationException(String message) {
        super(message);
    }

    public MappingGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}