package org.prestocloud.tosca.normative.primitives;

/**
 * @author Minh Khang VU
 */
public class Size extends ScalarUnit<SizeUnit> {

    public Size(double value, SizeUnit unit) {
        super(value, unit);
    }

    @Override
    protected SizeUnit getUnit(String unit) {
        return SizeUnit.valueOf(unit.toUpperCase());
    }
}
