package prestocloud.paas.wf.util;

import org.prestocloud.tosca.model.templates.Topology;
import org.prestocloud.tosca.model.workflow.Workflow;
import org.prestocloud.tosca.model.workflow.WorkflowStep;

import prestocloud.exceptions.NotFoundException;

public class NodeSubGraphFilter implements SubGraphFilter {

    private Workflow workflow;

    private String nodeId;

    private Topology topology;

    public NodeSubGraphFilter(Workflow workflow, String nodeId, Topology topology) {
        this.workflow = workflow;
        this.nodeId = nodeId;
        this.topology = topology;
    }

    @Override
    public boolean isInSubGraph(String stepId) {
        WorkflowStep stepFound = workflow.getSteps().get(stepId);
        if (stepFound == null) {
            throw new NotFoundException("Step " + stepId + " cannot be found");
        }
        return nodeId.equals(WorkflowGraphUtils.getConcernedNodeName(stepFound, topology));
    }

    @Override
    public String toString() {
        return "Node Filter { nodeId='" + nodeId + '\'' + '}';
    }
}
