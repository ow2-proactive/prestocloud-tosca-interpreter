package org.prestocloud.tosca.model.definitions;

/**
 * Abstract class for a value that doesn't have a property definition (such as scalar value or a function value).
 */
public abstract class AbstractPropertyValue implements IValue {

    @Override
    public boolean isDefinition() {
        return false;
    }
}