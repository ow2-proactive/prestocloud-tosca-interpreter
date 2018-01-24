package org.prestocloud.tosca.model.types;

import static prestocloud.dao.model.FetchContext.QUICK_SEARCH;
import static prestocloud.dao.model.FetchContext.SUMMARY;
import static prestocloud.dao.model.FetchContext.TAG_SUGGESTION;

import java.util.Map;

import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.query.FetchContext;
import org.prestocloud.tosca.model.definitions.DeploymentArtifact;
import org.prestocloud.tosca.model.definitions.IValue;
import org.prestocloud.tosca.model.definitions.Interface;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Getter;
import lombok.Setter;
import prestocloud.json.deserializer.AttributeDeserializer;
import prestocloud.utils.jackson.ConditionalAttributes;
import prestocloud.utils.jackson.ConditionalOnAttribute;
import prestocloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import prestocloud.utils.jackson.JSonMapEntryArraySerializer;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class AbstractInstantiableToscaType extends AbstractInheritableToscaType {
    @FetchContext(contexts = { SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    @ObjectField(enabled = false)
    private Map<String, DeploymentArtifact> artifacts;

    @ObjectField(enabled = false)
    @FetchContext(contexts = { SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    @ConditionalOnAttribute(value = { ConditionalAttributes.REST })
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class, contentUsing = AttributeDeserializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, IValue> attributes;

    @FetchContext(contexts = { SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    @ObjectField(enabled = false)
    private Map<String, Interface> interfaces;
}