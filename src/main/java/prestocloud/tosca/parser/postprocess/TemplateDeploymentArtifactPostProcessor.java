package prestocloud.tosca.parser.postprocess;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.ParsingErrorLevel;
import prestocloud.tosca.parser.impl.ErrorCode;

/**
 * Specific post processor that manages errors for node template deployment artifacts.
 */
@Component
public class TemplateDeploymentArtifactPostProcessor extends AbstractArtifactPostProcessor {
    @Override
    protected void postProcessArtifactRef(Node node, String artifactReference) {
        if (artifactReference == null) {
            Node referenceNode = ParsingContextExecution.getObjectToNodeMap().get(artifactReference);
            if (referenceNode == null) {
                referenceNode = node;
            }
            ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRESOLVED_ARTIFACT, "Deployment artifact",
                    node.getStartMark(), "No artifact reference is defined, user will have to define / override in order to make ", node.getEndMark(), null));
        }
    }
}