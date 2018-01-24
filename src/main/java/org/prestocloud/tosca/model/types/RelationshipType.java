package org.prestocloud.tosca.model.types;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RelationshipType extends AbstractInstantiableToscaType {
    private String[] validSources;

    private String[] validTargets;
}