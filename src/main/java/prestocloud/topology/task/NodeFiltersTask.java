package prestocloud.topology.task;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodeFiltersTask extends TopologyTask {
    // list of node filter with not satisfy
    private List<NodeFilterToSatisfy> nodeFiltersToSatisfy;
}
