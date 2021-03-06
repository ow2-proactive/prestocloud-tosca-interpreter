package org.prestocloud.tosca.exceptions;

import prestocloud.exceptions.FunctionalException;
import prestocloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import lombok.Getter;

/**
 * All functional error related to constraint processing must go here
 * 
 * @author mkv
 * 
 */
public class ConstraintFunctionalException extends FunctionalException {

    private static final long serialVersionUID = 1L;

    @Getter
    protected ConstraintInformation constraintInformation;

    public ConstraintFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstraintFunctionalException(String message) {
        super(message);
    }

    public ConstraintFunctionalException(String message, Throwable cause, ConstraintInformation constraintInformation) {
        super(message, cause);
        this.constraintInformation = constraintInformation;
    }

}
