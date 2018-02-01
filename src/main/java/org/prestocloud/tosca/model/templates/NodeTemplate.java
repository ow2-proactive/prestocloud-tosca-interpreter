package org.prestocloud.tosca.model.templates;

import java.util.Map;
import java.util.Set;

import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;
import org.prestocloud.tosca.model.definitions.DeploymentArtifact;
import org.prestocloud.tosca.model.definitions.IValue;
import org.prestocloud.tosca.model.definitions.Interface;
import org.prestocloud.tosca.model.definitions.NodeFilter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Specifies a kind of a component making up the cloud application.
 *
 * @author luc boutier
 */
@Getter
@Setter
@NoArgsConstructor
public class NodeTemplate extends AbstractInstantiableTemplate {
    /**
     * The requirement that this node template defines
     */
    private Map<String, Requirement> requirements;

    /**
     * Relationships between node templates
     */
    private Map<String, RelationshipTemplate> relationships;

    /**
     * The capabilities that this node template defines
     */
    private Map<String, Capability> capabilities;

    /** This flag allows to know if the node represents a dangling requirement. */
    private boolean isDanglingRequirement = false;

    /**
     * Optional node filter for abstract nodes that replaces the usage of properties and capabilities (moved to equals constraints in the node filter).
     * Node filter is also used for dangling requirements nodes.
     */
    private NodeFilter nodeFilter;

    /**
     * The {@link NodeGroup}s this template is member of.
     */
    private Set<String> groups;

    /**
     * Template portability indicators.
     */
    private Map<String, AbstractPropertyValue> portability;

    public NodeTemplate(String type, Map<String, AbstractPropertyValue> properties, Map<String, IValue> attributes,
            Map<String, RelationshipTemplate> relationships, Map<String, Requirement> requirements, Map<String, Capability> capabilities,
            Map<String, Interface> interfaces, Map<String, DeploymentArtifact> artifacts) {
        this.setType(type);
        this.setProperties(properties);
        this.setArtifacts(artifacts);
        this.setAttributes(attributes);
        this.relationships = relationships;
        this.requirements = requirements;
        this.capabilities = capabilities;
        this.setInterfaces(interfaces);
    }
}