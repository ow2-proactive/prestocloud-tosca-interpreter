package org.prestocloud.tosca.model.definitions;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConcatPropertyValue extends AbstractPropertyValue {
    private String function_concat;
    private List<AbstractPropertyValue> parameters;
}
