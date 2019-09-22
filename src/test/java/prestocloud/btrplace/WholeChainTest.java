package prestocloud.btrplace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Sets;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.btrplace.json.JSONConverterException;
import org.btrplace.model.*;
import org.btrplace.model.constraint.*;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.Action;
import org.btrplace.plan.event.BootVM;
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
import prestocloud.btrplace.cost.CostView;
import prestocloud.btrplace.cost.MinCost;
import prestocloud.btrplace.precedingRunning.PrecedingRunning;
import prestocloud.btrplace.tosca.ParsingUtils;
import prestocloud.btrplace.tosca.geoloc.UTM2Deg;
import prestocloud.btrplace.tosca.model.*;
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
     * Simple helper method to retrieve the selected cloud VM type for a fragment name *or* a node name (eg. proxy, master, etc.).
     *
     * @param selectedCloudVMTypes HashMap of all selected cloud VM types
     * @param vmName name of the fragment *or* the node/host to look for
     * @param nodeName name of the computed destination cloud
     * @return the selected VM type, `null` if not found.
     */
    private String getSelectedCloudVMType(Map<String, Map<String, Map<String, Map<String, String>>>> selectedCloudVMTypes, String vmName, String nodeName) {
        if (selectedCloudVMTypes.get(vmName) != null) {
            return selectedCloudVMTypes.get(vmName).get("execute").get(selectedCloudVMTypes.get(vmName).get("execute").keySet().stream().findFirst().get()).get(nodeName);
        }
        else {
            for (Map.Entry<String, Map<String, Map<String, Map<String, String>>>> selectedFragmentTypes : selectedCloudVMTypes.entrySet()) {
                for (Map.Entry<String, Map<String, Map<String, String>>> selectedTypes : selectedFragmentTypes.getValue().entrySet()) {
                    if (selectedTypes.getValue().containsKey(vmName)) {
                        // Assume that cloud name and region name are concatenated with a space
                        return selectedTypes.getValue().get(vmName).get(nodeName);
                    }
                }
            }
        }
        // This should never return null as a matching type must have be found for each VM
        return null;
    }

    @Test
    public void processToscaWithBtrPlace() throws ParsingException, IOException, JSONConverterException,Exception {

        String resourcesPath = filesPath + "resources/"; // "src/main/resources/repository";
        String typeLevelTOSCAFile = filesPath + "ICCS-example-v6.yml";
        String outputFile = filesPath + "test.json";

        // Parse the type level TOSCA file
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(typeLevelTOSCAFile));

        // Extract all required information
        Map<String, String> metadata = ParsingUtils.getMetadata(parsingResult);
        List<String> supportedClouds = ParsingUtils.getListOfCloudsFromMetadata(metadata);
        List<Relationship> relationships = ParsingUtils.getRelationships(parsingResult);
        List<PlacementConstraint> placementConstraints = ParsingUtils.getConstraints(parsingResult);
        List<Docker> dockers = ParsingUtils.getDockers(parsingResult);
        List<OptimizationVariables> optimizationVariables = ParsingUtils.getOptimizationVariables(parsingResult);
        List<VMTemplateDetails> vmTemplatesDetails = ParsingUtils.getVMTemplatesDetails(parser, resourcesPath).vmTemplatesDetails;
        // TODO: deal with health checks
        List<HealthCheck> healthChecks = ParsingUtils.getHealthChecks(parsingResult);

        // TODO: Retrieve list of desired regions per cloud from metadata and/or overlay deployment workflow?
        List<String> azureRegions = Collections.singletonList("westeurope");
        List<String> amazonRegions = Collections.singletonList("eu-west-1");

        // Select the best cloud and VM type for each host
        // Map structure:
        //   fragment name -> requirement type -> node name -> "cloud region" -> VM type
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
                                List<String> selectedRegionAndType = ParsingUtils.findBestSuitableRegionAndVMType(
                                        parser,
                                        resourcesPath,
                                        cloud,
                                        // TODO: improve a bit to manage more clouds (eg. openstack, google)
                                        cloud.equalsIgnoreCase("azure") ? azureRegions : amazonRegions,
                                        nodeConstraints.getHostingConstraints());
                                selectedRegionAndType.forEach(s -> {
                                    String[] tmp = s.split(" ");
                                    selectedTypes.put(cloud.toLowerCase() + " " + tmp[0], tmp[1]);
                                });
                                /*String region = selectedRegionAndType.split(" ")[0];
                                String vmType = selectedRegionAndType.split(" ")[1];
                                selectedTypes.put(cloud.toLowerCase() + " " + region, vmType);*/
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
        // TODO: import previous mapping if this run is not for initial placement
        //Model mo = new ReconfigurationPlanConverter().fromJSON("").getResult().copy();
        Mapping map = mo.getMapping();


        // Create and attach the cost view
        final CostView cv = new CostView();
        mo.attach(cv);

        // Create VMs
        Map<String, VM> vms = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Map<String, String>>>> selectedFragmentTypes : selectedCloudVMTypes.entrySet()) {
            for (Map.Entry<String, Map<String, Map<String, String>>> selectedTypes : selectedFragmentTypes.getValue().entrySet()) {
                // Add the fragment's execute node first
                if (selectedTypes.getKey().equalsIgnoreCase("execute")) {
                    vms.put(selectedFragmentTypes.getKey(), mo.newVM());
                }
                else {
                    // We can have duplicates (eg. a 'proxy' may be linked to multiple fragments)
                    String nodeName = selectedTypes.getValue().keySet().stream().findFirst().get();
                    if (!vms.containsKey(nodeName)) {
                        vms.put(nodeName, mo.newVM());
                    }
                }
            }
        }

        // TODO: declare edge devices, it depends how we get them
        /* Declare some edge devices (nodes)
        Map<String, Node> edgeNodes = new HashMap<>();
        for (String edgeNodeName : Arrays.asList("acfdgex98", "kdsfk31fw", "f2553fdfs", "bd5fgdx32")) {
            Node edgeNode = mo.newNode();
            edgeNodes.put(edgeNodeName, edgeNode);
            cv.edgeHost(edgeNode);
        }*/

        // Declare the public clouds
        Map<String, Node> publicClouds = new HashMap<>();
        for (String cloud : supportedClouds) {
            if (cloud.equalsIgnoreCase("azure")) {
                for (String region : azureRegions) {
                    publicClouds.put("azure " + region, mo.newNode());
                }
            }
            if (cloud.equalsIgnoreCase("amazon")) {
                for (String region : amazonRegions) {
                    publicClouds.put("amazon " + region, mo.newNode());
                }
            }
        }

        // Create and attach cpu, memory and disk resources
        ShareableResource cpu = new ShareableResource("cpu");
        ShareableResource mem = new ShareableResource("memory");
        ShareableResource disk = new ShareableResource("disk");
        mo.attach(cpu);
        mo.attach(mem);
        mo.attach(disk);

        // TODO: set cpu for edge devices
        /* Set edge devices cpu
        for (Map.Entry<String, Node> edgeNode : edgeNodes.entrySet()) {
            cpu.setCapacity(edgeNode.getValue(), 4);
        }*/

        // TODO: set memory for edge devices
        /* Set edge devices memory
        for (Map.Entry<String, Node> edgeNode : edgeNodes.entrySet()) {
            mem.setCapacity(edgeNode.getValue(), 2);
        }*/

        // TODO: set disk for edge devices
        /* Set edge devices disk
        for (Map.Entry<String, Node> edgeNode : edgeNodes.entrySet()) {
            disk.setCapacity(edgeNode.getValue(), 60);
        }*/

        // Set a pseudo infinite capacity for public clouds to simulate unlimited resources
        for (Map.Entry<String, Node> cloud : publicClouds.entrySet()) {
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

                                // TODO: manage more constraints on values if needed
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

        // Prepare the list of constraints
        final List<SatConstraint> cstrs = new ArrayList<>();

        // Set state for public clouds: online and running
        for (Map.Entry<String, Node> public_cloud : publicClouds.entrySet()) {
            map.on(public_cloud.getValue());
        }
        cstrs.addAll(Online.newOnline(mo.getMapping().getAllNodes()));

        // TODO: set the state for edge devices
        // All edge nodes are online and running
        /*for (Map.Entry<String, Node> edgeNode : edgeNodes.entrySet()) {
            map.on(edgeNode.getValue());
        }*/

        // Set initial placement: all fragments ready to be deployed
        for (Map.Entry<String, VM> vm : vms.entrySet()) {
            map.ready(vm.getValue());
        }
        cstrs.addAll(Running.newRunning(mo.getMapping().getAllVMs()));

        // Apply placement constraints
        for (PlacementConstraint placementConstraint : placementConstraints) {
            // TODO: manage more constraints if needed, use a dedicated method
            if (placementConstraint.getType().contains("Spread")) {
                Set<VM> constrained_vms = new HashSet<>();
                // Check if the VM actually exists (this is currently triggered as edge devices are not yet managed)
                if (vms.entrySet().containsAll(placementConstraint.getTargets())) {
                    for (String target : placementConstraint.getTargets()) {
                        constrained_vms.add(vms.get(target));
                    }
                    cstrs.add(new Spread(constrained_vms));
                }
                else {
                    System.out.println("Non consistent 'Spread' constraint detected (probably due to a missing edge device).");
                }
            }
            if (placementConstraint.getType().contains("Gather")) {
                Set<VM> constrained_vms = new HashSet<>();
                // Check if the VM actually exists (this is currently triggered as edge devices are not yet managed)
                if (vms.entrySet().containsAll(placementConstraint.getTargets())) {
                    for (String target : placementConstraint.getTargets()) {
                        constrained_vms.add(vms.get(target));
                    }
                    cstrs.add(new Gather(constrained_vms));
                }
                else {
                    System.out.println("Non consistent 'Gather' constraint detected (probably due to a missing edge device).");
                }
            }
            if (placementConstraint.getType().contains("Precedence")) {
                Set<VM> constrained_vms = new HashSet<>();
                // Check if the VM actually exists (this is currently triggered as edge devices are not yet managed)
                if (vms.entrySet().containsAll(placementConstraint.getTargets())) {
                    for (String target : placementConstraint.getTargets()) {
                        constrained_vms.add(vms.get(target));
                    }
                    String vm = placementConstraint.getDevices().stream().findFirst().get();
                    cstrs.add(new PrecedingRunning(vms.get(vm), Sets.newHashSet(constrained_vms)));
                }
                else {
                    System.out.println("Non consistent 'Precedence' constraint detected (probably due to a missing edge device).");
                }
            }
            // TODO: Ban constraints are only put on edge devices
            /*if (constraint.getType().contains("Ban")) {
                Set<Node> constrained_nodes = new HashSet<>();
                for (String node : constraint.getDevices()) {
                    constrained_nodes.add(edgeNodes.get(node));
                }
                String vm = null;
                for (String target : constraint.getTargets()) {
                    vm = target;
                }
                cstrs.add(new Ban(vms.get(vm), Sets.newHashSet(constrained_nodes)));
            }*/
        }

        // TODO: Add precedence constraints from relationships. 'proxy', 'master', and 'balanced_by' nodes must be started before fragment's host ('execute' node))

        // TODO: use a valid reference location to compute distances ("Sophia Antipolis" for testing only, must be retrieved from fragment's properties or dependencies)
        String sophiaAntipolisUTM = "32T 342479mE 4831495mN";

        // Set cost view for each node <-> vm pair (values are extracted from optimization objective variables & VM templates details)
        // TODO: do it also for private clouds and edge devices
        for (Map.Entry<String, VM> vm : vms.entrySet()) {
            // Find corresponding optimisation variables, note that they are only set on fragment (not their dependencies like proxy, master, etc.)
            Optional<OptimizationVariables> vmOptimVars = optimizationVariables.stream().filter(optimVars -> optimVars.getFragmentName().equalsIgnoreCase(vm.getKey())).findFirst();
            for (Map.Entry<String, Node> node : publicClouds.entrySet()) {
                // Find corresponding VM template
                VMTemplateDetails vmTemplateDetails = vmTemplatesDetails.stream().filter(vmTplDetails -> (vmTplDetails.getCloud() + " " + vmTplDetails.getRegion()).equalsIgnoreCase(node.getKey())).findFirst().get();
                // Default to 1
                int affinity = 1;
                int distance = 1;
                int cost = 1;
                if (vmOptimVars.isPresent()) {
                    // Check if a specific affinity was set for this specific node (VM cloud and region match)
                    for (Map.Entry<String, Integer> friendliness : vmOptimVars.get().getFriendliness().entrySet()) {
                        if (friendliness.getKey().equalsIgnoreCase(vmTemplateDetails.getCloud() + "_" + vmTemplateDetails.getRegion())) {
                            affinity = friendliness.getValue();
                        }
                    }
                    distance = vmOptimVars.get().getDistance();
                    cost = vmOptimVars.get().getCost();
                }
                // Set default values for 'dependent' hosting nodes
                cv.publicHost(node.getValue(), vm.getValue(), vmTemplateDetails.getPrice(), UTM2Deg.getDistance(sophiaAntipolisUTM, vmTemplateDetails.getGeolocation()), affinity, distance, cost);

            }
        }

        // Create an instance with MinCost objective
        Instance ii = new Instance(mo, cstrs, new MinCost());

        // Start an optimized scheduler and solve the problem
        final ChocoScheduler sched = PrestoCloudExtensions.newScheduler();
        sched.doOptimize(true);
        ReconfigurationPlan p = sched.solve(ii);
        Assert.assertNotNull(p);

        // TODO: save the mapping to a proper place to reimport it later
        /*File tmp = File.createTempFile("presto-plan-", ".json");
        final ReconfigurationPlanConverter rpc = new ReconfigurationPlanConverter();
        rpc.toJSON(p).writeJSONString(Files.newBufferedWriter(tmp.toPath()));*/

        // Get list of computed actions
        Set<Action> actions = p.getActions();

        // Generate JSON output
        JSONArray ja = new JSONArray();
        for (Action action : actions) {

            // TODO: manage more actions if needed (eg. "MigrateVM" to transform in delete and boot actions)
            if (action instanceof BootVM) {

                // Retrieve VM/fragment name
                String vmName = vms.entrySet().stream().filter(vm -> vm.getValue().equals(((BootVM) action).getVM())).findFirst().get().getKey();

                // Retrieve node/host name
                String nodeName = publicClouds.entrySet().stream().filter(pc -> pc.getValue().equals(((BootVM) action).getDestinationNode())).findFirst().get().getKey();

                String selectedVMType = getSelectedCloudVMType(selectedCloudVMTypes, vmName, nodeName);
                JSONObject jo = new JSONObject();
                jo.put("action", "boot");
                jo.put("start", action.getStart());
                jo.put("end", action.getEnd());
                jo.put("fragment", vmName);
                jo.put("cloud", nodeName.split(" ")[0]);
                jo.put("region", nodeName.split(" ")[1]);
                jo.put("type", selectedVMType);

                // Docker command is optional because 'proxy', 'master', and 'balanced_by' nodes don't have one
                Optional<Docker> docker = dockers.stream().filter(d -> d.getFragmentName().equalsIgnoreCase(vmName)).findFirst();
                docker.ifPresent(dck -> jo.put("docker", dck.printCmdline()));

                ja.add(jo);
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
