package prestocloud.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.btrplace.model.*;
import org.btrplace.model.constraint.*;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.*;
import org.btrplace.scheduler.choco.ChocoScheduler;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.prestocloud.tosca.model.templates.Topology;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class TypeLevelParsingSpace {

    private static final Pattern NETWORK_IP_PATTERN = Pattern.compile("(\\{ get_property: \\[([\\w,-]+),host,([\\w,]+),([\\w,]+),[\\d]+\\] \\})");
    private static final String TYPE_EXECUTE = "execute";
    private static final String TYPE_CLOUD = "cloud";
    private static final String PLACEMENT_EDGE = "edge ";
    private static final int SCHED_TIME_LIMIT = 10;
    private static final int MAX_NMB_VMS = 100;

    private Logger logger = LoggerFactory.getLogger(TypeLevelParsingSpace.class);
    // TODO: use a valid reference location to compute distances ("Sophia Antipolis" for testing only, must be retrieved from fragment's properties or dependencies)
    String sophiaAntipolisUTM = "32T 342479mE 4831495mN";

    private Map<String, String> metadata;
    private Map<String,Map<String, List<RegionCapacityDescriptor>>> regionsPerCloudPerCloudFile;
    private ParsingResult<ArchiveRoot> parsingResult;
    private ToscaParser parser;
    private String resourcesPath;

    // We describe couples of element we want to d integrate from our parsing.
    private Set<String> supportedCloudsResourceFiles;
    private List<Relationship> relationships;
    private List<PlacementConstraint> placementConstraints;
    private List<Docker> dockersCloud;
    private List<Docker> dockersEdge;
    private List<OptimizationVariables> optimizationVariables;
    private List<VMTemplateDetails> vmTemplatesDetails;
    private List<EdgeResourceTemplateDetails> edgeResourceDetails;
    private Map<String, Integer> occurencePerFragment;
    private Map<String, Boolean> scalablePerfragments;
    // TODO: deal with health checks
    private List<HealthCheck> healthChecks;
    private Map<String,SshKey> sshKeys;
    private Map<String, String> idPerFragment;
    private Map<String, Map<String, Map<String, Map<String, String>>>> selectedCloudVMTypes = new HashMap<>();
    private Map<String, String> hostingNodePerFragment = new HashMap<>();
    private Map<String,String> balancingNodes = new HashMap<>();
    private Map<String, String> balancedNodes = new HashMap<>();
    private Map<String,String> proxyingNodes = new HashMap<>();
    private Map<String,String> masteringNodes = new HashMap<>();

    //btrplace model related attributes
    private Map<String, VM> vmsPerName = new HashMap<>();
    private Map<VM, String> namePerVM = new HashMap<>();
    private Map<String, Set<String>> vmsPerFragment = new HashMap<>();
    private Map<String, String> fragmentsPerVm = new HashMap<>();
    private List<VM> alreadyRunningVms = new ArrayList<>();
    private ShareableResource cpu = new ShareableResource("cpu");
    private ShareableResource mem = new ShareableResource("memory");
    private ShareableResource disk = new ShareableResource("disk");
    private Model mo;
    private Mapping map;
    private CostView cv;

    // Public and Private Cloud identification - Contains Btrplace models.
    private Map<String, Node> nodePerName = new HashMap<>();
    private Map<Node,String> namePerNode = new HashMap<>();
    private Map<String, RegionCapacityDescriptor> regionCapabilityDescriptorPerCloud = new HashMap<>();
    private Map<String, RegionCapacityDescriptor> regionCapabilityDescriptorPerEdge = new HashMap<>();
    private HashMap<String, Boolean> cloudsToKeep = new HashMap<>();
    private HashMap<String, Boolean> edgeToKeep = new HashMap<>();

    private final List<SatConstraint> cstrs = new ArrayList<>();
    private final List<VM> vmToStop = new ArrayList<>();
    private Mapping dstmapping;
    private Set<Action> actions;

    public TypeLevelParsingSpace(ParsingResult<ArchiveRoot> result, GetVMTemplatesDetailsResult getVMTemplatesDetailsResult, List<EdgeResourceTemplateDetails> edgeResourceTemplateDetails, ToscaParser parser, String resourcesPath) {
        this.parsingResult = result;
        this.parser = parser;
        this.resourcesPath = resourcesPath;
        this.vmTemplatesDetails = getVMTemplatesDetailsResult.vmTemplatesDetails;
        this.regionsPerCloudPerCloudFile = getVMTemplatesDetailsResult.regionsPerCloudPerCloudFile;
        this.edgeResourceDetails = edgeResourceTemplateDetails;
    }

    public boolean retrieveResourceFromParsing() {
        // Retrieving main data from the parsed TOSCA.
        metadata = ParsingUtils.getMetadata(parsingResult);
        Set<String> allClouds = new HashSet<>();
        this.regionsPerCloudPerCloudFile.keySet().stream().forEach(s -> allClouds.add(s));
        supportedCloudsResourceFiles = ParsingUtils.getListOfCloudsFromMetadata(metadata,allClouds);
        if (supportedCloudsResourceFiles.isEmpty()) {
            this.supportedCloudsResourceFiles = allClouds;
        }
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
        logger.info(" -- {} supported cloud have been found", supportedCloudsResourceFiles.size());
        logger.info(" -- {} types of Edge devices have been found", this.edgeResourceDetails.size());
        relationships = ParsingUtils.getRelationships(parsingResult);
        placementConstraints = ParsingUtils.getConstraints(parsingResult);
        dockersCloud = ParsingUtils.getDockersCloud(parsingResult);
        dockersEdge = ParsingUtils.getDockersEdge(parsingResult);
        idPerFragment = ParsingUtils.getfragmentIds(parsingResult);
        optimizationVariables = ParsingUtils.getOptimizationVariables(parsingResult);
        sshKeys = ParsingUtils.getSshKeys(parsingResult);
        healthChecks = ParsingUtils.getHealthChecks(parsingResult);
        occurencePerFragment = ParsingUtils.getOccurencePerFragments(parsingResult);
        scalablePerfragments = ParsingUtils.getScalableFragments(parsingResult);
        return true;
    }

    public void classifyNodeAccordingToRelationships() {
        ConstrainedNode constraints;
        for (Relationship relationship : relationships) {
            if (relationship.getHostingNode().getType().equals(TYPE_EXECUTE)) {
                constraints = relationship.getHostingNode();
                hostingNodePerFragment.put(relationship.getFragmentName(), constraints.name);
                if (constraints.derivedTypes.contains("prestocloud.nodes.proxy.faas") || constraints.derivedTypes.contains("prestocloud.nodes.agent.faas")) {
                    logger.info(" -- {} fragment has been identified as operating a FaaS proxy node", relationship.getFragmentName());
                    this.proxyingNodes.put(constraints.getName(), relationship.getFragmentName());
                } else if (constraints.derivedTypes.contains("prestocloud.nodes.master.jppf")) {
                    logger.info(" -- {} fragment has been identified as operating a JPPF master node", relationship.getFragmentName());
                    this.masteringNodes.put(constraints.getName(), relationship.getFragmentName());
                } else if (constraints.derivedTypes.contains("prestocloud.nodes.proxy")) {
                    logger.info(" -- {} fragment has been identified as operating a load-balancing node", relationship.getFragmentName());
                    this.balancingNodes.put(constraints.getName(), relationship.getFragmentName());
                }
            }
        }
    }

    public boolean selectBestCloudVmType() throws Exception {
        for (Relationship relationship : relationships) {
            if (selectedCloudVMTypes.containsKey(relationship.getFragmentName())) {
                continue;
            }
            Map<String, Map<String, Map<String, String>>> allSelectedTypesWithRequirement = new HashMap<>();
            Map<String, Map<String, String>> allSelectedTypes = new HashMap<>();
            for (ConstrainedNode constrainedNode : relationship.getAllConstrainedNodes()) {
                logger.info(" -- Analyzing constraints on node {}", constrainedNode.name);
                for (NodeConstraints nodeConstraints : constrainedNode.getConstraints()) {
                    if (!nodeConstraints.getResourceConstraints().isEmpty()) {
                        // If the resource may run on cloud(s), select best matching types
                        if (nodeConstraints.getResourceConstraints().get("type").contains(TYPE_CLOUD)) {
                            investigateNodeConstraints(allSelectedTypes, constrainedNode, nodeConstraints);
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

    private void investigateNodeConstraints(Map<String, Map<String, String>> allSelectedTypes, ConstrainedNode constrainedNode, NodeConstraints nodeConstraints) throws Exception {
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
    }

    public void configureBtrPlace() {
        mo = new DefaultModel();
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
                if (selectedTypes.getKey().equalsIgnoreCase(TYPE_EXECUTE)) {
                    proceedVmRegistrationFromFragment(vmsName, " -- Registering fragment {} ...");
                } else if (selectedTypes.getKey().equalsIgnoreCase("master")) {
                    proceedVmRegistrationFromFragment(vmsName, " -- Registering slave fragment {} ...");
                    dependencyNode = masteringNodes.get(retrieveDependencyFragment(selectedTypes, masteringNodes));
                    proceedVmRegistrationFromFragment(dependencyNode, " -- Registering master fragment {} ...");
                } else if (selectedTypes.getKey().equalsIgnoreCase("balanced_by")) {
                    proceedVmRegistrationFromFragment(vmsName, " -- Registering balanced fragment {} ...");
                    dependencyNode = balancingNodes.get(retrieveDependencyFragment(selectedTypes, balancingNodes));
                    balancedNodes.put(vmsName, dependencyNode);
                    proceedVmRegistrationFromFragment(dependencyNode, " -- Registering balancing fragment {} ...");
                } else if (selectedTypes.getKey().equalsIgnoreCase("proxy")) {
                    proceedVmRegistrationFromFragment(vmsName, " -- Registering proxified fragment {} ...");
                    dependencyNode = proxyingNodes.get(retrieveDependencyFragment(selectedTypes, proxyingNodes));
                    proceedVmRegistrationFromFragment(dependencyNode, " -- Registering proxying fragment {} ...");
                } else {
                    // We can have duplicates (eg. a 'proxy' may be linked to multiple fragments)
                    vmsName = retrieveDependencyFragment(selectedTypes, null);
                    proceedVmRegistrationFromFragment(vmsName, " -- Registering an unclassified fragment {} ...");
                }
            }
        }
    }

    private String retrieveDependencyFragment(Map.Entry<String, Map<String, Map<String, String>>> selectedTypes, Map<String, String> typingNode) {
        Optional<String> tmp = selectedTypes.getValue().keySet().stream().filter(s -> (typingNode.containsKey(s))).findFirst();
        if (tmp.isPresent()) {
            return tmp.get();
        } else {
            throw new IllegalStateException("The selected Types doesn't reference any fragment name: " + selectedTypes.getKey());
        }
    }

    private void proceedVmRegistrationFromFragment(String fragmentName, String loggerMessage) {
        int i;
        Set<String> generatedVmsNames = new HashSet<>();
        for (i = 0; i < occurencePerFragment.get(fragmentName); i++) {
            generatedVmsNames.add(String.format("%s_%d", fragmentName, i));
        }
        vmsPerFragment.put(fragmentName, generatedVmsNames);
        for (String vmName : generatedVmsNames) {
            fragmentsPerVm.put(vmName, fragmentName);
            if (!vmsPerName.containsKey(vmName)) {
                VM newVM = mo.newVM();
                vmsPerName.put(vmName, newVM);
                namePerVM.put(newVM, vmName);
                logger.warn(loggerMessage, vmName);
            }
        }
    }

    private void proceedVmRegistrationFromDeployedFragment(String vmName, String loggerMessage) {
        // Retrieve what should be the fragment name
        String fragmentName = vmName.substring(0, vmName.lastIndexOf("_"));
        fragmentsPerVm.put(vmName, fragmentName);
        // If the fragment is not yet registered, I declare it
        if (!vmsPerFragment.containsKey(fragmentName)) {
            vmsPerFragment.put(fragmentName, new HashSet<>());
        }
        vmsPerFragment.get(fragmentName).add(vmName);
        // Declare the node
        if (!vmsPerName.containsKey(vmName)) {
            VM newVM = mo.newVM();
            vmsPerName.put(vmName, newVM);
            namePerVM.put(newVM, vmName);
            logger.warn(loggerMessage, vmName);
        }
    }

    public void populateNodesInBtrPlaceModel() {
        String placementString;
        // Proceeding w/ cloud nodes
        for (String cloudFile : this.regionsPerCloudPerCloudFile.keySet()) {
            for (String cloud : regionsPerCloudPerCloudFile.get(cloudFile).keySet()) {
                for (RegionCapacityDescriptor region : regionsPerCloudPerCloudFile.get(cloudFile).get(cloud)) {
                    placementString = cloud + " " + region.getRegion();
                    regionCapabilityDescriptorPerCloud.put(placementString, region);
                    if (!nodePerName.containsKey(placementString)) {
                        registerNewNode(placementString, false, " -- Registering node {} as {} ...", cloudsToKeep);
                    }
                }
            }
        }
        // Proceeding w/ edge nodes
        for (EdgeResourceTemplateDetails edgeResource : this.edgeResourceDetails) {
            placementString = PLACEMENT_EDGE + edgeResource.id;
            regionCapabilityDescriptorPerEdge.put(placementString, new RegionCapacityDescriptor(placementString,edgeResource.num_cpus,edgeResource.mem_size,edgeResource.disk_size.orElse("0 GB")));
            if (!nodePerName.containsKey(placementString)) {
                registerNewNode(placementString, false, " -- Registering edge node {} as {} ...", edgeToKeep);
            }
        }
    }


    private void registerNewNode(String placementString,boolean isToBeKept, String loggerMessage, Map<String,Boolean> arrayToKeep) {
        Node node = mo.newNode();
        map.addOnlineNode(node);
        nodePerName.put(placementString,node);
        namePerNode.put(node,placementString);
        arrayToKeep.put(placementString, isToBeKept);
        logger.info(loggerMessage, placementString, node);
    }

    public void setCloudNodeToKeep() {
        String placementString;
        for (String supportedCloud : this.supportedCloudsResourceFiles) {
            for (String cloud : regionsPerCloudPerCloudFile.get(supportedCloud).keySet()) {
                for (RegionCapacityDescriptor region : regionsPerCloudPerCloudFile.get(supportedCloud).get(cloud)) {
                    placementString = cloud + " " + region.getRegion();
                    map.addOnlineNode(nodePerName.get(placementString));
                    cloudsToKeep.put(placementString, true);
                    logger.info(" -- Setting the cloud whitelisted node {} online ...", placementString);
                }
            }
        }
        for (Map.Entry<String, Boolean> entree : cloudsToKeep.entrySet()) {
            Node node = nodePerName.get(entree.getKey());
            if (Boolean.TRUE.equals(entree.getValue())) {
                cstrs.add(new Online(node));
            } else {
                cstrs.add(new RunningCapacity(nodePerName.get(entree.getKey()), 0));
            }
        }
    }

    public void loadRunningEdgeNode(String edgeGwNodeFileContent) {
        // Could also be labelled setEdgeNodeToBeKept()
        if (!JSONValue.isValidJson(edgeGwNodeFileContent)) {
            throw new IllegalArgumentException("The provided mapping is not valid. I leave.");
        }
        JSONArray rawRmOutput = (JSONArray) JSONValue.parse(edgeGwNodeFileContent);
        List<String> availableNodeSource = rawRmOutput.stream().map(o -> ((JSONObject) o)).filter(jo -> jo.getAsString("nodeSourceStatus").equals("deployed")).map(jo -> jo.getAsString("nodeSourceName")).collect(Collectors.toList());
        List<String> referencedEdgeDevice = this.edgeResourceDetails.stream().map(edgeResourceTemplateDetails -> edgeResourceTemplateDetails.id).collect(Collectors.toList());
        for (String nodeName : availableNodeSource) {
            if (!referencedEdgeDevice.contains(nodeName)) {
                logger.info("{} has not recognized as a known resource, I skip it.", nodeName);
                continue;
            }
            map.addOnlineNode(nodePerName.get(PLACEMENT_EDGE + nodeName));
            edgeToKeep.put(PLACEMENT_EDGE + nodeName, Boolean.TRUE);
            logger.info(" -- Edge node {} is acknowledged as available", nodeName);
        }
        for (Map.Entry<String, Boolean> entree : edgeToKeep.entrySet()) {
            Node node = nodePerName.get(entree.getKey());
            if (Boolean.TRUE.equals(entree.getValue())) {
                cstrs.add(new Online(node));
                cstrs.add(new RunningCapacity(nodePerName.get(entree.getKey()), 1));
            } else {
                logger.info("Edge node {} is reported as down",entree.getKey());
                cstrs.add(new RunningCapacity(nodePerName.get(entree.getKey()), 0));
            }
        }
    }

    public void loadExistingMapping(String mappingContent) {
        if (!JSONValue.isValidJson(mappingContent)) {
            throw new IllegalArgumentException("The provided mapping is not valid. I leave.");
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
                proceedExistingVMRegistration(operatedVMSonNode,nodeName);
            }
        }
    }

    private void proceedExistingVMRegistration(String operatedVMSonNode, String nodeName) {
        boolean isVmAlreadyRegisteredFromToscaParsing = this.vmsPerName.containsKey(operatedVMSonNode);
        boolean isNodeReferencedInRequiredProvider = this.nodePerName.containsKey(nodeName);
        if (!isNodeReferencedInRequiredProvider) {
            if (isPlacementStringRelatedToEdge(nodeName)) {
                registerNewNode(nodeName, false, "-- Registering a edge node from an excluded provider {} as {}", edgeToKeep);
            } else {
                registerNewNode(nodeName, false, " -- Registering a cloud node from an excluded provider {} as {}", cloudsToKeep);
            }
        }
        if (isVmAlreadyRegisteredFromToscaParsing) {
            // The fragment is referenced by the type-level TOSCA: This fragment is expected to be running by the end of the parsing
            logger.info(" -- Reading mapping: Node {} is operating VM {}, left to be running", nodeName, operatedVMSonNode);
            map.addRunningVM(this.vmsPerName.get(operatedVMSonNode),this.nodePerName.get(nodeName));
        } else {
            // The fragment is no more referenced: we register this VM as running, but constraint it to be removed.
            proceedVmRegistrationFromDeployedFragment(operatedVMSonNode, " -- Registering fragment to be removed {}");
            map.addRunningVM(this.vmsPerName.get(operatedVMSonNode),this.nodePerName.get(nodeName));
            cstrs.add(new Ready(this.vmsPerName.get(operatedVMSonNode)));
            vmToStop.add(this.vmsPerName.get(operatedVMSonNode));
        }
        alreadyRunningVms.add(this.vmsPerName.get(operatedVMSonNode));
    }

    private boolean isPlacementStringRelatedToEdge(String placementString) {
        String[] tmp = placementString.split(" ");
        if (tmp.length == 2) {
            return tmp[0].equals("edge");
        } else {
            return false;
        }
    }


    public void setCapacity() {
        // Create and attach cpu, memory and disk resources
        mo.attach(cpu);
        mo.attach(mem);
        mo.attach(disk);

        for (Map.Entry<String, Node> nodeEntree : nodePerName.entrySet()) {
            if (isPlacementStringRelatedToEdge(nodeEntree.getKey())) {
                cpu.setCapacity(nodeEntree.getValue(), this.regionCapabilityDescriptorPerEdge.get(nodeEntree.getKey()).getCpuCapacity());
                mem.setCapacity(nodeEntree.getValue(), this.regionCapabilityDescriptorPerEdge.get(nodeEntree.getKey()).getMemoryCapacity());
                disk.setCapacity(nodeEntree.getValue(), this.regionCapabilityDescriptorPerEdge.get(nodeEntree.getKey()).getDiskCapacity());
            } else {
                cpu.setCapacity(nodeEntree.getValue(), this.regionCapabilityDescriptorPerCloud.get(nodeEntree.getKey()).getCpuCapacity());
                mem.setCapacity(nodeEntree.getValue(), this.regionCapabilityDescriptorPerCloud.get(nodeEntree.getKey()).getMemoryCapacity());
                disk.setCapacity(nodeEntree.getValue(), this.regionCapabilityDescriptorPerCloud.get(nodeEntree.getKey()).getDiskCapacity());
            }
        }
    }

    public void configuringVmsResourcesRequirementConstraint() {
        /* Set consumption of all required VM based on the requirement of processing nodes in TOSCA file
        Strategy (discussed w/ CNRS): fragment requirement for cloud are now useless: they are taken into account during the prior selection of preferred VM type in TOSCA
        Now, we prioritize on edge requirements, and only enforce cloud requirements if no edge requirement exist.
         */
        Set<String> edgeConfiguredFragments = new HashSet<>();
        Set<String> cloudConfiguredFragments = new HashSet<>();
        Map<String, List<String>> cameraConstraintPerFragmentName = new HashMap<>();
        Map<String, List<String>> microphoneConstraintPerFragmentName = new HashMap<>();
        Map<String, List<String>> temperatureConstrainPerFragmentName = new HashMap<>();
        configureFragmentsResourceConstraintForEdge(edgeConfiguredFragments);
        configureFragmentsResourceConstraintForCloud(edgeConfiguredFragments, cloudConfiguredFragments);
        retrieveSensorsConstraintPerFragment(cameraConstraintPerFragmentName, microphoneConstraintPerFragmentName, temperatureConstrainPerFragmentName);
        Set<String> edgeOnlyFragments = edgeConfiguredFragments.parallelStream().filter(s -> !cloudConfiguredFragments.contains(s)).collect(Collectors.toSet());
        Set<String> cloudOnlyFragments = cloudConfiguredFragments.parallelStream().filter(s -> !edgeConfiguredFragments.contains(s)).collect(Collectors.toSet());
        // Implementing Fence constraint
        if (!edgeOnlyFragments.isEmpty()) {
            // If we have at least one vm/framgent to be hosted specifically on a edge node ...
            logger.info(" -- The following fragments were recognized to have to be run on edge-specific nodes: {}", edgeOnlyFragments);
            Set<Node> edgeNodes = edgeToKeep.entrySet().parallelStream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .map(s -> nodePerName.get(s))
                    .collect(Collectors.toSet());
            edgeOnlyFragments.stream()
                    .map(fragmentName -> vmsPerFragment.get(fragmentName))
                    .forEach(vmsSet -> vmsSet.forEach(vm -> cstrs.add(new Fence(vmsPerName.get(vm), edgeNodes))));
        }
        if (!cloudOnlyFragments.isEmpty()) {
            // If we have at least one vm/fragment to be hosted specifically on a cloud node ...
            logger.info(" -- The following fragments were reconized to have to be run on cloud-specific nodes : {}", cloudOnlyFragments);
            Set<Node> cloudNodes = cloudsToKeep.entrySet().parallelStream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .map(s -> nodePerName.get(s))
                    .collect(Collectors.toSet());
            cloudOnlyFragments.stream()
                    .map(fragmentName -> vmsPerFragment.get(fragmentName))
                    .forEach(vmsSet -> vmsSet.forEach(vm -> cstrs.add(new Fence(vmsPerName.get(vm), cloudNodes))));
        }
        if (!cameraConstraintPerFragmentName.isEmpty()) {
            for (Map.Entry<String, List<String>> constrain : cameraConstraintPerFragmentName.entrySet()) {
                Set<Node> matchingEdgeDevice = edgeResourceDetails.stream()
                        .filter(details -> details.cameraSensor.isPresent())
                        .filter(detail -> constrain.getValue().parallelStream().anyMatch(specs -> specs.equals(detail.cameraSensor.get())))
                        .map(match -> String.format("edge %s", match.id))
                        .map(nodeName -> nodePerName.get(nodeName))
                        .collect(Collectors.toSet());
                logger.info(" -- Enforcing constraint camera sensors constraints for fragment {} on nodes {}", constrain.getKey(), matchingEdgeDevice);
                vmsPerFragment.get(constrain.getKey())
                        .forEach(vms -> cstrs.add(new Fence(vmsPerName.get(vms), matchingEdgeDevice)));
            }
        }
        if (!microphoneConstraintPerFragmentName.isEmpty()) {
            for (Map.Entry<String, List<String>> constrain : microphoneConstraintPerFragmentName.entrySet()) {
                Set<Node> matchingEdgeDevice = edgeResourceDetails.stream()
                        .filter(details -> details.microphoneSensor.isPresent())
                        .filter(detail -> constrain.getValue().parallelStream().anyMatch(specs -> specs.equals(detail.microphoneSensor.get())))
                        .map(match -> String.format("edge %s", match.id))
                        .map(nodeName -> nodePerName.get(nodeName))
                        .collect(Collectors.toSet());
                logger.info(" -- Enforcing constraint microphone sensors constraints for fragment {} on nodes {}", constrain.getKey(), matchingEdgeDevice);
                vmsPerFragment.get(constrain.getKey())
                        .forEach(vms -> cstrs.add(new Fence(vmsPerName.get(vms), matchingEdgeDevice)));
            }
        }
        if (!temperatureConstrainPerFragmentName.isEmpty()) {
            for (Map.Entry<String, List<String>> constrain : temperatureConstrainPerFragmentName.entrySet()) {
                Set<Node> matchingEdgeDevice = edgeResourceDetails.stream()
                        .filter(details -> details.temperatureSensor.isPresent())
                        .filter(detail -> constrain.getValue().parallelStream().anyMatch(specs -> specs.equals(detail.temperatureSensor.get())))
                        .map(match -> String.format("edge %s", match.id))
                        .map(nodeName -> nodePerName.get(nodeName))
                        .collect(Collectors.toSet());
                logger.info(" -- Enforcing constraint temperature sensors constraints for fragment {} on nodes {}", constrain.getKey(), matchingEdgeDevice);
                vmsPerFragment.get(constrain.getKey())
                        .forEach(vms -> cstrs.add(new Fence(vmsPerName.get(constrain.getKey()), matchingEdgeDevice)));
            }
        }
    }

    private void configureFragmentsResourceConstraintForEdge(Set<String> edgeConfiguredFragment) {
        String fragmentName;
        for (Relationship relationship : relationships) {
            for (ConstrainedNode constrainedNode : relationship.getAllConstrainedNodes()) {
                for (NodeConstraints nodeConstraints : constrainedNode.getConstraints()) {
                    if (!nodeConstraints.getResourceConstraints().isEmpty()) {
                        if (nodeConstraints.getResourceConstraints().get("type").contains("edge") && constrainedNode.getType().equalsIgnoreCase(TYPE_EXECUTE)) {
                            fragmentName = relationship.getFragmentName();
                            if (!edgeConfiguredFragment.contains(fragmentName)) {
                                logger.info(" -- Enforcing edge node constraints on VMS based on fragment {} in Btrplace model", fragmentName);
                                // Retrieve constraints from the edge resources hosting
                                configureGeneralHostingConstrains(fragmentName, nodeConstraints);
                                // Add support for edge-specific resource constrain: no additional constraint found.
                                // Should be a topic to be discuss w/ Andreas
                                edgeConfiguredFragment.add(fragmentName);
                            }
                        }
                    }
                }
            }
        }
    }

    private void configureFragmentsResourceConstraintForCloud(Set<String> edgeConfiguredFragments, Set<String> cloudConfiguredFragments) {
        String fragmentName;
        for (Relationship relationship : relationships) {
            for (ConstrainedNode constrainedNode : relationship.getAllConstrainedNodes()) {
                for (NodeConstraints nodeConstraints : constrainedNode.getConstraints()) {
                    if (!nodeConstraints.getResourceConstraints().isEmpty()) {
                        // If the resource may run on cloud(s), select best matching types
                        if (nodeConstraints.getResourceConstraints().get("type").contains(TYPE_CLOUD) && constrainedNode.getType().equalsIgnoreCase(TYPE_EXECUTE)) {
                            fragmentName = relationship.getFragmentName();
                            if (!cloudConfiguredFragments.contains(fragmentName)) {
                                if (!edgeConfiguredFragments.contains(fragmentName)) {
                                    logger.info(" -- Enforcing cloud node constraints on VMS based on fragment {} in Btrplace model", fragmentName);
                                    configureGeneralHostingConstrains(fragmentName, nodeConstraints);
                                }
                                cloudConfiguredFragments.add(fragmentName);
                            }
                        }
                    }
                }
            }
        }
    }

    private void retrieveSensorsConstraintPerFragment(Map<String, List<String>> cameraPerFragment, Map<String, List<String>> microphonePerFragment, Map<String, List<String>> temperaturePerFragment) {
        for (Relationship relationship : relationships) {
            for (ConstrainedNode constrainedNode : relationship.getAllConstrainedNodes()) {
                for (NodeConstraints nodeConstraints : constrainedNode.getConstraints()) {
                    // Configure here sensors
                    if (!nodeConstraints.getSensorsConstraints().isEmpty() && constrainedNode.getType().equalsIgnoreCase(TYPE_EXECUTE)) {
                        logger.info(" -- Sensor constraints detected for fragment vmName={}", relationship.getFragmentName());
                        if (nodeConstraints.getSensorsConstraints().get("camera") != null) {
                            cameraPerFragment.put(relationship.getFragmentName(), nodeConstraints.getSensorsConstraints().get("camera"));
                        }
                        if (nodeConstraints.getSensorsConstraints().get("microphone") != null) {
                            microphonePerFragment.put(relationship.getFragmentName(), nodeConstraints.getSensorsConstraints().get("microphone"));
                        }
                        if (nodeConstraints.getSensorsConstraints().get("temperature") != null) {
                            temperaturePerFragment.put(relationship.getFragmentName(), nodeConstraints.getSensorsConstraints().get("temperature"));
                        }
                    }
                }
            }
        }
    }

    private void configureGeneralHostingConstrains(String fragmentName, NodeConstraints nodeConstraints) {
        // Prepare VM name depending on requirement type ('execute' targets the fragment name, others target node/host name)
        for (Map.Entry<String, List<String>> hostingConstraint : nodeConstraints.getHostingConstraints().entrySet()) {
            String required = hostingConstraint.getValue().get(0);

            int constraint;
            if (required.contains("RangeMin")) {
                constraint = Integer.parseInt(required.split(",")[0].split(" ")[1]);
            } else if (required.contains("GreaterOrEqual")) {
                constraint = Integer.parseInt(required.split(" ")[1]);
            } else {
                constraint = Integer.parseInt(required);
            }

            if (hostingConstraint.getKey().equalsIgnoreCase("num_cpus")) {
                for (String vmName : vmsPerFragment.get(fragmentName)) {
                    cpu.setConsumption(vmsPerName.get(vmName), constraint);
                }
            } else if (hostingConstraint.getKey().equalsIgnoreCase("mem_size")) {
                // Mem in GB only
                if (required.contains("MB")) {
                    constraint = constraint / 1024;
                }
                for (String vmName : vmsPerFragment.get(fragmentName)) {
                    mem.setConsumption(vmsPerName.get(vmName), constraint);
                }
            } else if (hostingConstraint.getKey().equalsIgnoreCase("storage_size")) {
                // Storage in GB only
                if (required.contains("MB")) {
                    constraint = constraint / 1024;
                }
                for (String vmName : vmsPerFragment.get(fragmentName)) {
                    disk.setConsumption(vmsPerName.get(vmName), constraint);
                }
            } else {
                logger.error("Unrecognized hosting constraint: {} ", hostingConstraint.getKey());
            }
        }
    }


    public void banUnreferencedCloudInCloudList(List<Object> ja) {
        JSONObject cloud;
        String cloudName;
        String cloudType;
        String region;
        HashMap<String, String> identifiedClouds = new HashMap<>();
        for (Object entree : ja) {
            cloud = (JSONObject) entree;
            cloudName = cloud.getAsString("CLOUD_NAME");
            cloudType = cloud.getAsString("CLOUD_TYPE");
            if (cloudType.equalsIgnoreCase("aws")) {
                cloudType = "amazon";
                region = cloud.getAsString("AWS_DEFAULT_REGION");
            } else if (cloudType.equalsIgnoreCase("azure")) {
                cloudType = "azure";
                region = cloud.getAsString("AZ_LOCATION");
            } else if (cloudType.equalsIgnoreCase("openstack")) {
                cloudType = "openstack";
                region = cloud.getAsString("OS_REGION_NAME");
            } else {
                logger.error("Unable to recognize cloud type of cloud={}. I skip it", cloudName);
                continue;
            }
            identifiedClouds.put(cloudName, cloudType + " " + region);
        }
        identifiedClouds.forEach((s, s2) -> logger.info(" -- Cloud named {} in CLOUD_LIST is recognized as {} and is still whitelisted", s, s2));
        // Proceeding to the effective Ban of the resource.
        List<String> cloudsToIgnore = this.cloudsToKeep.entrySet().stream()
                .filter(Map.Entry::getValue)
                .filter(stringBooleanEntry -> (!identifiedClouds.containsValue(stringBooleanEntry.getKey())))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        for (String placementString : cloudsToIgnore) {
            logger.info(" -- Cloud {} is not in the CLOUD_LIST Workflow variable, I blacklist it", placementString);
            cstrs.add(new RunningCapacity(this.nodePerName.get(placementString), 0));
        }
    }

    public void defineFragmentDeployability() {
        for (Map.Entry<String, VM> vm : vmsPerName.entrySet()) {
            if (!alreadyRunningVms.contains(vm.getValue())) {
                map.ready(vm.getValue());
            }
            if (!vmToStop.contains(vm.getValue())) {
                cstrs.add(new Running(vm.getValue()));
            }
        }
    }

    public void configurePlacementConstraint() {
        // Apply placement constraints
        for (PlacementConstraint placementConstraint : placementConstraints) {
            if (placementConstraint.getType().contains("Spread")) {
                List<Set<VM>> constrainedVms = new ArrayList<>();
                if (vmsPerFragment.keySet().containsAll(placementConstraint.getTargets())) {
                    for (String targetFragments : placementConstraint.getTargets()) {
                        constrainedVms.add(this.vmsPerFragment.get(targetFragments).stream().map(vmName -> vmsPerName.get(vmName)).collect(Collectors.toSet()));
                    }
                    // To be checked
                    Sets.cartesianProduct(constrainedVms).forEach(cstSet -> cstrs.add(new Spread(new HashSet<>(cstSet))));
                } else {
                    logger.warn("Non consistent 'Spread' constraint detected.");
                }
            }
            if (placementConstraint.getType().contains("Gather")) {
                Set<VM> constrainedVms = new HashSet<>();
                // Check if the VM actually exists (this is currently triggered as edge devices are not yet managed)
                if (vmsPerFragment.keySet().containsAll(placementConstraint.getTargets())) {
                    for (String targetFragments : placementConstraint.getTargets()) {
                        constrainedVms.addAll(this.vmsPerFragment.get(targetFragments).stream().map(vmName -> vmsPerName.get(vmName)).collect(Collectors.toSet()));
                    }
                    cstrs.add(new Gather(constrainedVms));
                } else {
                    logger.warn("Non consistent 'Gather' constraint detected.");
                }
            }
            if (placementConstraint.getType().contains("Precedence")) {
                // Check if the VM actually exists (this is currently triggered as edge devices are not yet managed)
                if (vmsPerFragment.keySet().containsAll(placementConstraint.getTargets())) {
                    List<Set<VM>> ordonnedFragmentAllocation = placementConstraint.getTargets().stream()
                            .map(fragments -> vmsPerFragment.get(fragments))
                            .map(setNameVm -> setNameVm.stream().map(vmName -> vmsPerName.get(vmName)).collect(Collectors.toSet()))
                            .collect(Collectors.toList());
                    for (int i = 0; i < ordonnedFragmentAllocation.size() - 1; i++) {
                        for (VM vm : ordonnedFragmentAllocation.get(i + 1)) {
                            cstrs.add(new PrecedingRunning(vm, ordonnedFragmentAllocation.get(i)));
                        }
                    }
                } else {
                    logger.warn("Non consistent 'Precedence' constraint detected.");
                }
            }
        }
    }

    public void extractCost() {
        // Set cost view for each node <-> vm pair (values are extracted from optimization objective variables & VM templates details)
        for (Map.Entry<String, VM> vm : vmsPerName.entrySet()) {
            // Find corresponding optimisation variables, note that they are only set on fragment (not their dependencies like proxy, master, etc.)
            Optional<OptimizationVariables> vmOptimVars = optimizationVariables.stream().filter(optimVars -> optimVars.getFragmentName().equalsIgnoreCase(fragmentsPerVm.get(vm.getKey()))).findFirst();
            for (Map.Entry<String, Node> node : nodePerName.entrySet()) {
                if (node.getKey().startsWith("edge")) {
                    proceedCostExtractionEdge(node);
                } else {
                    proceedCostExtractionCloud(vm, vmOptimVars,node);
                }
            }
        }
    }

    private void proceedCostExtractionCloud(Map.Entry<String, VM> vm, Optional<OptimizationVariables> vmOptimVars, Map.Entry<String, Node> node ) {
        // Find corresponding VM template
        Optional<VMTemplateDetails> tmp;
        tmp = vmTemplatesDetails.stream()
                .filter(vmTplDetails -> (vmTplDetails.getCloud() + " " + vmTplDetails.getRegion()).equalsIgnoreCase(node.getKey()))
                .filter(vmtpl -> !this.selectedCloudVMTypes.containsKey(fragmentsPerVm.get(vm.getKey())) ||
                        this.selectedCloudVMTypes
                                .get(fragmentsPerVm.get(vm.getKey()))
                                .containsKey(TYPE_EXECUTE) ||
                        this.selectedCloudVMTypes
                        .get(fragmentsPerVm.get(vm.getKey()))
                        .get(TYPE_EXECUTE)
                        .get(hostingNodePerFragment.get(fragmentsPerVm.get(vm.getKey())))
                                .getOrDefault(
                                        String.format("%s %s", vmtpl.cloud, vmtpl.region)
                                        , "")
                        .equals(vmtpl.instanceName))
                .findFirst();
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

    private void proceedCostExtractionEdge(Map.Entry<String, Node> node) {
        // Set default values for 'dependent' hosting nodes
        cv.edgeHost(node.getValue());

    }
    public boolean performedBtrplaceSolving() {
        // Create an instance with MinCost objective
        Instance ii = new Instance(mo, cstrs, new MinCost());

        // Start an optimized scheduler and solve the problem
        final ChocoScheduler sched = PrestoCloudExtensions.newScheduler();
        sched.doOptimize(true);
        sched.setTimeLimit(SCHED_TIME_LIMIT);
        ReconfigurationPlan p = sched.solve(ii);
        if (p == null) {
            return false;
        }

        // Get list of computed actions
        int nmbVms = p.getResult().getMapping().getRunningVMs().size();
        if (nmbVms > MAX_NMB_VMS) {
            throw new IllegalStateException(String.format("I want to allocate %d vm(s) : the maximum number of allowed VM will be exceeded", nmbVms));
        }
        logger.info("{} vm will be allocated",nmbVms);
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
       String cloud;
       String region;
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

        String selectedVMType = getSelectedCloudVMType(selectedCloudVMTypes, fragmentsPerVm.get(vmName), nodeName);
       JSONObject jo = new JSONObject();
        cloud = nodeName.split(" ")[0];
        region = nodeName.split(" ")[1];
        jo.put(OutputField.ACTION, OutputField.ACTION_BOOT);
        jo.put(OutputField.ACTION_START, action.getStart());
        jo.put(OutputField.ACTION_END, action.getEnd());
        jo.put(OutputField.ACTION_FRAGMENT, vmName);
        jo.put(OutputField.ACTION_ID, this.idPerFragment.getOrDefault(fragmentsPerVm.get(vmName), ""));
        jo.put(OutputField.ACTION_CLOUD, cloud);
        jo.put(OutputField.ACTION_REGION, region);
        jo.put(OutputField.ACTION_TYPE, selectedVMType);
        jo.put(OutputField.ACTION_NODE, this.hostingNodePerFragment.get(fragmentsPerVm.get(vmName)));
        jo.put(OutputField.ACTION_SCALABLE, this.scalablePerfragments.getOrDefault(fragmentsPerVm.get(vmName), false));
        if (this.sshKeys.containsKey(fragmentsPerVm.get(vmName))) {
            if (this.sshKeys.get(fragmentsPerVm.get(vmName)).hasKey()) {
                jo.put(OutputField.ACTION_SSH_KEY, this.sshKeys.get(fragmentsPerVm.get(vmName)).getPublicKey());
            }
        }

        // Docker command is optional because 'proxy', 'master', and 'balanced_by' nodes don't have one
        Optional<Docker> docker;
        if (cloud.equals("edge")) {
            docker = dockersEdge.stream().filter(d -> d.getFragmentName().equalsIgnoreCase(fragmentsPerVm.get(vmName))).findFirst();
        } else {
            docker = dockersCloud.stream().filter(d -> d.getFragmentName().equalsIgnoreCase(fragmentsPerVm.get(vmName))).findFirst();
        }
        docker.ifPresent(dck -> jo.put(OutputField.ACTION_DOCKER, replaceHostPropertyValuesToMacros(dck.printCmdline())));
        docker.ifPresent(dck -> jo.put(OutputField.ACTION_PORTS, dck.getAllExposedPorts()));
        if (balancedNodes.containsKey(fragmentsPerVm.get(vmName))) {
            jo.put(OutputField.ACTION_LOADBALANCED_BY, vmsPerFragment.get(balancedNodes.get(fragmentsPerVm.get(vmName))).stream().findFirst().orElse(""));
        } else {
            if (balancingNodes.containsValue(fragmentsPerVm.get(vmName))) {
                docker.ifPresent(dck -> jo.put(OutputField.ACTION_PUBLICPORTS, dck.getAllPubliclyExposedPorts()));
            }
        }
        ja.add(jo);
    }

    private void generationMigrateVMOutput(Action action, JSONArray ja) {
        // Retrieve VM/fragment name
        String vmName = namePerVM.get(((MigrateVM) action).getVM());
        String cloud;
        String region;
        // Retrieve node/host nameBugfix: nullPointer w/ edge or cloud specific node
        String nodeNameSrc = namePerNode.get(((MigrateVM) action).getSourceNode());
        String nodeName = namePerNode.get(((MigrateVM) action).getDestinationNode());

        String selectedVMType = getSelectedCloudVMType(selectedCloudVMTypes, fragmentsPerVm.get(vmName), nodeName);
        JSONObject jo = new JSONObject();
        cloud = nodeName.split(" ")[0];
        region = nodeName.split(" ")[1];
        jo.put(OutputField.ACTION, OutputField.ACTION_MIGRATE);
        jo.put(OutputField.ACTION_START, action.getStart());
        jo.put(OutputField.ACTION_END, action.getEnd());
        jo.put(OutputField.ACTION_FRAGMENT, vmName);
        jo.put(OutputField.ACTION_ID, this.idPerFragment.getOrDefault(fragmentsPerVm.get(vmName), ""));
        jo.put(OutputField.ACTION_CLOUD, cloud);
        jo.put(OutputField.ACTION_REGION, region);
        jo.put(OutputField.ACTION_CLOUDSRC, nodeNameSrc.split(" ")[0]);
        jo.put(OutputField.ACTION_REGIONSRC, nodeNameSrc.split(" ")[1]);
        jo.put(OutputField.ACTION_TYPE, selectedVMType);
        jo.put(OutputField.ACTION_NODE, this.hostingNodePerFragment.get(fragmentsPerVm.get(vmName)));
        jo.put(OutputField.ACTION_SCALABLE, this.scalablePerfragments.getOrDefault(fragmentsPerVm.get(vmName), false));
        if (this.sshKeys.containsKey(fragmentsPerVm.get(vmName))) {
            if (this.sshKeys.get(fragmentsPerVm.get(vmName)).hasKey()) {
                jo.put(OutputField.ACTION_SSH_KEY, this.sshKeys.get(fragmentsPerVm.get(vmName)).getPublicKey());
            }
        }

        // Docker command is optional because 'proxy', 'master', and 'balanced_by' nodes don't have one
        Optional<Docker> docker;
        if (cloud.equals("edge")) {
            docker = dockersEdge.stream().filter(d -> d.getFragmentName().equalsIgnoreCase(fragmentsPerVm.get(vmName))).findFirst();
        } else {
            docker = dockersCloud.stream().filter(d -> d.getFragmentName().equalsIgnoreCase(fragmentsPerVm.get(vmName))).findFirst();
        }
        docker.ifPresent(dck -> jo.put(OutputField.ACTION_DOCKER, replaceHostPropertyValuesToMacros(dck.printCmdline())));
        docker.ifPresent(dck -> jo.put(OutputField.ACTION_PORTS, dck.getAllExposedPorts()));
        if (balancedNodes.containsKey(fragmentsPerVm.get(vmName))) {
            jo.put(OutputField.ACTION_LOADBALANCED_BY, vmsPerFragment.get(balancedNodes.get(fragmentsPerVm.get(vmName))).stream().findFirst().orElse(""));
        } else {
            docker.ifPresent(dck -> jo.put(OutputField.ACTION_PUBLICPORTS, dck.getAllPubliclyExposedPorts()));
        }
        ja.add(jo);
    }


    private void generationShutdownVMAndSuspendVMOutput(Action action, JSONArray ja) {
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

        JSONObject jo = new JSONObject();
        jo.put(OutputField.ACTION, OutputField.ACTION_DELETE);
        jo.put(OutputField.ACTION_START, action.getStart());
        jo.put(OutputField.ACTION_END, action.getEnd());
        jo.put(OutputField.ACTION_FRAGMENT, vmName);
        jo.put(OutputField.ACTION_ID, this.idPerFragment.getOrDefault(fragmentsPerVm.get(vmName), ""));
        jo.put(OutputField.ACTION_CLOUDSRC, nodeName.split(" ")[0]);
        jo.put(OutputField.ACTION_REGIONSRC, nodeName.split(" ")[1]);
        jo.put(OutputField.ACTION_SCALABLE, this.scalablePerfragments.getOrDefault(vmName, false));
        jo.put(OutputField.ACTION_SSH_KEY, "");
        ja.add(jo);
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
     * @param fragmentName name of the fragment *or* the node/host to look for
     * @param nodeName name of the computed destination cloud
     * @return the selected VM type, `null` if not found.
     */
    private String getSelectedCloudVMType(Map<String, Map<String, Map<String, Map<String, String>>>> selectedCloudVMTypes, String fragmentName, String nodeName) {
        Map<String, Map<String, Map<String, String>>> tmp = selectedCloudVMTypes.get(fragmentName);
        if (tmp != null) {
            Optional<Map<String, String>> tmp2 = Optional.ofNullable(tmp.get(TYPE_EXECUTE).get(hostingNodePerFragment.get(fragmentName)));
            if (tmp2.isPresent()) {
                return tmp2.get().get(nodeName);
            } else {
                return "";
            }
        } else {
            for (Map.Entry<String, Map<String, Map<String, Map<String, String>>>> selectedFragmentTypes : selectedCloudVMTypes.entrySet()) {
                for (Map.Entry<String, Map<String, Map<String, String>>> selectedTypes : selectedFragmentTypes.getValue().entrySet()) {
                    if (selectedTypes.getValue().containsKey(fragmentName)) {
                        // Assume that cloud name and region name are concatenated with a space
                        return selectedTypes.getValue().get(fragmentName).get(nodeName);
                    }
                }
            }
        }
        return null;
    }

    private String replaceHostPropertyValuesToMacros(String dockerCommand) {
        Matcher m = NETWORK_IP_PATTERN.matcher(dockerCommand);
        String result = dockerCommand;
        String wholePropertyDeclaration;
        String hostProperties;
        String processingNodeName;
        int nmbGroup = m.groupCount();
        while (m.find()) {
            for (int i = 0; i + 3 < nmbGroup; i += 3) {
                wholePropertyDeclaration = m.group(1 + i);
                processingNodeName = m.group(2 + i);
                hostProperties = m.group(3 + i);
                result = processReplacement(result, wholePropertyDeclaration, processingNodeName, hostProperties);
            }
        }
        return result;
    }

    private String processReplacement(String result, String wholePropertyDeclaration, String processingNodeName, String hostProperties) {
        Optional<Map.Entry<String, String>> fragmentName = hostingNodePerFragment.entrySet().stream().filter(valkey -> (valkey.getValue().equals(processingNodeName))).findFirst();
        if (fragmentName.isPresent()) {
            Optional<String> vmName = vmsPerFragment.get(fragmentName.get().getKey()).stream().findFirst();
            if (!vmName.isPresent()) {
                throw new IllegalStateException("Unable to find any fragment occurence to work with variable substitution: " + processingNodeName);
            }
            return result.replace(wholePropertyDeclaration, String.format("@variables_%s_%s", hostProperties, vmName.get()));
        } else {
            ArchiveRoot pr = this.parsingResult.getResult();
            Topology topology = pr.getTopology();
            Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
            NodeTemplate nodeTemplate = nodeTemplates.get(processingNodeName);
            String fragmentType = nodeTemplate.getType();
            fragmentName = hostingNodePerFragment.entrySet().stream().filter(valkey -> (valkey.getValue().equals(fragmentType))).findFirst();
            Optional<String> vmName = vmsPerFragment.get(fragmentName.get().getKey()).stream().findFirst();
            //return fragmentName.map(stringStringEntry -> result.replace(wholePropertyDeclaration, String.format("@variables_%s_%s", hostProperties, stringStringEntry.getKey()))).orElse("");
            return result.replace(wholePropertyDeclaration, String.format("@variables_%s_%s", hostProperties, vmName.get()));
        }
    }

    public double getHourlyCostOfDeployment() {
        return dstmapping.getRunningVMs().stream()
                .map(vm -> new AbstractMap.SimpleImmutableEntry<>(vm, dstmapping.getVMLocation(vm)))
                .map(tupple -> namePerNode.get(tupple.getValue()) + " " + fragmentsPerVm.get(namePerVM.get(tupple.getKey())))
                .filter(name -> !name.split(" ")[0].equals("edge"))
                .map(name -> vmTemplatesDetails.parallelStream().filter(vmTpl -> {
                    String[] splitted = name.split(" ");
                    String instanceType = this.selectedCloudVMTypes.get(splitted[2]).get(TYPE_EXECUTE).get(hostingNodePerFragment.get(splitted[2])).get(String.format("%s %s", splitted[0], splitted[1]));
                    return vmTpl.cloud.equalsIgnoreCase(splitted[0]) && vmTpl.region.equalsIgnoreCase(splitted[1]) && vmTpl.instanceName.equalsIgnoreCase(instanceType);
                }).findAny())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .mapToDouble(VMTemplateDetails::getPrice)
                .sum();
    }

    public double getTimePeriod() {
        return Double.parseDouble(this.metadata.get("TimePeriod"));
    }

    public double getCostThreshold() {
        return Double.parseDouble(this.metadata.get("CostThreshold"));
    }

    public String generateInstanceLevelToscaTemplate(List<Object> cloudList) throws IllegalAccessException {
        if (cloudList != null) {
            // Configuration
            GeneratorSpace gs = new GeneratorSpace();
            gs.configureMetadata(metadata);
            gs.configureEdgeResourceTemplateDetails(edgeResourceDetails);
            gs.configureVmTemplateDetailList(vmTemplatesDetails);
            gs.configureRcdPerRegion(regionCapabilityDescriptorPerCloud);
            gs.configurationCloudList(cloudList);
            // append
            List<String> toBeAppended = dstmapping.getRunningVMs().stream()
                    .map(vm -> new AbstractMap.SimpleImmutableEntry<>(vm, dstmapping.getVMLocation(vm)))
                    .map(tupple -> namePerNode.get(tupple.getValue()) + " " + namePerVM.get(tupple.getKey())).collect(Collectors.toList());
            String[] splittedRecord;
            String fragmentId;
            boolean isALb;
            String instanceType;
            for (String record : toBeAppended) {
                splittedRecord = record.split(" ");
                //Structure of splitted record: <cloud> <region> <instanceName>
                if (splittedRecord.length != 3) {
                    continue;
                }
                isALb = balancingNodes.containsKey(fragmentsPerVm.get(splittedRecord[2]));
                fragmentId = idPerFragment.get(fragmentsPerVm.get(splittedRecord[2]));
                if (splittedRecord[0].equals("edge")) {
                    gs.appendEdgeDeployedInstance(splittedRecord[2], fragmentId, splittedRecord[1], isALb);
                } else {
                    instanceType = getSelectedCloudVMType(selectedCloudVMTypes, fragmentsPerVm.get(splittedRecord[2]), splittedRecord[0] + " " + splittedRecord[1]);
                    gs.appendCloudDeployedInstance(splittedRecord[2], fragmentId, splittedRecord[0], splittedRecord[1], instanceType, isALb);
                }
            }
            return gs.generate();
        } else {
            throw new IllegalAccessException("No ADIAM environment detected. I won't generate an template of instance level TOSCA file");
        }
    }

    private static class OutputField {
        public static final String ACTION = "action";
        public static final String ACTION_BOOT = "boot";
        public static final String ACTION_MIGRATE = "migrate";
        public static final String ACTION_DELETE = "delete";
        public static final String ACTION_START = "start";
        public static final String ACTION_END = "end";
        public static final String ACTION_FRAGMENT = "fragment";
        public static final String ACTION_ID = "id";
        public static final String ACTION_REGION = "region";
        public static final String ACTION_CLOUD = "cloud";
        public static final String ACTION_REGIONSRC = "regionsrc";
        public static final String ACTION_CLOUDSRC = "cloudsrc";
        public static final String ACTION_TYPE = "type";
        public static final String ACTION_SSH_KEY = "ssh_key";
        public static final String ACTION_DOCKER = "docker";
        public static final String ACTION_NODE = "node";
        public static final String ACTION_PORTS = "ports";
        public static final String ACTION_PUBLICPORTS = "publicports";
        public static final String ACTION_SCALABLE = "scalable";
        public static final String ACTION_LOADBALANCED_BY = "balanced_by";
    }
}

