package prestocloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;
import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParsingContextExecution;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AbstractPropertyValueParser implements INodeParser<AbstractPropertyValue> {
    @Override
    public AbstractPropertyValue parse(Node node, ParsingContextExecution context) {
        String parserName = node instanceof ScalarNode ? "scalar_property_value" : "complex_property_value";
        INodeParser<AbstractPropertyValue> paser = ParsingContextExecution.getRegistry().get(parserName);
        return paser.parse(node, context);
    }
}