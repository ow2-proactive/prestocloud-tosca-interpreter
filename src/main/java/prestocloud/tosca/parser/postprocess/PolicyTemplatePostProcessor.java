package prestocloud.tosca.parser.postprocess;

import static prestocloud.utils.AlienUtils.safe;

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

        // check that the targets are exiting node templates
        // TODO should we also check the type of the target, see if it matches with one provided in PolicyType.getTargets() ?
        safe(instance.getTargets()).forEach(target -> {
            if (!safe((topology.getNodeTemplates())).containsKey(target)) {
                // Dispatch an error.
                Node node = ParsingContextExecution.getObjectToNodeMap().get(instance.getTargets());
                ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.POLICY_TARGET_NOT_FOUND, instance.getName(),
                        node.getStartMark(), null, node.getEndMark(), target));
            }
        });

        // Merge the policy template with data coming from the type (default values etc.).
        PolicyTemplate tempObject = TemplateBuilder.buildPolicyTemplate(policyType, instance, false);
        instance.setProperties(tempObject.getProperties());

        propertyValueChecker.checkProperties(policyType, instance.getProperties(), instance.getName());
    }
}