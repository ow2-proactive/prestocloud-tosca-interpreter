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
        StringBuilder sb = new StringBuilder();
        String headerUnstructured = "   processing_node_%s:\n" +
                "      type: prestocloud.nodes.compute.edge\n" +
                "      properties:\n" +
                "         id: %s\n" +
                "         type: edge\n";
        String headerNameunstructure = "         name: %s\n";
        String edgeUnstructured = "         edge:\n" +
                "            edge_type: %s\n" +
                "            edge_location: %s\n" +
                "            edge_credentials:\n" +
                "               username: %s\n";
        String edgePasswordUnstructured = "               password: %s\n";
        String edgePrivateKeyUnstructured = "               privatekey: %s\n";
        String edgeGpsCoordinatesUnstructured = "            gps_coordinates: %s\n";
        String networkUnstructured = "         network:\n" +
                "         network_name: %s\n" +
                "         network_id: %s\n" +
                "         addresses:" +
                "            - @variables_network_%s\n";
        String capabilitiesUnstructured = "      capabilities:\n";

        sb.append(String.format(headerUnstructured, this.fragmentName, this.computeId));
        if (this.computeName.isPresent()) {
            sb.append(String.format(headerNameunstructure, computeName.get()));
        }
        sb.append(String.format(edgeUnstructured, this.edgeType, this.edgeLocation, edgeCredentialsUsername));
        if (edgeCredentialsPassword.isPresent()) {
            sb.append(String.format(edgePasswordUnstructured, edgeCredentialsPassword.get()));
        }
        if (edgeCredentialsPrivateKey.isPresent()) {
            sb.append(String.format(edgePrivateKeyUnstructured, edgeCredentialsPrivateKey.get()));
        }
        if (edgeGpsCoordinate.isPresent()) {
            sb.append(String.format(edgeGpsCoordinatesUnstructured, edgeGpsCoordinate.get()));
        }
        return sb.toString();
    }

}
