package prestocloud.tosca.parser.impl.base;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParserUtils;
import prestocloud.tosca.parser.ParsingContextExecution;

/**
 * Very simple scalar parser that just returns the value as string.
 */
@Component
public class ScalarParser implements INodeParser<String> {
    @Override
    public String parse(Node node, ParsingContextExecution context) {
        return ParserUtils.getScalar(node, context);
    }
}