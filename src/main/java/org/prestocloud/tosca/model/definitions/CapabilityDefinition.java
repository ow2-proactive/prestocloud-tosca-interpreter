package org.prestocloud.tosca.model.definitions;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Specifies the capabilities that the Node Type exposes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "id" })
public class CapabilityDefinition implements UpperBoundedDefinition {
    private String id;
    private String description;
    /** Identifies the type of the capability. */
    private String type;

    /**
     * Specifies the upper boundary of client requirements the defined capability can serve. The default value for this attribute is unbounded. A value of
     * 'unbounded' indicates that there is no upper boundary.
     */
    private int upperBound = Integer.MAX_VALUE;

    /** Map of properties value(s) to define the capability. */
    private Map<String, AbstractPropertyValue> properties;

    private String[] validSources;

    /** Constructor for single line parsing definition based on type. */
    public CapabilityDefinition(String type) {
        this.type = type;
    }

    public CapabilityDefinition(String id, String type, int upperBound) {
        this.id = id;
        this.type = type;
        this.upperBound = upperBound;
    }
}