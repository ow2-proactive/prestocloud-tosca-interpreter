package prestocloud.tosca.parser.impl.advanced;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import prestocloud.tosca.parser.impl.base.BaseParserFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import org.prestocloud.tosca.model.definitions.LowerBoundedDefinition;
import org.prestocloud.tosca.model.definitions.UpperBoundedDefinition;
import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.impl.ErrorCode;
import prestocloud.tosca.parser.impl.base.ListParser;

@Component
public class OccurrencesParser implements INodeParser<List<String>> {
    @Resource
    private BaseParserFactory baseParserFactory;
    @Resource
    private BoundParser boundParser;

    private ListParser<String> listParser;

    @PostConstruct
    public void init() {
        listParser = baseParserFactory.getListParser(boundParser, "string");
    }

    @Override
    public List<String> parse(Node node, ParsingContextExecution context) {
        Object parent = context.getParent();
        List<String> result = (List<String>) listParser.parse(node, context);
        if (result.size() != 2) {
            context.getParsingErrors().add(new ParsingError(ErrorCode.SYNTAX_ERROR, null, node.getStartMark(), null, node.getEndMark(), null));
            return result;
        }
        if (parent instanceof LowerBoundedDefinition) {
            ((LowerBoundedDefinition) parent).setLowerBound(Integer.parseInt(result.get(0)));
        }
        if (parent instanceof UpperBoundedDefinition) {
            ((UpperBoundedDefinition) parent).setUpperBound(Integer.parseInt(result.get(1)));
        }
        return result;
    }

}
