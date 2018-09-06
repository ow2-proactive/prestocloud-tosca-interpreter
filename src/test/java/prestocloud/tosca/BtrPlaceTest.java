package prestocloud.tosca;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import org.btrplace.model.*;
import org.btrplace.model.constraint.*;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.scheduler.choco.ChocoScheduler;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.prestocloud.tosca.model.definitions.*;
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
import org.springframework.context.annotation.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import prestocloud.btrplace.PrestoCloudExtensions;
import prestocloud.btrplace.minUsed.MinUsed;
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

    public Map<String, Map<String, Map<String, String>>> getCloudNodesTemplates(String templateFile, String region) throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", templateFile));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        Map<String, Map<String, Map<String, String>>> vmTypes = new HashMap<>();

        // Look for node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplate : nodeTemplates.entrySet()) {

            // Discard VM types in the wrong region
            if (region != null && !region.equalsIgnoreCase(((ComplexPropertyValue)nodeTemplate.getValue().getCapabilities().get("resource").getProperties().get("cloud")).getValue().get("cloud_region").toString())) {
                continue;
            }

            // Look for capabilities
            Map<String, Map<String, String>> typesCapabilities = new HashMap<>();
            for (Map.Entry<String, Capability> capabilities : nodeTemplate.getValue().getCapabilities().entrySet()) {
                // Get 'host' capability properties
                if (capabilities.getKey().equalsIgnoreCase("host")) {
                    Map<String, String> hostCapabilities = new HashMap<>();
                    //System.out.println("Host properties: ");
                    for (Map.Entry<String, AbstractPropertyValue> properties : capabilities.getValue().getProperties().entrySet()) {
                        if (properties.getKey().equalsIgnoreCase("num_cpus")) {
                            String num_cpus = ((ScalarPropertyValue)properties.getValue()).getValue();
                            hostCapabilities.put("num_cpus", num_cpus);
                            //System.out.println("- " + properties.getKey() + " = " + num_cpus);
                        }
                        if (properties.getKey().equalsIgnoreCase("mem_size")) {
                            String mem_size = ((ScalarPropertyValue)properties.getValue()).getValue();
                            hostCapabilities.put("mem_size", mem_size);
                            //System.out.println("- " + properties.getKey() + " = " + mem_size);
                        }
                        if (properties.getKey().equalsIgnoreCase("disk_size")) {
                            if (properties.getValue() != null) {
                                String disk_size = ((ScalarPropertyValue) properties.getValue()).getValue();
                                hostCapabilities.put("disk_size", disk_size);
                                //System.out.println("- " + properties.getKey() + " = " + disk_size);
                            }
                        }
                        if (properties.getKey().equalsIgnoreCase("price")) {
                            String price = ((ScalarPropertyValue)properties.getValue()).getValue();
                            hostCapabilities.put("price", price);
                            //System.out.println("- " + properties.getKey() + " = " + price);
                        }
                    }
                    typesCapabilities.put(capabilities.getKey(), hostCapabilities);
                }
                // Get 'resource' capability properties
                if (capabilities.getKey().equalsIgnoreCase("resource")) {
                    Map<String, String> resourceCapabilities = new HashMap<>();
                    //System.out.println("Resource properties: ");
                    String type = null, name = null;
                    for (Map.Entry<String, AbstractPropertyValue> properties : capabilities.getValue().getProperties().entrySet()) {
                        if (properties.getKey().equalsIgnoreCase("type")) {
                            type = ((ScalarPropertyValue) properties.getValue()).getValue();
                            resourceCapabilities.put("type", type);
                            //System.out.println("- " + properties.getKey() + " = " + type);
                        }
                        if (properties.getKey().equalsIgnoreCase("name")) {
                            name = ((ScalarPropertyValue) properties.getValue()).getValue();
                            resourceCapabilities.put("name", name);
                            //System.out.println("- " + properties.getKey() + " = " + type);
                        }
                        // Cloud based resource detected
                        if (type != null && type.equalsIgnoreCase("cloud") && properties.getKey().equalsIgnoreCase("cloud")) {
                            //System.out.println("- " + properties.getKey() + ":");
                            ComplexPropertyValue cloud = (ComplexPropertyValue) properties.getValue();
                            for (Map.Entry<String, Object> cloudProperties : cloud.getValue().entrySet()) {
                                if (cloudProperties.getKey().equalsIgnoreCase("cloud_type")) {
                                    resourceCapabilities.put("cloud_type", cloudProperties.getValue().toString());
                                    //System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                }
                                if (cloudProperties.getKey().equalsIgnoreCase("cloud_region")) {
                                    resourceCapabilities.put("cloud_region", cloudProperties.getValue().toString());
                                    //System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                }
                                if (cloudProperties.getKey().equalsIgnoreCase("cloud_name")) {
                                    resourceCapabilities.put("cloud_name", cloudProperties.getValue().toString());
                                    //System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                }
                                // Get networking information
                                if (cloudProperties.getKey().equalsIgnoreCase("cloud_network")) {
                                    //System.out.println(" - " + cloudProperties.getKey() + ":");
                                    HashMap<String, Object> cloud_network = (HashMap<String, Object>) cloudProperties.getValue();
                                    for (Map.Entry<String, Object> cloudNetworkProperties : cloud_network.entrySet()) {
                                        if (cloudNetworkProperties.getKey().equalsIgnoreCase("network_id")) {
                                            //System.out.println("  - " + cloudNetworkProperties.getKey() + " = " + cloudNetworkProperties.getValue());
                                        }
                                        if (cloudNetworkProperties.getKey().equalsIgnoreCase("network_name")) {
                                            //System.out.println("  - " + cloudNetworkProperties.getKey() + " = " + cloudNetworkProperties.getValue());
                                        }
                                        if (cloudNetworkProperties.getKey().equalsIgnoreCase("addresses")) {
                                            //System.out.println("  - " + cloudNetworkProperties.getKey());
                                            for (String address : (List<String>) cloudNetworkProperties.getValue()) {
                                                //System.out.println("    - " + address);
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

    // TODO: PARSE MASTER AS WELL!!!
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

    public String findBestSuitableVMType(String cloud, String region, Map<String, List<String>> hostingConstraints) throws IOException, ParsingException {

        // TODO: MAKE A LIST OF CANDIDATES AND SELECT THE LOWEST PRICE

        Map<String, Map<String, Map<String, String>>> VMTypes;

        if (cloud.equalsIgnoreCase("amazon")) {
            VMTypes = getCloudNodesTemplates("amazon-vm-templates.yml", region);
        }
        else if (cloud.equalsIgnoreCase("azure")) {
            VMTypes = getCloudNodesTemplates("azure-vm-templates.yml", region);
        }
        else if (cloud.equalsIgnoreCase("openstack")) {
            System.out.println("OpenStack types not yet defined (flavors must be customized).");
            return null;
        }
        else {
            System.out.println("Cloud of type " + cloud + " not found.");
            return null;
        }

        // Extract hosting infos from local data struct
        boolean cpu, mem, disk, price;
        for (Map.Entry<String, Map<String, Map<String, String>>> hostingConstraint : VMTypes.entrySet()) {
            hostingConstraint.getKey();
            cpu = false;
            mem = false;
            disk = false;
            price = false;

            if (hostingConstraint.getValue().containsKey("host")) {

                // Compare cpu
                if (hostingConstraints.containsKey("num_cpus")) {
                    if (hostingConstraint.getValue().get("host").containsKey("num_cpus")) {
                        int required, available;
                        String required_num_cpus = hostingConstraints.get("num_cpus").get(0);
                        if (required_num_cpus.contains("RangeMin")) {
                            required = Integer.valueOf(required_num_cpus.split(",")[0].split(" ")[1]);
                        }
                        else if (required_num_cpus.contains("GreaterOrEqual")) {
                            required = Integer.valueOf(required_num_cpus.split(" ")[1]);
                        }
                        else {
                            required = Integer.valueOf(required_num_cpus);
                        }
                        available = Integer.valueOf(hostingConstraint.getValue().get("host").get("num_cpus"));
                        if (required <= available) {
                            //System.out.println("Good type or cpu: " + hostingConstraint.getKey());
                            cpu = true;
                        }
                    }
                }
                else {
                    System.out.println("No cpu constraint for : " + hostingConstraints);
                    cpu = true;
                }

                // Compare memory
                if (hostingConstraints.containsKey("mem_size")) {
                    if (hostingConstraint.getValue().get("host").containsKey("mem_size")) {
                        double required, available;
                        String required_mem_size = hostingConstraints.get("mem_size").get(0);
                        if (required_mem_size.contains("RangeMin")) {
                            required = Double.valueOf(required_mem_size.split(",")[0].split(" ")[1]);
                        }
                        else if (required_mem_size.contains("GreaterOrEqual")) {
                            required = Double.valueOf(required_mem_size.split(" ")[1]);
                        }
                        else {
                            required = Double.valueOf(required_mem_size);
                        }
                        available = Double.valueOf(hostingConstraint.getValue().get("host").get("mem_size").split(" ")[0]);
                        if (required <= available) {
                            //System.out.println("Good type for mem: " + hostingConstraint.getKey());
                            mem = true;
                        }
                    }
                }
                else {
                    mem = true;
                }

                // Compare disk
                if (hostingConstraints.containsKey("disk_size")) {
                    if (hostingConstraint.getValue().get("host").containsKey("disk_size")) {
                        double required, available;
                        String required_disk_size = hostingConstraints.get("disk_size").get(0);
                        if (required_disk_size.contains("RangeMin")) {
                            required = Double.valueOf(required_disk_size.split(",")[0].split(" ")[1]);
                        }
                        else if (required_disk_size.contains("GreaterOrEqual")) {
                            required = Double.valueOf(required_disk_size.split(" ")[1]);
                        }
                        else {
                            required = Double.valueOf(required_disk_size);
                        }
                        available = Double.valueOf(hostingConstraint.getValue().get("host").get("disk_size").split(" ")[0]);
                        if (required <= available) {
                            //System.out.println("Good type for disk: " + hostingConstraint.getKey());
                            disk = true;
                        }
                    }
                }
                else {
                    disk = true;
                }

                // Compare price
                if (hostingConstraints.containsKey("price")) {
                    if (hostingConstraint.getValue().get("host").containsKey("price")) {
                        double required, available;
                        String required_price = hostingConstraints.get("price").get(0);
                        if (required_price.contains("RangeMin")) {
                            required = Double.valueOf(required_price.split(",")[0].split(" ")[1]);
                        }
                        else if (required_price.contains("GreaterOrEqual")) {
                            required = Double.valueOf(required_price.split(" ")[1]);
                        }
                        else {
                            required = Double.valueOf(required_price);
                        }
                        available = Double.valueOf(hostingConstraint.getValue().get("host").get("price"));
                        if (required <= available) {
                            //System.out.println("Good type for price: " + hostingConstraint.getKey());
                            price = true;
                        }
                    }
                }
                else {
                    price = true;
                }
                if (cpu && mem && disk && price) {
                    //System.out.println("Best suitable type found: " + hostingConstraint.getKey());
                    return hostingConstraint.getValue().get("cloud").get("name");
                }
            }
        }
        System.out.println("No suitable type found for hosting constraints: " + hostingConstraints);
        return null;
    }

    @Test
    public void testParsingVMTypes() throws IOException, ParsingException {
        Map<String, Map<String, Map<String, String>>> AmazonVMTypes = getCloudNodesTemplates("amazon-vm-templates.yml", null);
        Map<String, Map<String, Map<String, String>>> AzureVMTypes = getCloudNodesTemplates("azure-vm-templates.yml", null);
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

        String tosca_file = "ICCS-example.yml";

        // Parse the TOSCA input file
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get("src/test/resources/prestocloud/", tosca_file));
        Map<String, String> metadata = getMetadata(parsingResult);
        List<String> supported_clouds = getListOfCloudsFromMetadata(metadata);
        List<Relationship> relationships = getRelationships(parsingResult);
        List<Constraint> constraints = getConstraints(parsingResult);

        // Select a VM type for each host in each cloud
        // TODO: Parse involved edge devices to retrieve resources capacity
        Map<String, Map<String, String>> selected_vm_types = new HashMap<>();
        for (Relationship relationship : relationships) {
            // If the resource may run on cloud(s), select best matching types
            if (relationship.getResourceConstraints().get("type").contains("cloud")) {
                // Loop for all clouds supported (metadata)
                Map<String, String> selected_types = new HashMap<>();
                for (String cloud : supported_clouds) {
                    String fragment = relationship.getFragment();
                    // TODO: manage desired regions in a better way
                    String selected_type = findBestSuitableVMType(cloud, cloud.equalsIgnoreCase("azure")?"francecentral":"eu-west-1", relationship.getHostingConstraints());
                    selected_types.put(cloud.toLowerCase(), selected_type);
                    selected_vm_types.put(fragment, selected_types);
                }
            }
            else {
                System.out.println("Edge only resource found: " + relationship.getHost());
            }
        }

        // Initialize BtrPlace model and mapping
        Model mo = new DefaultModel();
        Mapping map = mo.getMapping();

        // Create fragments (VMs)
        Map<String, VM> vms = new HashMap<>();
        for (Relationship relationship : relationships) {
            vms.put(relationship.getFragment(), mo.newVM());
        }

        // Declare some edge devices (nodes)
        Map<String, Node> edge_nodes = new HashMap<>();
        // These edge devices are part of ban constraints
        edge_nodes.put("acfdgex98", mo.newNode());
        edge_nodes.put("kdsfk31fw", mo.newNode());
        edge_nodes.put("f2553fdfs", mo.newNode());
        edge_nodes.put("bd5fgdx32", mo.newNode());
        // Another two extras
        edge_nodes.put("deiuhde91", mo.newNode());
        edge_nodes.put("dzol52kza", mo.newNode());

        // Declare the 2 public clouds
        // TODO: take them (along with the REGION) from METADATA
        Map<String, Node> public_clouds = new HashMap<>();
        public_clouds.put("amazon_eu_west_1", mo.newNode());
        public_clouds.put("azure_france_central", mo.newNode());

        // Set cpu
        ShareableResource cpu = new ShareableResource("cpu");
        mo.attach(cpu);
        // Set edge devices cpu
        for (Map.Entry<String, Node> edge_node : edge_nodes.entrySet()) {
            cpu.setCapacity(edge_node.getValue(), 4);
        }
        // Pseudo infinite capacity for the public clouds.
        for (Map.Entry<String, Node> cloud : public_clouds.entrySet()) {
            cpu.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
        }
        // Set cpu consumption of fragments
        for (Relationship relationship : relationships) {
            String memory_required = relationship.hostingConstraints.get("mem_size").get(0);
            int constraint;
            if (memory_required.contains("RangeMin")) {
                constraint = Integer.valueOf(memory_required.split(",")[0].split(" ")[1]);
            }
            else if (memory_required.contains("GreaterOrEqual")) {
                constraint = Integer.valueOf(memory_required.split(" ")[1]);
            }
            else {
                constraint = Integer.valueOf(memory_required);
            }
            cpu.setConsumption(vms.get(relationship.getFragment()), constraint);
        }

        // Set memory
        ShareableResource mem = new ShareableResource("memory");
        mo.attach(mem);
        // Set edge devices memory
        for (Map.Entry<String, Node> edge_node : edge_nodes.entrySet()) {
            mem.setCapacity(edge_node.getValue(), 2);
        }
        // Pseudo infinite capacity for the public clouds.
        for (Map.Entry<String, Node> cloud : public_clouds.entrySet()) {
            mem.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
        }
        // Set memory consumption of fragments
        for (Relationship relationship : relationships) {
            String memory_required = relationship.hostingConstraints.get("mem_size").get(0);
            int constraint;
            if (memory_required.contains("RangeMin")) {
                constraint = (int)Math.round(Double.valueOf(memory_required.split(",")[0].split(" ")[1]));
            }
            else if (memory_required.contains("GreaterOrEqual")) {
                constraint = (int)Math.round(Double.valueOf(memory_required.split(" ")[1]));
            }
            else {
                constraint = (int)Math.round(Double.valueOf(memory_required));
            }
            mem.setConsumption(vms.get(relationship.getFragment()), constraint);
        }

        // Set disk
        ShareableResource disk = new ShareableResource("disk");
        mo.attach(disk);
        // Set edge devices disk
        for (Map.Entry<String, Node> edge_node : edge_nodes.entrySet()) {
            disk.setCapacity(edge_node.getValue(), 60);
        }
        // Pseudo infinite capacity for the public clouds.
        for (Map.Entry<String, Node> cloud : public_clouds.entrySet()) {
            disk.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
        }
        // Set disk consumption of fragments
        for (Relationship relationship : relationships) {
            String disk_required = relationship.hostingConstraints.get("mem_size").get(0);
            int constraint;
            if (disk_required.contains("RangeMin")) {
                constraint = (int)Math.round(Double.valueOf(disk_required.split(",")[0].split(" ")[1]));
            }
            else if (disk_required.contains("GreaterOrEqual")) {
                constraint = (int)Math.round(Double.valueOf(disk_required.split(" ")[1]));
            }
            else {
                constraint = (int)Math.round(Double.valueOf(disk_required));
            }
            disk.setConsumption(vms.get(relationship.getFragment()), constraint);
        }

        // Initial model setup
        final List<SatConstraint> cstrs = new ArrayList<>();
        // All edge nodes and public clouds are online and running
        for (Map.Entry<String, Node> edge_node : edge_nodes.entrySet()) {
            map.on(edge_node.getValue());
        }
        for (Map.Entry<String, Node> public_cloud : public_clouds.entrySet()) {
            map.on(public_cloud.getValue());
        }
        cstrs.addAll(Online.newOnline(mo.getMapping().getAllNodes()));
        // Initial placement. VMs ready to be deployed
        for (Map.Entry<String, VM> vm : vms.entrySet()) {
            map.ready(vm.getValue());
        }
        cstrs.addAll(Running.newRunning(mo.getMapping().getAllVMs()));

        // Apply placement constraints
        for (Constraint constraint : constraints) {
            // TODO: manage more constraints
            if (constraint.getType().contains("Spread")) {
                Set<VM> constrained_vms = new HashSet<>();
                for (String target : constraint.getTargets()) {
                    constrained_vms.add(vms.get(target));
                }
                cstrs.add(new Spread(constrained_vms));
            }
            if (constraint.getType().contains("Gather")) {
                Set<VM> constrained_vms = new HashSet<>();
                for (String target : constraint.getTargets()) {
                    constrained_vms.add(vms.get(target));
                }
                cstrs.add(new Gather(constrained_vms));
            }
            if (constraint.getType().contains("Ban")) {
                Set<Node> constrained_nodes = new HashSet<>();
                for (String node : constraint.getDevices()) {
                    constrained_nodes.add(edge_nodes.get(node));
                }
                String vm = null;
                for (String target : constraint.getTargets()) {
                    vm = target;
                }
                cstrs.add(new Ban(vms.get(vm), Sets.newHashSet(constrained_nodes)));
            }
        }

        // Detect cloud only & edge only constraints
        for (Relationship relationship : relationships) {
            // If the resource can only be run on cloud or edge => this is a constraint!
            if (relationship.getResourceConstraints().get("type").size() == 1) {
                // Here we ban edge devices
                if(relationship.getResourceConstraints().get("type").get(0).equalsIgnoreCase("cloud")) {
                    cstrs.add(new Ban(vms.get(relationship.getFragment()), Sets.newHashSet(edge_nodes.values())));
                }
                // Here we ban all clouds (public & private)
                if(relationship.getResourceConstraints().get("type").get(0).equalsIgnoreCase("edge")) {
                    cstrs.add(new Ban(vms.get(relationship.getFragment()), Sets.newHashSet(public_clouds.values())));
                    // TODO: add private clouds if defined
                }
            }
        }

        // Create an instance, set the objective, start an optimized scheduler and solve the problem
        Instance ii = new Instance(mo, cstrs, new MinUsed(Sets.newHashSet(public_clouds.values())));
        final ChocoScheduler sched = PrestoCloudExtensions.newScheduler();
        sched.doOptimize(true);
        ReconfigurationPlan p = sched.solve(ii);
        Assert.assertNotNull(p);

        /*
        //Show the results
        System.out.println("-- Computation done: ");
        System.out.println(p);
        */

        // Get the new model and mapping
        mo = p.getResult();
        map = mo.getMapping();

        // Print statistics
        System.out.println("==================================================================================================================");
        System.out.println("Solver statistics:");
        System.out.println("==================================================================================================================");
        System.out.println(sched.getStatistics());
        System.out.println("==================================================================================================================");

        // Show the resulting placement
        System.out.println("==================================================================================================================");
        System.out.println("FRAGMENT_NAME \t\t\t\t\t\t\t\t\t\t -> \tCLOUD_NAME / EDGE_NAME\t -> \tVM_TYPE");
        System.out.println("==================================================================================================================");
        for (Map.Entry<String, VM> vm : vms.entrySet()) {
            // Extract the destination node
            Node destination_node = map.getVMLocation(vm.getValue());
            // Look into edge devices
            for (Map.Entry<String, Node> node : edge_nodes.entrySet()) {
                if (node.getValue().equals(destination_node)) {
                    System.out.println(vm.getKey() + "\t -> \t" + node.getKey());
                    break;
                }
            }
            // Look into public clouds devices
            for (Map.Entry<String, Node> node : public_clouds.entrySet()) {
                if (node.getValue().equals(destination_node)) {
                    System.out.println(vm.getKey() + "\t -> \t" + (node.getKey().contains("amazon")?(node.getKey()+"\t"):node.getKey()) + "\t -> \t" + selected_vm_types.get(vm.getKey()).get(node.getKey().split("_")[0]));
                    break;
                }
            }
            // TODO: look into private clouds
        }
        System.out.println("==================================================================================================================");
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