package org.prestocloud.tosca.model.types;

import java.util.Map;

import org.prestocloud.tosca.model.definitions.IValue;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;
import lombok.Setter;
import prestocloud.json.deserializer.AttributeDeserializer;

@Getter
@Setter
public class CapabilityType extends AbstractInheritableToscaType {
    @JsonDeserialize(contentUsing = AttributeDeserializer.class)
    private Map<String, IValue> attributes;
    /**
     * An optional list of one or more valid names of Node Types that are supported as valid sources of any relationship established to the declared Capability
     * Type.
     */
    private String[] validSources;
}