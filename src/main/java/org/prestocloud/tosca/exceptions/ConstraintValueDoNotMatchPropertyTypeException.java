package org.prestocloud.tosca.exceptions;

import prestocloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;

/**
 * Exception to be thrown when a constraint definition is invalid because the specified value doesn't match the property type.
 * 
 * @author luc boutier
 */
public class ConstraintValueDoNotMatchPropertyTypeException extends ConstraintFunctionalException {

    private static final long serialVersionUID = 4342613849660957651L;

    public ConstraintValueDoNotMatchPropertyTypeException(String message) {
        super(message);
    }

    public ConstraintValueDoNotMatchPropertyTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstraintValueDoNotMatchPropertyTypeException(String message, Throwable cause, ConstraintInformation constraintInformation) {
        super(message, cause);
        this.constraintInformation = constraintInformation;
    }
}