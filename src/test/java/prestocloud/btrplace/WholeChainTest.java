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
import prestocloud.btrplace.tosca.model.*;
import prestocloud.btrplace.tosca.model.PlacementConstraint;
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
@ActiveProfiles("WholeChainTest")
public class WholeChainTest {

    @Profile("WholeChainTest")
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

    /**
     * Simple helper method to retrieve the selected VM type for a fragment name *or* a node name (eg. proxy, master, etc.).
     *
     * @param selectedCloudVMTypes HashMap of all selected cloud VM types
     * @param vmName name of the fragment *or* the node/host to look for
     * @param nodeName name of the computed destination cloud
     * @return the selected VM type, `null` if not found.
     */
    private String getSelectedCloudVMType(Map<String, Map<String, Map<String, Map<String, String>>>> selectedCloudVMTypes, String vmName, String nodeName) {
        if (selectedCloudVMTypes.get(vmName) != null) {
            return selectedCloudVMTypes.get(vmName).get("execute").get(selectedCloudVMTypes.get(vmName).get("execute").keySet().stream().findFirst().get()).get(nodeName.split("_")[0]);
        }
        else {
            for (Map.Entry<String, Map<String, Map<String, Map<String, String>>>> selectedFragmentTypes : selectedCloudVMTypes.entrySet()) {
                for (Map.Entry<String, Map<String, Map<String, String>>> selectedTypes : selectedFragmentTypes.getValue().entrySet()) {
                    if (selectedTypes.getValue().containsKey(vmName)) {
                        // Assume that cloud name and region name are concatenated with '_'
                        return selectedTypes.getValue().get(vmName).get(nodeName.split("_")[0]);
                    }
                }
            }
        }
        // This should never return null as a matching type must have be found for each VM
        return null;
    }

    @Test
    //public void processToscaWithBtrPlace(String resourcesPath, String instanceLevelTOSCAFile, String outputFile) throws ParsingException, IOException {
    public void processToscaWithBtrPlace() throws ParsingException, IOException {

        String resourcesPath = filesPath; // "src/main/resources/repository";
        String typeLevelTOSCAFile = filesPath + "ICCS-example-v6.yml";
        String outputFile = filesPath + "test.json";

        String azure_region = "westeurope";
        String amazon_region = "eu-west-1";

        // Parse the type level TOSCA file
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(typeLevelTOSCAFile));

        // Extract all required information
        Map<String, String> metadata = ParsingUtils.getMetadata(parsingResult);
        List<String> supportedClouds = ParsingUtils.getListOfCloudsFromMetadata(metadata);
        List<Relationship> relationships = ParsingUtils.getRelationships(parsingResult);
        List<PlacementConstraint> placementConstraints = ParsingUtils.getConstraints(parsingResult);
        List<OptimizationVariables> optimizationVariables = ParsingUtils.getOptimizationVariables(parsingResult);
        List<Docker> dockers = ParsingUtils.getDockers(parsingResult);
        // TODO: deal with health checks
        List<HealthCheck> healthChecks = ParsingUtils.getHealthChecks(parsingResult);

        // Select the best VM type for each host in each available cloud
        // Map structure: fragment name -> requirement type -> node name -> cloud name -> VM type
        Map<String, Map<String, Map<String, Map<String, String>>>> selectedCloudVMTypes = new HashMap<>();
        for (Relationship relationship : relationships) {
            Map<String, Map<String, Map<String, String>>> allSelectedTypesWithRequirement = new HashMap<>();
            Map<String, Map<String, String>> allSelectedTypes = new HashMap<>();
            for (ConstrainedNode constrainedNode : relationship.getAllConstrainedNodes()) {
                for (NodeConstraints nodeConstraints : constrainedNode.getConstraints()) {
                    if (!nodeConstraints.getResourceConstraints().isEmpty()) {
                        // If the resource may run on cloud(s), select best matching types
                        if (nodeConstraints.getResourceConstraints().get("type").contains("cloud")) {
                            // Loop for all clouds supported (metadata)
                            Map<String, String> selectedTypes = new HashMap<>();
                            for (String cloud : supportedClouds) {
                                // TODO: manage desired regions in a better way
                                String selected_type = ParsingUtils.findBestSuitableVMType(
                                        parser,
                                        resourcesPath,
                                        cloud,
                                        cloud.equalsIgnoreCase("azure") ? azure_region : amazon_region,
                                        nodeConstraints.getHostingConstraints());
                                selectedTypes.put(cloud.toLowerCase(), selected_type);
                            }
                            allSelectedTypes.put(constrainedNode.getName(), selectedTypes);
                            allSelectedTypesWithRequirement.put(constrainedNode.getType(), allSelectedTypes);
                        } else {
                            System.out.println("Edge-only hosting resource constraint found: " + constrainedNode.getName());
                        }
                    }
                }
            }
            selectedCloudVMTypes.put(relationship.getFragmentName(), allSelectedTypesWithRequirement);
        }

