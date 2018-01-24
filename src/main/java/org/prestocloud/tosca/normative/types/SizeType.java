package org.prestocloud.tosca.normative.types;

import org.prestocloud.tosca.exceptions.InvalidPropertyValueException;
import org.prestocloud.tosca.normative.primitives.Size;
import org.prestocloud.tosca.normative.primitives.SizeUnit;

public class SizeType extends ScalarType<Size, SizeUnit> {

    public static final String NAME = "scalar-unit.size";

    @Override
    protected Size doParse(Double value, String unitText) throws InvalidPropertyValueException {
        try {
            return new Size(value, SizeUnit.valueOf(unitText.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new InvalidPropertyValueException("Could not parse size scalar unit from value " + unitText, e);
        }
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}