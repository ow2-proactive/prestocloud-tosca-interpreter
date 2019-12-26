package prestocloud.model.generator;

import prestocloud.btrplace.tosca.model.EdgeResourceTemplateDetails;

import java.util.Optional;

public class GeneratedFragmentFaasOnEdge extends GeneratedNode {

    protected static final String HEADER_UNSTRUCT = "   processing_node_%s:\n" +
            "      type: prestocloud.nodes.compute.edge\n" +
            "      properties:\n" +
            "         id: %s\n" +
            "         type: edge\n";
    protected static final String HEADER_NAME_UNSTRUCT = "         name: %s\n";
    protected static final String EDGE_UNSTRUCT = "         edge:\n" +
            "            edge_type: %s\n" +
            "            edge_location: %s\n" +
            "            edge_credentials:\n" +
            "               username: %s\n";
    protected static final String EDGE_PASSWD_UNSTRUCT = "               password: %s\n";
    protected static final String EDGE_PRIVATEKEY_UNSTRUCT = "               privatekey: %s\n";
    protected static final String EDGE_GPSCOORDINATES_UNSTRUCT = "            gps_coordinates: %s\n";
    protected static final String NETWORK_UNSTRUCT = "         network:\n" +
            "         network_name: %s\n" +
            "         network_id: %s\n" +
            "         addresses:" +
            "            - @variables_network_%s\n";
    protected static final String CAP_RESOURCE_UNSTRUCT = "      capabilities:\n" +
            "         resource:" +
            "            properties:\n" +
            "               type: edge\n" +
            "               edge:\n" +
            "                  edge_type: %s\n" +
            "                  edge_location: %s\n" +
            "                  edge_credentials:\n" +
            "                     username: %s\n";
    protected static final String CAP_PASWD_UNSTRUCT = "      " + EDGE_PASSWD_UNSTRUCT;
    protected static final String CAP_PRIVATEKEY_UNSTRUCT = "      " + EDGE_PRIVATEKEY_UNSTRUCT;

    protected static final String CAP_HOST_UNSTRUCT = "         resource:\n" +
            "            properties:\n" +
            "               num_cpus: %s\n" +
            "               mem_size: %s\n";
    protected static final String CAP_HOSTDISKSIZE_UNSTRUCT = "               disk_size: %s\n";
    protected static final String CAP_HOSTCPUFREQ_UNSTRUCT = "               cpu_frequency: %s\n";
    protected static final String CAP_HOSTPRICE_UNSTRUCT = "               price: %s\n";
    protected static final String CAP_SENSORS_UNSTRUCT = "         sensors:\n" +
            "            properties:\n";
    protected static final String CAP_SENSORSCAMERA_UNSTRUCT = "               camera: %s\n";
    protected static final String CAP_SENSORSMICROPHONE_UNSTRUCT = "               microphone: %s\n";
    protected static final String CAP_SENSORSTEMPERATURE_UNSTRUCTURED = "               temperature: %s\n";

    // Computing resource - TOSCA Type: prestocloud.nodes.compute.edge, tosca.datatypes.Credential, tosca.datatypes.network.NetworkInfo, prestocloud.capabilities.container, prestocloud.capabilities.resource, prestocloud.capabilities.sensors, prestocloud.datatypes.edge
    private String edgeType;
    private String edgeLocation;
    private Optional<String> edgeGpsCoordinate;
    private String edgeCredentialsUsername;
    private Optional<String> edgeCredentialsPassword;
    private Optional<String> edgeCredentialsPrivateKey;

    // Sensors
    private Optional<String> sensorsPropertiesCamera;
    private Optional<String> sensorsPropertiesTemperature;
    private Optional<String> sensorsPropertiesMicrophone;
    // Get the info structured

