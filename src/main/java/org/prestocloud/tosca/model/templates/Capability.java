package org.prestocloud.tosca.model.templates;

import java.util.Map;

import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Capability for a node template. This should match a capability definition from the node's type.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Capability {
    /**
     * The QName value of this attribute refers to the Capability Type definition of the Capability. This Capability Type denotes the semantics and well as
     * potential properties of the Capability.
     */
    private String type;
    /**
     * This element specifies initial values for one or more of the Capability Properties according to the Capability Type providing the property definitions.
     * Properties are provided in the form of an XML fragment. The same rules as outlined for the Properties element of the Node Template apply.
     */
    private Map<String, AbstractPropertyValue> properties;
}