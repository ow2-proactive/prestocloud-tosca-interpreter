package org.prestocloud.tosca.model.types;

import static prestocloud.dao.model.FetchContext.QUICK_SEARCH;
import static prestocloud.dao.model.FetchContext.TAG_SUGGESTION;

import java.util.List;
import java.util.Map;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.NumberField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.mapping.IndexType;
import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;
import org.prestocloud.tosca.model.definitions.CapabilityDefinition;
import org.prestocloud.tosca.model.definitions.RequirementDefinition;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import prestocloud.component.portability.ESPortabilityPropertiesPathsGenerator;
import prestocloud.json.deserializer.PropertyValueDeserializer;
import prestocloud.utils.jackson.ConditionalAttributes;
import prestocloud.utils.jackson.ConditionalOnAttribute;

@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
@ESObject
public class NodeType extends AbstractInstantiableToscaType {
    @FetchContext(contexts = { QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false })
    @TermsFacet(paths = "type")
    private List<CapabilityDefinition> capabilities;

    @FetchContext(contexts = { QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false })
    @TermsFacet(paths = "type")
    private List<RequirementDefinition> requirements;

    @FetchContext(contexts = { QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false })
    @TermsFacet
    private List<String> defaultCapabilities;

    @NumberField(index = IndexType.not_analyzed, includeInAll = false)
    private long alienScore;

    /** When the type is created from a topology template (substitution), contains the topology id. */
    private String substitutionTopologyId;

    /** Portability information. */
    @ConditionalOnAttribute({ ConditionalAttributes.ES, ConditionalAttributes.REST })
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    @TermFilter(pathGenerator = ESPortabilityPropertiesPathsGenerator.class)
    private Map<String, AbstractPropertyValue> portability;
}