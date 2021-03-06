package org.prestocloud.tosca.model.definitions.constraints;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.prestocloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.prestocloud.tosca.exceptions.ConstraintViolationException;
import org.prestocloud.tosca.normative.types.IPropertyType;

import com.google.common.collect.Lists;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import prestocloud.tosca.properties.constraints.ConstraintUtil;

@SuppressWarnings({ "unchecked", "rawtypes" })
@EqualsAndHashCode(callSuper = false, of = { "inRange" })
public class InRangeConstraint extends AbstractPropertyConstraint {

    @Getter
    @Setter
    private List<String> inRange;

    private Comparable min;
    private Comparable max;

    @Override
    public void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        // Perform verification that the property type is supported for comparison
        ConstraintUtil.checkComparableType(propertyType);
        if (inRange == null || inRange.size() != 2) {
            throw new ConstraintValueDoNotMatchPropertyTypeException("In range constraint must have two elements.");
        }
        String minRawText = inRange.get(0);
        String maxRawText = inRange.get(1);
        min = ConstraintUtil.convertToComparable(propertyType, minRawText);
        max = ConstraintUtil.convertToComparable(propertyType, maxRawText);
    }

    @Override
    public void validate(Object propertyValue) throws ConstraintViolationException {
        if (propertyValue == null) {
            throw new ConstraintViolationException("Value to check is null");
        }
        if (!(min.getClass().isAssignableFrom(propertyValue.getClass()))) {
            throw new ConstraintViolationException(
                    "Value to check is not comparable to range type, value type [" + propertyValue.getClass() + "], range type [" + min.getClass() + "]");
        }
        if (min.compareTo(propertyValue) > 0 || max.compareTo(propertyValue) < 0) {
            throw new ConstraintViolationException("The value [" + propertyValue + "] is out of range " + inRange);
        }
    }

    @NotNull
    public String getRangeMinValue() {
        if (inRange != null) {
            return inRange.get(0);
        } else {
            return null;
        }
    }

    public void setRangeMinValue(String minValue) {
        if (inRange == null) {
            inRange = Lists.newArrayList(minValue, "");
        } else {
            inRange.set(0, minValue);
        }
    }

    @NotNull
    public String getRangeMaxValue() {
        if (inRange != null) {
            return inRange.get(1);
        } else {
            return null;
        }
    }

    public void setRangeMaxValue(String maxValue) {
        if (inRange == null) {
            inRange = Lists.newArrayList("", maxValue);
        } else {
            inRange.set(1, maxValue);
        }
    }
}
