package prestocloud.tosca.parser;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import prestocloud.tosca.model.ArchiveRoot;

public class NormativeTypesTest extends AbstractToscaParserSimpleProfileTest {

    @Override
    protected String getRootDirectory() {
        return "src/test/resources/parser/";
    }

    @Override
    protected String getToscaVersion() {
        return "tosca_prestocloud_mapping_1_2";
    }

    @Test
    public void testParsingNormativeTypesv12() throws IOException, ParsingException {

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/types/", "tosca-normative-types-1.2.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        Assert.assertEquals(6, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(8, parsingResult.getResult().getArtifactTypes().size());
        Assert.assertEquals(12, parsingResult.getResult().getCapabilityTypes().size());
        Assert.assertEquals(7, parsingResult.getResult().getRelationshipTypes().size());
        Assert.assertEquals(13, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(5, parsingResult.getResult().getPolicyTypes().size());
    }
}
