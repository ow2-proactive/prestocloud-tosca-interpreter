package prestocloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import org.prestocloud.tosca.model.definitions.ImplementationArtifact;
import prestocloud.tosca.parser.ParsingContextExecution;

@Deprecated
@Component
public class ImplementationArtifactParser extends ArtifactParser<ImplementationArtifact> {
    @Override
    public ImplementationArtifact parse(Node node, ParsingContextExecution context) {
        return doParse(new ImplementationArtifact(), node, context);
    }
}