    public GeneratedFragmentFaasOnEdge(String fragmentName, String fragmentId, String edgeDeviceId, boolean isALb, EdgeResourceTemplateDetails ertd) {
        //General
        this.isALoadBalancer = isALb;
        this.computeId = edgeDeviceId;
        this.computeName = ertd.name;

        // Host Resource
        this.cpuFrequency = ertd.cpu_frequency;
        this.diskSize = ertd.disk_size;
        this.numCpus = Integer.parseInt(ertd.num_cpus);
        this.memSize = ertd.mem_size;
        this.price = ertd.price.isPresent() ? Double.parseDouble(ertd.price.get()) : 0;

        //Compute resource - Network
        // We have no access to any cloud resource from here
        this.networkId = "@variables_EDGE_GW_IPV4";
        this.networkName = "DEFAULT_EDGE_NETWORK";

        // Fragment
        this.id = fragmentId;
        this.fragmentName = fragmentName;
        this.onLoadable = false;
        this.proxyFragment = Optional.empty();

        // Computing resource, specific to edge
        edgeType = ertd.edgeType;
        edgeLocation = ertd.location;
        edgeGpsCoordinate = ertd.gps_coordinate;
        edgeCredentialsUsername = ertd.username;
        edgeCredentialsPassword = ertd.password;
        edgeCredentialsPrivateKey = ertd.privatekey;
        sensorsPropertiesMicrophone = ertd.microphoneSensor;
        sensorsPropertiesTemperature = ertd.temperatureSensor;
        sensorsPropertiesCamera = ertd.cameraSensor;
    }

    public String getStructureProcessingNode() {
        StringBuilder sb = new StringBuilder();
        // Main properties
        sb.append(String.format(HEADER_UNSTRUCT, fragmentName, computeId));
        computeName.ifPresent(s -> sb.append(String.format(HEADER_NAME_UNSTRUCT, s)));
        // Edge properties
        sb.append(String.format(EDGE_UNSTRUCT, edgeType, edgeLocation, edgeCredentialsUsername));
        edgeCredentialsPassword.ifPresent(s -> sb.append(String.format(EDGE_PASSWD_UNSTRUCT, s)));
        edgeCredentialsPrivateKey.ifPresent(s -> sb.append(String.format(EDGE_PRIVATEKEY_UNSTRUCT, s)));
        edgeGpsCoordinate.ifPresent(s -> sb.append(String.format(EDGE_GPSCOORDINATES_UNSTRUCT, s)));
        sb.append(String.format(NETWORK_UNSTRUCT, networkName, networkId, fragmentName));
        // Capability structure  - resource
        sb.append(String.format(CAP_RESOURCE_UNSTRUCT, edgeType, edgeLocation, edgeCredentialsUsername));
        edgeCredentialsPassword.ifPresent(s -> sb.append(String.format(CAP_PASWD_UNSTRUCT, s)));
        edgeCredentialsPrivateKey.ifPresent(s -> sb.append(String.format(CAP_PRIVATEKEY_UNSTRUCT, s)));
        // Capability structure  - host
        sb.append(String.format(CAP_HOST_UNSTRUCT, numCpus, memSize));
        diskSize.ifPresent(s -> sb.append(String.format(CAP_HOSTDISKSIZE_UNSTRUCT, s)));
        cpuFrequency.ifPresent(s -> sb.append(String.format(CAP_HOSTCPUFREQ_UNSTRUCT, s)));
        sb.append(String.format(CAP_HOSTPRICE_UNSTRUCT, price));
        // Capability structure  - sensors
        if (sensorsPropertiesCamera.isPresent() || sensorsPropertiesMicrophone.isPresent() || sensorsPropertiesTemperature.isPresent()) {
            sb.append(CAP_SENSORS_UNSTRUCT);
            sensorsPropertiesCamera.ifPresent(s -> sb.append(String.format(CAP_SENSORSCAMERA_UNSTRUCT, s)));
            sensorsPropertiesTemperature.ifPresent(s -> sb.append(String.format(CAP_SENSORSTEMPERATURE_UNSTRUCTURED, s)));
            sensorsPropertiesMicrophone.ifPresent(s -> sb.append(String.format(CAP_SENSORSMICROPHONE_UNSTRUCT, s)));
        }
        sb.append("\n");
        return sb.toString();
    }

}
