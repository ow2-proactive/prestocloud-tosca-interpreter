package prestocloud.tosca.parser.impl.advanced;

import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;
import org.prestocloud.tosca.model.definitions.ComplexPropertyValue;
import org.prestocloud.tosca.model.definitions.ListPropertyValue;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.SequenceNode;

import prestocloud.exceptions.InvalidArgumentException;
import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParserUtils;
import prestocloud.tosca.parser.ParsingContextExecution;

@Component
public class ComplexPropertyValueParser implements INodeParser<AbstractPropertyValue> {

    @Override
    public AbstractPropertyValue parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            return new ComplexPropertyValue(ParserUtils.parseMap((MappingNode) node));
        } else if (node instanceof SequenceNode) {
            return new ListPropertyValue(ParserUtils.parseSequence((SequenceNode) node));
        } else {
            throw new InvalidArgumentException("Do not expect other node than MappingNode or SequenceNode here " + node.getClass().getName());
        }
    }
}
