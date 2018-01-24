package prestocloud.topology.warning;

import prestocloud.topology.task.AbstractTask;
import prestocloud.topology.task.TaskCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class IllegalOperationWarning extends AbstractTask {
    private String nodeTemplateName;
    private String interfaceName;
    private String operationName;
    private String serviceName;
    private String relationshipType;

    public IllegalOperationWarning(){
        setCode(TaskCode.FORBIDDEN_OPERATION);
    }
}
