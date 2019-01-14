package prestocloud.btrplace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Sets;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.btrplace.model.*;
import org.btrplace.model.constraint.*;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.scheduler.choco.ChocoScheduler;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import prestocloud.btrplace.minUsed.MinUsed;
import prestocloud.btrplace.tosca.ParsingUtils;
import prestocloud.btrplace.tosca.model.Constraint;
import prestocloud.btrplace.tosca.model.Docker;
import prestocloud.btrplace.tosca.model.RelationshipFaaS;
import prestocloud.btrplace.tosca.model.RelationshipJPPF;
import prestocloud.component.ICSARRepositorySearchService;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingException;
import prestocloud.tosca.parser.ParsingResult;
import prestocloud.tosca.parser.ToscaParser;
import prestocloud.tosca.repository.LocalRepositoryImpl;

import javax.annotation.Resource;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("BtrPlaceParsingTest")
public class BtrPlaceParsingTest {

    @Profile("BtrPlaceParsingTest")
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

    private final static String filesPath = "src/test/resources/prestocloud/";

    @Resource
    protected ToscaParser parser;

    @Resource
    protected ICSARRepositorySearchService csarRepositorySearchService;

    @Test
    public void testParsingVMTypes() throws IOException, ParsingException {
        Map<String, Map<String, Map<String, String>>> AmazonVMTypes = ParsingUtils.getCloudNodesTemplates(parser.parseFile(Paths.get(filesPath,"amazon-vm-templates.yml")), null);
        Map<String, Map<String, Map<String, String>>> AzureVMTypes = ParsingUtils.getCloudNodesTemplates(parser.parseFile(Paths.get(filesPath,"azure-vm-templates.yml")), null);
        Assert.assertEquals(617, AmazonVMTypes.size());
        Assert.assertEquals(824, AzureVMTypes.size());
    }

    @Test
    public void testParsingICCSExampleFaaSPoC() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(filesPath, "ICCS-example-poc.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        Map<String, String> metadata = ParsingUtils.getMetadata(parsingResult);
        Assert.assertEquals(11, metadata.size());

        List<String> clouds = ParsingUtils.getListOfCloudsFromMetadata(metadata);
        Assert.assertEquals(2, clouds.size());

        List<Constraint> constraints = ParsingUtils.getConstraints(parsingResult);
        Assert.assertEquals(1, constraints.size());

        List<RelationshipFaaS> relationships = ParsingUtils.getFaaSRelationships(parsingResult);
        Assert.assertEquals(2, relationships.size());
    }

    @Test
    public void testBtrPlaceComputationFaaSPoC() throws IOException, ParsingException {

        String tosca_file = "ICCS-example-poc.yml";
        String azure_region = "westeurope";
        String amazon_region = "eu-west-1";

        // Parse the TOSCA input file
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(filesPath, tosca_file));
        Map<String, String> metadata = ParsingUtils.getMetadata(parsingResult);
        List<String> supported_clouds = ParsingUtils.getListOfCloudsFromMetadata(metadata);
        List<RelationshipFaaS> relationships = ParsingUtils.getFaaSRelationships(parsingResult);
        List<Docker> dockers = ParsingUtils.getDockerParameters(parsingResult);
        List<Constraint> constraints = ParsingUtils.getConstraints(parsingResult);

