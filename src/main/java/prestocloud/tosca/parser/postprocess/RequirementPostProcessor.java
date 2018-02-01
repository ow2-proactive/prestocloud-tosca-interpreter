package prestocloud.tosca.parser.postprocess;

import java.util.Map;

import javax.annotation.Resource;

import org.prestocloud.tosca.model.templates.Requirement;
import org.prestocloud.tosca.model.types.CapabilityType;
import org.springframework.stereotype.Component;

import prestocloud.tosca.context.ToscaContext;

/**
 * Post processor that performs validation of references in a tosca template.
 */
@Component
public class RequirementPostProcessor implements IPostProcessor<Map.Entry<String, Requirement>> {
    @Resource
    private CapabilityOrNodeReferencePostProcessor capabilityOrNodeReferencePostProcessor;

    @Resource
    private PropertyValueChecker propertyValueChecker;

    @Override
    public void process(Map.Entry<String, Requirement> instance) {
        capabilityOrNodeReferencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance, instance.getValue().getType()));
        CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, instance.getValue().getType());
        propertyValueChecker.checkProperties(capabilityType, instance.getValue().getProperties(), instance.getKey());
    }
}