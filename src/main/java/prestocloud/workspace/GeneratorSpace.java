package prestocloud.workspace;

import net.minidev.json.JSONObject;
import prestocloud.btrplace.tosca.model.EdgeResourceTemplateDetails;
import prestocloud.btrplace.tosca.model.RegionCapacityDescriptor;
import prestocloud.btrplace.tosca.model.VMTemplateDetails;
import prestocloud.model.generator.CloudListRegistration;
import prestocloud.model.generator.GeneratedFragmentFaasOnCloud;
import prestocloud.model.generator.GeneratedFragmentFaasOnEdge;
import prestocloud.model.generator.GeneratedNode;

import java.util.*;
import java.util.stream.Collectors;

public class GeneratorSpace {

    public static final String DEFAULT_HEADER = "tosca_definitions_version: tosca_prestocloud_mapping_1_2\n";
    public static final String DEFAULT_DESCRIPTION = "Instance level TOSCA file, generated on " + new Date();
    private Map<String, String> metadata;
    private List<EdgeResourceTemplateDetails> edgeResourceDetails;
    private List<VMTemplateDetails> vmTemplateDetailsList;
    private List<CloudListRegistration> cloudList;
    private List<GeneratedNode> fragments;
    private Map<String, RegionCapacityDescriptor> rcdPerRegion;
    private StringBuilder outputDocument;

    public GeneratorSpace() {
        cloudList = new ArrayList<>();
        fragments = new ArrayList<>();
        outputDocument = new StringBuilder();
    }

    public void configureMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public void configurationCloudList(List<Object> ja) {
        ja.forEach(o -> cloudList.add(new CloudListRegistration(((JSONObject) o))));
    }

    public void configureEdgeResourceTemplateDetails(List<EdgeResourceTemplateDetails> ertd) {
        this.edgeResourceDetails = ertd;
    }

    public void configureVmTemplateDetailList(List<VMTemplateDetails> vmtpllist) {
        this.vmTemplateDetailsList = vmtpllist;
    }

    public void configureRcdPerRegion(Map rcdPerRegion) {
        this.rcdPerRegion = rcdPerRegion;
    }

    public void appendEdgeDeployedInstance(String instanceName, String fragmentId, String edgeId, boolean isALb) {
        // Determining the right ertd
        Optional<EdgeResourceTemplateDetails> ertd = edgeResourceDetails.parallelStream().filter(edgeResourceTemplateDetails -> edgeResourceTemplateDetails.id.equals(edgeId)).findAny();
        if (ertd.isPresent()) {
            GeneratedFragmentFaasOnEdge result = new GeneratedFragmentFaasOnEdge(instanceName, fragmentId, edgeId, isALb, ertd.get());
            fragments.add(result);
        } else {
            throw new IllegalStateException(String.format("Instance %s : Unable to retrieve the edge device specification for the specified Id.", instanceName));
        }
    }

    public void appendCloudDeployedInstance(String instanceName, String fragmentId, String cloud, String region, String instanceType, boolean isALb) {
        Optional<CloudListRegistration> clr = cloudList.parallelStream().filter(cloudRegistration -> (cloudRegistration.getRegion().equals(region) && cloudRegistration.getInstanceType().equals(instanceType) && cloudRegistration.getCloudType().equals(cloud))).findAny();
        RegionCapacityDescriptor rcd = rcdPerRegion.get(String.format("%s %s", cloud, region));
        Optional<VMTemplateDetails> vmt = vmTemplateDetailsList.parallelStream().filter(vmTemplateDetails -> (vmTemplateDetails.cloud.equals(cloud) && vmTemplateDetails.region.equals(region) && vmTemplateDetails.getInstanceName().equals(instanceType))).findAny();
        if (clr.isPresent() && rcd != null && vmt.isPresent()) {
            GeneratedFragmentFaasOnCloud result = new GeneratedFragmentFaasOnCloud(instanceName, fragmentId, isALb, clr.get(), rcd, vmt.get());
            fragments.add(result);
        } else {
            // Something has gone wrong. What happened ?
            throw new IllegalStateException(String.format("Instance %s: Unable to retrieve cloud specification from specified cloud. CloudList is valid = %s , RegionCapacityDescriptor is valid = %s, VMTemplateDetails = %s", instanceName, clr.isPresent(), rcd != null, vmt.isPresent()));
        }
    }

    public String generate() {
        if (!checkConfiguration()) {
            throw new IllegalArgumentException("Instance TOSCA generator is configured with wrong parameters");
        }
        writeHeader();
        writeMetadata();
        writeDescription();
        writeImport();
        writeDocumentStucture();
        writeProcessingNode();
        writeDeploymentNode();
        writeFaasNode();
        return outputDocument.toString();
    }

    private void writeHeader() {
        outputDocument.append(DEFAULT_HEADER);
        outputDocument.append("\n");
    }

    private void writeMetadata() {
        outputDocument.append("metadata:\n");
        metadata.entrySet().stream().map(metadataEntry -> String.format("   %s: %s", metadataEntry.getKey(), metadataEntry.getValue())).collect(Collectors.joining("\n"));
        outputDocument.append("\n");
    }

    private void writeDescription() {
        outputDocument.append("description: " + DEFAULT_DESCRIPTION + "\n");
    }

    private void writeImport() {
        outputDocument.append("imports:\n");
        outputDocument.append("   - tosca-normative-types:1.2\n" +
                "   - iccs-normative-types:1.1\n" +
                "   - resource-descriptions:1.0\n" +
                "   - placement-constraints:1.0\n");
    }

    private void writeDocumentStucture() {
        outputDocument.append("topology_template:\n   node_templates:\n\n");
    }

    private void writeProcessingNode() {
        for (GeneratedNode node : fragments) {
            outputDocument.append(node.getStructureProcessingNode());
        }
    }

    private void writeDeploymentNode() {
        for (GeneratedNode node : fragments) {
            outputDocument.append(node.getStructureDeploymentNode());
        }
    }

    private void writeFaasNode() {
        for (GeneratedNode node : fragments) {
            outputDocument.append(node.getStructureFragmentNode());
        }
    }

    private boolean checkConfiguration() {
        boolean result = true;
        result &= (metadata != null && !metadata.isEmpty());
        result &= (cloudList != null);
        result &= (vmTemplateDetailsList != null && !vmTemplateDetailsList.isEmpty());
        result &= (edgeResourceDetails != null && !edgeResourceDetails.isEmpty());
        result &= (rcdPerRegion != null && !rcdPerRegion.isEmpty());
        return result;
    }

}
