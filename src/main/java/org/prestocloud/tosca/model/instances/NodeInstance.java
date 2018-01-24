package org.prestocloud.tosca.model.instances;

import java.util.Map;

import org.prestocloud.tosca.model.templates.NodeTemplate;

import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.Setter;

/**
 * An instance of a node.
 */
@Getter
@Setter
public class NodeInstance {
    // The node template actually does not include the type version (maybe we should add that to the node template ?).
    private String typeVersion;

    private NodeTemplate nodeTemplate;

    private Map<String, String> attributeValues = Maps.newHashMap();

    public void setAttribute(String key, String value) {
        attributeValues.put(key, value);
    }
}
