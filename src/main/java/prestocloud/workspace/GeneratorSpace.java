package prestocloud.workspace;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GeneratorSpace {

    public static String DEFAULT_HEADER = "tosca_definitions_version: tosca_prestocloud_mapping_1_2\n";
    public static String DEFAULT_DESCRIPTION = "Instance level TOSCA file, generated on " + new Date();
    private Map<String, String> metadata;
    private List<Object> cloudList;
    private StringBuilder outPutDocument;

    public GeneratorSpace() {
        outPutDocument = new StringBuilder();
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
        return outPutDocument.toString();
    }

    private void writeHeader() {
        outPutDocument.append(DEFAULT_HEADER);
        outPutDocument.append("\n");
    }

    private void writeMetadata() {
        outPutDocument.append("metadata:\n");
        metadata.entrySet().stream().map(metadataEntry -> String.format("   %s: %s", metadataEntry.getKey(), metadataEntry.getValue())).collect(Collectors.joining("\n"));
        outPutDocument.append("\n");
    }

    private void writeDescription() {
        outPutDocument.append("description: " + this.DEFAULT_DESCRIPTION + "\n");
    }

    private void writeImport() {
        outPutDocument.append("imports:\n");
        outPutDocument.append("   - tosca-normative-types:1.2\n" +
                "   - iccs-normative-types:1.1\n" +
                "   - resource-descriptions:1.0\n" +
                "   - placement-constraints:1.0\n");
    }

    private void writeDocumentStucture() {
        outPutDocument.append("topology_template:\n   node_templates:\n\n");
    }

    private boolean checkConfiguration() {
        boolean result = true;
        result &= !metadata.isEmpty();
        result &= (metadata != null && !metadata.isEmpty());
        return result;
    }

}
