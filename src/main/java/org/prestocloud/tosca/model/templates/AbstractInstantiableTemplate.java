package org.prestocloud.tosca.model.templates;

import java.util.Map;

import org.elasticsearch.annotation.MapKeyValue;
import org.elasticsearch.annotation.ObjectField;
import org.prestocloud.tosca.model.definitions.DeploymentArtifact;
import org.prestocloud.tosca.model.definitions.IValue;
import org.prestocloud.tosca.model.definitions.Interface;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import prestocloud.json.deserializer.AttributeDeserializer;
import prestocloud.utils.jackson.ConditionalAttributes;
import prestocloud.utils.jackson.ConditionalOnAttribute;
import prestocloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import prestocloud.utils.jackson.JSonMapEntryArraySerializer;

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
    @ObjectField(enabled = false)
    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class, contentUsing = AttributeDeserializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, IValue> attributes;

    /**
     * The deployment artifacts
     */
    @MapKeyValue
    @ConditionalOnAttribute(value = { ConditionalAttributes.ES_1_2 })
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, DeploymentArtifact> artifacts;

    /**
     * The interfaces that are defined at the template level (overriding type's one).
     */
    @MapKeyValue
    @ConditionalOnAttribute(value = { ConditionalAttributes.REST, ConditionalAttributes.ES_1_2 })
    private Map<String, Interface> interfaces;
}