package org.prestocloud.tosca.model.templates;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.prestocloud.tosca.model.definitions.PolicyTrigger;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.Setter;

/**
 * Referred as policy definition in TOSCA.
 */
@Getter
@Setter
public class PolicyTemplate extends AbstractTemplate {
    private LinkedHashSet<String> targets = Sets.newLinkedHashSet();
    private Map<String, PolicyTrigger> triggers = Maps.newLinkedHashMap();
}
