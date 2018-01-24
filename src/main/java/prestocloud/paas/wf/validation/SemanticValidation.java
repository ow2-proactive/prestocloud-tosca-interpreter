package prestocloud.paas.wf.validation;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.prestocloud.tosca.model.workflow.RelationshipWorkflowStep;
import org.prestocloud.tosca.model.workflow.Workflow;
import org.prestocloud.tosca.model.workflow.WorkflowStep;
import org.prestocloud.tosca.model.workflow.activities.InlineWorkflowActivity;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;
import prestocloud.paas.wf.TopologyContext;
import prestocloud.paas.wf.exception.WorkflowException;

@Slf4j
public class SemanticValidation implements Rule {

    @Override
    public List<AbstractWorkflowError> validate(TopologyContext topologyContext, Workflow workflow) throws WorkflowException {
        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            return null;
        }
        List<AbstractWorkflowError> errors = Lists.newArrayList();
        for (WorkflowStep step : workflow.getSteps().values()) {
            if (step.getActivity() instanceof InlineWorkflowActivity) {
                // TODO when the tosca model is clear we should create InlineWorkflowStep
                String inlinedWorkflow = ((InlineWorkflowActivity) step.getActivity()).getInline();
                if (topologyContext.getTopology().getWorkflows() == null || !topologyContext.getTopology().getWorkflows().containsKey(inlinedWorkflow)) {
                    errors.add(new InlinedWorkflowNotFoundError(step.getName(), inlinedWorkflow));
                }
            } else if (StringUtils.isEmpty(step.getTarget())) {
                errors.add(new UnknownNodeError(step.getName(), "undefined target in non inline workflow activity"));
            } else {
                String nodeId = step.getTarget();
                NodeTemplate nodeTemplate = null;
                if (topologyContext.getTopology().getNodeTemplates() != null) {
                    nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
                }
                if (nodeTemplate == null) {
                    errors.add(new UnknownNodeError(step.getName(), nodeId));
                } else if (step instanceof RelationshipWorkflowStep) {
                    RelationshipWorkflowStep relationshipWorkflowStep = (RelationshipWorkflowStep) step;
                    String relationshipId = relationshipWorkflowStep.getTargetRelationship();
                    if (nodeTemplate.getRelationships() == null || !nodeTemplate.getRelationships().containsKey(relationshipId)) {
                        errors.add(new UnknownRelationshipError(step.getName(), nodeId, relationshipId));
                    }
                }
                // TODO: here we should check interface & operation
            }
        }
        return errors;
    }
}
