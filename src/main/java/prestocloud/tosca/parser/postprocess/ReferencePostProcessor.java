package prestocloud.tosca.parser.postprocess;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import org.prestocloud.tosca.model.types.AbstractInheritableToscaType;
import prestocloud.tosca.context.ToscaContext;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.impl.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Post processor that performs validation of references in a tosca template.
 */
@Component
@Slf4j
public class ReferencePostProcessor implements IPostProcessor<ReferencePostProcessor.TypeReference> {
    @Override
    public void process(TypeReference typeReference) {
        for (Class<? extends AbstractInheritableToscaType> clazz : typeReference.classes) {
            AbstractInheritableToscaType reference = ToscaContext.get(clazz, typeReference.getKey());
            if (reference != null) {
                return;
            }
        }
        Node node = ParsingContextExecution.getObjectToNodeMap().get(typeReference.getKey());
        if (node == null) {
            node = ParsingContextExecution.getObjectToNodeMap().get(typeReference.getParent());
            if (node == null) {
                log.info("Node not found, probably it's from an transitive dependency archive");
            } else {
                ParsingContextExecution.getParsingErrors()
                        .add(new ParsingError(ErrorCode.TYPE_NOT_FOUND, "Type not found", node.getStartMark(),
                                "The type from the element is not found neither in the archive or it's dependencies or is not defined while required.",
                                node.getEndMark(), typeReference.getKey()));
            }
        } else {
            ParsingContextExecution.getParsingErrors().add(new ParsingError(ErrorCode.TYPE_NOT_FOUND, "Type not found", node.getStartMark(),
                    "The referenced type is not found neither in the archive or it's dependencies.", node.getEndMark(), typeReference.getKey()));
        }
    }

    @Getter
    @Setter
    public static class TypeReference {
        private Class<? extends AbstractInheritableToscaType>[] classes;
        private Object parent;
        private String key;

        public TypeReference(Object parent, String key, Class<? extends AbstractInheritableToscaType>... classes) {
            this.parent = parent;
            this.key = key;
            this.classes = classes;
        }
    }
}