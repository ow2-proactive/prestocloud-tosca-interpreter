package prestocloud.paas.wf.validation;

import java.util.List;

import org.prestocloud.tosca.model.workflow.Workflow;

import prestocloud.paas.wf.TopologyContext;

public interface Rule {

    List<AbstractWorkflowError> validate(TopologyContext topologyContext, Workflow workflow);

}
