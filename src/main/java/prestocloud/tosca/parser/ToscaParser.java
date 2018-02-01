package prestocloud.tosca.parser;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;
import prestocloud.tosca.context.ToscaContextual;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.impl.ErrorCode;
import prestocloud.tosca.parser.mapping.generator.MappingGenerator;
import prestocloud.tosca.parser.postprocess.ArchiveRootPostProcessor;

/**
 * Main entry point for TOSCA template parsing.
 */
@Slf4j
@Component
public class ToscaParser extends YamlParser<ArchiveRoot> {

    public static final String NORMATIVE_DSL_1_0 = "tosca_simple_yaml_1_0";
    public static final String NORMATIVE_DSL_1_0_URL = "http://docs.oasis-open.org/tosca/ns/simple/yaml/1.0";
    public static final String NORMATIVE_DSL_1_2 = "tosca_simple_yaml_1_2";
    public static final String NORMATIVE_DSL_1_2_URL = "http://docs.oasis-open.org/tosca/ns/simple/yaml/1.2";

    public static String LATEST_DSL = NORMATIVE_DSL_1_2;

    private static final String DEFINITION_TYPE = "definition";
    private Map<String, Map<String, INodeParser>> parserRegistriesByVersion = Maps.newHashMap();

    @Resource
    private MappingGenerator mappingGenerator;

    @Resource
    private ArchiveRootPostProcessor archiveRootPostProcessor;

    @Value("${tosca.new_archives_dsl:#{null}}")
    public void setNewArchiveDSL(String newArchiveDSL) {
        if (newArchiveDSL != null) {
            log.warn(
                    "User has explicitly configured a TOSCA dsl version ({}) to override the default new archive dsl ({}).",
                    newArchiveDSL, LATEST_DSL);
            LATEST_DSL = newArchiveDSL;
        }
    }

    @PostConstruct
    public void initialize() throws ParsingException {
        // Initialize the supported DSL.
        Map<String, INodeParser> registry = mappingGenerator.process("classpath:tosca_simple_yaml_1_0.yml");
        parserRegistriesByVersion.put(NORMATIVE_DSL_1_0, registry);
        parserRegistriesByVersion.put(NORMATIVE_DSL_1_0_URL, registry);
        // experimental
        mappingGenerator.process("classpath:tosca_simple_yaml_1_2.yml");
        parserRegistriesByVersion.put(NORMATIVE_DSL_1_2, registry);
        parserRegistriesByVersion.put(NORMATIVE_DSL_1_2_URL, registry);
    }

    @Override
    @ToscaContextual
    public ParsingResult<ArchiveRoot> parseFile(String filePath, String fileName, InputStream yamlStream, ArchiveRoot instance) throws ParsingException {
        return super.parseFile(filePath, fileName, yamlStream, instance);
    }

    @Override
    @ToscaContextual
    public ParsingResult<ArchiveRoot> parseFile(Path yamlPath) throws ParsingException {
        return super.parseFile(yamlPath);
    }

    @Override
    @ToscaContextual
    public ParsingResult<ArchiveRoot> parseFile(Path yamlPath, ArchiveRoot instance) throws ParsingException {
        return super.parseFile(yamlPath, instance);
    }

    @Override
    protected void postParsing(ArchiveRoot result) {
        // Perform post processing model manipulation and validations.
        archiveRootPostProcessor.process(result);
    }

    @Override
    protected INodeParser<ArchiveRoot> getParser(Node rootNode, ParsingContextExecution context) throws ParsingException {
        if (rootNode instanceof MappingNode) {
            // try to find the tosca version
            DefinitionVersionInfo definitionVersionInfo = getToscaDefinitionVersion(((MappingNode) rootNode).getValue(), context);
            // call the parser for the given tosca version
            Map<String, INodeParser> registry = parserRegistriesByVersion.get(definitionVersionInfo.definitionVersion);
            if (registry == null) {
                throw new ParsingException(context.getFileName(),
                        new ParsingError(ErrorCode.UNKNOWN_TOSCA_VERSION, "Definition version is not supported",
                                definitionVersionInfo.definitionVersionTuple.getKeyNode().getStartMark(), "Version is not supported",
                                definitionVersionInfo.definitionVersionTuple.getValueNode().getStartMark(), definitionVersionInfo.definitionVersion));
            }

            context.setRegistry(registry);
            context.setDefinitionVersion(definitionVersionInfo.definitionVersion);
            return registry.get(DEFINITION_TYPE);
        } else {
            throw new ParsingException(null,
                    new ParsingError(ErrorCode.SYNTAX_ERROR, "File is not a valid tosca definition file.", new Mark("root", 0, 0, 0, null, 0),
                            "The provided yaml file doesn't follow the Top-level key definitions of a valid TOSCA Simple profile file.",
                            new Mark("root", 0, 0, 0, null, 0), "TOSCA Definitions"));
        }
    }

    private DefinitionVersionInfo getToscaDefinitionVersion(List<NodeTuple> topLevelNodes, ParsingContextExecution context) throws ParsingException {
        boolean first = true;
        for (NodeTuple node : topLevelNodes) {
            Node key = node.getKeyNode();
            if (key instanceof ScalarNode) {
                ScalarNode scalarKey = (ScalarNode) key;
                if (scalarKey.getValue().equals("tosca_definitions_version")) {
                    if (!first) {
                        // TOSCA definition version must be the first yaml element
                        context.getParsingErrors()
                                .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.TOSCA_VERSION_NOT_FIRST,
                                        "File is not a valid tosca definition file.", node.getKeyNode().getStartMark(),
                                        "tosca_definitions_version must be the first element of the document.", node.getValueNode().getEndMark(), null));
                    }
                    return new DefinitionVersionInfo(ParserUtils.getScalar(node.getValueNode(), context), node);
                }
            }
            first = false;
        }
        throw new ParsingException(null, new ParsingError(ErrorCode.MISSING_TOSCA_VERSION, "File is not a valid tosca definition file.",
                new Mark("root", 0, 0, 0, null, 0), "Unable to find the mandatory tosca_definitions_version.", new Mark("root", 0, 0, 0, null, 0), null));
    }

    private class DefinitionVersionInfo {
        private final String definitionVersion;
        private final NodeTuple definitionVersionTuple;

        public DefinitionVersionInfo(String definitionVersion, NodeTuple definitionVersionTuple) {
            this.definitionVersion = definitionVersion;
            this.definitionVersionTuple = definitionVersionTuple;
        }
    }
}