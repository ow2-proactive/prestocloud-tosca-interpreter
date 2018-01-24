package org.prestocloud.tosca.model.definitions;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import prestocloud.json.deserializer.PropertyConstraintDeserializer;
import prestocloud.json.deserializer.PropertyValueDeserializer;
import prestocloud.model.components.IncompatiblePropertyDefinitionException;
import prestocloud.tosca.container.validation.ToscaPropertyConstraint;
import prestocloud.tosca.container.validation.ToscaPropertyConstraintDuplicate;
import prestocloud.tosca.container.validation.ToscaPropertyDefaultValueConstraints;
import prestocloud.tosca.container.validation.ToscaPropertyDefaultValueType;
import prestocloud.tosca.container.validation.ToscaPropertyPostValidationGroup;
import prestocloud.tosca.container.validation.ToscaPropertyType;

/**
 * A TOSCA property definition.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "type", "required", "description", "defaultValue", "constraints", "entrySchema" })
@ToscaPropertyDefaultValueType
@ToscaPropertyConstraint
@ToscaPropertyDefaultValueConstraints(groups = { ToscaPropertyPostValidationGroup.class })
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class PropertyDefinition implements IValue {
    @ToscaPropertyType
    @NotNull
    private String type;

    private PropertyDefinition entrySchema;

    @NotNull
    private boolean required = true;

    @JsonProperty("default")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private PropertyValue defaultValue;

    private String description;

    private String suggestionId;

    @Valid
    @ToscaPropertyConstraintDuplicate
    @JsonDeserialize(contentUsing = PropertyConstraintDeserializer.class)
    private List<PropertyConstraint> constraints;

    private boolean isPassword;

    public PropertyDefinition(PropertyDefinition from) {
        this.type = from.type;
        this.entrySchema = from.entrySchema;
        this.required = from.required;
        this.defaultValue = from.defaultValue;
        this.description = from.description;
        this.suggestionId = from.suggestionId;
        this.constraints = from.constraints;
        this.isPassword = from.isPassword;
    }

    @JsonDeserialize(using = PropertyValueDeserializer.class)
    public PropertyValue getDefault() {
        return this.defaultValue;
    }

    public void setDefault(PropertyValue defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean isDefinition() {
        return true;
    }

    /**
     * Check if all constraint are equals
     *
     * @param propertyDefinition
     * @throws IncompatiblePropertyDefinitionException
     */
    public void checkIfCompatibleOrFail(final PropertyDefinition propertyDefinition) throws IncompatiblePropertyDefinitionException {
        if (propertyDefinition == null) {
            throw new IncompatiblePropertyDefinitionException();
        } else if (!this.getType().equals(propertyDefinition.getType())) {
            throw new IncompatiblePropertyDefinitionException();
        } else if (this.getConstraints() == null && propertyDefinition.getConstraints() == null) {
            return;
        } else if (this.getConstraints() == null || propertyDefinition.getConstraints() == null
                || this.getConstraints().size() != propertyDefinition.getConstraints().size()) {
            throw new IncompatiblePropertyDefinitionException();
        }

        ArrayList<PropertyConstraint> copyOfOtherConstraints = new ArrayList<PropertyConstraint>(propertyDefinition.getConstraints());
        for (PropertyConstraint constraint : this.getConstraints()) {
            for (int i = 0; i <= copyOfOtherConstraints.size(); i++) {
                if (copyOfOtherConstraints.size() == 0) { // If all elements are compatible
                    return;
                } else if (i == copyOfOtherConstraints.size()) { // If the constraint is not compatible with an constraint from the other PropertyDefinition
                    throw new IncompatiblePropertyDefinitionException();
                } else if (constraint.equals(copyOfOtherConstraints.get(i))) { // If the two constraints are compatible
                    copyOfOtherConstraints.remove(i); // we remove the constraint in the copy of the other propertyDefinition constraints and continue
                    break;
                }
            }
        }
    }
}