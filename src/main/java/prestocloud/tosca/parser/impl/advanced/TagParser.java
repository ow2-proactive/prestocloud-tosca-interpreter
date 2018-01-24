package prestocloud.tosca.parser.impl.advanced;

import java.util.List;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import com.google.common.collect.Lists;

import prestocloud.model.common.Tag;
import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParserUtils;
import prestocloud.tosca.parser.ParsingContextExecution;

@Component
public class TagParser implements INodeParser<List<Tag>> {

    @Override
    public List<Tag> parse(Node node, ParsingContextExecution context) {
        List<Tag> tagList = Lists.newArrayList();
        if (node instanceof MappingNode) {
            MappingNode mapNode = (MappingNode) node;
            for (NodeTuple entry : mapNode.getValue()) {
                String key = ParserUtils.getScalar(entry.getKeyNode(), context);
                String value = ParserUtils.getScalar(entry.getValueNode(), context);
                if (value != null) {
                    tagList.add(new Tag(key, value));
                }
            }
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "metadata");
        }
        return tagList;
    }
}