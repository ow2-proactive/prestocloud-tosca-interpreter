package prestocloud.tosca.parser.impl.base;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.Node;

import com.google.common.collect.Maps;

import prestocloud.tosca.parser.ParsingContextExecution;
import prestocloud.tosca.parser.ParsingErrorLevel;
import prestocloud.tosca.parser.impl.ErrorCode;

/**
 * Internal junit tests to check yaml mapping framework management for reference tosca.
 */
// @Ignore
public class ReferencedParserTest {

    @Test
    public void referenceToMissingTypeShouldFail() {
        ReferencedParser referencedParser = new ReferencedParser("missingParser");
        ParsingContextExecution contextExecution = new ParsingContextExecution();
        try {
            contextExecution.init();
            contextExecution.setRegistry(Maps.newHashMap());
            Node node = Mockito.mock(Node.class);
            Mockito.when(node.getStartMark()).thenReturn(new Mark("name", 0, 10, 10, "", 0));
            Mockito.when(node.getEndMark()).thenReturn(new Mark("name", 0, 10, 10, "", 0));
            referencedParser.parse(node, contextExecution);
            Assert.assertEquals(ParsingErrorLevel.ERROR, contextExecution.getParsingErrors().get(0).getErrorLevel());
            Assert.assertEquals(ErrorCode.MAPPING_ERROR, contextExecution.getParsingErrors().get(0).getErrorCode());
        } finally {
            ParsingContextExecution.destroy();
        }
    }
}