package prestocloud.tosca.parser.mapping.generator;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;

import com.google.common.collect.Maps;

import prestocloud.tosca.parser.MappingTarget;
import prestocloud.tosca.parser.ParserUtils;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.impl.base.BaseParserFactory;

/**
 * Mapping of a type reference.
 */
@Component
public class ReferenceMappingBuilder implements IMappingBuilder {
    private static final String REFERENCE_KEY = "reference";

    private static final String TYPE = "type";

    @Resource
    private BaseParserFactory baseParserFactory;

    @Override
    public String getKey() {
        return REFERENCE_KEY;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        Map<String, String> map = Maps.newHashMap();
        for (NodeTuple tuple : mappingNode.getValue()) {
            String key = ParserUtils.getScalar(tuple.getKeyNode(), context);
            String value = ParserUtils.getScalar(tuple.getValueNode(), context);
            map.put(key, value);
        }
        return new MappingTarget(map.get(REFERENCE_KEY), baseParserFactory.getReferencedParser(map.get(TYPE)));
    }
}