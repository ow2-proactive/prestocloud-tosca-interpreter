package prestocloud.tosca;

import java.io.IOException;
import java.nio.file.Paths;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import prestocloud.component.ICSARRepositorySearchService;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingException;
import prestocloud.tosca.parser.ParsingResult;
import prestocloud.tosca.parser.ToscaParser;
import prestocloud.tosca.repository.LocalRepositoryImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("PrEstoCloudTOSCAParser")
public class PrEstoCloudTest {

    @Profile("PrEstoCloudTOSCAParser")
    @Configuration
    @EnableAutoConfiguration(exclude = { HypermediaAutoConfiguration.class })
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @ComponentScan(basePackages = { "prestocloud.tosca.context", "prestocloud.tosca.parser" })
    static class ContextConfiguration {
        @Bean
        public ICSARRepositorySearchService repositorySearchService() {
            LocalRepositoryImpl repository = new LocalRepositoryImpl();
            repository.setPath("src/main/resources/repository/");
            return repository;
        }
    }

    @Resource
    protected ToscaParser parser;

    @Resource
    protected ICSARRepositorySearchService csarRepositorySearchService;

    @Test
    public void testParsingRequirementsCapabilitiesWithNormativeTypesImport() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/parser/", "requirement_capabilities.yaml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testParsingNodeFilter() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/parser/", "tosca-node-type-nodefilter.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testParsingICCS() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "ICCS_example.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testParsingPlacementConstraints() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "placement_example.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }

    @Ignore
    @Test
    public void testParsingCloudTemplates() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResultAzure = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "azure-vm-templates.yml"));
        Assert.assertEquals(0, parsingResultAzure.getContext().getParsingErrors().size());
        ParsingResult<ArchiveRoot> parsingResultAmazon = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "amazon-vm-templates.yml"));
        Assert.assertEquals(0, parsingResultAmazon.getContext().getParsingErrors().size());
    }

    @Ignore
    @Test
    public void testParsingICCS_v2() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "ICCS_types_definition_v2.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testParsingICCS_v3() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "ICCS_types_definition_v3.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }
}
