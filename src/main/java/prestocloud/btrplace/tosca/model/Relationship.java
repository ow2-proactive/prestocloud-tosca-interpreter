package prestocloud.btrplace.tosca.model;

import java.util.List;

public interface Relationship {
    String getFragmentName();
    ConstrainedNode getHostingNode();
    List<ConstrainedNode> getAllConstrainedNodes();
}
