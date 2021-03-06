package org.prestocloud.tosca.exceptions;

import prestocloud.exceptions.FunctionalException;

/**
 * This exceptions is thrown when text cannot be deserialized into property value
 * 
 * @author Minh Khang VU
 */
public class InvalidPropertyValueException extends FunctionalException {

    public InvalidPropertyValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPropertyValueException(String message) {
        super(message);
    }
}
