package prestocloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.prestocloud.tosca.model.CSARDependency;
import org.prestocloud.tosca.model.Csar;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import prestocloud.tosca.context.ToscaContext;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.ParsingErrorLevel;
import prestocloud.tosca.parser.impl.ErrorCode;
import prestocloud.utils.VersionUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ImportParser implements INodeParser<CSARDependency> {

    @Resource
    private LaxImportParser laxImportParser;

    @Override
    public CSARDependency parse(Node node, ParsingContextExecution context) {
        CSARDependency dependency = laxImportParser.parse(node, context);
        if (dependency == null) {
            return null;
        }
        String valueAsString = dependency.getName() + ":" + dependency.getVersion();
        String currentArchiveVersion = context.<ArchiveRoot> getRootObj().getArchive().getVersion();
        Csar csar = ToscaContext.get().getArchive(dependency.getName(), dependency.getVersion());
        log.debug("Import {} {} {}", dependency.getName(), dependency.getVersion(), csar);
        if (csar == null) {
            // error is not a blocker, as long as no type is missing we just mark it as a warning.
            context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.MISSING_DEPENDENCY, "Import definition is not valid",
                    node.getStartMark(), "Specified dependency is not found in repository.", node.getEndMark(), valueAsString));
            return null;
        } else {
            if (!VersionUtil.isSnapshot(currentArchiveVersion) && VersionUtil.isSnapshot(dependency.getVersion())) {
                // the current archive is a released version but depends on a snapshot version
                context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.SNAPSHOT_DEPENDENCY, "Import definition is not valid",
                        node.getStartMark(), "A released archive cannot depends on snapshots archives.", node.getEndMark(), valueAsString));
            }
            dependency.setHash(csar.getHash());
            ToscaContext.get().addDependency(dependency);
            return dependency;
        }
    }

}