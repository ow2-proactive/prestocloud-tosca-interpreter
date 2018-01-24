package prestocloud.tosca.parser.postprocess;

import java.util.List;
import java.util.Map;

import org.prestocloud.tosca.model.definitions.CapabilityDefinition;
import org.prestocloud.tosca.model.templates.Capability;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.prestocloud.tosca.model.types.NodeType;

public interface ICapabilityMatcherService {
    Map<String, Capability> getCompatibleCapabilityByType(NodeTemplate nodeTemplate, String type);

    List<CapabilityDefinition> getCompatibleCapabilityByType(NodeType nodeType, String type);
}
