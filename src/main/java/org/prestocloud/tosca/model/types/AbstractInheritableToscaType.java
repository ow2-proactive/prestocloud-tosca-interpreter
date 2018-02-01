package org.prestocloud.tosca.model.types;

import java.util.List;
import java.util.Map;

import org.prestocloud.tosca.model.definitions.PropertyDefinition;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
public class AbstractInheritableToscaType extends AbstractToscaType {
    private boolean isAbstract;

    private List<String> derivedFrom;

    private Map<String, PropertyDefinition> properties;
}