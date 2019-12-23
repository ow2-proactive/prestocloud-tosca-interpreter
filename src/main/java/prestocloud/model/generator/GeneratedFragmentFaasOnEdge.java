package prestocloud.model.generator;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GeneratedFragmentFaasOnEdge extends GeneratedNode {

    // Computing resource - TOSCA Type: prestocloud.nodes.compute.edge, tosca.datatypes.Credential, tosca.datatypes.network.NetworkInfo, prestocloud.capabilities.container, prestocloud.capabilities.resource, prestocloud.capabilities.sensors, prestocloud.datatypes.edge
    public String computeId;
    public Optional<String> computeName;
    public final String NODE_TYPE = "edge";
    public String edgeType;
    public String edgeLocation;
    public Optional<String> edgeGpsCoordinate;
    public String edgeCredentialsUsername;
    public Optional<String> edgeCredentialsPassword;
    public Optional<String> edgeCredentialsPrivateKey;

    // Sensors
    public Optional<String> sensorsPropertiesCamera;
    public Optional<String> sensorsPropertiesTemperature;
    public Optional<String> sensorsPropertiesMicrophone;
    // Get the info structured

    public String getStructureProcessingNode() {
        // TODO
        return "";
    }

}
