package org.prestocloud.tosca.model.types;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Complex data type used for property definition
 */
@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
public class DataType extends AbstractInheritableToscaType {

    private boolean deriveFromSimpleType = false;

}
