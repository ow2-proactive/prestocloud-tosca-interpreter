package prestocloud.tosca.parser.impl.v12.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import org.prestocloud.tosca.model.definitions.ImplementationArtifact;
import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParserUtils;
import prestocloud.tosca.parser.ParsingContextExecution;

@Component("implementationArtifactParser-v12")
public class ImplementationArtifactParser implements INodeParser<ImplementationArtifact> {

    @Override
    public ImplementationArtifact parse(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            String artifactReference = ((ScalarNode) node).getValue();
            ImplementationArtifact artifact = new ImplementationArtifact();
            artifact.setArtifactRef(artifactReference);
            return artifact;
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Artifact definition");
        }
        return null;
    }
}