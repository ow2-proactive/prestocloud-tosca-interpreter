package prestocloud.topology.task;

import org.prestocloud.tosca.model.types.AbstractInheritableToscaType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import prestocloud.json.deserializer.TaskIndexedInheritableToscaElementDeserializer;
import prestocloud.utils.jackson.ConditionalAttributes;
import prestocloud.utils.jackson.ConditionalOnAttribute;

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
    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(using = TaskIndexedInheritableToscaElementDeserializer.class)
    private AbstractInheritableToscaType component;
}
