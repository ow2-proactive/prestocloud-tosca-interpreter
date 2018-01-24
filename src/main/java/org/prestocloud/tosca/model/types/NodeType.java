package org.prestocloud.tosca.model.types;

import java.util.List;
import java.util.Map;

import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;
import org.prestocloud.tosca.model.definitions.CapabilityDefinition;
import org.prestocloud.tosca.model.definitions.RequirementDefinition;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import prestocloud.json.deserializer.PropertyValueDeserializer;
import prestocloud.utils.jackson.ConditionalAttributes;
import prestocloud.utils.jackson.ConditionalOnAttribute;

@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
public class NodeType extends AbstractInstantiableToscaType {
    private List<CapabilityDefinition> capabilities;

    private List<RequirementDefinition> requirements;

    private List<String> defaultCapabilities;

    private long alienScore;

    /** When the type is created from a topology template (substitution), contains the topology id. */
    private String substitutionTopologyId;

    /** Portability information. */
    @ConditionalOnAttribute({ ConditionalAttributes.ES, ConditionalAttributes.REST })
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    private Map<String, AbstractPropertyValue> portability;
}