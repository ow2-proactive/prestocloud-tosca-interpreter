package prestocloud.topology.task;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertiesTask extends TopologyTask {
    // list of required properties not set
    private Map<TaskLevel, List<String>> properties;
}
