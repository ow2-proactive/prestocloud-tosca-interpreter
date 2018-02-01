package prestocloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.prestocloud.tosca.model.CSARDependency;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import lombok.extern.slf4j.Slf4j;
import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.ParsingErrorLevel;
import prestocloud.tosca.parser.impl.ErrorCode;
import prestocloud.tosca.parser.impl.base.ScalarParser;
import prestocloud.utils.VersionUtil;
import prestocloud.utils.version.InvalidVersionException;

/**
 * Import parser that doesn't validate anything
 * For validation of version or presence in catalog, see {@link ImportParser}
 */
@Slf4j
@Component
public class LaxImportParser implements INodeParser<CSARDependency> {
    @Resource
    private ScalarParser scalarParser;

    @Override
    public CSARDependency parse(Node node, ParsingContextExecution context) {
        String valueAsString = scalarParser.parse(node, context);
        if (StringUtils.isNotBlank(valueAsString)) {
            if (valueAsString.contains(":")) {
                String[] dependencyStrs = valueAsString.split(":");
                if (dependencyStrs.length == 2) {
                    String dependencyName = dependencyStrs[0];
                    String dependencyVersion = dependencyStrs[1];
                    // check that version has the righ format
                    try {
                        VersionUtil.parseVersion(dependencyVersion);
                    } catch (InvalidVersionException e) {
                        context.getParsingErrors()
                                .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR,
                                        "Version specified in the dependency is not a valid version.", node.getStartMark(),
                                        "Dependency should be specified as name:version", node.getEndMark(), "Import"));
                        return null;
                    }
                    return new CSARDependency(dependencyName, dependencyVersion);
                }
                context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR, "Import definition is not valid",
                        node.getStartMark(), "Dependency should be specified as name:version", node.getEndMark(), "Import"));
            } else {
                context.getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR, "Relative import is currently not supported",
                                node.getStartMark(), "Dependency should be specified as name:version", node.getEndMark(), "Import"));
            }
        }
        return null;
    }
}