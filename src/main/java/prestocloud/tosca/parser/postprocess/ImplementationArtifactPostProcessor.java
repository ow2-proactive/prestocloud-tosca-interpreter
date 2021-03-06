package prestocloud.tosca.parser.postprocess;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.impl.ErrorCode;

/**
 * Post process an implementation artifact definition.
 */
@Component
public class ImplementationArtifactPostProcessor extends AbstractArtifactPostProcessor {
    @Override
    protected void postProcessArtifactRef(Node node, String artifactReference) {
        if (artifactReference == null) {
            Node referenceNode = ParsingContextExecution.getObjectToNodeMap().get(artifactReference);
            if (referenceNode == null) {
                referenceNode = node;
            }
            ParsingContextExecution.getParsingErrors().add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Implementation artifact", referenceNode.getStartMark(),
                    "No artifact reference is defined, 'file' is mandatory in a long notation artifact definition", referenceNode.getEndMark(), null));
        }
    }
}
