package prestocloud.tosca.parser.postprocess;

import java.util.Iterator;
import java.util.Set;

import org.prestocloud.tosca.model.templates.NodeGroup;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import com.google.common.collect.Sets;

import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.ParsingErrorLevel;
import prestocloud.tosca.parser.impl.ErrorCode;

/**
 * Group post processor ensure that referenced nodes indeed exists as well as inject the groups into the node as we keep reversed mapping.
 */
@Component
public class GroupPostProcessor implements IPostProcessor<NodeGroup> {
    @Override
    public void process(NodeGroup instance) {
        final ArchiveRoot archiveRoot = (ArchiveRoot) ParsingContextExecution.getRoot().getWrappedInstance();
        // ensure that member groups exists and add the group to the nodes.
        Iterator<String> groupMembers = instance.getMembers().iterator();
        while (groupMembers.hasNext()) {
            String nodeTemplateId = groupMembers.next();
            NodeTemplate nodeTemplate = archiveRoot.getTopology().getNodeTemplates().get(nodeTemplateId);
            if (nodeTemplate == null) {
                Node node = ParsingContextExecution.getObjectToNodeMap().get(instance);
                // add an error to the context
                ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNKOWN_GROUP_MEMBER, null,
                        node.getStartMark(), null, node.getEndMark(), nodeTemplateId));
                // and remove the member
                groupMembers.remove();
            } else {
                Set<String> groups = nodeTemplate.getGroups();
                if (groups == null) {
                    groups = Sets.newHashSet();
                    nodeTemplate.setGroups(groups);
                }
                groups.add(instance.getName());
            }
        }
    }
}