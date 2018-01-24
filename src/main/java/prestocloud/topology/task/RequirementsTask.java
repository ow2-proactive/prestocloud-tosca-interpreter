package prestocloud.topology.task;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequirementsTask extends TopologyTask {
    // list of requirements for which to satisfy lowerbound
    private List<RequirementToSatisfy> requirementsToImplement;
}
