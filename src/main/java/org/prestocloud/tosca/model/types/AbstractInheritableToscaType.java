package org.prestocloud.tosca.model.types;

import java.util.List;
import java.util.Map;

import org.prestocloud.tosca.model.definitions.PropertyDefinition;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import prestocloud.utils.jackson.ConditionalAttributes;
import prestocloud.utils.jackson.ConditionalOnAttribute;
import prestocloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import prestocloud.utils.jackson.JSonMapEntryArraySerializer;

@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
public class AbstractInheritableToscaType extends AbstractToscaType {
    private boolean isAbstract;

    private List<String> derivedFrom;

    @ConditionalOnAttribute(value = { ConditionalAttributes.REST, ConditionalAttributes.ES_1_2 })
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, PropertyDefinition> properties;
}