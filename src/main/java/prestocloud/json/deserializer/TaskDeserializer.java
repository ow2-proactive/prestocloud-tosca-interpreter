package prestocloud.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import prestocloud.topology.task.AbstractRelationshipTask;
import prestocloud.topology.task.AbstractTask;
import prestocloud.topology.task.ArtifactTask;
import prestocloud.topology.task.IllegalOperationsTask;
import prestocloud.topology.task.InputArtifactTask;
import prestocloud.topology.task.LocationPolicyTask;
import prestocloud.topology.task.LogTask;
import prestocloud.topology.task.NodeFiltersTask;
import prestocloud.topology.task.NodeMatchingTask;
import prestocloud.topology.task.PropertiesTask;
import prestocloud.topology.task.RequirementsTask;
import prestocloud.topology.task.ScalableTask;
import prestocloud.topology.task.SuggestionsTask;
import prestocloud.topology.task.TaskCode;
import prestocloud.topology.task.UnavailableLocationTask;
import prestocloud.topology.task.WorkflowTask;

/**
 * Custom deserializer to handle multiple {@link AbstractTask} types.
 */
public class TaskDeserializer extends AbstractFieldValueDiscriminatorPolymorphicDeserializer<AbstractTask> {

    public TaskDeserializer() {
        super("code", AbstractTask.class);
        addToRegistry(PropertiesTask.class, TaskCode.INPUT_PROPERTY, TaskCode.PROPERTIES, TaskCode.ORCHESTRATOR_PROPERTY);
        addToRegistry(ScalableTask.class, TaskCode.SCALABLE_CAPABILITY_INVALID);
        addToRegistry(LocationPolicyTask.class, TaskCode.LOCATION_POLICY);
        addToRegistry(NodeMatchingTask.class, TaskCode.NO_NODE_MATCHES);
        addToRegistry(NodeFiltersTask.class, TaskCode.NODE_FILTER_INVALID);
        addToRegistry(RequirementsTask.class, TaskCode.SATISFY_LOWER_BOUND);
        addToRegistry(WorkflowTask.class, TaskCode.WORKFLOW_INVALID);
        addToRegistry(SuggestionsTask.class, TaskCode.REPLACE, TaskCode.IMPLEMENT);
        addToRegistry(ArtifactTask.class, TaskCode.ARTIFACT_INVALID);
        addToRegistry(InputArtifactTask.class, TaskCode.INPUT_ARTIFACT_INVALID);
        addToRegistry(AbstractRelationshipTask.class, TaskCode.IMPLEMENT_RELATIONSHIP);
        addToRegistry(IllegalOperationsTask.class, TaskCode.FORBIDDEN_OPERATION);
        addToRegistry(LogTask.class, TaskCode.LOG);
        addToRegistry(UnavailableLocationTask.class, TaskCode.LOCATION_DISABLED, TaskCode.LOCATION_UNAUTHORIZED);
    }

    @Override
    protected AbstractTask deserializeAfterRead(JsonParser jp, DeserializationContext ctxt, ObjectMapper mapper, ObjectNode root)
            throws JsonProcessingException {
        AbstractTask result = super.deserializeAfterRead(jp, ctxt, mapper, root);

        if (result == null) {
            result = mapper.treeToValue(root, LogTask.class);
        }

        return result;
    }

    private void addToRegistry(Class<? extends AbstractTask> clazz, TaskCode... taskCodes) {
        for (TaskCode taskCode : taskCodes) {
            addToRegistry(taskCode.toString(), clazz);
        }
    }
}