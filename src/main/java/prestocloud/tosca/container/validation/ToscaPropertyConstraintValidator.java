package prestocloud.tosca.container.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import lombok.extern.slf4j.Slf4j;
import org.prestocloud.tosca.model.definitions.PropertyConstraint;
import org.prestocloud.tosca.model.definitions.PropertyDefinition;
import org.prestocloud.tosca.normative.types.IPropertyType;
import org.prestocloud.tosca.normative.types.ToscaTypes;
import org.prestocloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;

@Slf4j
public class ToscaPropertyConstraintValidator implements ConstraintValidator<ToscaPropertyConstraint, PropertyDefinition> {

    @Override
    public void initialize(ToscaPropertyConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(PropertyDefinition value, ConstraintValidatorContext context) {
        if (value.getConstraints() == null) {
            return true;
        }
        IPropertyType<?> toscaType = ToscaTypes.fromYamlTypeName(value.getType());
        if (toscaType == null) {
            return false;
        }
        boolean isValid = true;
        for (int i = 0; i < value.getConstraints().size(); i++) {
            PropertyConstraint constraint = value.getConstraints().get(i);
            try {
                constraint.initialize(toscaType);
            } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                log.info("Constraint definition error", e);
                context.buildConstraintViolationWithTemplate("CONSTRAINTS.VALIDATION.TYPE").addPropertyNode("constraints").addBeanNode().inIterable()
                        .atIndex(i).addConstraintViolation();
                isValid = false;
            }
        }
        return isValid;
    }
}