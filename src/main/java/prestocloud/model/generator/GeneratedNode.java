package prestocloud.model.generator;

import java.util.List;
import java.util.Optional;

public abstract class GeneratedNode {

    private static final String DEPLOYMENT_UNSTRUCT = "   # Deployment for %s fragment\n" +
            "   deployment_node_%s:\n" +
            "      type: prestocloud.nodes.%s.faas\n" +
            "      requirements:\n" +
            "         - host: processing_node_%s\n\n";

    protected static final String FRAGMENT_UNSTRUCTURED = "   %s:\n" +
            "      type: prestocloud.nodes.fragment.faas\n" +
            "      properties:\n" +
            "         id: %s\n" +
            "         name: %s\n" +
            "         onloadable: %s\n" +
            "      requirements:\n" +
            "         - execute: deployment_node_%s\n";

    protected boolean isALoadBalancer;
    protected String computeId;
    protected Optional<String> computeName;

    // Host resource
    protected int numCpus;
    protected String memSize;
    protected Optional<String> diskSize;
    protected Optional<String> cpuFrequency;
    protected int price;

    // Compute Resource - Networks
    protected String networkId;
    protected String networkName;
    protected List<String> addresses;
    // Processing - TOSCA type: prestocloud.nodes.proxy.faas
    // Nothing specific

    //Fragment FaaS - TOSCA type: prestocloud.nodes.fragment.faas
    protected String id;
    protected String fragmentName;
    protected boolean onLoadable;
    protected Optional<String> proxyFragment;

    public abstract String getStructureProcessingNode();

    public String getStructureDeploymentNode() {
        return String.format(DEPLOYMENT_UNSTRUCT, fragmentName, fragmentName, isALoadBalancer ? "proxy" : "agent", fragmentName);
    }

    public String getStructureFragmentNode() {
        String formattedText = String.format(FRAGMENT_UNSTRUCTURED, fragmentName, id, fragmentName, onLoadable, fragmentName);
        return (proxyFragment.isPresent()) ? String.format("%s         - proxy: processing_node_%s\n\n", formattedText, proxyFragment.get()) : formattedText + "\n";
    }
}
