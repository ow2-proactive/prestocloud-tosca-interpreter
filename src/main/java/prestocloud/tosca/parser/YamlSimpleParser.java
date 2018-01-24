package prestocloud.tosca.parser;

import lombok.AllArgsConstructor;

import org.yaml.snakeyaml.nodes.Node;

@AllArgsConstructor
public class YamlSimpleParser<T> extends YamlParser<T> {
    private INodeParser<T> nodeParser;

    @Override
    protected INodeParser<T> getParser(Node rootNode, ParsingContextExecution context) throws ParsingException {
        return nodeParser;
    }
}