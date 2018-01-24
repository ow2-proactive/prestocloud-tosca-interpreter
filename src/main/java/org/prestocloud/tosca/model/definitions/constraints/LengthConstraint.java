package org.prestocloud.tosca.model.definitions.constraints;

import javax.validation.constraints.NotNull;

import org.prestocloud.tosca.exceptions.ConstraintViolationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = { "length" })
public class LengthConstraint extends AbstractLengthConstraint {
    @NotNull
    private Integer length;

    @Override
    protected void doValidate(int propertyValue) throws ConstraintViolationException {
        if (propertyValue != length) {
            throw new ConstraintViolationException("The length of the value is not equals to [" + length + "]");
        }
    }
}