package org.prestocloud.tosca.model.definitions;

import org.prestocloud.tosca.model.types.CapabilityType;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Specifies the requirements that the Node Type exposes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "id" })
public class RequirementDefinition implements LowerBoundedDefinition, UpperBoundedDefinition {
    private String id;
    /**
     * <p>
     * Identifies the type of the requirement.
     * </p>
     * <p>
     * This must be a qualified name: Either namespace:type, either type only if the {@link CapabilityType} is defined in the same namespace as the
     * {@link RequirementDefinition definition}.
     * </p>
     */
    private String type;
    /** Restriction to the node type that can fullfill the requirement. */
    private String nodeType;

    private String description;
    /** Specifies the default relationship type to be used for the relationship. This can be overriden by user but should be used as default. */
    private String relationshipType;
    /** Can specify the optional target capability name on which to bind the relationship. */
    private String capabilityName;
    /**
     * Specifies the lower boundary by which a requirement MUST be matched for Node Templates according to the current Node Type, or for instances created for
     * those Node Templates. The default value for this attribute is one. A value of zero would indicate that matching of the requirement is optional.
     */
    private int lowerBound = 1;
    /**
     * Specifies the upper boundary by which a requirement MUST be matched for Node Templates according to the current Node Type, or for instances created for
     * those Node Templates. The default value for this attribute is one. A value of "unbounded" indicates that there is no upper boundary.
     */
    private int upperBound = 1;

    /** Constraints to specify on the target capability or node's properties. */
    private NodeFilter nodeFilter;

    /** Constructor for single line parsing definition based on type. */
    public RequirementDefinition(String type) {
        this.type = type;
    }

    /**
     * Quick constructor to create a requirement definition from id and type.
     * 
     * @param id The requirement id.
     * @param type The requirement type.
     */
    public RequirementDefinition(String id, String type) {
        this.id = id;
        this.type = type;
    }
}