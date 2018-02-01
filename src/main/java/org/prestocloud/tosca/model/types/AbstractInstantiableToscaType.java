package org.prestocloud.tosca.model.types;

import java.util.Map;

import org.prestocloud.tosca.model.definitions.DeploymentArtifact;
import org.prestocloud.tosca.model.definitions.IValue;
import org.prestocloud.tosca.model.definitions.Interface;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbstractInstantiableToscaType extends AbstractInheritableToscaType {
    private Map<String, DeploymentArtifact> artifacts;

    private Map<String, IValue> attributes;

    private Map<String, Interface> interfaces;
}