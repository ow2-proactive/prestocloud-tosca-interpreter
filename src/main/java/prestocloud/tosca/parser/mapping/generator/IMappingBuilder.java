package prestocloud.tosca.parser.mapping.generator;

import org.yaml.snakeyaml.nodes.MappingNode;

import prestocloud.tosca.parser.MappingTarget;
import prestocloud.tosca.parser.ParsingContextExecution;

/**
 * Build a mapping for a given key.
 */
public interface IMappingBuilder {
    /**
     *
     * @return
     */
    String getKey();

    MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context);
}