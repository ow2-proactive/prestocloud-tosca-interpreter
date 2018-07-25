package prestocloud.tosca.parser.postprocess;

import static prestocloud.utils.PrestocloudUtils.safe;

import javax.annotation.Resource;

import org.prestocloud.tosca.model.templates.PolicyTemplate;
import org.prestocloud.tosca.model.templates.Topology;
import org.prestocloud.tosca.model.types.PolicyType;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import prestocloud.tosca.context.ToscaContext;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.ParsingErrorLevel;
import prestocloud.tosca.parser.impl.ErrorCode;
import prestocloud.tosca.topology.TemplateBuilder;

/**
 * Post process a node template
 */
@Component
public class PolicyTemplatePostProcessor implements IPostProcessor<PolicyTemplate> {
    @Resource
    private ReferencePostProcessor referencePostProcessor;
    @Resource
    private PropertyValueChecker propertyValueChecker;

    @Override
    public void process(final PolicyTemplate instance) {
        // ensure type exists
        referencePostProcessor.process(new ReferencePostProcessor.TypeReference(instance, instance.getType(), PolicyType.class));
        final PolicyType policyType = ToscaContext.get(PolicyType.class, instance.getType());
        if (policyType == null) {
            return; // error managed by the reference post processor.
        }

        final Topology topology = ((ArchiveRoot) ParsingContextExecution.getRoot().getWrappedInstance()).getTopology();

        safe(instance.getTargets()).forEach(target -> {
            if (!safe((topology.getNodeTemplates())).containsKey(target)) {
                // Dispatch an error.
                Node node = ParsingContextExecution.getObjectToNodeMap().get(instance.getTargets());
                ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.POLICY_TARGET_NOT_FOUND, instance.getName(),
                        node.getStartMark(), "The target " + target + " is not found", node.getEndMark(), target));
            }
            else if (policyType.getTargets() == null) {
                // Dispatch an error
                Node node = ParsingContextExecution.getObjectToNodeMap().get(instance.getName());
                ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.INVALID_POLICY_TARGET, instance.getName(),
                        node.getStartMark(), "The target " + target + " does not match with 'null' target types: ", node.getEndMark(), target));
            }
            else {
                // Check that the policy targets match with node template target type or their derived (derived naming should match parent name subparts)
                if (!policyType.getTargets().contains(safe((topology.getNodeTemplates())).get(target).getType()) && policyType.getTargets().stream().noneMatch(policyTypeTarget -> safe((topology.getNodeTemplates())).get(target).getType().startsWith(policyTypeTarget))) {
                    Node node = ParsingContextExecution.getObjectToNodeMap().get(instance.getTargets());
                    ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.INVALID_POLICY_TARGET, instance.getName(),
                            node.getStartMark(), "The target " + target + " does not match with required target types: " + policyType.getTargets(), node.getEndMark(), target));
                }
            }
        });

        // Merge the policy template with data coming from the type (default values etc.).
        PolicyTemplate tempObject = TemplateBuilder.buildPolicyTemplate(policyType, instance, false);
        instance.setProperties(tempObject.getProperties());

        propertyValueChecker.checkProperties(policyType, instance.getProperties(), instance.getName());
    }
}