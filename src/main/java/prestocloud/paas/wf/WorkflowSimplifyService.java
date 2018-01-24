package prestocloud.paas.wf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.prestocloud.tosca.model.workflow.Workflow;
import org.prestocloud.tosca.model.workflow.WorkflowStep;
import org.springframework.stereotype.Component;

import prestocloud.paas.wf.util.NodeSubGraphFilter;
import prestocloud.paas.wf.util.SimpleGraphConsumer;
import prestocloud.paas.wf.util.SubGraph;
import prestocloud.paas.wf.util.SubGraphFilter;
import prestocloud.paas.wf.util.WorkflowGraphUtils;
import prestocloud.paas.wf.util.WorkflowStepWeightComparator;
import prestocloud.utils.AlienUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WorkflowSimplifyService {

    private interface DoWithNodeCallBack {
        void doWithNode(SubGraph subGraph, Workflow workflow);
    }

    @Getter
    private static class ComputeNodesWeightsGraphConsumer extends SimpleGraphConsumer {

        private Map<String, Integer> allNodeWeights = new HashMap<>();

        @Override
        public boolean onNewPath(List<WorkflowStep> path) {
            WorkflowStep currentNode = path.get(path.size() - 1);
            int parentWeight;
            if (path.size() > 1) {
                parentWeight = allNodeWeights.get(path.get(path.size() - 2).getName());
            } else {
                parentWeight = 0;
            }
            Integer currentWeight = allNodeWeights.computeIfAbsent(currentNode.getName(), k -> 0);
            Integer newComputedWeight = Math.max(parentWeight + 1, currentWeight);
            allNodeWeights.put(currentNode.getName(), newComputedWeight);
            return true;
        }
    }

    @Getter
    private static class LastPathGraphConsumer extends SimpleGraphConsumer {

        private List<WorkflowStep> lastPath;

        @Override
        public boolean onNewPath(List<WorkflowStep> path) {
            lastPath = path;
            return true;
        }
    }

    public void simplifyWorkflow(TopologyContext topologyContext) {
        doWithNode(topologyContext, (subGraph, workflow) -> flattenWorkflow(topologyContext, subGraph));
        doWithNode(topologyContext, (subGraph, workflow) -> removeUnnecessarySteps(topologyContext, workflow, subGraph));
    }

    private void removeUnnecessarySteps(TopologyContext topologyContext, Workflow workflow, SubGraph subGraph) {
        LastPathGraphConsumer consumer = new LastPathGraphConsumer();
        subGraph.browse(consumer);
        if (consumer.getAllNodes().isEmpty()) {
            // This is really strange as we have a node template without any workflow step
            return;
        }
        Set<String> allStepIds = consumer.getAllNodes().keySet();
        List<WorkflowStep> sortedByWeightsSteps = consumer.getLastPath();
        List<Integer> nonEmptyIndexes = new ArrayList<>();
        LinkedHashSet<Integer> emptyIndexes = new LinkedHashSet<>();
        int lastIndexWithOutgoingLinks = -1;
        int firstIndexWithOutgoingLinks = -1;
        for (int i = 0; i < sortedByWeightsSteps.size(); i++) {
            WorkflowStep step = sortedByWeightsSteps.get(i);
            if (!allStepIds.containsAll(step.getOnSuccess())) {
                lastIndexWithOutgoingLinks = i;
                if (firstIndexWithOutgoingLinks == -1) {
                    firstIndexWithOutgoingLinks = i;
                }
            }
            if (!WorkflowGraphUtils.isStepEmpty(step, topologyContext)) {
                nonEmptyIndexes.add(i);
            } else {
                emptyIndexes.add(i);
            }
        }
        Map<Integer, Integer> emptyIndexToFollowingSubstituteMap = new HashMap<>();
        Map<Integer, Integer> emptyIndexToPrecedingSubstituteMap = new HashMap<>();
        for (Integer emptyIndex : emptyIndexes) {
            Optional<Integer> substitutePrecedingIndex = nonEmptyIndexes.stream().filter(nonEmptyIndex -> nonEmptyIndex > emptyIndex).findFirst();
            Optional<Integer> substituteFollowingIndex = nonEmptyIndexes.stream().filter(nonEmptyIndex -> nonEmptyIndex < emptyIndex)
                    .reduce((first, second) -> second);
            substitutePrecedingIndex.ifPresent(index -> emptyIndexToPrecedingSubstituteMap.put(emptyIndex, index));
            substituteFollowingIndex.ifPresent(index -> emptyIndexToFollowingSubstituteMap.put(emptyIndex, index));
        }
        for (int index = 0; index < sortedByWeightsSteps.size(); index++) {
            WorkflowStep step = sortedByWeightsSteps.get(index);
            boolean stepIsEmpty = emptyIndexes.contains(index);
            Integer followingSubstitutedIndex = emptyIndexToFollowingSubstituteMap.get(index);
            Integer precedingSubstitutedIndex = emptyIndexToPrecedingSubstituteMap.get(index);
            if (stepIsEmpty) {
                if (followingSubstitutedIndex == null && !allStepIds.containsAll(step.getOnSuccess())) {
                    // the step is empty but no substitute for following links, it means that before the step there are no non empty steps
                    if (firstIndexWithOutgoingLinks >= 0 && firstIndexWithOutgoingLinks <= index) {
                        // the step or before the step, outgoing links exist out of the sub graph so we should not remove it, because it will impact the
                        // structure of the graph
                        emptyIndexes.remove(index);
                        continue;
                    }
                }
                if (precedingSubstitutedIndex == null && !allStepIds.containsAll(step.getPrecedingSteps())) {
                    // the step is empty but no substitute for preceding links, it means that after the step there are no non empty steps
                    if (lastIndexWithOutgoingLinks >= index) {
                        // the step or after the step, outgoing links exist out of the sub graph so we should not remove it, because it will impact the
                        // structure of the graph
                        emptyIndexes.remove(index);
                        continue;
                    }
                }
                // Empty so the step will be removed, so unlink all
                for (String following : step.getOnSuccess()) {
                    workflow.getSteps().get(following).removePreceding(step.getName());
                }
                for (String preceding : step.getPrecedingSteps()) {
                    workflow.getSteps().get(preceding).removeFollowing(step.getName());
                }
                if (followingSubstitutedIndex != null) {
                    WorkflowStep substitutedFollowingStep = sortedByWeightsSteps.get(followingSubstitutedIndex);
                    for (String following : step.getOnSuccess()) {
                        workflow.getSteps().get(following).addPreceding(substitutedFollowingStep.getName());
                    }
                    // Copy all links to the substituted node
                    substitutedFollowingStep.addAllFollowings(step.getOnSuccess());
                }
                if (precedingSubstitutedIndex != null) {
                    WorkflowStep substitutedPrecedingStep = sortedByWeightsSteps.get(precedingSubstitutedIndex);
                    for (String preceding : step.getPrecedingSteps()) {
                        workflow.getSteps().get(preceding).addFollowing(substitutedPrecedingStep.getName());
                    }
                    substitutedPrecedingStep.addAllPrecedings(step.getPrecedingSteps());
                }
            }
        }
        int index = 0;
        Iterator<WorkflowStep> stepIterator = sortedByWeightsSteps.iterator();
        while (stepIterator.hasNext()) {
            WorkflowStep step = stepIterator.next();
            if (emptyIndexes.contains(index)) {
                stepIterator.remove();
                workflow.getSteps().remove(step.getName());
            }
            index++;
        }
    }

    private void doWithNode(TopologyContext topologyContext, DoWithNodeCallBack callBack) {
        AlienUtils.safe(topologyContext.getTopology().getWorkflows()).values().stream().filter(workflow -> !workflow.isHasCustomModifications())
                .forEach(workflow -> AlienUtils.safe(topologyContext.getTopology().getNodeTemplates()).keySet().forEach(nodeId -> {
                    SubGraphFilter stepFilter = new NodeSubGraphFilter(workflow, nodeId, topologyContext.getTopology());
                    SubGraph subGraph = new SubGraph(workflow, stepFilter);
                    callBack.doWithNode(subGraph, workflow);
                }));
    }

    private void flattenWorkflow(TopologyContext topologyContext, SubGraph subGraph) {
        ComputeNodesWeightsGraphConsumer consumer = new ComputeNodesWeightsGraphConsumer();
        subGraph.browse(consumer);
        if (consumer.getAllNodes().isEmpty()) {
            // This is really strange as we have a node template without any workflow step
            return;
        }
        Map<String, WorkflowStep> allNodes = consumer.getAllNodes();
        LinkedList<WorkflowStep> sortedByWeightsSteps = new LinkedList<>(allNodes.values());
        sortedByWeightsSteps.sort(new WorkflowStepWeightComparator(consumer.getAllNodeWeights(), topologyContext.getTopology()));
        Set<String> allSubGraphNodeIds = allNodes.keySet();
        sortedByWeightsSteps.forEach(workflowStep -> {
            // Remove all old links between the steps in the graph
            workflowStep.removeAllPrecedings(allSubGraphNodeIds);
            workflowStep.removeAllFollowings(allSubGraphNodeIds);
        });
        // Create a sequence with sorted sub graph steps
        for (int i = 0; i < sortedByWeightsSteps.size() - 1; i++) {
            sortedByWeightsSteps.get(i).addFollowing(sortedByWeightsSteps.get(i + 1).getName());
            sortedByWeightsSteps.get(i + 1).addPreceding(sortedByWeightsSteps.get(i).getName());
        }
    }
}
