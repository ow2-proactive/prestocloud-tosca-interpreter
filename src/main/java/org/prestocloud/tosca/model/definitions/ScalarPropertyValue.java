package org.prestocloud.tosca.model.definitions;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a simple scalar property value.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ScalarPropertyValue extends PropertyValue<String> {

    public ScalarPropertyValue(String value) {
        super(value);
    }
}