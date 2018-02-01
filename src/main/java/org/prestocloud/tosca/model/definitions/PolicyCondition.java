package org.prestocloud.tosca.model.definitions;

import java.util.List;

import javax.validation.Valid;

import lombok.Getter;
import lombok.Setter;
import prestocloud.tosca.container.validation.ToscaPropertyConstraintDuplicate;

/**
 * Defines the condition to be applied on the policy attribute.
 */
@Getter
@Setter
public class PolicyCondition {
    @Valid
    @ToscaPropertyConstraintDuplicate
    private List<PropertyConstraint> constraints;
    private String period;
    private int evaluations = 1;
    private String method;
}
