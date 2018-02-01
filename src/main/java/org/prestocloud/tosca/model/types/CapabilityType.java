package org.prestocloud.tosca.model.types;

import java.util.Map;

import org.prestocloud.tosca.model.definitions.IValue;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CapabilityType extends AbstractInheritableToscaType {
    private Map<String, IValue> attributes;
    /**
     * An optional list of one or more valid names of Node Types that are supported as valid sources of any relationship established to the declared Capability
     * Type.
     */
    private String[] validSources;
}