        // Select a VM type for each host in each cloud
        // TODO: Parse involved edge devices to retrieve resources capacity
        Map<String, Map<String, String>> selected_vm_types = new HashMap<>();
        for (RelationshipFaaS relationship : relationships) {
            // If the resource may run on cloud(s), select best matching types
            if (relationship.getResourceConstraints().get("type").contains("cloud")) {
                // Loop for all clouds supported (metadata)
                Map<String, String> selected_types = new HashMap<>();
                for (String cloud : supported_clouds) {
                    String fragment = relationship.getFragment();
                    // TODO: manage desired regions in a better way
                    String selected_type = ParsingUtils.findBestSuitableVMType(parser, filesPath, cloud, cloud.equalsIgnoreCase("azure")?azure_region:amazon_region, relationship.getHostingConstraints());
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
        for (RelationshipFaaS relationship : relationships) {
            vms.put(relationship.getFragment(), mo.newVM());
        }

        /* Declare some edge devices (nodes)
        Map<String, Node> edge_nodes = new HashMap<>();
        // These edge devices are part of ban constraints
        edge_nodes.put("acfdgex98", mo.newNode());
        edge_nodes.put("kdsfk31fw", mo.newNode());
        edge_nodes.put("f2553fdfs", mo.newNode());
        edge_nodes.put("bd5fgdx32", mo.newNode());
        // Another two extras
        edge_nodes.put("deiuhde91", mo.newNode());
        edge_nodes.put("dzol52kza", mo.newNode());
        */

        // Declare the 2 public clouds
        // TODO: take them (along with the REGION) from METADATA
        Map<String, Node> public_clouds = new HashMap<>();
        public_clouds.put("amazon_" + amazon_region, mo.newNode());
        public_clouds.put("azure_" + azure_region, mo.newNode());

        // Set cpu
        ShareableResource cpu = new ShareableResource("cpu");
        mo.attach(cpu);
        /* Set edge devices cpu
        for (Map.Entry<String, Node> edge_node : edge_nodes.entrySet()) {
            cpu.setCapacity(edge_node.getValue(), 4);
        }*/
        // Pseudo infinite capacity for the public clouds.
        for (Map.Entry<String, Node> cloud : public_clouds.entrySet()) {
            cpu.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
        }
        // Set cpu consumption of fragments
        for (RelationshipFaaS relationship : relationships) {
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
        /* Set edge devices memory
        for (Map.Entry<String, Node> edge_node : edge_nodes.entrySet()) {
            mem.setCapacity(edge_node.getValue(), 2);
        }*/
        // Pseudo infinite capacity for the public clouds.
        for (Map.Entry<String, Node> cloud : public_clouds.entrySet()) {
            mem.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
        }
        // Set memory consumption of fragments
        for (RelationshipFaaS relationship : relationships) {
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
        /* Set edge devices disk
        for (Map.Entry<String, Node> edge_node : edge_nodes.entrySet()) {
            disk.setCapacity(edge_node.getValue(), 60);
        }*/
        // Pseudo infinite capacity for the public clouds.
        for (Map.Entry<String, Node> cloud : public_clouds.entrySet()) {
            disk.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
        }
        // Set disk consumption of fragments
        for (RelationshipFaaS relationship : relationships) {
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
        /* All edge nodes and public clouds are online and running
        for (Map.Entry<String, Node> edge_node : edge_nodes.entrySet()) {
            map.on(edge_node.getValue());
        }*/
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
            // Ban is for edge devices only
            /*if (constraint.getType().contains("Ban")) {
                Set<Node> constrained_nodes = new HashSet<>();
                for (String node : constraint.getDevices()) {
                    constrained_nodes.add(edge_nodes.get(node));
                }
                String vm = null;
                for (String target : constraint.getTargets()) {
                    vm = target;
                }
                cstrs.add(new Ban(vms.get(vm), Sets.newHashSet(constrained_nodes)));
            }*/
        }

        /* Detect cloud only & edge only constraints
        for (RelationshipFaaS relationship : relationships) {
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
        }*/

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
        System.out.println("FRAGMENT_NAME \t\t\t\t -> \tCLOUD_NAME / EDGE_NAME\t -> \tVM_TYPE");
        System.out.println("==================================================================================================================");
        for (Map.Entry<String, VM> vm : vms.entrySet()) {
            // Extract the destination node
            Node destination_node = map.getVMLocation(vm.getValue());
            /* Look into edge devices
            for (Map.Entry<String, Node> node : edge_nodes.entrySet()) {
                if (node.getValue().equals(destination_node)) {
                    System.out.println(vm.getKey() + "\t -> \t" + node.getKey());
                    break;
                }
            }*/
            // Look into public clouds devices
            for (Map.Entry<String, Node> node : public_clouds.entrySet()) {
                if (node.getValue().equals(destination_node)) {
                    System.out.print(vm.getKey() + "\t -> \t" + node.getKey() + "\t -> \t" + selected_vm_types.get(vm.getKey()).get(node.getKey().split("_")[0]));
                    for (Docker docker : dockers) {
                        if (docker.getFragmentName().equalsIgnoreCase(vm.getKey())) {
                            System.out.println(" \t -> \t docker run " +  docker.getImage() + " " + docker.getCmd());
                            break;
                        }
                    }
                    break;
                }
            }
            // TODO: look into private clouds
        }
        System.out.println("==================================================================================================================");

        // Format output to JSON
        JSONArray ja = new JSONArray();
        for (Map.Entry<String, VM> vm : vms.entrySet()) {
            // Extract the destination node
            Node destination_node = map.getVMLocation(vm.getValue());
            for (Map.Entry<String, Node> node : public_clouds.entrySet()) {
                if (node.getValue().equals(destination_node)) {
                    JSONObject jo = new JSONObject();
                    jo.put("fragment", vm.getKey());
                    jo.put("cloud", node.getKey().substring(0, node.getKey().lastIndexOf('_')));
                    jo.put("region", node.getKey().substring(node.getKey().lastIndexOf('_') + 1));
                    jo.put("type", selected_vm_types.get(vm.getKey()).get(node.getKey().split("_")[0]));
                    Optional<Docker> docker = dockers.stream().filter(d -> d.getFragmentName().equalsIgnoreCase(vm.getKey())).findFirst();
                    docker.ifPresent(docker1 -> jo.put("docker", "docker run " + docker1.getImage() + " " + docker1.getCmd()));
                    ja.add(jo);
                }
            }
        }
        String formattedOutput = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true).writeValueAsString(ja);
        System.out.println(formattedOutput);

        // Write file
        FileWriter file = new FileWriter("btrplace.output");
        file.write(formattedOutput);
        file.flush();
        file.close();
    }

    @Test
    public void testParsingICCSExampleJPPF() throws IOException, ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(filesPath, "ICCS-example-jppf.yml"));
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        Map<String, String> metadata = ParsingUtils.getMetadata(parsingResult);
        Assert.assertEquals(14, metadata.size());

        List<String> clouds = ParsingUtils.getListOfCloudsFromMetadata(metadata);
        Assert.assertEquals(2, clouds.size());

        List<Constraint> constraints = ParsingUtils.getConstraints(parsingResult);
        Assert.assertEquals(15, constraints.size());

        List<RelationshipJPPF> relationships = ParsingUtils.getJPPFRelationships(parsingResult);
        Assert.assertEquals(10, relationships.size());
    }

    @Test
    public void testBtrPlaceComputationJPPF() throws IOException, ParsingException {

        String tosca_file = "ICCS-example-jppf.yml";

        // Parse the TOSCA input file
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(filesPath, tosca_file));
        Map<String, String> metadata = ParsingUtils.getMetadata(parsingResult);
        List<String> supported_clouds = ParsingUtils.getListOfCloudsFromMetadata(metadata);
        List<RelationshipJPPF> relationships = ParsingUtils.getJPPFRelationships(parsingResult);
        List<Constraint> constraints = ParsingUtils.getConstraints(parsingResult);

        // Select a VM type for each host in each cloud
        // TODO: Parse involved edge devices to retrieve resources capacity
        Map<String, Map<String, String>> selected_vm_types = new HashMap<>();
        for (RelationshipJPPF relationship : relationships) {
            // If the resource may run on cloud(s), select best matching types
            if (relationship.getResourceConstraints().get("type").contains("cloud")) {
                // Loop for all clouds supported (metadata)
                Map<String, String> selected_types = new HashMap<>();
                for (String cloud : supported_clouds) {
                    String fragment = relationship.getFragment();
                    // TODO: manage desired regions in a better way
                    String selected_type = ParsingUtils.findBestSuitableVMType(parser, filesPath, cloud, cloud.equalsIgnoreCase("azure")?"francecentral":"eu-west-1", relationship.getHostingConstraints());
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
        for (RelationshipJPPF relationship : relationships) {
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
        for (RelationshipJPPF relationship : relationships) {
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
        for (RelationshipJPPF relationship : relationships) {
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
        for (RelationshipJPPF relationship : relationships) {
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
        for (RelationshipJPPF relationship : relationships) {
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
