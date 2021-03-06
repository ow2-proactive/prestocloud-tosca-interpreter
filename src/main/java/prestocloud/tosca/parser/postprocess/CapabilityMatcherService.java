package prestocloud.tosca.parser.postprocess;

import static prestocloud.utils.PrestocloudUtils.safe;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.prestocloud.tosca.model.definitions.CapabilityDefinition;
import org.prestocloud.tosca.model.templates.Capability;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.prestocloud.tosca.model.types.AbstractToscaType;
import org.prestocloud.tosca.model.types.CapabilityType;
import org.prestocloud.tosca.model.types.NodeType;
import org.prestocloud.tosca.utils.ToscaTypeUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import prestocloud.tosca.context.ToscaContext;
import lombok.Setter;

@Component
public class CapabilityMatcherService implements ICapabilityMatcherService {

    public interface IToscaContextFinder {
        <T extends AbstractToscaType> T find(Class<T> clazz, String id);
    }

    @Setter
    private IToscaContextFinder toscaContextFinder;

    public CapabilityMatcherService() {
        toscaContextFinder = ToscaContext::get;
    }

    @Override
    public Map<String, Capability> getCompatibleCapabilityByType(NodeTemplate nodeTemplate, String type) {
        Map<String, Capability> capabilities = nodeTemplate.getCapabilities();
        if (capabilities == null) {
            return Collections.emptyMap();
        }

        Map<String, Capability> targetCapabilitiesMatch = Maps.newHashMap();
        for (Map.Entry<String, Capability> capabilityEntry : capabilities.entrySet()) {
            String capabilityTypeName = capabilityEntry.getValue().getType();
            CapabilityType capabilityType = toscaContextFinder.find(CapabilityType.class, capabilityTypeName);

            if (ToscaTypeUtils.isOfType(capabilityType, type)) {
                targetCapabilitiesMatch.put(capabilityEntry.getKey(), capabilityEntry.getValue());
            }
        }
        return targetCapabilitiesMatch;
    }

    @Override
    public List<CapabilityDefinition> getCompatibleCapabilityByType(NodeType nodeType, String type) {
        List<CapabilityDefinition> targetCapabilitiesMatch = Lists.newArrayList();
        for (CapabilityDefinition capabilityDefinition : safe(nodeType.getCapabilities())) {
            CapabilityType capabilityType = toscaContextFinder.find(CapabilityType.class, capabilityDefinition.getType());

            if (ToscaTypeUtils.isOfType(capabilityType, type)) {
                targetCapabilitiesMatch.add(capabilityDefinition);
            }
        }
        return targetCapabilitiesMatch;
    }
}
