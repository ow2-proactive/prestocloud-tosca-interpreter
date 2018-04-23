package prestocloud.tosca.parser.impl.advanced;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.prestocloud.tosca.model.templates.NodeGroup;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import com.google.common.collect.Maps;

import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParserUtils;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.ParsingErrorLevel;
import prestocloud.tosca.parser.impl.ErrorCode;
import prestocloud.tosca.parser.impl.base.ScalarParser;

@Component
public class PolicyGroupParser implements INodeParser<NodeGroup> {

    @Resource
    private ScalarParser scalarParser;

    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String VALUE = "value";

    private static final Map<String, Class<? extends NodeGroup>> NODE_GROUPS = Maps.newLinkedHashMap();

    /*static {
        NODE_GROUPS.put(HaPolicy.HA_POLICY, HaPolicy.class);
    }*/

    @Override
    public NodeGroup parse(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            // Spec at A.8.1.5.1 says it is a "list of names of policies"
            // though the examples treat it as maps, and in some cases it seems the type might be specified;
            // accept all syntaxes for now
            Map<String, Object> nodeMap = Maps.newHashMap();
            String name = scalarParser.parse(node, context);
            nodeMap.put(NAME, name);
            return buildGroup(nodeMap, node, context);
        }

        if (!(node instanceof MappingNode)) {
            // we expect a MappingNode
            context.getParsingErrors().add(new ParsingError(ErrorCode.YAML_MAPPING_NODE_EXPECTED, null, node.getStartMark(), null, node.getEndMark(), null));
            return null;
        }
        Map<String, Object> nodeMap = ParserUtils.parseMap((MappingNode) node);

        Object nameO = (Object) nodeMap.get(NAME);
        Object typeO = (Object) nodeMap.get(TYPE);

        if (nodeMap.size() == 1 && nameO == null && typeO == null) {
            // short notation '<key>: <value>' where (in priority order)
            // - <value> is a map and <key> matches a known pre-defined type, then <value> is a map of data passed to the type
            // - <value> matches a known pre-defined type, then <key> is taken as a name
            // - else taken as a generic policy with name <key>, with <value> set as the map (if it's a map) or as a `value` in the map (if it's not a map)
            Entry<String, Object> e = nodeMap.entrySet().iterator().next();

            nodeMap.clear();
            if (e.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> v = (Map<String, Object>) e.getValue();
                nodeMap.putAll(v);
                nodeMap.put(NAME, e.getKey());
            } else if (e.getValue() instanceof CharSequence) {
                nodeMap.put(NAME, e.getKey());
                nodeMap.put(VALUE, e.getValue().toString());
            }
        } else {
            if (!(nameO instanceof String)) {
                context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.SYNTAX_ERROR, null, node.getStartMark(), null,
                        node.getEndMark(), nameO.toString()));
                return null;
            }
            if (!(typeO instanceof String)) {
                context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.SYNTAX_ERROR, null, node.getStartMark(), null,
                        node.getEndMark(), nameO.toString()));
                return null;
            }
        }

        return buildGroup(nodeMap, node, context);
    }

    private NodeGroup buildGroup(Map<String, Object> nodeMap, Node node, ParsingContextExecution context) {
        String type = (String) nodeMap.get(TYPE);
        return new NodeGroup();
    }

}