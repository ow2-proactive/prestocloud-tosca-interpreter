package prestocloud.tosca.parser.impl.base;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.impl.ErrorCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Parser implementation that delegates parsing to a tosca referenced in the tosca registry based on the type key.
 */
@Slf4j
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReferencedParser<T> implements INodeParser<T> {
    private String typeName;

    public ReferencedParser(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public T parse(Node node, ParsingContextExecution context) {
        INodeParser<?> delegate = context.getRegistry().get(typeName);
        if (delegate == null) {
            log.error("No tosca found for yaml type {}", typeName);
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.MAPPING_ERROR, "No tosca found for yaml type", node.getStartMark(), "", node.getEndMark(), typeName));
            return null;
        }
        return (T) delegate.parse(node, context);
    }
}
