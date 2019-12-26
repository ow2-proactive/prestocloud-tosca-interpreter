package prestocloud.workspace;

import prestocloud.model.generator.GeneratedNode;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GeneratorSpace {

    public static String DEFAULT_HEADER = "tosca_definitions_version: tosca_prestocloud_mapping_1_2\n";
    public static String DEFAULT_DESCRIPTION = "Instance level TOSCA file, generated on " + new Date();
    private Map<String, String> metadata;
    private List<Object> cloudList;
    private List<GeneratedNode> fragments;
    private StringBuilder outputDocument;

    public GeneratorSpace() {
        outputDocument = new StringBuilder();
    }

    public void configureMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public void appendEdgeDeployedInstance(String instanceName) {

    }

    public void appendCloudDeployedInstance(String instanceName, String cloud) {

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
        outputDocument.append("description: " + this.DEFAULT_DESCRIPTION + "\n");
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
        result &= !metadata.isEmpty();
        result &= (metadata != null && !metadata.isEmpty());
        return result;
    }

}
