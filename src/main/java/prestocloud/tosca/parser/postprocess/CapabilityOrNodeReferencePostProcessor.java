package prestocloud.tosca.parser.postprocess;

import org.springframework.stereotype.Component;

import org.prestocloud.tosca.model.types.CapabilityType;
import org.prestocloud.tosca.model.types.NodeType;

/**
 * Post process references with a tolerance over a capability type or a node type.
 */
@Component
public class CapabilityOrNodeReferencePostProcessor extends ReferencePostProcessor {
    @Override
    public void process(TypeReference typeReference) {
        typeReference.setClasses(new Class[] { CapabilityType.class, NodeType.class });
        super.process(typeReference);
    }
}