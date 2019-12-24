package prestocloud.model.generator;

import java.util.List;
import java.util.Optional;

public abstract class GeneratedNode {

    private static final String deploymentStructured = "   # Deployment for %s fragment\n" +
            "   deployment_node_%s:\n" +
            "      type: prestocloud.nodes.%s.faas\n" +
            "      requirements:\n" +
            "         - host: processing_node_%s\n\n";

    protected static final String fragmentUnstructured = "   %s:\n" +
            "      type: prestocloud.nodes.fragment.faas\n" +
            "      properties:\n" +
            "         id: %s\n" +
            "         name: %s\n" +
            "         onloadable: %s\n" +
            "      requirements:\n" +
            "         - execute: deployment_node_%s\n";

    public boolean isALoadBalancer;

    // Host resource
    public int numCpus;
    public String memSize;
    public Optional<String> diskSize;
    public Optional<String> cpuFrequency;
    public int price;

    // Compute Resource - Networks
    public String networkId;
    public String networkName;
    public List<String> addresses;
    // Processing - TOSCA type: prestocloud.nodes.proxy.faas
    // Nothing specific

    //Fragment FaaS - TOSCA type: prestocloud.nodes.fragment.faas
    public String id;
    public String fragmentName;
    public boolean onLoadable;
    public Optional<String> proxyFragment;

    public abstract String getStructureProcessingNode();

    public String getStructureDeploymentNode() {
        return String.format(deploymentStructured, fragmentName, fragmentName, isALoadBalancer ? "proxy" : "agent", fragmentName);
    }

    public String getStructureFragmentNode() {
        String formattedText = String.format(fragmentUnstructured, fragmentName, id, fragmentName, onLoadable, fragmentName);
        return (proxyFragment.isPresent()) ? String.format(formattedText + "         - proxy: processing_nde_%s\n\n", proxyFragment.get()) : formattedText + "\n";
    }
}
