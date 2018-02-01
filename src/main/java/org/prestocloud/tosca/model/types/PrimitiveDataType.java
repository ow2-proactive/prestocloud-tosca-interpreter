package org.prestocloud.tosca.model.types;

import java.util.List;

import javax.validation.Valid;

import org.prestocloud.tosca.model.definitions.PropertyConstraint;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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
    private List<PropertyConstraint> constraints;

    public PrimitiveDataType() {
        super();
        this.setDeriveFromSimpleType(true);
    }

}
