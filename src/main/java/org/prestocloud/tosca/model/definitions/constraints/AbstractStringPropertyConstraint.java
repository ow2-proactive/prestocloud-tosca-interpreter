package org.prestocloud.tosca.model.definitions.constraints;

import org.prestocloud.tosca.normative.types.IPropertyType;
import prestocloud.tosca.properties.constraints.ConstraintUtil;
import org.prestocloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.prestocloud.tosca.exceptions.ConstraintViolationException;

public abstract class AbstractStringPropertyConstraint extends AbstractPropertyConstraint {
    protected abstract void doValidate(String propertyValue) throws ConstraintViolationException;

    @Override
    public void validate(Object propertyValue) throws ConstraintViolationException {
        if (propertyValue == null) {
            throw new ConstraintViolationException("Value to validate is null");
        }
        if (!(propertyValue instanceof String)) {
            throw new ConstraintViolationException("This constraint can only be applied on String value");
        }
        doValidate((String) propertyValue);
    }

    @Override
    public void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        ConstraintUtil.checkStringType(propertyType);
    }
}
