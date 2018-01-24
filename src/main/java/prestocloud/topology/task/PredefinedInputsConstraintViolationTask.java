package prestocloud.topology.task;

import java.util.Map;

import prestocloud.tosca.properties.constraints.ConstraintUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PredefinedInputsConstraintViolationTask extends AbstractTask {

    private Map<String, ConstraintUtil.ConstraintInformation> violatingInputs;

    public PredefinedInputsConstraintViolationTask(Map<String, ConstraintUtil.ConstraintInformation> violatingInputs, TaskCode taskCode) {
        this.violatingInputs = violatingInputs;
        this.setCode(taskCode);
    }
}
