package prestocloud.tosca.parser.impl.advanced;

import org.prestocloud.tosca.normative.constants.RangeConstants;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.impl.base.ScalarParser;

@Component
public class BoundParser extends ScalarParser {

    @Override
    public String parse(Node node, ParsingContextExecution context) {
        String value = super.parse(node, context);
        if (value == null) {
            value = "";
        }
        return RangeConstants.UNBOUNDED.equalsIgnoreCase(value) ? String.valueOf(Integer.MAX_VALUE) : value;
    }
}
