package org.prestocloud.tosca.normative.types;

import org.prestocloud.tosca.exceptions.InvalidPropertyValueException;

public class FloatType implements IComparablePropertyType<Double> {

    public static final String NAME = "float";

    @Override
    public Double parse(String text) throws InvalidPropertyValueException {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new InvalidPropertyValueException("Cannot parse float value " + text, e);
        }
    }

    @Override
    public String print(Double value) {
        return String.valueOf(value);
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}
