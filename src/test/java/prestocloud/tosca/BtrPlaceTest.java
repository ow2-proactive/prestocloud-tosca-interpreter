package prestocloud.tosca;

import lombok.Getter;
import lombok.Setter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.prestocloud.tosca.model.definitions.*;
import org.prestocloud.tosca.model.definitions.constraints.EqualConstraint;
import org.prestocloud.tosca.model.definitions.constraints.InRangeConstraint;
import org.prestocloud.tosca.model.templates.Capability;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.prestocloud.tosca.model.templates.PolicyTemplate;
import org.prestocloud.tosca.model.types.NodeType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import prestocloud.component.ICSARRepositorySearchService;
import prestocloud.model.common.Tag;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingException;
import prestocloud.tosca.parser.ParsingResult;
import prestocloud.tosca.parser.ToscaParser;
import prestocloud.tosca.repository.LocalRepositoryImpl;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("PrEstoCloudTOSCAParser")
public class BtrPlaceTest {

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

    public Map<String, Map<String, Map<String, String>>> getCloudNodesTemplates(String templateFile) throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", templateFile));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        Map<String, Map<String, Map<String, String>>> vmTypes = new HashMap<>();

        // Look for node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplate : nodeTemplates.entrySet()) {
            // Look for capabilities
            Map<String, Map<String, String>> typesCapabilities = new HashMap<>();
            for (Map.Entry<String, Capability> capabilities : nodeTemplate.getValue().getCapabilities().entrySet()) {
                // Get 'host' capability properties
                if (capabilities.getKey().equalsIgnoreCase("host")) {
                    Map<String, String> hostCapabilities = new HashMap<>();
                    System.out.println("Host properties: ");
                    for (Map.Entry<String, AbstractPropertyValue> properties : capabilities.getValue().getProperties().entrySet()) {
                        if (properties.getKey().equalsIgnoreCase("num_cpus")) {
                            String num_cpus = ((ScalarPropertyValue)properties.getValue()).getValue();
                            hostCapabilities.put("num_cpus", num_cpus);
                            System.out.println("- " + properties.getKey() + " = " + num_cpus);
                        }
                        if (properties.getKey().equalsIgnoreCase("mem_size")) {
                            String mem_size = ((ScalarPropertyValue)properties.getValue()).getValue();
                            hostCapabilities.put("mem_size", mem_size);
                            System.out.println("- " + properties.getKey() + " = " + mem_size);
                        }
                        if (properties.getKey().equalsIgnoreCase("disk_size")) {
                            if (properties.getValue() != null) {
                                String disk_size = ((ScalarPropertyValue) properties.getValue()).getValue();
                                hostCapabilities.put("disk_size", disk_size);
                                System.out.println("- " + properties.getKey() + " = " + disk_size);
                            }
                        }
                        if (properties.getKey().equalsIgnoreCase("price")) {
                            String price = ((ScalarPropertyValue)properties.getValue()).getValue();
                            hostCapabilities.put("price", price);
                            System.out.println("- " + properties.getKey() + " = " + price);
                        }
                    }
                    typesCapabilities.put(capabilities.getKey(), hostCapabilities);
                }
                // Get 'resource' capability properties
                if (capabilities.getKey().equalsIgnoreCase("resource")) {
                    Map<String, String> resourceCapabilities = new HashMap<>();
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
                                    resourceCapabilities.put("cloud_type", cloudProperties.getValue().toString());
                                    System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                }
                                if (cloudProperties.getKey().equalsIgnoreCase("cloud_region")) {
                                    resourceCapabilities.put("cloud_region", cloudProperties.getValue().toString());
                                    System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                }
                                if (cloudProperties.getKey().equalsIgnoreCase("cloud_name")) {
                                    resourceCapabilities.put("cloud_name", cloudProperties.getValue().toString());
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
                            typesCapabilities.put("cloud", resourceCapabilities);
                        }
                    }
                }
            }
            vmTypes.put(nodeTemplate.getValue().getName(), typesCapabilities);
        }
        return vmTypes;
    }

    public List<Constraint> getConstraints(ParsingResult<ArchiveRoot> parsingResult) {
        List<Constraint> constraints = new ArrayList<>();

        // Look for placement constraints
        Map<String, PolicyTemplate> policyTemplates = parsingResult.getResult().getTopology().getPolicies();
        for (Map.Entry<String, PolicyTemplate> policyTemplate : policyTemplates.entrySet()) {
            Constraint constraint = new Constraint(policyTemplate.getKey());
            constraint.setType(policyTemplate.getValue().getType());
            constraint.setTargets(policyTemplate.getValue().getTargets());
            constraints.add(constraint);
        }
        return constraints;
    }

    public List<Relationship> getRelationships(ParsingResult<ArchiveRoot> parsingResult) {

        List<Relationship> relationships = new ArrayList<>();

        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment")) {
                // Look for the corresponding JPPF agent
                for (Map.Entry<String, NodeTemplate> nodeTemplateJPPF : nodeTemplates.entrySet()) {
                    // Corresponding JPPF agent found
                    if (nodeTemplateJPPF.getKey().equalsIgnoreCase(nodeTemplateFragment.getValue().getRelationships().get("execute").getTarget())) {
                        System.out.println("JPPF node type pointer found: " + nodeTemplateJPPF.getValue().getType());
                        // Look for the corresponding node type
                        for (Map.Entry<String, NodeType> nodeTypeJPPF : parsingResult.getResult().getNodeTypes().entrySet()) {
                            if (nodeTypeJPPF.getKey().equalsIgnoreCase(nodeTemplateJPPF.getValue().getType())) {
                                System.out.println("Node type found for type: " + nodeTemplateJPPF.getValue().getType());
                                Relationship relationship = new Relationship(nodeTemplateFragment.getKey(), nodeTemplateJPPF.getKey(), nodeTypeJPPF.getKey());
                                // Look for requirements
                                for (RequirementDefinition requirement : nodeTypeJPPF.getValue().getRequirements()) {
                                    // Look for requirement capabilities if present
                                    if (requirement.getNodeFilter() != null) {
                                        for (Map.Entry<String, FilterDefinition> capability : requirement.getNodeFilter().getCapabilities().entrySet()) {
                                            if (capability.getKey().equalsIgnoreCase("host")) {
                                                System.out.println("Host properties: ");
                                                for (Map.Entry<String, List<PropertyConstraint>> properties : capability.getValue().getProperties().entrySet()) {
                                                    List<String> constraints = new ArrayList<>();
                                                    for (PropertyConstraint propertyConstraint : properties.getValue()) {
                                                        if (propertyConstraint instanceof InRangeConstraint) {
                                                            constraints.add("RangeMin: " + ((InRangeConstraint) propertyConstraint).getInRange().get(0) + ", RangeMax: " + ((InRangeConstraint) propertyConstraint).getInRange().get(1));
                                                        } else if (propertyConstraint instanceof EqualConstraint) {
                                                            constraints.add("Equal: " + ((EqualConstraint) propertyConstraint).getEqual());
                                                        } else {
                                                            // Contraint not yet managed
                                                            System.out.println("Constraint not managed: " + propertyConstraint.toString());
                                                        }
                                                    }
                                                    relationship.addHostingConstraint(properties.getKey(), constraints);
                                                }
                                            }
                                        }
                                    }
                                }
                                relationships.add(relationship);
                            }
                        }
                    }
                }
            }
        }
        return relationships;
    }

    public Map<String, String> getMetadata(ParsingResult<ArchiveRoot> parsingResult) {
        Map<String, String> metadata = new HashMap<>();
        for (Tag tag : parsingResult.getResult().getArchive().getTags()) {
            metadata.put(tag.getName(), tag.getValue());
        }
        return metadata;
    }

    @Test
    public void testParsingVMTypes() throws IOException, ParsingException {
        Map<String, Map<String, Map<String, String>>> AmazonVMTypes = getCloudNodesTemplates("amazon-vm-templates.yml");
        Map<String, Map<String, Map<String, String>>> AzureVMTypes = getCloudNodesTemplates("azure-vm-templates.yml");
        Assert.assertEquals(617, AmazonVMTypes.size());
        Assert.assertEquals(824, AzureVMTypes.size());
    }

    @Test
    public void testParsing() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "ICCS-example.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        Map<String, String> metadata = getMetadata(parsingResult);
        Assert.assertEquals(11, metadata.size());

        List<Constraint> constraints = getConstraints(parsingResult);
        Assert.assertEquals(3, constraints.size());

        List<Relationship> relationships = getRelationships(parsingResult);
        Assert.assertEquals(10, relationships.size());
    }
}

class Constraint {

    public String name;

    @Getter @Setter
    public String type;
    @Getter @Setter
    public Set<String> targets;

    Constraint(String name) {
        this.name = name;
        targets = new HashSet<>();
    }
}

class Relationship {
    @Getter @Setter
    public String fragment;
    @Getter @Setter
    public String jppf;
    @Getter @Setter
    public String host;
    @Getter @Setter
    public Map<String, List<String>> hostingConstraints;

    Relationship(String fragment, String jppf, String host) {
        this.fragment = fragment;
        this.jppf = jppf;
        this.host = host;
        hostingConstraints = new HashMap<>();
    }

    public void addHostingConstraint(String name, List<String> hostingConstraint) {
        hostingConstraints.put(name, hostingConstraint);
    }
}