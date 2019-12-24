package prestocloud.model.generator;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GeneratedFragmentFaasOnEdge extends GeneratedNode {

    protected static final String headerUnstructured = "   processing_node_%s:\n" +
            "      type: prestocloud.nodes.compute.edge\n" +
            "      properties:\n" +
            "         id: %s\n" +
            "         type: edge\n";
    protected static final String headerNameunstructure = "         name: %s\n";
    protected static final String edgeUnstructured = "         edge:\n" +
            "            edge_type: %s\n" +
            "            edge_location: %s\n" +
            "            edge_credentials:\n" +
            "               username: %s\n";
    protected static final String edgePasswordUnstructured = "               password: %s\n";
    protected static final String edgePrivateKeyUnstructured = "               privatekey: %s\n";
    protected static final String edgeGpsCoordinatesUnstructured = "            gps_coordinates: %s\n";
    protected static final String networkUnstructured = "         network:\n" +
            "         network_name: %s\n" +
            "         network_id: %s\n" +
            "         addresses:" +
            "            - @variables_network_%s\n";
    protected static final String capabilitiesResourceUnstructured = "      capabilities:\n" +
            "         resource:" +
            "            properties:\n" +
            "               type: edge\n" +
            "               edge:\n" +
            "                  edge_type: %s\n" +
            "                  edge_location: %s\n" +
            "                  edge_credentials:\n" +
            "                     username: %s\n";
    protected static final String capabilitiesPasswordUnstructured = "      " + edgePasswordUnstructured;
    protected static final String capabilitiesPrivatekeyUnstructured = "      " + edgePrivateKeyUnstructured;

    protected static final String capabilitiesHostUnstructured = "         resource:\n" +
            "            properties:\n" +
            "               num_cpus: %s\n" +
            "               mem_size: %s\n";
    protected static final String capabilitiesHostDiskSizeUnstructured = "               disk_size: %s\n";
    protected static final String capabilitiesHostCpuFreqUnstructured = "               cpu_frequency: %s\n";
    protected static final String capabilitiesHostPriceUnstructured = "               price: %s\n";
    protected static final String capabilitiesSensorsUnstructured = "         resource:\n" +
            "            properties:\n";
    protected static final String capabilitiesSensorsCameraUnstructured = "               camera: %s";
    protected static final String capabilitiesSensorsMicrophoneUnstructured = "               microphone: %s";
    protected static final String capabilitiesSensorsTemperatureUnstructured = "               temperature: %s";

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
        sb.append(String.format(networkUnstructured, networkName, networkId));
        // Capability structure
        sb.append(String.format(capabilitiesResourceUnstructured, edgeType, edgeLocation, edgeCredentialsUsername));
        if (edgeCredentialsPassword.isPresent()) {
            sb.append(String.format(capabilitiesPasswordUnstructured, edgeCredentialsPassword.get()));
        }
        if (edgeCredentialsPrivateKey.isPresent()) {
            sb.append(String.format(capabilitiesPrivatekeyUnstructured, edgeCredentialsPrivateKey.get()));
        }
        sb.append(String.format(capabilitiesHostUnstructured, numCpus, memSize));
        if (diskSize.isPresent()) {
            sb.append(String.format(capabilitiesHostDiskSizeUnstructured, diskSize.get()));
        }
        if (cpuFrequency.isPresent()) {
            sb.append(String.format(capabilitiesHostCpuFreqUnstructured, cpuFrequency.get()));
        }
        sb.append(String.format(capabilitiesHostPriceUnstructured, price));
        if (sensorsPropertiesCamera.isPresent() || sensorsPropertiesMicrophone.isPresent() || sensorsPropertiesTemperature.isPresent()) {
            sb.append(capabilitiesSensorsUnstructured);
            sensorsPropertiesCamera.ifPresent(s -> sb.append(String.format(capabilitiesSensorsCameraUnstructured, s)));
            sensorsPropertiesTemperature.ifPresent(s -> sb.append(String.format(capabilitiesSensorsTemperatureUnstructured, s)));
            sensorsPropertiesMicrophone.ifPresent(s -> sb.append(String.format(capabilitiesSensorsMicrophoneUnstructured, s)));
        }
        return sb.toString();
    }

}
