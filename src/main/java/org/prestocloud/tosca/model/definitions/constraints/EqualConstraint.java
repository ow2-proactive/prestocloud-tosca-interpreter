package org.prestocloud.tosca.model.definitions.constraints;

import javax.validation.constraints.NotNull;

import org.prestocloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.prestocloud.tosca.exceptions.ConstraintViolationException;
import org.prestocloud.tosca.normative.types.IPropertyType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import prestocloud.tosca.properties.constraints.ConstraintUtil;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = { "equal" })
public class EqualConstraint extends AbstractPropertyConstraint implements IMatchPropertyConstraint {
    @NotNull
    private String equal;

    private Object typed;

    @Override
    public void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        typed = ConstraintUtil.convert(propertyType, equal);
    }

    @Override
    public void setConstraintValue(IPropertyType<?> toscaType, String textValue) throws ConstraintValueDoNotMatchPropertyTypeException {
        equal = textValue;
        typed = ConstraintUtil.convert(toscaType, textValue);
    }

    @Override
    public void validate(Object propertyValue) throws ConstraintViolationException {
        if (propertyValue == null) {
            if (typed != null) {
                fail(propertyValue);
            }
        } else if (typed == null) {
            fail(propertyValue);
        } else if (!typed.equals(propertyValue)) {
            fail(propertyValue);
        }
    }

    private void fail(Object propertyValue) throws ConstraintViolationException {
        throw new ConstraintViolationException(
                "Equal constraint violation, the reference is <" + equal + "> but the value to compare is <" + propertyValue + ">");
    }
}