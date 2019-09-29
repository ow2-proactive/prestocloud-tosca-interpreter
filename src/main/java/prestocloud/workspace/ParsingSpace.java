package prestocloud.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONUtil;
import net.minidev.json.JSONValue;
import net.minidev.json.writer.JsonReader;
import org.btrplace.model.*;
import org.btrplace.model.constraint.*;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.*;
import org.btrplace.scheduler.choco.ChocoScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import prestocloud.btrplace.PrestoCloudExtensions;
import prestocloud.btrplace.cost.CostView;
import prestocloud.btrplace.cost.MinCost;
import prestocloud.btrplace.precedingRunning.PrecedingRunning;
import prestocloud.btrplace.tosca.GetVMTemplatesDetailsResult;
import prestocloud.btrplace.tosca.ParsingUtils;
import prestocloud.btrplace.tosca.geoloc.UTM2Deg;
import prestocloud.btrplace.tosca.model.*;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class ParsingSpace {

    private Logger logger = LoggerFactory.getLogger(ParsingSpace.class);

    private Map<String,Map<String, List<RegionCapacityDescriptor>>> regionsPerCloudPerCloudFile;
    private ParsingResult<ArchiveRoot> parsingResult;
    private ToscaParser parser;
    private String resourcesPath;

    // We describe couples of element we want to d integrate from our parsing.
    private List<String> supportedCloudsResourceFiles;
    private List<Relationship> relationships;
    private List<PlacementConstraint> placementConstraints;
    private List<Docker> dockers;
    private List<OptimizationVariables> optimizationVariables;
    private List<VMTemplateDetails> vmTemplatesDetails;
    // TODO: deal with health checks
    private List<HealthCheck> healthChecks;
    private Map<String,SshKey> sshKeys;
    private Map<String, Map<String, Map<String, Map<String, String>>>> selectedCloudVMTypes = new HashMap<>();
    private Map<String,String> balancingNodes = new HashMap<>();
    private Map<String,String> proxyingNodes = new HashMap<>();
    private Map<String,String> masteringNodes = new HashMap<>();

    //btrplace model related attributes
    private Map<String, VM> vmsPerName = new HashMap<>();
    private Map<VM, String> namePerVM = new HashMap<>();
    private ShareableResource cpu = new ShareableResource("cpu");
    private ShareableResource mem = new ShareableResource("memory");
    private ShareableResource disk = new ShareableResource("disk");
    private Model mo;
    private Mapping map;
    private CostView cv;

    // Public and Private Cloud identification - Contains Btrplace models.
    private Map<String, Node> nodePerName = new HashMap<>();
    private Map<Node,String> namePerNode = new HashMap<>();
    Map<String,RegionCapacityDescriptor> regionCapabilityDescriptorPerCloud = new HashMap<>();

    private final List<SatConstraint> cstrs = new ArrayList<>();
    private Mapping dstmapping;
    private Set<Action> actions;

    public ParsingSpace(ParsingResult<ArchiveRoot> result, GetVMTemplatesDetailsResult getVMTemplatesDetailsResult, ToscaParser parser, String resourcesPath) {
        this.parsingResult = result;
        this.parser = parser;
        this.resourcesPath  = resourcesPath;
        this.vmTemplatesDetails = getVMTemplatesDetailsResult.vmTemplatesDetails;
        this.regionsPerCloudPerCloudFile = getVMTemplatesDetailsResult.regionsPerCloudPerCloudFile;
    }

    public boolean retrieveResourceFromParsing() {
        // Retrieving main data from the parsed TOSCA.
        Map<String, String> metadata = ParsingUtils.getMetadata(parsingResult);
        supportedCloudsResourceFiles = ParsingUtils.getListOfCloudsFromMetadata(metadata);
        Optional<Set<String>> cloudListFromRegion = this.regionsPerCloudPerCloudFile.values().stream().map(Map::keySet).reduce((strings, strings2) -> {
            Set<String> result = new HashSet<>();
            result.addAll(strings);
            result.addAll(strings2);
            return strings;});
         if (!cloudListFromRegion.isPresent()) {
             throw new IllegalArgumentException("No clouds were detected from the region");
         }
         if (cloudListFromRegion.get().containsAll(supportedCloudsResourceFiles)){
             throw new IllegalArgumentException("There is a mismatch between specified clouds in Type-level TOSCA and the clouds specified in the repository");
         }
        logger.info("{} supported cloud have been found", supportedCloudsResourceFiles.size());
        relationships = ParsingUtils.getRelationships(parsingResult);
        placementConstraints = ParsingUtils.getConstraints(parsingResult);
        dockers = ParsingUtils.getDockers(parsingResult);
        optimizationVariables = ParsingUtils.getOptimizationVariables(parsingResult);
        sshKeys = ParsingUtils.getSshKeys(parsingResult);
        healthChecks = ParsingUtils.getHealthChecks(parsingResult);
        return true;
    }

    public void identifiesNodeRelatedToPrecedenceConstraints() {
        ConstrainedNode constraints;
        for (Relationship relationship : relationships) {
            if (relationship.getHostingNode().getType().equals("execute")) {
                constraints = relationship.getHostingNode();
                if (constraints.derivedTypes.contains("prestocloud.nodes.proxy.faas") || constraints.derivedTypes.contains("prestocloud.nodes.agent.faas")) {
                    logger.info("{} fragment has been identified as operating a FaaS proxy node", relationship.getFragmentName());
                    this.proxyingNodes.put(constraints.getName(), relationship.getFragmentName());
                } else if (constraints.derivedTypes.contains("prestocloud.nodes.master.jppf")) {
                    logger.info("{} fragment has been identified as operating a JPPF master node", relationship.getFragmentName());
                    this.masteringNodes.put(constraints.getName(), relationship.getFragmentName());
                } else if (constraints.derivedTypes.contains("prestocloud.nodes.proxy")) {
                    logger.info("{} fragment has been identified as operating a load-balancing node", relationship.getFragmentName());
                    this.balancingNodes.put(constraints.getName(), relationship.getFragmentName());
                }
            }
        }
    }

    public boolean selectBestCloudVmType() throws Exception{
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
                            for (String cloudFile : supportedCloudsResourceFiles) {
                                    for (String cloud : this.regionsPerCloudPerCloudFile.get(cloudFile).keySet()) {
                                    List<String> selectedRegionAndType = ParsingUtils.findBestSuitableRegionAndVMType(
                                            parser,
                                            resourcesPath,
                                            cloudFile,
                                            this.regionsPerCloudPerCloudFile.get(cloudFile).get(cloud).stream().map(RegionCapacityDescriptor::getRegion).collect(Collectors.toList()),
                                            nodeConstraints.getHostingConstraints());
                                    selectedRegionAndType.forEach(s -> {
                                       String[] tmp = s.split(" ");
                                       selectedTypes.put(cloud.toLowerCase() + " " + tmp[0], tmp[1]);
                                    });
                                }
                            }
                            allSelectedTypes.put(constrainedNode.getName(), selectedTypes);
                        } else {
                            logger.warn("Edge-only hosting resource constraint found: {}", constrainedNode.getName());
                        }
                    }
                }
                allSelectedTypesWithRequirement.put(constrainedNode.getType(), allSelectedTypes);
            }
            selectedCloudVMTypes.put(relationship.getFragmentName(), allSelectedTypesWithRequirement);
            logger.debug("{} types were identified for the fragment {}", allSelectedTypes.size(), relationship.getFragmentName());
        }
        return true;
    }

    public void configureBtrPlace() {
        mo = new DefaultModel();
        // TODO: import previous mapping if this run is not for initial placement
        //Model mo = new ReconfigurationPlanConverter().fromJSON("").getResult().copy();
        map = mo.getMapping();
        cv = new CostView();
        mo.attach(cv);
    }

    public void populateVmsInBtrPlaceModel() {
        String vmsName;
        String dependencyNode;
        // For each fragment to be deployed ....
        for (Map.Entry<String, Map<String, Map<String, Map<String, String>>>> selectedFragmentTypes : selectedCloudVMTypes.entrySet()) {
            // For each relationship in the fragment definition
            for (Map.Entry<String, Map<String, Map<String, String>>> selectedTypes : selectedFragmentTypes.getValue().entrySet()) {
                vmsName = selectedFragmentTypes.getKey();
                // Add the fragment's execute node first
                if (selectedTypes.getKey().equalsIgnoreCase("execute")) {
                    proceedVmRegistration(vmsName, "Registering fragment {} ...");
                } else if (selectedTypes.getKey().equalsIgnoreCase("master")) {
                    proceedVmRegistration(vmsName, "Registering slave fragment {} ...");
                    dependencyNode = masteringNodes.get(retrieveDependencyFragment(selectedTypes));
                    proceedVmRegistration(dependencyNode, "Registering master fragment {} ...");
                    //cstrs.add(new PrecedingRunning(vmsPerName.get(vmsName), vmsPerName.get(dependencyNode)));
                } else if (selectedTypes.getKey().equalsIgnoreCase("balanced_by")) {
                    proceedVmRegistration(vmsName, "Registering balanced fragment {} ...");
                    dependencyNode = balancingNodes.get(retrieveDependencyFragment(selectedTypes));
                    proceedVmRegistration(dependencyNode, "Registering balancing fragment {} ...");
                    //cstrs.add(new PrecedingRunning(vmsPerName.get(vmsName), vmsPerName.get(dependencyNode)));
                } else if (selectedTypes.getKey().equalsIgnoreCase("proxy")) {
                    proceedVmRegistration(vmsName, "Registering proxified fragment {} ...");
                    dependencyNode = proxyingNodes.get(retrieveDependencyFragment(selectedTypes));
                    proceedVmRegistration(dependencyNode, "Registering proxying fragment {} ...");
                    //cstrs.add(new PrecedingRunning(vms.get(nodeName),vms.get(dependencyNode)));
                } else {
                    // We can have duplicates (eg. a 'proxy' may be linked to multiple fragments)
                    vmsName = retrieveDependencyFragment(selectedTypes);
                    proceedVmRegistration(vmsName, "Registering an unclassified fragment {} ...");
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
    }

    private String retrieveDependencyFragment(Map.Entry<String, Map<String, Map<String, String>>> selectedTypes) {
        Optional<String> tmp = selectedTypes.getValue().keySet().stream().findFirst();
        if (tmp.isPresent()) {
            return tmp.get();
        } else {
            throw new IllegalStateException("The selected Types doesn't reference any fragment name: " + selectedTypes.getKey());
        }
    }

    private void proceedVmRegistration(String nodeName, String loggerMessage) {
        if (!vmsPerName.containsKey(nodeName)) {
            VM newVM = mo.newVM();
            vmsPerName.put(nodeName, newVM);
            namePerVM.put(newVM, nodeName);
            logger.warn(loggerMessage, nodeName);
        }
    }

    public void populateNodesInBtrPlaceModel() {
        String placementString;
        Node node;
        for (String cloudFile : this.supportedCloudsResourceFiles) {
            //for (String cloudFile : this.regionsPerCloudPerCloudFile.keySet()) {
            for (String cloud : regionsPerCloudPerCloudFile.get(cloudFile).keySet()) {
                for (RegionCapacityDescriptor region : regionsPerCloudPerCloudFile.get(cloudFile).get(cloud)) {
                    placementString = cloud + " " + region.getRegion();
                    regionCapabilityDescriptorPerCloud.put(placementString, region);
                    if (!nodePerName.containsKey(placementString)) {
                        node = mo.newNode();
                        map.addOfflineNode(node);
                        nodePerName.put(placementString,node);
                        namePerNode.put(node,placementString);
                        logger.info("Registering node {} as {} ...", placementString, node);
                    }
                }
            }
        }
    }

    public void setOnlineNode() {
        String placementString;
        for (String supportedCloud : this.supportedCloudsResourceFiles) {
            for (String cloud : regionsPerCloudPerCloudFile.get(supportedCloud).keySet()) {
                for (RegionCapacityDescriptor region : regionsPerCloudPerCloudFile.get(supportedCloud).get(cloud)) {
                    placementString = cloud + " " + region.getRegion();
                    map.addOnlineNode(nodePerName.get(placementString));
                    logger.info("Setting the whitelisted node {} online ...", placementString);
                }
            }
        }
    }

    public void loadExistingMapping(String mappingContent) {
        if (!JSONValue.isValidJson(mappingContent)) {
            throw new IllegalArgumentException("The provided mappping is not valid. I leave.");
        }
        JSONArray ja = (JSONArray) JSONValue.parse(mappingContent);
        String nodeName;
        JSONObject nodeAssignation;
        Optional<String> tmp;
        String operatedVMSonNode;
        for (Object nodeAssignationObject : ja) {
            nodeAssignation = (JSONObject) nodeAssignationObject;
            tmp = nodeAssignation.keySet().stream().findFirst();
            if (!tmp.isPresent()) {
                logger.warn("Malformed JSON Object entree in the mapping file, I skip this entree");
                continue;
            }
            nodeName = tmp.get();
            for (Object operatedVMonNodeObject : (JSONArray) nodeAssignation.get(nodeName)) {
                operatedVMSonNode = (String) operatedVMonNodeObject;
                logger.info("Reading mapping : Node {} is operating VM {}", nodeName, operatedVMSonNode);
            }
        }
    }


    public void setCapacity() {
        // Create and attach cpu, memory and disk resources
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

        for (Map.Entry<String, Node> cloud : nodePerName.entrySet()) {
            cpu.setCapacity(cloud.getValue(), this.regionCapabilityDescriptorPerCloud.get(cloud.getKey()).getCpuCapacity());
            mem.setCapacity(cloud.getValue(), this.regionCapabilityDescriptorPerCloud.get(cloud.getKey()).getMemoryCapacity());
            disk.setCapacity(cloud.getValue(), this.regionCapabilityDescriptorPerCloud.get(cloud.getKey()).getDiskCapacity());
        }
    }

    public void configuringNodeComputingRequirementConstraint() {
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
                                    constraint = Integer.parseInt(required.split(",")[0].split(" ")[1]);
                                } else if (required.contains("GreaterOrEqual")) {
                                    constraint = Integer.parseInt(required.split(" ")[1]);
                                } else {
                                    constraint = Integer.parseInt(required);
                                }

                                // TODO: manage more units if needed
                                if (hostingConstraint.getKey().equalsIgnoreCase("num_cpus")) {
                                    cpu.setConsumption(vmsPerName.get(vmName), constraint);
                                } else if (hostingConstraint.getKey().equalsIgnoreCase("mem_size")) {
                                    // Mem in GB only
                                    if (required.contains("MB")) {
                                        constraint = constraint / 1024;
                                    }
                                    mem.setConsumption(vmsPerName.get(vmName), constraint);
                                } else if (hostingConstraint.getKey().equalsIgnoreCase("storage_size")) {
                                    // Storage in GB only
                                    if (required.contains("MB")) {
                                        constraint = constraint / 1024;
                                    }
                                    disk.setConsumption(vmsPerName.get(vmName), constraint);
                                } else {
                                    logger.error("Unrecognized hosting constraint: {} ", hostingConstraint.getKey());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void detectResourceAvailability() {
        // Set state for public clouds: online and running
        /*for (Map.Entry<String, Node> nodeEntree : nodePerName.entrySet()) {
            map.on(nodeEntree.getValue());
        }
        cstrs.addAll(Online.newOnline(mo.getMapping().getAllNodes()));*/
        //logger.info("{} public and private clouds are set online", nodePerName.keySet());
        logger.info("{} public and private clouds are set online", map.getAllNodes().stream().filter(node -> (map.isOnline(node))).map(node -> (namePerNode.get(node))).collect((Collectors.toList())));

        // TODO : with edge resource, we will need to act check the effective reachability of the need. Point to be discussed.
        // TODO: set the state for edge devices
        // All edge nodes are online and running
        /*for (Map.Entry<String, Node> edgeNode : edgeNodes.entrySet()) {
            map.on(edgeNode.getValue());
        }*/
    }

    public void defineFragmentDeployability() {
        for (Map.Entry<String, VM> vm : vmsPerName.entrySet()) {
            map.ready(vm.getValue());
        }
        cstrs.addAll(Running.newRunning(map.getAllVMs()));
    }

    public void configurePlacementConstraint() {
        // Apply placement constraints
        for (PlacementConstraint placementConstraint : placementConstraints) {
            // TODO: manage more constraints if needed, use a dedicated method
            if (placementConstraint.getType().contains("Spread")) {
                Set<VM> constrainedVms = new HashSet<>();
                // Check if the VM actually exists (this is currently triggered as edge devices are not yet managed)
                if (vmsPerName.keySet().containsAll(placementConstraint.getTargets())) {
                    for (String target : placementConstraint.getTargets()) {
                        constrainedVms.add(vmsPerName.get(target));
                    }
                    cstrs.add(new Spread(constrainedVms));
                }
                else {
                    logger.warn("Non consistent 'Spread' constraint detected (probably due to a missing edge device).");
                }
            }
            if (placementConstraint.getType().contains("Gather")) {
                Set<VM> constrainedVms = new HashSet<>();
                // Check if the VM actually exists (this is currently triggered as edge devices are not yet managed)
                if (vmsPerName.keySet().containsAll(placementConstraint.getTargets())) {
                    for (String target : placementConstraint.getTargets()) {
                        constrainedVms.add(vmsPerName.get(target));
                    }
                    cstrs.add(new Gather(constrainedVms));
                }
                else {
                    logger.warn("Non consistent 'Gather' constraint detected (probably due to a missing edge device).");
                }
            }
            if (placementConstraint.getType().contains("Precedence")) {
                // Check if the VM actually exists (this is currently triggered as edge devices are not yet managed)
                if (vmsPerName.keySet().containsAll(placementConstraint.getTargets())) {
                    Object[] ordonnedVmAllocation = placementConstraint.getTargets().toArray();
                    for(int i = 0; i < ordonnedVmAllocation.length -1; i++) {
                        HashSet<VM> tmp = Sets.newHashSet();
                        tmp.add(vmsPerName.get((String) ordonnedVmAllocation[i]));
                       cstrs.add(new PrecedingRunning(vmsPerName.get( (String) ordonnedVmAllocation[i+1]),tmp));
                    }
                } else {
                    logger.warn("Non consistent 'Precedence' constraint detected (probably due to a missing edge device).");
                }
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

    public void extractCost() {
        // TODO: use a valid reference location to compute distances ("Sophia Antipolis" for testing only, must be retrieved from fragment's properties or dependencies)
        // Point to be discussed with ICCS.
        String sophiaAntipolisUTM = "32T 342479mE 4831495mN";

        // Set cost view for each node <-> vm pair (values are extracted from optimization objective variables & VM templates details)
        // TODO: do it also for edge devices
        for (Map.Entry<String, VM> vm : vmsPerName.entrySet()) {
            // Find corresponding optimisation variables, note that they are only set on fragment (not their dependencies like proxy, master, etc.)
            Optional<OptimizationVariables> vmOptimVars = optimizationVariables.stream().filter(optimVars -> optimVars.getFragmentName().equalsIgnoreCase(vm.getKey())).findFirst();
            for (Map.Entry<String, Node> node : nodePerName.entrySet()) {
                // Find corresponding VM template
                Optional<VMTemplateDetails> tmp = vmTemplatesDetails.stream().filter(vmTplDetails -> (vmTplDetails.getCloud() + " " + vmTplDetails.getRegion()).equalsIgnoreCase(node.getKey())).findFirst();
                if (tmp.isPresent()) {
                    VMTemplateDetails vmTemplateDetails = tmp.get();
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
        }
    }

    public boolean performedBtrplaceSolving() {
        // Create an instance with MinCost objective
        Instance ii = new Instance(mo, cstrs, new MinCost());

        // Start an optimized scheduler and solve the problem
        final ChocoScheduler sched = PrestoCloudExtensions.newScheduler();
        sched.doOptimize(true);
        ReconfigurationPlan p = sched.solve(ii);
        if (p == null) {
            return false;
        }

        // TODO: save the mapping to a proper place to reimport it later
       /* try {
            File tmp = File.createTempFile("presto-plan-", ".json");
//            final ReconfigurationPlanConverter rpc = new ReconfigurationPlanConverter();
            ModelConverter rpc = new ModelConverter();
            Model dst = p.getResult();
            dst.detach(this.cv);
            rpc.toJSON(dst).writeJSONString(Files.newBufferedWriter(tmp.toPath()));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }*/

        // Get list of computed actions
        actions = p.getActions();
        dstmapping = p.getResult().getMapping();
        return true;
    }

   public String generationJsonOutput() {
       // Generate JSON output
       JSONArray ja = new JSONArray();
       for (Action action : actions) {
           if (action instanceof BootVM || action instanceof ResumeVM) {
                generationBootVMAndResumeVMOutput(action, ja);
           } else if (action instanceof MigrateVM) {
                generationMigrateVMOutput(action,ja);
           } else if (action instanceof ShutdownVM || action instanceof SuspendVM) {
                generationShutdownVMAndSuspendVMOutput(action,ja);
           } else {
               throw new IllegalArgumentException("Fragment management action has not been understood");
           }
       }
       try {
           return new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true).writeValueAsString(ja);
       } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
          logger.error("Unable to process the JSON structure");
          return  "";
       }
   }

    private void generationBootVMAndResumeVMOutput(Action action, JSONArray ja) {
       // Retrieve VM/fragment name
       String vmName;
       if (action instanceof BootVM) {
           vmName = namePerVM.get(((BootVM) action).getVM());
       } else {
           vmName = namePerVM.get(((ResumeVM) action).getVM());
       }

       // Retrieve node/host name
       String nodeName;
       if (action instanceof BootVM) {
           nodeName = namePerNode.get(((BootVM) action).getDestinationNode());
       } else {
           nodeName = namePerNode.get(((ResumeVM) action).getDestinationNode());
       }

       String selectedVMType = getSelectedCloudVMType(selectedCloudVMTypes, vmName, nodeName);
       JSONObject jo = new JSONObject();
       jo.put("action", "boot");
       jo.put("start", action.getStart());
       jo.put("end", action.getEnd());
       jo.put("fragment", vmName);
       jo.put("cloud", nodeName.split(" ")[0]);
       jo.put("region", nodeName.split(" ")[1]);
       jo.put("type", selectedVMType);
       if  (this.sshKeys.containsKey(vmName)) {
           if (this.sshKeys.get(vmName).hasKey()) {
               jo.put("ssh_key",this.sshKeys.get(vmName).getPublicKey());
           }
       }

       // Docker command is optional because 'proxy', 'master', and 'balanced_by' nodes don't have one
       Optional<Docker> docker = dockers.stream().filter(d -> d.getFragmentName().equalsIgnoreCase(vmName)).findFirst();
       docker.ifPresent(dck -> jo.put("docker", dck.printCmdline()));

       ja.add(jo);
   }

    private boolean generationMigrateVMOutput(Action action, JSONArray ja) {
        // Retrieve VM/fragment name
        String vmName = namePerVM.get(((MigrateVM) action).getVM());

        // Retrieve node/host name
        String nodeNameSrc = namePerNode.get(((MigrateVM) action).getSourceNode());
        String nodeName = namePerNode.get(((MigrateVM) action).getDestinationNode());

        String selectedVMType = getSelectedCloudVMType(selectedCloudVMTypes, vmName, nodeName);
        JSONObject jo = new JSONObject();
        jo.put("action", "migrate");
        jo.put("start", action.getStart());
        jo.put("end", action.getEnd());
        jo.put("fragment", vmName);
        jo.put("cloud", nodeName.split(" ")[0]);
        jo.put("region", nodeName.split(" ")[1]);
        jo.put("cloudsrc", nodeNameSrc.split(" ")[0]);
        jo.put("regionsrc", nodeNameSrc.split(" ")[1]);
        jo.put("type", selectedVMType);
        if  (this.sshKeys.containsKey(vmName)) {
            if (this.sshKeys.get(vmName).hasKey()) {
                jo.put("ssh_key",this.sshKeys.get(vmName).getPublicKey());
            }
        }

        // Docker command is optional because 'proxy', 'master', and 'balanced_by' nodes don't have one
        Optional<Docker> docker = dockers.stream().filter(d -> d.getFragmentName().equalsIgnoreCase(vmName)).findFirst();
        docker.ifPresent(dck -> jo.put("docker", dck.printCmdline()));

        ja.add(jo);
        return true;
    }


    private boolean generationShutdownVMAndSuspendVMOutput(Action action, JSONArray ja) {
        // Retrieve VM/fragment name
        String vmName;
        if (action instanceof ShutdownVM) {
            vmName = namePerVM.get(((ShutdownVM) action).getVM());
        } else {
            vmName = namePerVM.get(((SuspendVM) action).getVM());
        }

        // Retrieve node/host name
        String nodeName;
        if (action instanceof ShutdownVM) {
            nodeName = namePerNode.get(((ShutdownVM) action).getNode());
        } else {
            nodeName = namePerNode.get(((SuspendVM) action).getSourceNode());
        }

        String selectedVMType = getSelectedCloudVMType(selectedCloudVMTypes, vmName, nodeName);
        JSONObject jo = new JSONObject();
        jo.put("action", "boot");
        jo.put("start", action.getStart());
        jo.put("end", action.getEnd());
        jo.put("fragment", vmName);
        jo.put("cloudsrc", nodeName.split(" ")[0]);
        jo.put("regionsrc", nodeName.split(" ")[1]);
        jo.put("type", selectedVMType);
        if  (this.sshKeys.containsKey(vmName)) {
            if (this.sshKeys.get(vmName).hasKey()) {
                jo.put("ssh_key",this.sshKeys.get(vmName).getPublicKey());
            }
        }

        // Docker command is optional because 'proxy', 'master', and 'balanced_by' nodes don't have one
        Optional<Docker> docker = dockers.stream().filter(d -> d.getFragmentName().equalsIgnoreCase(vmName)).findFirst();
        docker.ifPresent(dck -> jo.put("docker", dck.printCmdline()));

        ja.add(jo);
        return true;
    }

    public String generateOutputMapping() {
        Set<Node> operatingNodes = dstmapping.getOnlineNodes();
        JSONArray result = new JSONArray();

        Set<VM>  operatedFragments;
        JSONObject association;
        JSONArray fragments;
        // For each node operating fragments ...
        for (Node operatedNode : operatingNodes) {
            operatedFragments = dstmapping.getRunningVMs(operatedNode);
            // Let's ignore nodes w/o fragments on it
            if (!operatedFragments.isEmpty()) {
                association = new JSONObject();
                fragments = new JSONArray();
                // For each fragment operated on the specified node ...
                for (VM fragment : operatedFragments) {
                    fragments.add(namePerVM.get(fragment));
                }
                association.put(namePerNode.get(operatedNode), fragments);
                result.add(association);
            }
        }

        return  result.toJSONString();
    }

    /**
     * Simple helper method to retrieve the selected cloud VM type for a fragment name *or* a node name (eg. proxy, master, etc.).
     *
     * @param selectedCloudVMTypes HashMap of all selected cloud VM types
     * @param vmName name of the fragment *or* the node/host to look for
     * @param nodeName name of the computed destination cloud
     * @return the selected VM type, `null` if not found.
     */
    private String getSelectedCloudVMType(Map<String, Map<String, Map<String, Map<String, String>>>> selectedCloudVMTypes, String vmName, String nodeName) {
        Map<String, Map<String, Map<String, String>>> tmp = selectedCloudVMTypes.get(vmName);
        if (tmp != null) {
            Optional<Map<String, String>> tmp2 = tmp.get("execute").values().stream().findFirst();
            if (tmp2.isPresent()) {
                return tmp2.get().get(nodeName);
            } else {
                throw new IllegalStateException("No property execute doesn't reference any TOSCA node");
            }

        } else {
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
}
