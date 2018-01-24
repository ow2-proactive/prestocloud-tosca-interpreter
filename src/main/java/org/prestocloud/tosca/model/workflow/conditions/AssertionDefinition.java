package org.prestocloud.tosca.model.workflow.conditions;

import java.util.List;

import org.prestocloud.tosca.model.definitions.PropertyConstraint;

import lombok.Getter;
import lombok.Setter;

/**
 * Assertion condition clause.
 */
@Getter
@Setter
public class AssertionDefinition {
    private String attribute;
    private List<PropertyConstraint> constraints;
}