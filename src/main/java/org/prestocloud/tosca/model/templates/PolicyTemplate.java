package org.prestocloud.tosca.model.templates;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.prestocloud.tosca.model.definitions.PolicyTrigger;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.Setter;

/**
 * Referred as policy definition in TOSCA.
 */
@Getter
@Setter
public class PolicyTemplate extends AbstractTemplate {
    @StringField(indexType = IndexType.no, includeInAll = false)
    private Set<String> targets = Sets.newLinkedHashSet();
    @ObjectField(enabled = false)
    private Map<String, PolicyTrigger> triggers = Maps.newLinkedHashMap();
}
