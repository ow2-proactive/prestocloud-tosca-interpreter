package org.prestocloud.tosca.normative.types;

import org.prestocloud.tosca.exceptions.InvalidPropertyValueException;
import org.prestocloud.tosca.normative.primitives.Frequency;
import org.prestocloud.tosca.normative.primitives.FrequencyUnit;

public class FrequencyType extends ScalarType<Frequency, FrequencyUnit> {

    public static final String NAME = "scalar-unit.frequency";

    @Override
    protected Frequency doParse(Double value, String unitText) throws InvalidPropertyValueException {
        try {
            return new Frequency(value, FrequencyUnit.valueOf(unitText.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new InvalidPropertyValueException("Could not parse size scalar unit from value " + unitText, e);
        }
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}
