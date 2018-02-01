package org.prestocloud.tosca.model.definitions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import prestocloud.tosca.container.validation.ToscaPropertyConstraint;
import prestocloud.tosca.container.validation.ToscaPropertyDefaultValueConstraints;
import prestocloud.tosca.container.validation.ToscaPropertyDefaultValueType;
import prestocloud.tosca.container.validation.ToscaPropertyPostValidationGroup;
import prestocloud.tosca.container.validation.ToscaPropertyType;

@Getter
@Setter
@NoArgsConstructor
@ToscaPropertyDefaultValueType
@ToscaPropertyConstraint
@ToscaPropertyDefaultValueConstraints(groups = { ToscaPropertyPostValidationGroup.class })
public class AttributeDefinition implements IValue {
    @ToscaPropertyType
    private String type;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String defaultValue;
    private String description;

    public String getDefault() {
        return this.defaultValue;
    }

    public void setDefault(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean isDefinition() {
        return true;
    }
}