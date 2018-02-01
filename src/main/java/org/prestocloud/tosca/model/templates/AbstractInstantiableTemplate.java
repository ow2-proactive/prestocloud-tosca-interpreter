package org.prestocloud.tosca.model.templates;

import java.util.Map;

import org.prestocloud.tosca.model.definitions.DeploymentArtifact;
import org.prestocloud.tosca.model.definitions.IValue;
import org.prestocloud.tosca.model.definitions.Interface;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Abstract template is parent of {@link NodeTemplate} and {@link RelationshipTemplate}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractInstantiableTemplate extends AbstractTemplate {
    /**
     * Attributes of the node template
     */
    private Map<String, IValue> attributes;

    /**
     * The deployment artifacts
     */
    private Map<String, DeploymentArtifact> artifacts;

    /**
     * The interfaces that are defined at the template level (overriding type's one).
     */
    private Map<String, Interface> interfaces;
}