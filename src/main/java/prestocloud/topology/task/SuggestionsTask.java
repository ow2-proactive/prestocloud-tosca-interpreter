package prestocloud.topology.task;

import org.prestocloud.tosca.model.types.NodeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionsTask extends TopologyTask {
    // Array of suggested non abstract node types
    private NodeType[] suggestedNodeTypes;
}
