package prestocloud.btrplace.tosca.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeConstraints {

    @Getter
    @Setter
    public Map<String, List<String>> hostingConstraints;
    @Getter @Setter
    public Map<String, List<String>> resourceConstraints;
    @Getter @Setter
    public Map<String, List<String>> osConstraints;

    public NodeConstraints() {
        hostingConstraints = new HashMap<>();
        resourceConstraints = new HashMap<>();
        osConstraints = new HashMap<>();
    }

    public void addHostingConstraint(String name, List<String> hostingConstraint) {
        hostingConstraints.put(name, hostingConstraint);
    }

    public void addResourceConstraint(String name, List<String> resourceConstraint) {
        resourceConstraints.put(name, resourceConstraint);
    }

    public void addOSConstraint(String name, List<String> osConstraint) {
        osConstraints.put(name, osConstraint);
    }
}
