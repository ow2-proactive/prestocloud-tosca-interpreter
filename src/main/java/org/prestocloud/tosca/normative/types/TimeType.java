package org.prestocloud.tosca.normative.types;

import org.prestocloud.tosca.exceptions.InvalidPropertyValueException;
import org.prestocloud.tosca.normative.primitives.Time;
import org.prestocloud.tosca.normative.primitives.TimeUnit;

public class TimeType extends ScalarType<Time, TimeUnit> {

    public static final String NAME = "scalar-unit.time";

    @Override
    protected Time doParse(Double value, String unitText) throws InvalidPropertyValueException {
        try {
            return new Time(value, TimeUnit.valueOf(unitText.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new InvalidPropertyValueException("Could not parse time scalar unit from value " + unitText, e);
        }
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}
