package org.prestocloud.tosca.model.types;

import java.util.List;

import javax.validation.Valid;

import org.prestocloud.tosca.model.definitions.PropertyConstraint;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import prestocloud.json.deserializer.PropertyConstraintDeserializer;
import prestocloud.tosca.container.validation.ToscaPropertyConstraintDuplicate;

/**
 * Complex data type used for property definition
 */
@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
public class PrimitiveDataType extends DataType {

    /**
     * Only data types that derive from a simple type have associated constraints.
     */
    @Valid
    @ToscaPropertyConstraintDuplicate
    @JsonDeserialize(contentUsing = PropertyConstraintDeserializer.class)
    private List<PropertyConstraint> constraints;

    public PrimitiveDataType() {
        super();
        this.setDeriveFromSimpleType(true);
    }

}
