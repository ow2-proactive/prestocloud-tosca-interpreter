package prestocloud.json.deserializer;

import org.prestocloud.tosca.model.types.AbstractInheritableToscaType;
import org.prestocloud.tosca.model.types.NodeType;
import org.prestocloud.tosca.model.types.RelationshipType;
import prestocloud.topology.task.TopologyTask;

/**
 * Custom deserializer to handle multiple {@link AbstractInheritableToscaType} types in {@link TopologyTask}.
 */
public class TaskIndexedInheritableToscaElementDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<AbstractInheritableToscaType> {

    public TaskIndexedInheritableToscaElementDeserializer() {
        super(AbstractInheritableToscaType.class);
        addToRegistry("capabilities", NodeType.class);
        addToRegistry("validSources", RelationshipType.class);
    }
}