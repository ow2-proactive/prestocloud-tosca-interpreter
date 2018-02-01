package org.prestocloud.tosca.model.templates;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A node group is a group of nodes in a topology. All members share the same policies.
 */
@Getter
@Setter
@NoArgsConstructor
public class NodeGroup {

    private String name;

    private Set<String> members;

    /**
     * The group index for a given topology.
     */
    private int index;

    private List<AbstractPolicy> policies;

}
