package prestocloud.json.deserializer;

import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.prestocloud.tosca.model.templates.ServiceNodeTemplate;

/**
 * Custom deserializer to handle multiple IOperationParameter types.
 */
public class NodeTemplateDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<NodeTemplate> {

    public NodeTemplateDeserializer() {
        super(NodeTemplate.class);
        addToRegistry("serviceResourceId", ServiceNodeTemplate.class);
        setDefaultClass(NodeTemplate.class);
    }

}