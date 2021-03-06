package prestocloud.tosca.parser.impl.advanced;

import java.util.Map;

import javax.annotation.Resource;

import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;
import org.prestocloud.tosca.model.definitions.Interface;
import org.prestocloud.tosca.model.templates.RelationshipTemplate;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParserUtils;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.ParsingErrorLevel;
import prestocloud.tosca.parser.impl.ErrorCode;
import prestocloud.tosca.parser.impl.base.BaseParserFactory;
import prestocloud.tosca.parser.impl.base.MapParser;
import prestocloud.tosca.parser.impl.base.ScalarParser;

/**
 * Parse a relationship
 */
@Component("relationshipTemplateParser")
public class RelationshipTemplateParser implements INodeParser<RelationshipTemplate> {
    @Resource
    private ScalarParser scalarParser;
    @Resource
    private BaseParserFactory baseParserFactory;

    @Override
    public RelationshipTemplate parse(Node node, ParsingContextExecution context) {
        // To parse a relationship template we actually get the parent node to retrieve the requirement name;
        if (!(node instanceof MappingNode) || ((MappingNode) node).getValue().size() != 1) {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Requirement assignment");
        }
        MappingNode assignmentNode = (MappingNode) node;
        RelationshipTemplate relationshipTemplate = new RelationshipTemplate();
        String relationshipId = scalarParser.parse(assignmentNode.getValue().get(0).getKeyNode(), context);
        // The relationship's id which is used to identify the relationship within the source
        relationshipTemplate.setName(relationshipId);
        // By default the relationship id is the requirement name, it can be overridden with 'type_requirement'
        relationshipTemplate.setRequirementName(relationshipId);
        // Now parse the content of the relationship assignment.
        node = assignmentNode.getValue().get(0).getValueNode();
        if (node instanceof ScalarNode) { // Short notation (host: compute)
            relationshipTemplate.setTarget(scalarParser.parse(node, context));
        } else if (node instanceof MappingNode) {

            MappingNode mappingNode = (MappingNode) node;
            for (NodeTuple nodeTuple : mappingNode.getValue()) {
                String key = scalarParser.parse(nodeTuple.getKeyNode(), context);
                switch (key) {
                case "node":
                    relationshipTemplate.setTarget(scalarParser.parse(nodeTuple.getValueNode(), context));
                    break;
                case "capability":
                    relationshipTemplate.setTargetedCapabilityName(scalarParser.parse(nodeTuple.getValueNode(), context));
                    break;
                case "type_requirement":
                    relationshipTemplate.setRequirementName(scalarParser.parse(nodeTuple.getValueNode(), context));
                    break;
                case "relationship":
                    relationshipTemplate.setType(scalarParser.parse(nodeTuple.getValueNode(), context));
                    break;
                case "properties":
                    INodeParser<AbstractPropertyValue> propertyValueParser = context.getRegistry().get("node_template_property");
                    MapParser<AbstractPropertyValue> mapParser = baseParserFactory.getMapParser(propertyValueParser, "node_template_property");
                    relationshipTemplate.setProperties(mapParser.parse(nodeTuple.getValueNode(), context));
                    break;
                case "interfaces":
                    INodeParser<Map<String, Interface>> interfacesParser = context.getRegistry().get("interfaces");
                    relationshipTemplate.setInterfaces(interfacesParser.parse(nodeTuple.getValueNode(), context));
                    break;
                default:
                    context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNKNOWN_ARTIFACT_KEY, null, node.getStartMark(),
                            "Unrecognized key while parsing implementation artifact", node.getEndMark(), key));
                }
            }
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Requirement assignment");
        }

        return relationshipTemplate;
    }
}