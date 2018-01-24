package org.prestocloud.tosca.model.types;

import static prestocloud.dao.model.FetchContext.QUICK_SEARCH;
import static prestocloud.dao.model.FetchContext.SUMMARY;
import static prestocloud.dao.model.FetchContext.TAG_SUGGESTION;

import java.util.List;
import java.util.Map;

import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.MapKeyValue;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.mapping.IndexType;
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
    @TermsFacet
    @TermFilter
    @BooleanField(includeInAll = false, index = IndexType.not_analyzed)
    private boolean isAbstract;

    @FetchContext(contexts = { QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false })
    @TermsFacet
    private List<String> derivedFrom;

    @MapKeyValue
    @ConditionalOnAttribute(value = { ConditionalAttributes.REST, ConditionalAttributes.ES_1_2 })
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    private Map<String, PropertyDefinition> properties;
}