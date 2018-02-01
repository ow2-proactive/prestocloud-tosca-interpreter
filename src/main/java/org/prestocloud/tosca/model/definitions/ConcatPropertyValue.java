package org.prestocloud.tosca.model.definitions;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConcatPropertyValue extends AbstractPropertyValue {
    private String function_concat;
    private List<AbstractPropertyValue> parameters;
}
