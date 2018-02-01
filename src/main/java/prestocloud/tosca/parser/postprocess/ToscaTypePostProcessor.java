package prestocloud.tosca.parser.postprocess;

import static prestocloud.utils.PrestocloudUtils.safe;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import org.prestocloud.tosca.model.types.AbstractInheritableToscaType;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingContextExecution;

/**
 * Performs post processing for a TOSCA type:
 * - Set it's archive version and name
 */
@Component
public class ToscaTypePostProcessor implements IPostProcessor<AbstractInheritableToscaType> {
    @Resource
    private PropertyDefinitionPostProcessor propertyDefinitionPostProcessor;

    @Override
    public void process(AbstractInheritableToscaType instance) {
        ArchiveRoot archiveRoot = ParsingContextExecution.getRootObj();
        instance.setArchiveName(archiveRoot.getArchive().getName());
        instance.setArchiveVersion(archiveRoot.getArchive().getVersion());

        safe(instance.getProperties()).entrySet().stream().forEach(propertyDefinitionPostProcessor);
    }
}