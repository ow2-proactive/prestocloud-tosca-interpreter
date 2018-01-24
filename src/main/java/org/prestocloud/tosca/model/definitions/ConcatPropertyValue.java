package org.prestocloud.tosca.model.definitions;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;
import lombok.Setter;
import prestocloud.json.deserializer.PropertyValueDeserializer;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConcatPropertyValue extends AbstractPropertyValue {
    private String function_concat;
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    private List<AbstractPropertyValue> parameters;
}
