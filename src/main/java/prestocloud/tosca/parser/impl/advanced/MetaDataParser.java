package prestocloud.tosca.parser.impl.advanced;

import java.util.List;

import javax.annotation.Resource;

import org.prestocloud.tosca.model.Csar;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import com.google.common.collect.Lists;

import prestocloud.model.common.Tag;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.INodeParser;
import prestocloud.tosca.parser.ParserUtils;
import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.impl.base.ScalarParser;
import prestocloud.utils.VersionUtil;

/**
 * Specific tosca to enrich the Csar archive object with meta-data
 */
@Component
public class MetaDataParser implements INodeParser<Csar> {
    private static final String TEMPLATE_NAME = "template_name";
    private static final String TEMPLATE_AUTHOR = "template_author";
    private static final String TEMPLATE_VERSION = "template_version";

    @Resource
    private ScalarParser scalarParser;

    @Override
    public Csar parse(Node node, ParsingContextExecution context) {
        ArchiveRoot parent = (ArchiveRoot) context.getParent();
        Csar csar = parent.getArchive();

        List<Tag> tagList = Lists.newArrayList();
        if (node instanceof MappingNode) {
            MappingNode mapNode = (MappingNode) node;
            for (NodeTuple entry : mapNode.getValue()) {
                String key = scalarParser.parse(entry.getKeyNode(), context);
                String value = scalarParser.parse(entry.getValueNode(), context);
                if (TEMPLATE_NAME.equals(key)) {
                    csar.setName(value);
                } else if (TEMPLATE_AUTHOR.equals(key)) {
                    csar.setTemplateAuthor(value);
                } else if (TEMPLATE_VERSION.equals(key)) {
                    csar.setVersion(value);
                    if (!VersionUtil.isValid(value)) {
                        ParserUtils.addTypeError(entry.getValueNode(), context.getParsingErrors(), "version");
                    }
                } else if (value != null) {
                    tagList.add(new Tag(key, value));
                }
            }
            csar.setTags(tagList);
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "meta-data");
        }

        return csar;
    }
}
