package prestocloud.tosca.parser.postprocess;

import static prestocloud.utils.AlienUtils.safe;

import javax.annotation.Resource;

import org.prestocloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

/**
 * Post process a node type.
 */
@Component
public class NodeTypePostProcessor implements IPostProcessor<NodeType> {
    @Resource
    private CapabilityDefinitionPostProcessor capabilityDefinitionPostProcessor;
    @Resource
    private RequirementDefinitionPostProcessor requirementDefinitionPostProcessor;

    @Override
    public void process(NodeType instance) {
        safe(instance.getCapabilities()).forEach(capabilityDefinitionPostProcessor);
        safe(instance.getRequirements()).forEach(requirementDefinitionPostProcessor);
    }
}