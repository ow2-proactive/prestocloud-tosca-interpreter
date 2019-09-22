package prestocloud.workspace;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.btrplace.model.*;
import org.btrplace.model.constraint.*;
import org.btrplace.model.view.ShareableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import prestocloud.TOSCAParserApp;
import prestocloud.btrplace.cost.CostView;
import prestocloud.btrplace.precedingRunning.PrecedingRunning;
import prestocloud.btrplace.tosca.GetVMTemplatesDetailsResult;
import prestocloud.btrplace.tosca.ParsingUtils;
import prestocloud.btrplace.tosca.model.*;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class ParsingSpace {

    public final List<String>  PUBLIC_CLOUDS_DEFINITION = Arrays.asList("amazon azure gce".split(" "));

    private Logger logger = LoggerFactory.getLogger(TOSCAParserApp.class);

    private HashMap<String, List<RegionCapacityDescriptor>> regionsPerClouds;
    private ParsingResult<ArchiveRoot> parsingResult;
    private ToscaParser parser;
    private String resourcesPath;

    // We describe couples of element we want to d integrate from our parsing.
    private Map<String, String> metadata;
    private List<String> supportedClouds;
    private List<Relationship> relationships;
    private List<PlacementConstraint> placementConstraints;
    private List<Docker> dockers;
    private List<OptimizationVariables> optimizationVariables;
    private List<VMTemplateDetails> vmTemplatesDetails;
    // TODO: deal with health checks
    private List<HealthCheck> healthChecks;
    private Map<String, Map<String, Map<String, Map<String, String>>>> selectedCloudVMTypes = new HashMap<>();

    //btrplace model related attributes
    Map<String, VM> vms = new HashMap<>();
    ShareableResource cpu = new ShareableResource("cpu");
    ShareableResource mem = new ShareableResource("memory");
    ShareableResource disk = new ShareableResource("disk");
    private Model mo;
    private Mapping map;
    private CostView cv;

    // Public and Private Cloud identificiation
    Map<String, Node> publicClouds = new HashMap<>();
    Map<String, Node> privateClouds = new HashMap<>();
    Map<String,RegionCapacityDescriptor> regionCapabilityDescriptorPerCloud = new HashMap<>();

    final List<SatConstraint> cstrs = new ArrayList<>();


    public ParsingSpace(ParsingResult<ArchiveRoot> result, GetVMTemplatesDetailsResult getVMTemplatesDetailsResult, ToscaParser parser, String resourcesPath) {
        this.parsingResult = result;
        this.parser = parser;
        this.resourcesPath  = resourcesPath;
        this.vmTemplatesDetails = getVMTemplatesDetailsResult.vmTemplatesDetails;
        this.regionsPerClouds = getVMTemplatesDetailsResult.regionsPerClouds;
    }

    public boolean retrieveResourceFromParsing() {
        // Retrieving main data from the parsed TOSCA.
        metadata = ParsingUtils.getMetadata(parsingResult);
        supportedClouds = ParsingUtils.getListOfCloudsFromMetadata(metadata);
        logger.info(String.format("%d supported cloud have been found",supportedClouds.size()));
        relationships = ParsingUtils.getRelationships(parsingResult);
        placementConstraints = ParsingUtils.getConstraints(parsingResult);
        dockers = ParsingUtils.getDockers(parsingResult);
        optimizationVariables = ParsingUtils.getOptimizationVariables(parsingResult);
        healthChecks = ParsingUtils.getHealthChecks(parsingResult);
        return true;
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
                            for (String cloud : supportedClouds) {
                                String selectedRegionAndType = ParsingUtils.findBestSuitableRegionAndVMType(
                                        parser,
                                        resourcesPath,
                                        cloud,
                                        this.regionsPerClouds.get(cloud).stream().map(RegionCapacityDescriptor::getRegion).collect(Collectors.toList()),
                                        nodeConstraints.getHostingConstraints());
                                String region = selectedRegionAndType.split(" ")[0];
                                String vmType = selectedRegionAndType.split(" ")[1];
                                selectedTypes.put(cloud.toLowerCase() + " " + region, vmType);
                            }
                            allSelectedTypes.put(constrainedNode.getName(), selectedTypes);
                            allSelectedTypesWithRequirement.put(constrainedNode.getType(), allSelectedTypes);
                        } else {
                            logger.warn("Edge-only hosting resource constraint found: " + constrainedNode.getName());
                        }
                    }
                }
            }
            selectedCloudVMTypes.put(relationship.getFragmentName(), allSelectedTypesWithRequirement);
            logger.info(String.format("%d types were identified for the fragment %s",allSelectedTypes.size(),relationship.getFragmentName()));
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

    public void createVmsResourceInBtrPlace() {
        for (Map.Entry<String, Map<String, Map<String, Map<String, String>>>> selectedFragmentTypes : selectedCloudVMTypes.entrySet()) {
            for (Map.Entry<String, Map<String, Map<String, String>>> selectedTypes : selectedFragmentTypes.getValue().entrySet()) {
                // Add the fragment's execute node first
                if (selectedTypes.getKey().equalsIgnoreCase("execute")) {
                    vms.put(selectedFragmentTypes.getKey(), mo.newVM());
                    logger.info(String.format("Registering fragment %s ...",selectedFragmentTypes.getKey()));
                }
                else {
                    // We can have duplicates (eg. a 'proxy' may be linked to multiple fragments)
                    String nodeName = selectedTypes.getValue().keySet().stream().findFirst().get();
                    if (!vms.containsKey(nodeName)) {
                        vms.put(nodeName, mo.newVM());
                        logger.info(String.format("Registering %s ...",nodeName));
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

    }

    public void populatePublicAndPrivateCloud() {
        String placementString;
        for (String cloud : supportedClouds) {
            for (RegionCapacityDescriptor region : regionsPerClouds.get(cloud)) {
                placementString = cloud + " " + region.getRegion();
                regionCapabilityDescriptorPerCloud.put(placementString,region);
                if (PUBLIC_CLOUDS_DEFINITION.contains(cloud)) {
                    publicClouds.put(placementString, mo.newNode());
                } else {
                    privateClouds.put(cloud + " " + region.getRegion(), mo.newNode());
                }
            }
/*                if (cloud.equalsIgnoreCase("azure")) {
                    for (String region : azureRegions) {
                        publicClouds.put("azure " + region, mo.newNode());
                    }
                }
                if (cloud.equalsIgnoreCase("amazon")) {
                    for (String region : amazonRegions) {
                        publicClouds.put("amazon " + region, mo.newNode());
                    }
               }*/
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

        Set<Map.Entry<String, Node>> tmp = new HashSet<>();
        tmp.addAll(publicClouds.entrySet());
        tmp.addAll(privateClouds.entrySet());
        for (Map.Entry<String, Node> cloud : tmp) {
/*            cpu.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
            mem.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
            disk.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);*/
            cpu.setCapacity(cloud.getValue(), this.regionCapabilityDescriptorPerCloud.get(cloud.getKey()).getCpuCapacity());
            mem.setCapacity(cloud.getValue(), this.regionCapabilityDescriptorPerCloud.get(cloud.getKey()).getMemoryCapacity());
            disk.setCapacity(cloud.getValue(), this.regionCapabilityDescriptorPerCloud.get(cloud.getKey()).getDiskCapacity());
        }
    }

    public void configuringNodeConstraint() {
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
                                    logger.error("Unrecognized hosting constraint: " + hostingConstraint.getKey());
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
        for (Map.Entry<String, Node> public_cloud : publicClouds.entrySet()) {
            map.on(public_cloud.getValue());
        }
        cstrs.addAll(Online.newOnline(mo.getMapping().getAllNodes()));
        logger.info(String.format("%s public clouds are set online",publicClouds.keySet().toString()));

        // Proceed similarly with private cloud.
        for (Map.Entry<String, Node> public_cloud : privateClouds.entrySet()) {
            map.on(public_cloud.getValue());
        }
        cstrs.addAll(Online.newOnline(mo.getMapping().getAllNodes()));
        logger.info(String.format("%s private clouds are set online",privateClouds.keySet().toString()));

        // TODO : with edge resource, we will need to act check the effective reachability of the need. Point to be discussed.

        // TODO: set the state for edge devices
        // All edge nodes are online and running
        /*for (Map.Entry<String, Node> edgeNode : edgeNodes.entrySet()) {
            map.on(edgeNode.getValue());
        }*/
    }

    public void defineFragmentDeployability() {
        for (Map.Entry<String, VM> vm : vms.entrySet()) {
            map.ready(vm.getValue());
        }
        cstrs.addAll(Running.newRunning(mo.getMapping().getAllVMs()));
    }

    public void configurePlacementConstraint() {
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
                    logger.warn("Non consistent 'Spread' constraint detected (probably due to a missing edge device).");
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
                    logger.warn("Non consistent 'Gather' constraint detected (probably due to a missing edge device).");
                }
            }
            if (placementConstraint.getType().contains("Precedence")) {
                Set<VM> constrained_vms = new LinkedHashSet<>();
                // Check if the VM actually exists (this is currently triggered as edge devices are not yet managed)
                logger.info(placementConstraint.getTargets().toString());
                if (vms.entrySet().containsAll(placementConstraint.getTargets())) {
                    // Here is the former implementation of the constraint.
                /*    for (String target : placementConstraint.getTargets()) {
                        logger.info(String.format("Enforcing precedence constraints on the fragment %s", target));
                        constrained_vms.add(vms.get(target));
                    }
                    String vm = placementConstraint.getDevices().stream().findFirst().get();
                    cstrs.add(new PrecedingRunning(vms.get(vm), Sets.newHashSet(constrained_vms)));*/
                    String[] ordonnedVmAllocation = (String[]) placementConstraint.getTargets().toArray();
                    for(int i = 0; i < ordonnedVmAllocation.length -2; i++) {
                        HashSet<VM> tmp = Sets.newHashSet();
                        tmp.add(vms.get(ordonnedVmAllocation[i]));
                       cstrs.add(new PrecedingRunning(vms.get(ordonnedVmAllocation[i+1]),tmp));
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

    // TODO: Add precedence constraints from relationships. 'proxy', 'master', and 'balanced_by' nodes must be started before fragment's host ('execute' node))

}
