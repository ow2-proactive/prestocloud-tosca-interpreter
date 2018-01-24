package org.prestocloud.tosca.normative.types;

import org.prestocloud.tosca.exceptions.InvalidPropertyValueException;

public class BooleanType implements IPropertyType<Boolean> {
    public static final String NAME = "boolean";

    @Override
    public Boolean parse(String text) throws InvalidPropertyValueException {
        return Boolean.parseBoolean(text);
    }

    @Override
    public String print(Boolean value) {
        return String.valueOf(value);
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}
