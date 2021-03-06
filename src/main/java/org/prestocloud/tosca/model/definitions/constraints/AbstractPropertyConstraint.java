package org.prestocloud.tosca.model.definitions.constraints;

import org.prestocloud.tosca.model.definitions.PropertyConstraint;
import org.prestocloud.tosca.normative.types.IPropertyType;
import org.prestocloud.tosca.exceptions.InvalidPropertyValueException;
import org.prestocloud.tosca.exceptions.ConstraintViolationException;

public abstract class AbstractPropertyConstraint implements PropertyConstraint {

    @Override
    public void validate(IPropertyType<?> toscaType, String propertyTextValue) throws ConstraintViolationException {
        try {
            validate(toscaType.parse(propertyTextValue));
        } catch (InvalidPropertyValueException e) {
            throw new ConstraintViolationException("String value [" + propertyTextValue + "] is not valid for type [" + toscaType.getTypeName() + "]", e);
        }
    }
}