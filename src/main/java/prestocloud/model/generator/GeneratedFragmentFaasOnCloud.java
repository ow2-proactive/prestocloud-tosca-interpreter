package prestocloud.model.generator;

import java.util.Optional;

public class GeneratedFragmentFaasOnCloud extends GeneratedNode {
    public final String NODE_TYPE = "cloud";

    // prestocloud.datatypes.cloud
    public String cloudType;
    public String cloudName;
    public String cloudRegions;
    public Optional<String> cloudCredentialsUsername;
    public Optional<String> cloudCredentialsPassword;
    public Optional<String> cloudCredentialsSubscription;
    public Optional<String> cloudCredentialsDomain;
    public Optional<String> cloudInstance;
    public Optional<String> cloudImage;
    public Optional<String> cpuCapacity;
    public Optional<String> memoryCapacity;
    public Optional<String> diskCapacity;
    public Optional<String> gpsCoordinate;


    @Override
    public String getStructureProcessingNode() {
        return null;
    }
}
