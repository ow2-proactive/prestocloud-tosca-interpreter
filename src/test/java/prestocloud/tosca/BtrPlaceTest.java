package prestocloud.tosca;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;
import org.prestocloud.tosca.model.definitions.ComplexPropertyValue;
import org.prestocloud.tosca.model.definitions.FilterDefinition;
import org.prestocloud.tosca.model.definitions.ListPropertyValue;
import org.prestocloud.tosca.model.definitions.PropertyConstraint;
import org.prestocloud.tosca.model.definitions.RequirementDefinition;
import org.prestocloud.tosca.model.definitions.ScalarPropertyValue;
import org.prestocloud.tosca.model.definitions.constraints.EqualConstraint;
import org.prestocloud.tosca.model.definitions.constraints.GreaterOrEqualConstraint;
import org.prestocloud.tosca.model.definitions.constraints.InRangeConstraint;
import org.prestocloud.tosca.model.definitions.constraints.ValidValuesConstraint;
import org.prestocloud.tosca.model.templates.Capability;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.prestocloud.tosca.model.templates.PolicyTemplate;
import org.prestocloud.tosca.model.types.NodeType;
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

import lombok.Getter;
import lombok.Setter;
import prestocloud.component.ICSARRepositorySearchService;
import prestocloud.model.common.Tag;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingException;
import prestocloud.tosca.parser.ParsingResult;
import prestocloud.tosca.parser.ToscaParser;
import prestocloud.tosca.repository.LocalRepositoryImpl;

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
            if (policyTemplate.getValue().getType().equalsIgnoreCase("prestocloud.placement.Ban")) {
                List<Object> excludedDevices = ((ListPropertyValue)policyTemplate.getValue().getProperties().get("excluded_devices")).getValue();
                for (Object excludedDevice : excludedDevices) {
                    constraint.addDevice((String)excludedDevice);
                }
            }
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
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment.jppf")) {
                // Look for the corresponding JPPF agent
                for (Map.Entry<String, NodeTemplate> nodeTemplateJPPF : nodeTemplates.entrySet()) {
                    if (nodeTemplateJPPF.getKey().equalsIgnoreCase(nodeTemplateFragment.getValue().getRelationships().get("execute").getTarget())) {
                        // Look for the corresponding node type
                        for (Map.Entry<String, NodeType> nodeTypeJPPF : parsingResult.getResult().getNodeTypes().entrySet()) {
                            if (nodeTypeJPPF.getKey().equalsIgnoreCase(nodeTemplateJPPF.getValue().getType())) {
                                Relationship relationship = new Relationship(nodeTemplateFragment.getKey(), nodeTemplateJPPF.getKey(), nodeTypeJPPF.getKey());
                                // Look for requirements
                                for (RequirementDefinition requirement : nodeTypeJPPF.getValue().getRequirements()) {
                                    if (requirement.getId().equalsIgnoreCase("master")) {
                                        relationship.setMaster(requirement.getType());
                                    }
                                    // Look for a requirement with nodeFilter's capabilities (hosting)
                                    if (requirement.getNodeFilter() != null) {
                                        for (Map.Entry<String, FilterDefinition> capability : requirement.getNodeFilter().getCapabilities().entrySet()) {
                                            // Find the host properties
                                            if (capability.getKey().equalsIgnoreCase("host")) {
                                                for (Map.Entry<String, List<PropertyConstraint>> properties : capability.getValue().getProperties().entrySet()) {
                                                    List<String> constraints = new ArrayList<>();
                                                    for (PropertyConstraint propertyConstraint : properties.getValue()) {
                                                        if (propertyConstraint instanceof InRangeConstraint) {
                                                            constraints.add("RangeMin: " + ((InRangeConstraint) propertyConstraint).getInRange().get(0) + ", RangeMax: " + ((InRangeConstraint) propertyConstraint).getInRange().get(1));
                                                        } else if (propertyConstraint instanceof EqualConstraint) {
                                                            constraints.add("Equal: " + ((EqualConstraint) propertyConstraint).getEqual());
                                                        } else if (propertyConstraint instanceof GreaterOrEqualConstraint) {
                                                            constraints.add("GreaterOrEqual: " + ((GreaterOrEqualConstraint) propertyConstraint).getGreaterOrEqual());
                                                        } else {
                                                            // Constraint not yet managed
                                                            System.out.println("Host constraint not managed: " + propertyConstraint.toString());
                                                        }
                                                    }
                                                    relationship.addHostingConstraint(properties.getKey(), constraints);
                                                }
                                            }
                                            // Find the OS properties
                                            if (capability.getKey().equalsIgnoreCase("os")) {
                                                for (Map.Entry<String, List<PropertyConstraint>> properties : capability.getValue().getProperties().entrySet()) {
                                                    List<String> constraints = new ArrayList<>();
                                                    for (PropertyConstraint propertyConstraint : properties.getValue()) {
                                                        if (propertyConstraint instanceof EqualConstraint) {
                                                            constraints.add(((EqualConstraint) propertyConstraint).getEqual());
                                                        } else if (propertyConstraint instanceof ValidValuesConstraint) {
                                                            constraints.addAll(((ValidValuesConstraint) propertyConstraint).getValidValues());
                                                        } else {
                                                            // Constraint not yet managed
                                                            System.out.println("OS constraint not managed: " + propertyConstraint.toString());
                                                        }
                                                    }
                                                    relationship.addOSConstraint(properties.getKey(), constraints);
                                                }
                                            }
                                            // Find the resource properties
                                            if (capability.getKey().equalsIgnoreCase("resource")) {
                                                for (Map.Entry<String, List<PropertyConstraint>> properties : capability.getValue().getProperties().entrySet()) {
                                                    List<String> constraints = new ArrayList<>();
                                                    for (PropertyConstraint propertyConstraint : properties.getValue()) {
                                                        if (propertyConstraint instanceof EqualConstraint) {
                                                            constraints.add(((EqualConstraint) propertyConstraint).getEqual());
                                                        } else if (propertyConstraint instanceof ValidValuesConstraint) {
                                                            constraints.addAll(((ValidValuesConstraint) propertyConstraint).getValidValues());
                                                        } else {
                                                            // Constraint not yet managed
                                                            System.out.println("Resource constraint not managed: " + propertyConstraint.toString());
                                                        }
                                                    }
                                                    relationship.addResourceConstraint(properties.getKey(), constraints);
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

    public List<String> getListOfCloudsFromMetadata(Map<String, String> metadata) {
        List<String> clouds = new ArrayList<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (entry.getKey().contains("ProviderName")) {
                String cloud_id = entry.getKey().split("_")[1];
                for (Map.Entry<String, String> entryCloud : metadata.entrySet()) {
                    if (entryCloud.getKey().contains("ProviderRequired" + "_" + cloud_id)) {
                        if (entryCloud.getValue().equalsIgnoreCase("true")) {
                            clouds.add(entry.getValue().toLowerCase());
                        }
                        break;
                    }
                    if (entryCloud.getKey().contains("ProviderExcluded" + "_" + cloud_id)) {
                        if (entryCloud.getValue().equalsIgnoreCase("false")) {
                            clouds.add(entry.getValue().toLowerCase());
                        }
                        break;
                    }
                }
            }
        }
        return clouds;
    }

    public String findBestSuitableVMType(String type, String region, Map<String, List<String>> hostingConstraints) {
        return null;
    }

    @Test
    public void testParsingVMTypes() throws IOException, ParsingException {
        Map<String, Map<String, Map<String, String>>> AmazonVMTypes = getCloudNodesTemplates("amazon-vm-templates.yml");
        Map<String, Map<String, Map<String, String>>> AzureVMTypes = getCloudNodesTemplates("azure-vm-templates.yml");
        Assert.assertEquals(617, AmazonVMTypes.size());
        Assert.assertEquals(824, AzureVMTypes.size());
    }

    @Test
    public void testParsingICCSExample() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "ICCS-example.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        Map<String, String> metadata = getMetadata(parsingResult);
        Assert.assertEquals(11, metadata.size());
        List<String> clouds = getListOfCloudsFromMetadata(metadata);

        List<Constraint> constraints = getConstraints(parsingResult);
        Assert.assertEquals(15, constraints.size());

        List<Relationship> relationships = getRelationships(parsingResult);
        Assert.assertEquals(10, relationships.size());
    }

    @Test
    public void testBtrPlaceComputation() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", "ICCS-example.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        Map<String, String> metadata = getMetadata(parsingResult);
        Assert.assertEquals(11, metadata.size());

        List<Constraint> constraints = getConstraints(parsingResult);
        Assert.assertEquals(15, constraints.size());

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
    @Getter @Setter
    public Set<String> devices;

    Constraint(String name) {
        this.name = name;
        targets = new HashSet<>();
        devices = new HashSet<>();
    }

    public void addDevice(String device) {
        devices.add(device);
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
    public String master;
    @Getter @Setter
    public Map<String, List<String>> hostingConstraints;
    @Getter @Setter
    public Map<String, List<String>> resourceConstraints;
    @Getter @Setter
    public Map<String, List<String>> osConstraints;

    Relationship(String fragment, String jppf, String host) {
        this.fragment = fragment;
        this.jppf = jppf;
        this.host = host;
        this.master = null;
        hostingConstraints = new HashMap<>();
        resourceConstraints = new HashMap<>();
        osConstraints = new HashMap<>();
    }

    public void addHostingConstraint(String name, List<String> hostingConstraint) {
        hostingConstraints.put(name, hostingConstraint);
    }

    public void addResourceConstraint(String name, List<String> resourceConstraint) {
        resourceConstraints.put(name, resourceConstraint);
    }

    public void addOSConstraint(String name, List<String> osConstraint) {
        osConstraints.put(name, osConstraint);
    }
}