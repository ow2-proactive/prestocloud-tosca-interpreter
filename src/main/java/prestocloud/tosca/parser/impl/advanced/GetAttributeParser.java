package prestocloud.tosca.parser.impl.advanced;

import org.prestocloud.tosca.model.definitions.FunctionPropertyValue;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParsingContextExecution;

/**
 * Specific get_attribute tosca
 * { get_attribute: [TARGET, protocol] }: fetch from the node
 * { get_attribute: [TARGET, protocol] }: fetch from the capability.
 */
@Component
public class GetAttributeParser implements INodeParser<FunctionPropertyValue> {

    @Override
    public FunctionPropertyValue parse(Node node, ParsingContextExecution context) {
        FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) ParsingContextExecution.get().getRegistry().get("tosca_function").parse(node,
                context);
        return functionPropertyValue;
    }
}