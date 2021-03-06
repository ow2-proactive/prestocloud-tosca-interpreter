package prestocloud.tosca.container.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.prestocloud.tosca.model.definitions.PropertyDefinition;
import org.prestocloud.tosca.model.definitions.PropertyValue;
import org.prestocloud.tosca.model.definitions.ScalarPropertyValue;
import org.prestocloud.tosca.normative.types.IPropertyType;
import org.prestocloud.tosca.exceptions.InvalidPropertyValueException;
import org.prestocloud.tosca.normative.types.ToscaTypes;

public class ToscaPropertyDefaultValueTypeValidator implements ConstraintValidator<ToscaPropertyDefaultValueType, PropertyDefinition> {

    @Override
    public void initialize(ToscaPropertyDefaultValueType constraintAnnotation) {
    }

    @Override
    public boolean isValid(PropertyDefinition value, ConstraintValidatorContext context) {
        PropertyValue defaultValue = value.getDefault();
        if (defaultValue == null) {
            // no default value is specified.
            return true;
        }
        if (!(defaultValue instanceof ScalarPropertyValue)) {
            // No constraint can be made on other thing than scalar values
            return false;
        }
        IPropertyType<?> toscaType = ToscaTypes.fromYamlTypeName(value.getType());

        if (toscaType == null) {
            return false;
        }
        try {
            toscaType.parse(((ScalarPropertyValue) defaultValue).getValue());
        } catch (InvalidPropertyValueException e) {
            return false;
        }
        return true;
    }
}