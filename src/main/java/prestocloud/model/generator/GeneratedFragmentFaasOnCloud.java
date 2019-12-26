package prestocloud.model.generator;

import java.util.Optional;

public class GeneratedFragmentFaasOnCloud extends GeneratedNode {

    protected static final String HEADER_UNSTRUCT = "   processing_node_%s:\n" +
            "      type: prestocloud.nodes.compute.cloud.%s\n" +
            "      properties:\n" +
            "         id: %s\n" +
            "         type: cloud\n";
    protected static final String HEADER_NAME_UNSTRUCT = "         name: %s\n";

    // Cloud properties
    protected static final String CLOUD_UNSTRUCT = "         cloud:\n" +
            "            cloud_name: %s\n" +
            "            cloud_type: %s\n" +
            "            cloud_region: %s\n" +
            "            cloud_instance: %s\n";

    protected static final String CAP_RES_UNSTRUCT = "      capabilities:\n" +
            "         resources:\n" +
            "            properties:\n" +
            "               type: cloud\n" +
            "               cloud:" +
            "                  cloud_name: %s\n" +
            "                  cloud_type: %s\n" +
            "                  cloud_region: %s\n" +
            "                  cloud_instance: %s\n";
    protected static final String CAP_HOST_UNSTRUCT = "         resource:\n" +
            "            properties:\n" +
            "               num_cpus: %s\n" +
            "               mem_size: %s\n";
    protected static final String CAP_HOSTDISKSIZE_UNSTRUCT = "               disk_size: %s\n";
    protected static final String CAP_HOSTCPUFREQ_UNSTRUCT = "               cpu_frequency: %s\n";
    protected static final String CAP_HOSTPRICE_UNSTRUCT = "               price: %s\n";
    protected static final String NETWORK_UNSTRUCT = "         network:\n" +
            "         network_name: %s\n" +
            "         network_id: %s\n" +
            "         addresses:" +
            "            - @variables_network_%s\n";

    // prestocloud.datatypes.cloud
    private String cloudType;
    private String cloudName;
    private String cloudRegions;
    private Optional<String> cloudCredentialsUsername;
    private Optional<String> cloudCredentialsPassword;
    private Optional<String> cloudCredentialsSubscription;
    private Optional<String> cloudCredentialsDomain;
    private Optional<String> cloudInstance;
    private Optional<String> cloudImage;
    private Optional<String> cpuCapacity;
    private Optional<String> memoryCapacity;
    private Optional<String> diskCapacity;
    private Optional<String> gpsCoordinate;


    @Override
    public String getStructureProcessingNode() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(HEADER_UNSTRUCT, fragmentName, cloudType, computeId));
        computeName.ifPresent(s -> sb.append(String.format(HEADER_NAME_UNSTRUCT, s)));
        // Cloud properties
        sb.append(String.format(CLOUD_UNSTRUCT, cloudName, cloudType, cloudRegions, cloudInstance.orElse("default")));
        // Network
        sb.append(String.format(NETWORK_UNSTRUCT, networkName, networkId, fragmentName));
        // Capability - resource
        sb.append(String.format(CAP_RES_UNSTRUCT, cloudName, cloudType, cloudRegions, cloudInstance.orElse("default")));
        // Capability structure  - host
        sb.append(String.format(CAP_HOST_UNSTRUCT, numCpus, memSize));
        diskSize.ifPresent(s -> sb.append(String.format(CAP_HOSTDISKSIZE_UNSTRUCT, s)));
        cpuFrequency.ifPresent(s -> sb.append(String.format(CAP_HOSTCPUFREQ_UNSTRUCT, s)));
        sb.append(String.format(CAP_HOSTPRICE_UNSTRUCT, price));
        return sb.toString();
    }
}
