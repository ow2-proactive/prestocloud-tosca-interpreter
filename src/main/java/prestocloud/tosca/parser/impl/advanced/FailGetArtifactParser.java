package prestocloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.ParsingErrorLevel;
import prestocloud.tosca.parser.impl.ErrorCode;

/**
 * Parser that state that get_artifact is not supported and will be ignored.
 */
@Component
public class FailGetArtifactParser implements INodeParser<Object> {
    @Override
    public Object parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            NodeTuple nodeTuple = ((MappingNode) node).getValue().get(0);
            if (nodeTuple.getKeyNode() instanceof ScalarNode) {
                String key = ((ScalarNode) nodeTuple.getKeyNode()).getValue();
                context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRECOGNIZED_PROPERTY, "Ignored field during import",
                        nodeTuple.getKeyNode().getStartMark(), "tosca key is not recognized", nodeTuple.getValueNode().getEndMark(), key));
            }
        }
        return null;
    }
}