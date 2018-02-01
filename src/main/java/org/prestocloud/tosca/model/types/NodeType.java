package org.prestocloud.tosca.model.types;

import java.util.List;
import java.util.Map;

import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;
import org.prestocloud.tosca.model.definitions.CapabilityDefinition;
import org.prestocloud.tosca.model.definitions.RequirementDefinition;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
public class NodeType extends AbstractInstantiableToscaType {
    private List<CapabilityDefinition> capabilities;

    private List<RequirementDefinition> requirements;

    private List<String> defaultCapabilities;

    /** When the type is created from a topology template (substitution), contains the topology id. */
    private String substitutionTopologyId;

    /** Portability information. */
    private Map<String, AbstractPropertyValue> portability;
}