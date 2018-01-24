package prestocloud.tosca.parser.postprocess;

import static prestocloud.utils.AlienUtils.safe;

import java.util.Objects;

import javax.annotation.Resource;

import org.prestocloud.tosca.model.definitions.Operation;
import org.prestocloud.tosca.model.types.AbstractInstantiableToscaType;
import org.springframework.stereotype.Component;

/**
 * Performs validation of a type with artifacts.
 */
@Component
public class ToscaArtifactTypePostProcessor implements IPostProcessor<AbstractInstantiableToscaType> {
    @Resource
    private TypeDeploymentArtifactPostProcessor typeDeploymentArtifactPostProcessor;
    @Resource
    private ImplementationArtifactPostProcessor implementationArtifactPostProcessor;

    @Override
    public void process(AbstractInstantiableToscaType instance) {
        safe(instance.getArtifacts()).values().forEach(typeDeploymentArtifactPostProcessor);
        // TODO Manage interfaces inputs to copy them to all operations.
        safe(instance.getInterfaces()).values().stream().flatMap(anInterface -> safe(anInterface.getOperations()).values().stream())
                .map(Operation::getImplementationArtifact).filter(Objects::nonNull).forEach(implementationArtifactPostProcessor);
    }
}