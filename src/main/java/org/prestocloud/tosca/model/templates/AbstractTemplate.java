package org.prestocloud.tosca.model.templates;

import java.util.List;
import java.util.Map;

import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import prestocloud.model.common.Tag;

/**
 * Abstract template is parent of both instantiable templates as nodes, relationships and groups as well as other templates as policies.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractTemplate {
    /** Name of the template. Same value as it's key in the map. */
    private String name;

    /**
     * The QName value of this attribute refers to the Node Type providing the type of the Node Template.
     *
     * Note: If the Node Type referenced by the type attribute of a Node Template is declared as abstract, no instances of the specific Node Template can be
     * created. Instead, a substitution of the Node Template with one having a specialized, derived Node Type has to be done at the latest during the
     * instantiation time of the Node Template.
     */
    private String type;

    private String description;

    /* Tosca metadata */
    private List<Tag> tags;

    /** Properties of the template. */
    private Map<String, AbstractPropertyValue> properties;
}
