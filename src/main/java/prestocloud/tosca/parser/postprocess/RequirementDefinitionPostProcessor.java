package prestocloud.tosca.parser.postprocess;

import javax.annotation.Resource;

import org.prestocloud.tosca.model.definitions.RequirementDefinition;
import org.prestocloud.tosca.model.types.NodeType;
import org.prestocloud.tosca.model.types.RelationshipType;
import org.springframework.stereotype.Component;

/**
 * Performs validation of a requirement definition.
 */
@Component
public class RequirementDefinitionPostProcessor implements IPostProcessor<RequirementDefinition> {
    @Resource
    private ReferencePostProcessor referencePostProcessor;

    @Resource
    private CapabilityOrNodeReferencePostProcessor capabilityOrNodeReferencePostProcessor;

    @Override
    public void process(RequirementDefinition instance) {
        capabilityOrNodeReferencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance, instance.getType()));
        if(instance.getNodeType() != null) {
            referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance, instance.getNodeType(), NodeType.class));
        }

        if (instance.getRelationshipType() != null) {
            referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance, instance.getRelationshipType(), RelationshipType.class));
        }
    }
}
