package prestocloud.topology.task;

import org.prestocloud.tosca.model.types.AbstractInheritableToscaType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * Represent one task to do to have a deployable topology
 *
 * @author 'Igor Ngouagna'
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class TopologyTask extends AbstractTask {
    // Name of the node template that needs to be fixed.
    private String nodeTemplateName;
    // related component
    private AbstractInheritableToscaType component;
}
