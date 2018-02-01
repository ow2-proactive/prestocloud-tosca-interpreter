package org.prestocloud.tosca.model.definitions;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FilterDefinition {
    /** Property constraint list by property */
    private Map<String, List<PropertyConstraint>> properties = Maps.newHashMap();
}