package org.prestocloud.tosca.model.definitions;

import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;
import lombok.Setter;
import prestocloud.json.deserializer.PropertyConstraintDeserializer;
import prestocloud.tosca.container.validation.ToscaPropertyConstraintDuplicate;

/**
 * Defines the condition to be applied on the policy attribute.
 */
@Getter
@Setter
public class PolicyCondition {
    @Valid
    @ToscaPropertyConstraintDuplicate
    @JsonDeserialize(contentUsing = PropertyConstraintDeserializer.class)
    private List<PropertyConstraint> constraints;
    private String period;
    private int evaluations = 1;
    private String method;
}
