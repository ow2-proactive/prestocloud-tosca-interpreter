package prestocloud.paas.wf;

import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.prestocloud.tosca.model.templates.RelationshipTemplate;
import org.prestocloud.tosca.model.workflow.Workflow;
import org.springframework.stereotype.Component;

import prestocloud.paas.wf.exception.BadWorkflowOperationException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CustomWorkflowBuilder extends AbstractWorkflowBuilder {

    @Override
    public void addNode(Workflow wf, String nodeId, TopologyContext toscaTypeFinder, boolean isCompute) {
        // nodes are not added to custom workflows

    }

    @Override
    public void addRelationship(Workflow wf, String nodeId, NodeTemplate nodeTemplate, String relationshipName, RelationshipTemplate RelationshipTemplate,
            TopologyContext toscaTypeFinder) {
        // relationships are not added to custom workflows
    }

    @Override
    public Workflow reinit(Workflow wf, TopologyContext toscaTypeFinder) {
        throw new BadWorkflowOperationException(String.format("Reinit can not be performed on non standard workflow '%s'", wf.getName()));
    }

}