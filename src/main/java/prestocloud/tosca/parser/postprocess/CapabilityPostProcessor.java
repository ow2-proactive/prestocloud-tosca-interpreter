package prestocloud.tosca.parser.postprocess;

import java.util.Map;

import javax.annotation.Resource;

import org.prestocloud.tosca.model.templates.Capability;
import org.prestocloud.tosca.model.types.CapabilityType;
import org.springframework.stereotype.Component;

import prestocloud.tosca.context.ToscaContext;

/**
 * Ensure that the type exists and check capability properties.
 */
@Component
public class CapabilityPostProcessor implements IPostProcessor<Map.Entry<String, Capability>> {
    @Resource
    private ReferencePostProcessor referencePostProcessor;
    @Resource
    private PropertyValueChecker propertyValueChecker;

    @Override
    public void process(Map.Entry<String, Capability> instance) {
        referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance.getValue(), instance.getValue().getType(), CapabilityType.class));
        CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, instance.getValue().getType());
        if (capabilityType == null) {
            return;
        }
        propertyValueChecker.checkProperties(capabilityType, instance.getValue().getProperties(), instance.getKey());
    }
}