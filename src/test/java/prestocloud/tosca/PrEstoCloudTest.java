package prestocloud.tosca;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;
import org.prestocloud.tosca.model.definitions.ComplexPropertyValue;
import org.prestocloud.tosca.model.definitions.ScalarPropertyValue;
import org.prestocloud.tosca.model.templates.Capability;
import org.prestocloud.tosca.model.templates.NodeTemplate;
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
@ActiveProfiles("PrEstoCloudTest")
public class PrEstoCloudTest {

    @Profile("PrEstoCloudTest")
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
    public void testParsingPlacementConstraints() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "placement-example.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testParsingTypes() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/types/", "tosca-normative-types-1.2.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/types/", "resource-descriptions-1.0.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/types/", "iccs-normative-types-1.0.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/types/", "placement-constraints-1.0.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testParsingCloudTemplates() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResultAzure = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "azure-vm-templates.yml"));
        Assert.assertEquals(0, parsingResultAzure.getContext().getParsingErrors().size());
        ParsingResult<ArchiveRoot> parsingResultAmazon = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "amazon-vm-templates.yml"));
        Assert.assertEquals(0, parsingResultAmazon.getContext().getParsingErrors().size());
    }

    @Test
    public void testParsingTypeLevelTOSCA() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "ICCS-example-jppf.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "ICCS-example-faas.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testParsingInstanceLevelTOSCA() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "ActiveEon-example-jppf.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "ActiveEon-example-faas.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testParsingActiveEon() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "ActiveEon-example-jppf.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment.jppf")) {
                // Look for the corresponding JPPF agent
                for (Map.Entry<String, NodeTemplate> nodeTemplateJPPF  : nodeTemplates.entrySet()) {
                    // Corresponding JPPF agent found
                    if (nodeTemplateJPPF.getValue().getType().equalsIgnoreCase("prestocloud.nodes.agent.jppf") &&  nodeTemplateJPPF.getKey().equalsIgnoreCase(nodeTemplateFragment.getValue().getRelationships().get("execute").getTarget())) {
                        // Look for the corresponding host
                        for (Map.Entry<String, NodeTemplate> nodeTemplateHost : nodeTemplates.entrySet()) {
                            // Corresponding host found
                            if (nodeTemplateHost.getKey().equalsIgnoreCase(nodeTemplateJPPF.getValue().getRelationships().get("host").getTarget())) {
                                // Complete dependency found: fragment -> JPPF agent -> host
                                System.out.println("Found fragment: '" + nodeTemplateFragment.getValue().getName() + "' executed on JPPF agent '" + nodeTemplateFragment.getValue().getRelationships().get("execute").getTarget() + "' hosted on '" + nodeTemplateJPPF.getValue().getRelationships().get("host").getTarget() + "'");
                                // Look for capabilities
                                for (Map.Entry<String, Capability> capabilities : nodeTemplateHost.getValue().getCapabilities().entrySet()) {
                                    // Get 'host' capability properties
                                    if (capabilities.getKey().equalsIgnoreCase("host")) {
                                        System.out.println("Host properties: ");
                                        for (Map.Entry<String, AbstractPropertyValue> properties : capabilities.getValue().getProperties().entrySet()) {
                                            if (properties.getKey().equalsIgnoreCase("num_cpus")) {
                                                String num_cpus = ((ScalarPropertyValue)properties.getValue()).getValue();
                                                System.out.println("- " + properties.getKey() + " = " + num_cpus);
                                            }
                                            if (properties.getKey().equalsIgnoreCase("mem_size")) {
                                                String mem_size = ((ScalarPropertyValue)properties.getValue()).getValue();
                                                System.out.println("- " + properties.getKey() + " = " + mem_size);
                                            }
                                            if (properties.getKey().equalsIgnoreCase("disk_size")) {
                                                String disk_size = ((ScalarPropertyValue)properties.getValue()).getValue();
                                                System.out.println("- " + properties.getKey() + " = " + disk_size);
                                            }
                                            if (properties.getKey().equalsIgnoreCase("price")) {
                                                String price = ((ScalarPropertyValue)properties.getValue()).getValue();
                                                System.out.println("- " + properties.getKey() + " = " + price);
                                            }
                                        }
                                    }
                                    // Get 'resource' capability properties
                                    if (capabilities.getKey().equalsIgnoreCase("resource")) {
                                        System.out.println("Resource properties: ");
                                        String type = null;
                                        for (Map.Entry<String, AbstractPropertyValue> properties : capabilities.getValue().getProperties().entrySet()) {
                                            if (properties.getKey().equalsIgnoreCase("type")) {
                                                type = ((ScalarPropertyValue) properties.getValue()).getValue();
                                                System.out.println("- " + properties.getKey() + " = " + type);
                                            }
                                            // Cloud based resource detected
                                            if (type != null && type.equalsIgnoreCase("cloud") && properties.getKey().equalsIgnoreCase("cloud")) {
                                                System.out.println("- " + properties.getKey() + ":");
                                                ComplexPropertyValue cloud = (ComplexPropertyValue) properties.getValue();
                                                for (Map.Entry<String, Object> cloudProperties : cloud.getValue().entrySet()) {
                                                    if (cloudProperties.getKey().equalsIgnoreCase("cloud_type")) {
                                                        System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                                    }
                                                    if (cloudProperties.getKey().equalsIgnoreCase("cloud_region")) {
                                                        System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                                    }
                                                    if (cloudProperties.getKey().equalsIgnoreCase("cloud_name")) {
                                                        System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                                    }
                                                    // Get networking informations
                                                    if (cloudProperties.getKey().equalsIgnoreCase("cloud_network")) {
                                                        System.out.println(" - " + cloudProperties.getKey() + ":");
                                                        HashMap<String, Object> cloud_network = (HashMap<String, Object>) cloudProperties.getValue();
                                                        for (Map.Entry<String, Object> cloudNetworkProperties : cloud_network.entrySet()) {
                                                            if (cloudNetworkProperties.getKey().equalsIgnoreCase("network_id")) {
                                                                System.out.println("  - " + cloudNetworkProperties.getKey() + " = " + cloudNetworkProperties.getValue());
                                                            }
                                                            if (cloudNetworkProperties.getKey().equalsIgnoreCase("network_name")) {
                                                                System.out.println("  - " + cloudNetworkProperties.getKey() + " = " + cloudNetworkProperties.getValue());
                                                            }
                                                            if (cloudNetworkProperties.getKey().equalsIgnoreCase("addresses")) {
                                                                System.out.println("  - " + cloudNetworkProperties.getKey());
                                                                for (String address : (List<String>) cloudNetworkProperties.getValue()) {
                                                                    System.out.println("    - " + address);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
