package prestocloud.workspace;

import lombok.extern.slf4j.Slf4j;
import org.btrplace.model.*;
import org.btrplace.model.view.ShareableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import prestocloud.TOSCAParserApp;
import prestocloud.btrplace.cost.CostView;
import prestocloud.btrplace.tosca.GetVMTemplatesDetailsResult;
import prestocloud.btrplace.tosca.ParsingUtils;
import prestocloud.btrplace.tosca.model.*;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.*;

import java.util.*;

@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class ParsingSpace {

    public final List<String>  PUBLIC_CLOUDS_DEFINITION = Arrays.asList("amazon azure gce".split(" "));

    private Logger logger = LoggerFactory.getLogger(TOSCAParserApp.class);

    private HashMap<String, List<String>> regionsPerClouds;
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
    private Model mo;
    private Mapping map;
    private CostView cv;

    // Public and Private Cloud identificiation
    Map<String, Node> publicClouds = new HashMap<>();
    Map<String, Node> privateClouds = new HashMap<>();


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
                                        this.regionsPerClouds.get(cloud),
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

    }

    public void populatePublicAndPrivateCloud() {
        for (String cloud : supportedClouds) {
            for (String region : regionsPerClouds.get(cloud)) {
                if (PUBLIC_CLOUDS_DEFINITION.contains(cloud)) {
                    publicClouds.put(cloud + " " + region, mo.newNode());
                } else {
                    privateClouds.put(cloud + " " + region, mo.newNode());
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

        for (Map.Entry<String, Node> cloud : publicClouds.entrySet()) {
            cpu.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
            mem.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
            disk.setCapacity(cloud.getValue(), Integer.MAX_VALUE / 1000);
        }
        // TODO: Proceed similarly for private clouds.
    }
}
