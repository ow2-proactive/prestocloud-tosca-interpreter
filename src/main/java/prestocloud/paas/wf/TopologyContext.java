package prestocloud.paas.wf;

import org.prestocloud.tosca.model.templates.Topology;
import org.prestocloud.tosca.utils.IToscaTypeFinder;

public interface TopologyContext extends IToscaTypeFinder {

    String getDSLVersion();

    Topology getTopology();
}
