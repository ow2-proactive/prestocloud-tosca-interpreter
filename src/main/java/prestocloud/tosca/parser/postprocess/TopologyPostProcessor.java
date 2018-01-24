package prestocloud.tosca.parser.postprocess;

import static prestocloud.utils.AlienUtils.safe;

import java.util.Map;

import javax.annotation.Resource;

import org.prestocloud.tosca.model.templates.NodeGroup;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.prestocloud.tosca.model.templates.Topology;
import org.prestocloud.tosca.utils.TopologyUtils;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import com.google.common.collect.Sets;

import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.ParsingErrorLevel;
import prestocloud.tosca.parser.impl.ErrorCode;

/**
 * Post process a topology.
 */
@Component
public class TopologyPostProcessor implements IPostProcessor<Topology> {
    @Resource
    private NodeTemplatePostProcessor nodeTemplatePostProcessor;
    @Resource
    private PolicyTemplatePostProcessor policyTemplatePostProcessor;
    @Resource
    private NodeTemplateRelationshipPostProcessor nodeTemplateRelationshipPostProcessor;
    @Resource
    private SubstitutionMappingPostProcessor substitutionMappingPostProcessor;
    @Resource
    private GroupPostProcessor groupPostProcessor;
    @Resource
    private PropertyDefinitionPostProcessor propertyDefinitionPostProcessor;
    // Inputs do not define artifact reference so we don't perform validation on them like for types.
    @Resource
    private TypeDeploymentArtifactPostProcessor typeDeploymentArtifactPostProcessor;
    @Resource
    private WorkflowPostProcessor workflowPostProcessor;

    @Override
    public void process(Topology instance) {
        if (instance == null) {
            return;
        }
        ArchiveRoot archiveRoot = ParsingContextExecution.getRootObj();
        Node node = ParsingContextExecution.getObjectToNodeMap().get(instance); // The yaml node for the topology

        setDependencies(instance, archiveRoot);

        if (instance.isEmpty()) {
            // if the topology doesn't contains any node template it won't be imported so add a warning.
            ParsingContextExecution.getParsingErrors()
                    .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.EMPTY_TOPOLOGY, null, node.getStartMark(), null, node.getEndMark(), ""));
        }

        // archive name and version
        instance.setArchiveName(archiveRoot.getArchive().getName());
        instance.setArchiveVersion(archiveRoot.getArchive().getVersion());

        // Inputs validation
        safe(instance.getInputs()).entrySet().forEach(propertyDefinitionPostProcessor);
        safe(instance.getInputArtifacts()).values().forEach(typeDeploymentArtifactPostProcessor);

        int groupIndex = 0;
        // Groups validation
        for (NodeGroup nodeGroup : safe(instance.getGroups()).values()) {
            nodeGroup.setIndex(groupIndex++);
            groupPostProcessor.process(nodeGroup);
        }

        // Policies templates validation
        safe(instance.getPolicies()).forEach((policyName, policyTemplate) -> {
            // set the templateName
            policyTemplate.setName(policyName);
            policyTemplatePostProcessor.process(policyTemplate);
        });

        // Node templates validation
        for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : safe(instance.getNodeTemplates()).entrySet()) {
            nodeTemplateEntry.getValue().setName(nodeTemplateEntry.getKey());
            nodeTemplatePostProcessor.process(nodeTemplateEntry.getValue());
        }
        safe(instance.getNodeTemplates()).values().forEach(nodeTemplateRelationshipPostProcessor);

        substitutionMappingPostProcessor.process(instance.getSubstitutionMapping());

        // first validate names
        TopologyUtils.normalizeAllNodeTemplateName(instance, ParsingContextExecution.getParsingErrors(), ParsingContextExecution.getObjectToNodeMap());

        // Post process workflows
        workflowPostProcessor.processWorkflows(instance, node);
    }

    private void setDependencies(Topology instance, ArchiveRoot archiveRoot) {
        if (archiveRoot.getArchive().getDependencies() == null) {
            return;
        }
        instance.setDependencies(Sets.newHashSet(archiveRoot.getArchive().getDependencies()));
    }

}