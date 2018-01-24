package prestocloud.tosca.parser.postprocess;

import javax.annotation.Resource;

import org.prestocloud.tosca.model.types.NodeType;
import org.prestocloud.tosca.model.types.PolicyType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import prestocloud.tosca.parser.postprocess.ReferencePostProcessor.TypeReference;

/**
 * Policy type post processor ensure that the policy types targets are valid types and actually defined in the catalog.
 */
@Component
public class PolicyTypePostProcessor implements IPostProcessor<PolicyType> {
    @Resource
    private ReferencePostProcessor referencePostProcessor;

    @Override
    public void process(PolicyType instance) {
        if (!CollectionUtils.isEmpty(instance.getTargets())) {
            for (String target : instance.getTargets()) {
                // FIXME add group type once parsed.
                referencePostProcessor.process(new TypeReference(instance.getTargets(), target, NodeType.class));
            }
        }
    }
}