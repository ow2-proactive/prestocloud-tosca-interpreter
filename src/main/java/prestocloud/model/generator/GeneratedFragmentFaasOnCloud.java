package prestocloud.model.generator;

import prestocloud.btrplace.tosca.model.RegionCapacityDescriptor;
import prestocloud.btrplace.tosca.model.VMTemplateDetails;

import java.util.Optional;

public class GeneratedFragmentFaasOnCloud extends GeneratedNode {

    protected static final String HEADER_UNSTRUCT = "      processing_node_%s:\n" +
            "         type: prestocloud.nodes.compute.cloud.%s\n" +
            "         properties:\n" +
            "            id: %s\n" +
            "            type: cloud\n";
    protected static final String HEADER_NAME_UNSTRUCT = "            name: %s\n";

    // Cloud properties
    protected static final String CLOUD_UNSTRUCT = "            cloud:\n" +
            "               cloud_name: %s\n" +
            "               cloud_type: %s\n" +
            "               cloud_region: %s\n" +
            "               cloud_instance: %s\n";

    protected static final String CAP_RES_UNSTRUCT = "         capabilities:\n" +
            "            resource:\n" +
            "               properties:\n" +
            "                  type: cloud\n" +
            "                  cloud:\n" +
            "                     cloud_name: %s\n" +
            "                     cloud_type: %s\n" +
            "                     cloud_region: %s\n" +
            "                     cloud_instance: %s\n";
    protected static final String CAP_HOST_UNSTRUCT = "            host:\n" +
            "               properties:\n" +
            "                  num_cpus: %s\n" +
            "                  mem_size: %s\n";
    protected static final String CAP_HOSTDISKSIZE_UNSTRUCT = "                  disk_size: %s\n";
    protected static final String CAP_HOSTCPUFREQ_UNSTRUCT = "                  cpu_frequency: %s\n";
    protected static final String CAP_HOSTPRICE_UNSTRUCT = "                  price: %s\n";
    protected static final String NETWORK_UNSTRUCT = "            network:\n" +
            "               network_name: %s\n" +
            "               network_id: %s\n" +
            "               addresses:\n" +
            "                  - @variables_network_%s\n";

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

    public GeneratedFragmentFaasOnCloud(String fragmentName, String fragmentId, boolean isALb, CloudListRegistration clr, RegionCapacityDescriptor rcd, VMTemplateDetails vmt) {
        //General
        this.isALoadBalancer = isALb;
        this.computeId = String.format("%s:%s:%s", clr.getCloudType(), clr.getRegion(), clr.getInstanceType());
        this.computeName = Optional.of(clr.getCloudName());

        // Host Resource
        this.cpuFrequency = (vmt.cpuFreq != null) ? Optional.of(vmt.cpuFreq) : Optional.empty();
        this.diskSize = (vmt.diskSize != null) ? Optional.of(vmt.diskSize) : Optional.empty();
        this.numCpus = Integer.parseInt(vmt.getCpuNum());
        this.memSize = vmt.getMem();
        this.price = vmt.getPrice();

        //Compute resource - Network
        // We have no access to any cloud resource from here
        this.networkId = clr.getSubnetId();
        this.networkName = clr.getNetworkName();

        // Fragment
        this.id = fragmentId;
        this.fragmentName = fragmentName;
        this.onLoadable = false;
        this.proxyFragment = Optional.empty();

        // Cloud specific
        cloudType = clr.getCloudType();
        cloudName = clr.getCloudName();
        cloudRegions = clr.getRegion();
        cloudCredentialsUsername = Optional.of(clr.getAccessKey());
        cloudCredentialsPassword = Optional.empty();
        cloudCredentialsSubscription = Optional.empty();
        cloudCredentialsDomain = Optional.empty();
        cloudInstance = Optional.of(clr.getInstanceType());
        cloudImage = Optional.of(clr.getImage());
        cpuCapacity = (rcd.getCpuCapacity() == RegionCapacityDescriptor.INFINITY) ? Optional.empty() : Optional.of("" + rcd.getCpuCapacity());
        memoryCapacity = Optional.empty();
        diskCapacity = Optional.empty();
        gpsCoordinate = Optional.of(vmt.getGeolocation());
    }

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
        sb.append("\n\n");
        return sb.toString();
    }
}
