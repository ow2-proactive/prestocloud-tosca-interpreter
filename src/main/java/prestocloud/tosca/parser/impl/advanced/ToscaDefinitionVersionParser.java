package prestocloud.tosca.parser.impl.advanced;

import java.util.HashSet;
import java.util.Set;

import org.prestocloud.tosca.model.CSARDependency;
import org.prestocloud.tosca.model.Csar;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import prestocloud.tosca.context.ToscaContext;
import prestocloud.tosca.model.ArchiveRoot;
import org.prestocloud.tosca.normative.ToscaNormativeImports;
import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParserUtils;
import prestocloud.tosca.parser.ParsingContextExecution;

@Component
public class ToscaDefinitionVersionParser implements INodeParser<String> {
    private static ThreadLocal<Boolean> loadingNormative = new ThreadLocal<>();

    @Override
    public String parse(Node node, ParsingContextExecution context) {
        ArchiveRoot archiveRoot = (ArchiveRoot) context.getParent();
        String toscaDefinitionVersion = ParserUtils.getScalar(node, context);
        if (toscaDefinitionVersion != null) {
            CSARDependency dependency = ToscaNormativeImports.IMPORTS.get(toscaDefinitionVersion);
            if (dependency != null) {
                // File based parsing implementation of the requirement of normative types will load them from file (meaning basically that we will loop here)
                if (loadingNormative.get() == null) {
                    loadingNormative.set(true);
                } else {
                    return toscaDefinitionVersion;
                }

                Csar csar = ToscaContext.get().getArchive(dependency.getName(), dependency.getVersion());
                if (csar == null) {
                    return toscaDefinitionVersion;
                }

                // Normative imports are automatically injected and supposed to be accessible, no specific validation is performed here.
                dependency.setHash(csar.getHash());
                ToscaContext.get().addDependency(dependency);

                Set<CSARDependency> dependencies = archiveRoot.getArchive().getDependencies();
                if (dependencies == null) {
                    dependencies = new HashSet<>();
                    archiveRoot.getArchive().setDependencies(dependencies);
                }
                dependencies.add(dependency);
                loadingNormative.remove();
            }
        }
        return toscaDefinitionVersion;
    }
}