        // Initialize BtrPlace model and mapping
        Model mo = new DefaultModel();
        Mapping map = mo.getMapping();

        // Create VMs
        Map<String, VM> vms = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Map<String, String>>>> selectedFragmentTypes : selectedCloudVMTypes.entrySet()) {
            for (Map.Entry<String, Map<String, Map<String, String>>> selectedTypes : selectedFragmentTypes.getValue().entrySet()) {
                // Add the fragment's execute node first
                if (selectedTypes.getKey().equalsIgnoreCase("execute")) {
                    vms.put(selectedFragmentTypes.getKey(), mo.newVM());
                }
                else {
                    // Remove duplicates (eg. a 'proxy' may be linked to multiple fragments)
                    String nodeName = selectedTypes.getValue().keySet().stream().findFirst().get();
                    if (!vms.containsKey(nodeName)) {
                        vms.put(nodeName, mo.newVM());
                    }
                }
            }
        }

        // TODO: declare edge devices (depending how we get them)
        /* Declare some edge devices (nodes)
        Map<String, Node> edge_nodes = new HashMap<>();
        // These edge devices are part of ban constraints
        edge_nodes.put("acfdgex98", mo.newNode());
        edge_nodes.put("kdsfk31fw", mo.newNode());
        edge_nodes.put("f2553fdfs", mo.newNode());
        edge_nodes.put("bd5fgdx32", mo.newNode());
        */

        // Declare the 2 public clouds
        // TODO: take them (along with the REGION) from METADATA
        Map<String, Node> public_clouds = new HashMap<>();
        public_clouds.put("amazon_" + amazon_region, mo.newNode());
        public_clouds.put("azure_" + azure_region, mo.newNode());

        // Create and attach cpu, memory and disk resources
        ShareableResource cpu = new ShareableResource("cpu");
        ShareableResource mem = new ShareableResource("memory");
        ShareableResource disk = new ShareableResource("disk");
        mo.attach(cpu);
        mo.attach(mem);
        mo.attach(disk);

        // TODO: set cpu for edge devices
        /* Set edge devices cpu
        for (Map.Entry<String, Node> edge_node : edge_nodes.entrySet()) {
            cpu.setCapacity(edge_node.getValue(), 4);
        }*/

        // TODO: set memory for edge devices
        /* Set edge devices memory
        for (Map.Entry<String, Node> edge_node : edge_nodes.entrySet()) {
            mem.setCapacity(edge_node.getValue(), 2);
        }*/

        // TODO: set disk for edge devices
        /* Set edge devices disk
        for (Map.Entry<String, Node> edge_node : edge_nodes.entrySet()) {
            disk.setCapacity(edge_node.getValue(), 60);
        }*/

        // Pseudo infinite capacity for the public clouds.
        for (Map.Entry<String, Node> cloud : public_clouds.entrySet()) {
            cpu.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
            mem.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
            disk.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
        }

        // Set consumption of all required nodes/hosts
        for (Relationship relationship : relationships) {
            for (ConstrainedNode constrainedNode : relationship.getAllConstrainedNodes()) {
                for (NodeConstraints nodeConstraints : constrainedNode.getConstraints()) {
                    if (!nodeConstraints.getResourceConstraints().isEmpty()) {
                        // If the resource may run on cloud(s), select best matching types
                        if (nodeConstraints.getResourceConstraints().get("type").contains("cloud")) {
                            // Prepare VM name depending on requirement type ('execute' targets the fragment name, others target node/host name)
                            String vmName = constrainedNode.getType().equalsIgnoreCase("execute") ? relationship.getFragmentName() : constrainedNode.getType();
                            for (Map.Entry<String, List<String>> hostingConstraint : nodeConstraints.getHostingConstraints().entrySet()) {
                                String required = hostingConstraint.getValue().get(0);

                                // TODO: manage more value constraints if needed
                                int constraint;
                                if (required.contains("RangeMin")) {
                                    constraint = Integer.valueOf(required.split(",")[0].split(" ")[1]);
                                } else if (required.contains("GreaterOrEqual")) {
                                    constraint = Integer.valueOf(required.split(" ")[1]);
                                } else {
                                    constraint = Integer.valueOf(required);
                                }

                                // TODO: manage more units if needed
                                if (hostingConstraint.getKey().equalsIgnoreCase("num_cpus")) {
                                    cpu.setConsumption(vms.get(vmName), constraint);
                                } else if (hostingConstraint.getKey().equalsIgnoreCase("mem_size")) {
                                    // Mem in GB only
                                    if (required.contains("MB")) {
                                        constraint = Math.round(constraint / 1024);
                                    }
                                    mem.setConsumption(vms.get(vmName), constraint);
                                } else if (hostingConstraint.getKey().equalsIgnoreCase("storage_size")) {
                                    // Storage in GB only
                                    if (required.contains("MB")) {
                                        constraint = Math.round(constraint / 1024);
                                    }
                                    disk.setConsumption(vms.get(vmName), constraint);
                                } else {
                                    System.out.println("Unrecognized hosting constraint: " + hostingConstraint.getKey());
                                }
                            }
                        }
                    }
                }
            }
        }

        // Initial model setup
        final List<SatConstraint> cstrs = new ArrayList<>();

        // TODO: set the state for edge devices
        // All edge nodes are online and running
        /*for (Map.Entry<String, Node> edge_node : edge_nodes.entrySet()) {
            map.on(edge_node.getValue());
        }*/

        // All public clouds are online and running
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
        for (PlacementConstraint placementConstraint : placementConstraints) {
            // TODO: manage more constraints if needed
            if (placementConstraint.getType().contains("Spread")) {
                Set<VM> constrained_vms = new HashSet<>();
                for (String target : placementConstraint.getTargets()) {
                    // Check if the VM actually exists in case edge devices are not yet managed
                    if (vms.get(target) != null) {
                        constrained_vms.add(vms.get(target));
                    }
                    else {
                        System.out.println("Non consistent 'Gather' constraint detected for: " + target);
                    }
                }
                cstrs.add(new Spread(constrained_vms));
            }
            if (placementConstraint.getType().contains("Gather")) {
                Set<VM> constrained_vms = new HashSet<>();
                for (String target : placementConstraint.getTargets()) {
                    // Check if the VM actually exists in case edge devices are not yet managed
                    if (vms.get(target) != null) {
                        constrained_vms.add(vms.get(target));
                    }
                    else {
                        System.out.println("Non consistent 'Gather' constraint detected for: " + target);
                    }
                }
                cstrs.add(new Gather(constrained_vms));
            }
            // TODO: precedence constraint
            /*if (constraint.getType().contains("Precedence")) {
                Set<VM> constrained_vms = new HashSet<>();
                for (String target : constraint.getTargets()) {
                    // Check if the VM actually exists in case edge devices are not yet managed
                    if (vms.get(target) != null) {
                        constrained_vms.add(vms.get(target));
                    }
                    else {
                        System.out.println("Non consistent 'Gather' constraint detected for: " + target);
                    }
                }
                String vm = constraint.getDevices().stream().findFirst().get();
                cstrs.add(new Precedence(vms.get(vm), Sets.newHashSet(constrained_vms)));
            }*/
            // TODO: Ban is usually put on edge devices only
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

        // TODO: Add precedence constraints inferred from relationships => 'proxy', 'master', and 'balanced_by' nodes must be started before fragments's hosting node ('execute')

        // Create an instance, set the objective, start an optimized scheduler and solve the problem
        Instance ii = new Instance(mo, cstrs, new MinUsed(Sets.newHashSet(public_clouds.values())));
        final ChocoScheduler sched = PrestoCloudExtensions.newScheduler();
        sched.doOptimize(true);
        ReconfigurationPlan p = sched.solve(ii);
        Assert.assertNotNull(p);

        // DEBUG: Show the computed placement actions
        //System.out.println(p);

        // Get the new model and mapping
        mo = p.getResult();
        map = mo.getMapping();

        // Print statistics & resulting placement
        System.out.println("==================================================================================================================");
        System.out.println("Solver statistics:");
        System.out.println("==================================================================================================================");
        System.out.println(sched.getStatistics());
        System.out.println("==================================================================================================================");
        System.out.println("FRAGMENT/NODE NAME -> CLOUD & REGION NAME -> VM TYPE SELECTED");
        System.out.println("==================================================================================================================");

        for (Map.Entry<String, VM> vm : vms.entrySet()) {
            // Extract the destination node
            Node destination_node = map.getVMLocation(vm.getValue());

            // TODO: print edge devices placement
            /* Look into edge devices
            for (Map.Entry<String, Node> node : edge_nodes.entrySet()) {
                if (node.getValue().equals(destination_node)) {
                    System.out.println(vm.getKey() + "\t -> \t" + node.getKey());
                    for (Docker docker : dockers) {
                        if (docker.getFragmentName().equalsIgnoreCase(vm.getKey()) && docker.getResourceType().contains("edge")) {
                            System.out.println(" \t -> \t docker run " +  docker.getImage() + " " + docker.getCmd());
                            break;
                        }
                    }
                    break;
                }
            }*/

            // Look into public clouds devices
            for (Map.Entry<String, Node> node : public_clouds.entrySet()) {
                if (node.getValue().equals(destination_node)) {
                    System.out.println(vm.getKey() + " -> " + node.getKey() + " -> " + getSelectedCloudVMType(selectedCloudVMTypes, vm.getKey(), node.getKey()));
                    for (Docker docker : dockers) {
                        if (docker.getFragmentName().equalsIgnoreCase(vm.getKey()) && docker.getResourceType().contains("cloud")) {
                            System.out.println(docker.printCmdline());
                            break;
                        }
                    }
                    break;
                }
            }
        }
        System.out.println("==================================================================================================================");

        // Generate JSON output
        JSONArray ja = new JSONArray();
        for (Map.Entry<String, VM> vm : vms.entrySet()) {
            // Extract the destination node
            Node destination_node = map.getVMLocation(vm.getValue());

            // TODO: write edge devices computed placement

            // Output all public clouds devices
            for (Map.Entry<String, Node> node : public_clouds.entrySet()) {
                if (node.getValue().equals(destination_node)) {
                    JSONObject jo = new JSONObject();
                    jo.put("fragment", vm.getKey());
                    jo.put("cloud", node.getKey().substring(0, node.getKey().lastIndexOf('_')));
                    jo.put("region", node.getKey().substring(node.getKey().lastIndexOf('_') + 1));
                    jo.put("type", getSelectedCloudVMType(selectedCloudVMTypes, vm.getKey(), node.getKey()));

                    // INFO: docker command is optional because 'proxy', 'master', and 'balanced_by' nodes don't have one!
                    Optional<Docker> docker = dockers.stream().filter(d -> d.getFragmentName().equalsIgnoreCase(vm.getKey())).findFirst();
                    docker.ifPresent(dck -> jo.put("docker", dck.printCmdline()));
                    ja.add(jo);
                }
            }
        }
        String formattedOutput = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true).writeValueAsString(ja);
        System.out.println(formattedOutput);

        // Write file
        FileWriter file = new FileWriter(outputFile);
        file.write(formattedOutput);
        file.flush();
        file.close();
    }
}
