package prestocloud.topology.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import prestocloud.rest.model.RestErrorCode;
import prestocloud.tosca.properties.constraints.ConstraintUtil;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NodeFilterConstraintViolation {
    private RestErrorCode errorCode;
    private String message;
    private ConstraintUtil.ConstraintInformation constraintInformation;
}
