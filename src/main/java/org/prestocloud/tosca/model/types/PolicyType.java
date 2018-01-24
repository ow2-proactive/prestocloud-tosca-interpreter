package org.prestocloud.tosca.model.types;

import java.util.Map;
import java.util.Set;

import org.prestocloud.tosca.model.definitions.PolicyTrigger;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a TOSCA policy type.
 */
@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
public class PolicyType extends AbstractInheritableToscaType {
    private Set<String> targets;
    private Map<String, PolicyTrigger> triggers;
}