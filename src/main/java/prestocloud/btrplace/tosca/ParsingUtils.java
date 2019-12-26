/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package prestocloud.btrplace.tosca;

import org.junit.Assert;
import org.prestocloud.tosca.model.definitions.*;
import org.prestocloud.tosca.model.definitions.constraints.EqualConstraint;
import org.prestocloud.tosca.model.definitions.constraints.GreaterOrEqualConstraint;
import org.prestocloud.tosca.model.definitions.constraints.InRangeConstraint;
import org.prestocloud.tosca.model.definitions.constraints.ValidValuesConstraint;
import org.prestocloud.tosca.model.templates.Capability;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.prestocloud.tosca.model.templates.PolicyTemplate;
import org.prestocloud.tosca.model.types.NodeType;
import prestocloud.btrplace.tosca.model.*;
import prestocloud.model.VmTypeCostRegistration;
import prestocloud.model.common.Tag;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingException;
import prestocloud.tosca.parser.ParsingResult;
import prestocloud.tosca.parser.ToscaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ActiveEon Team
 * @since 25/09/18
 */
public class ParsingUtils {

    /**
     * Extract templates information.
     *
     * Return structure:
     *  node template name -> capability name -> property -> value
     *
     * @param parsingResult template parsing results
     * @param regions list of regions to take into consideration
     * @return templates information.
     * @throws IOException
     * @throws ParsingException
     */
    public static Map<String, Map<String, Map<String, String>>> getCloudNodesTemplates(ParsingResult<ArchiveRoot> parsingResult, List<String> regions) throws IOException, ParsingException {

        // First make sure there is no parsing error
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        Map<String, Map<String, Map<String, String>>> vmTypes = new HashMap<>();

        // Look for node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplate : nodeTemplates.entrySet()) {

            // Discard VM types in the wrong region
            if (regions != null && !regions.contains((((ComplexPropertyValue)nodeTemplate.getValue().getCapabilities().get("resource").getProperties().get("cloud")).getValue().get("cloud_region").toString()))) {
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
                                if (cloudProperties.getKey().equalsIgnoreCase("gps_coordinates")) {
                                    resourceCapabilities.put("gps_coordinates", cloudProperties.getValue().toString());
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

    /**
     * Extract templates information.
     * <p>
     * Return structure:
     * node template name -> capability name -> property -> value
     *
     * @param parsingResult template parsing results
     * @return templates information.
     * @throws IOException
     * @throws ParsingException
     */
    public static Map<String, Map<String, Map<String, String>>> getEdgeNodesTemplates(ParsingResult<ArchiveRoot> parsingResult) throws IOException, ParsingException {

        // First make sure there is no parsing error
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        Map<String, Map<String, Map<String, String>>> edgeNodeTemplate = new HashMap<>();

        // Look for node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplate : nodeTemplates.entrySet()) {

            // Look for capabilities
            Map<String, Map<String, String>> typesCapabilities = new HashMap<>();
            for (Map.Entry<String, Capability> capabilities : nodeTemplate.getValue().getCapabilities().entrySet()) {
                // Get 'host' capability properties
                if (capabilities.getKey().equalsIgnoreCase("host")) {
                    Map<String, String> hostCapabilities = new HashMap<>();
                    //System.out.println("Host properties: ");
                    for (Map.Entry<String, AbstractPropertyValue> properties : capabilities.getValue().getProperties().entrySet()) {
                        if (properties.getKey().equalsIgnoreCase("num_cpus")) {
                            String num_cpus = ((ScalarPropertyValue) properties.getValue()).getValue();
                            hostCapabilities.put("num_cpus", num_cpus);
                            //System.out.println("- " + properties.getKey() + " = " + num_cpus);
                        }
                        if (properties.getKey().equalsIgnoreCase("mem_size")) {
                            String mem_size = ((ScalarPropertyValue) properties.getValue()).getValue();
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
                            String price = ((ScalarPropertyValue) properties.getValue()).getValue();
                            hostCapabilities.put("price", price);
                            //System.out.println("- " + properties.getKey() + " = " + price);
                        }
                    }
                    typesCapabilities.put(capabilities.getKey(), hostCapabilities);
                }
                // Get sensors capability
                if (capabilities.getKey().equalsIgnoreCase("sensors")) {
                    Map<String, String> sensorCapabilities = new HashMap<>();
                    String camera = null, temperature = null, microphone = null;
                    for (Map.Entry<String, AbstractPropertyValue> properties : capabilities.getValue().getProperties().entrySet()) {
                        if (properties.getKey().equalsIgnoreCase("temperature")) {
                            temperature = ((ScalarPropertyValue) properties.getValue()).getValue();
                            sensorCapabilities.put("temperature", temperature);
                        }
                        if (properties.getKey().equalsIgnoreCase("camera")) {
                            camera = ((ScalarPropertyValue) properties.getValue()).getValue();
                            sensorCapabilities.put("camera", camera);
                        }
                        if (properties.getKey().equalsIgnoreCase("microphone")) {
                            microphone = ((ScalarPropertyValue) properties.getValue()).getValue();
                            sensorCapabilities.put("microphone", microphone);
                        }
                    }
                    typesCapabilities.put(capabilities.getKey(), sensorCapabilities);
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
                        // Edge based resource detected
                        if (type != null && type.equalsIgnoreCase("edge") && properties.getKey().equalsIgnoreCase("edge")) {
                            //System.out.println("- " + properties.getKey() + ":");
                            ComplexPropertyValue edgeResource = (ComplexPropertyValue) properties.getValue();
                            for (Map.Entry<String, Object> edgeProperties : edgeResource.getValue().entrySet()) {
                                if (edgeProperties.getKey().equalsIgnoreCase("edge_type")) {
                                    resourceCapabilities.put("edge_type", edgeProperties.getValue().toString());
                                    //System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                }
                                if (edgeProperties.getKey().equalsIgnoreCase("edge_location")) {
                                    resourceCapabilities.put("edge_location", edgeProperties.getValue().toString());
                                    //System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                }
                                if (edgeProperties.getKey().equalsIgnoreCase("gps_coordinates")) {
                                    resourceCapabilities.put("gps_coordinates", edgeProperties.getValue().toString());
                                }
                                if (edgeProperties.getKey().equalsIgnoreCase("edge_credentials")) {
                                    HashMap<String, Object> edge_credentials = (HashMap<String, Object>) edgeProperties.getValue();
                                    for (Map.Entry<String, Object> edgeCredentialsProperties : edge_credentials.entrySet()) {
                                        if (edgeCredentialsProperties.getKey().equalsIgnoreCase("username")) {
                                            //System.out.println("  - " + cloudNetworkProperties.getKey() + " = " + cloudNetworkProperties.getValue());
                                            resourceCapabilities.put("username", edgeCredentialsProperties.getValue().toString());
                                        }
                                        if (edgeCredentialsProperties.getKey().equalsIgnoreCase("password")) {
                                            //System.out.println("  - " + cloudNetworkProperties.getKey() + " = " + cloudNetworkProperties.getValue());
                                            resourceCapabilities.put("password", edgeCredentialsProperties.getValue().toString());
                                        }
                                        if (edgeCredentialsProperties.getKey().equalsIgnoreCase("privatekey")) {
                                            resourceCapabilities.put("privatekey", edgeCredentialsProperties.getValue().toString());
                                            //System.out.println("  - " + cloudNetworkProperties.getKey() + " = " + cloudNetworkProperties.getValue());
                                        }
                                    }
                                    //resourceCapabilities.put("edge_credentials", edgeProperties.getValue().toString());
                                    //System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                }
                                // Get networking information
                                if (edgeProperties.getKey().equalsIgnoreCase("edge_network")) {
                                    //System.out.println(" - " + cloudProperties.getKey() + ":");
                                    HashMap<String, Object> edge_network = (HashMap<String, Object>) edgeProperties.getValue();
                                    for (Map.Entry<String, Object> edgeNetworkProperties : edge_network.entrySet()) {
                                        if (edgeNetworkProperties.getKey().equalsIgnoreCase("network_id")) {
                                            //System.out.println("  - " + cloudNetworkProperties.getKey() + " = " + cloudNetworkProperties.getValue());
                                        }
                                        if (edgeNetworkProperties.getKey().equalsIgnoreCase("network_name")) {
                                            //System.out.println("  - " + cloudNetworkProperties.getKey() + " = " + cloudNetworkProperties.getValue());
                                        }
                                        if (edgeNetworkProperties.getKey().equalsIgnoreCase("addresses")) {
                                            //System.out.println("  - " + cloudNetworkProperties.getKey());
                                            for (String address : (List<String>) edgeNetworkProperties.getValue()) {
                                                //System.out.println("    - " + address);
                                            }
                                        }
                                    }
                                }
                            }
                            typesCapabilities.put("edge", resourceCapabilities);
                        }
                    }
                }
            }
            edgeNodeTemplate.put(String.format ("%s %s", nodeTemplate.getValue().getName(), ((ScalarPropertyValue) nodeTemplate.getValue().getProperties().get("id")).getValue()),typesCapabilities);
        }
        return edgeNodeTemplate;
    }

    public static List<PlacementConstraint> getConstraints(ParsingResult<ArchiveRoot> parsingResult) {
        List<PlacementConstraint> placementConstraints = new ArrayList<>();

        // Look for placement constraints
        Map<String, PolicyTemplate> policyTemplates = parsingResult.getResult().getTopology().getPolicies();
        if (policyTemplates != null) {
            for (Map.Entry<String, PolicyTemplate> policyTemplate : policyTemplates.entrySet()) {
                PlacementConstraint placementConstraint = new PlacementConstraint(policyTemplate.getKey());
                placementConstraint.setType(policyTemplate.getValue().getType());
                placementConstraint.setTargets(policyTemplate.getValue().getTargets());
                // TODO: retrieve properties for all custom constraints that requires it
                if (policyTemplate.getValue().getType().equalsIgnoreCase("prestocloud.placement.Ban")) {
                    List<Object> excludedDevices = ((ListPropertyValue) policyTemplate.getValue().getProperties().get("excluded_devices")).getValue();
                    for (Object excludedDevice : excludedDevices) {
                        placementConstraint.addDevice((String) excludedDevice);
                    }
                }
//                if (policyTemplate.getValue().getType().equalsIgnoreCase("prestocloud.placement.Precedence")) {
//                    placementConstraint.addDevice(policyTemplate.getValue().getProperties().get("preceding").toString());
//                    System.out.println(policyTemplate.getValue().getTargets());
//                }
                placementConstraints.add(placementConstraint);
            }
        }
        return placementConstraints;
    }

    public static List<Relationship> getRelationships(ParsingResult<ArchiveRoot> parsingResult) {

        List<Relationship> relationships = new ArrayList<>();

        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        if (nodeTemplates == null) {
            nodeTemplates = new HashMap<>();
        }
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment.jppf")) {
                relationships.addAll(getJPPFRelationships(parsingResult));
            }
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment.faas")) {
                relationships.addAll(getFaaSRelationships(parsingResult));
            }
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment.loadBalanced")) {
                relationships.addAll(getLBFragmentRelationships(parsingResult));
            }
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment")) {
                relationships.addAll(getFragmentRelationships(parsingResult));
            }
        }
        return relationships;
    }

    private static List<RelationshipJPPF> getJPPFRelationships(ParsingResult<ArchiveRoot> parsingResult) {

        List<RelationshipJPPF> relationships = new ArrayList<>();

        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment.jppf")) {
                // Fragment detected, start to build a relationship
                RelationshipJPPF relationship = new RelationshipJPPF(nodeTemplateFragment.getKey(),
                        nodeTemplateFragment.getValue().getRelationships().get("execute").getTarget(),
                        nodeTemplateFragment.getValue().getRelationships().get("master").getTarget());
                relationship.setHostingNode(getConstrainedNode("execute", nodeTemplateFragment, nodeTemplates, parsingResult));
                relationship.setHostingMaster(getConstrainedNode("master", nodeTemplateFragment, nodeTemplates, parsingResult));
                relationships.add(relationship);
            }
        }
        return relationships;
    }

    private static List<RelationshipFaaS> getFaaSRelationships(ParsingResult<ArchiveRoot> parsingResult) {

        List<RelationshipFaaS> relationships = new ArrayList<>();

        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Detect only fragment at highest level
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment.faas")) {
                // Build the relationship
                RelationshipFaaS relationship = new RelationshipFaaS(nodeTemplateFragment.getKey(),
                        nodeTemplateFragment.getValue().getRelationships().get("execute").getTarget(),
                        nodeTemplateFragment.getValue().getRelationships().get("proxy").getTarget());
                relationship.setHostingNode(getConstrainedNode("execute", nodeTemplateFragment, nodeTemplates, parsingResult));
                relationship.setHostingProxy(getConstrainedNode("proxy", nodeTemplateFragment, nodeTemplates, parsingResult));
                relationships.add(relationship);
            }
        }
        return relationships;
    }

    private static List<RelationshipFragmentLB> getLBFragmentRelationships(ParsingResult<ArchiveRoot> parsingResult) {

        List<RelationshipFragmentLB> relationships = new ArrayList<>();

        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment.loadBalanced")) {
                // Fragment detected, start to build a relationship
                RelationshipFragmentLB relationship = new RelationshipFragmentLB(nodeTemplateFragment.getKey(),
                        nodeTemplateFragment.getValue().getRelationships().get("execute").getTarget(),
                        nodeTemplateFragment.getValue().getRelationships().get("balanced_by").getTarget());
                relationship.setHostingNode(getConstrainedNode("execute", nodeTemplateFragment, nodeTemplates, parsingResult));
                relationship.setHostingLB(getConstrainedNode("balanced_by", nodeTemplateFragment, nodeTemplates, parsingResult));
                relationships.add(relationship);
            }
        }
        return relationships;
    }

    private static List<RelationshipFragment> getFragmentRelationships(ParsingResult<ArchiveRoot> parsingResult) {

        List<RelationshipFragment> relationships = new ArrayList<>();

        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment")) {
                // Fragment detected, start to build a relationship
                RelationshipFragment relationship = new RelationshipFragment(nodeTemplateFragment.getKey(),
                        nodeTemplateFragment.getValue().getRelationships().get("execute").getTarget());
                relationship.setHostingNode(getConstrainedNode("execute", nodeTemplateFragment, nodeTemplates, parsingResult));
                relationships.add(relationship);
            }
        }
        return relationships;
    }

    private static ConstrainedNode getConstrainedNode(String requirementName, Map.Entry<String, NodeTemplate> nodeTemplateFragment, Map<String, NodeTemplate> nodeTemplates, ParsingResult<ArchiveRoot> parsingResult) {

        ConstrainedNode constrainedNode = null;

        // // Look for the corresponding deployment node template declaration
        for (Map.Entry<String, NodeTemplate> nodeTemplateFaaS : nodeTemplates.entrySet()) {
            // Look for the constrained node
            if (nodeTemplateFaaS.getKey().equalsIgnoreCase(nodeTemplateFragment.getValue().getRelationships().get(requirementName).getTarget())) {

                // Look for the corresponding node type
                for (Map.Entry<String, NodeType> nodeTypeFaaS : parsingResult.getResult().getNodeTypes().entrySet()) {
                    if (nodeTypeFaaS.getKey().equalsIgnoreCase(nodeTemplateFaaS.getValue().getType())) {
                        // Start to build a constrained node
                        constrainedNode = buildConstrainedNode(nodeTypeFaaS.getKey(), requirementName, nodeTypeFaaS.getValue().getDerivedFrom(), nodeTypeFaaS.getValue().getRequirements());
                        break;
                    }
                }
            }
        }
        return constrainedNode;
    }

    private static ConstrainedNode buildConstrainedNode(String name, String type, List<String> derivedFrom, List<RequirementDefinition> requirements) {

        ConstrainedNode node = new ConstrainedNode(name, type, derivedFrom);

        // Look for requirements
        for (RequirementDefinition requirement : requirements) {
            // Look for a requirement with nodeFilter's capabilities (hosting)
            if (requirement.getNodeFilter() != null) {
                NodeConstraints nodeConstraints = new NodeConstraints();
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
                            nodeConstraints.addHostingConstraint(properties.getKey(), constraints);
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
                            nodeConstraints.addOSConstraint(properties.getKey(), constraints);
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
                            nodeConstraints.addResourceConstraint(properties.getKey(), constraints);
                        }
                    }
                    if (capability.getKey().equalsIgnoreCase("sensors")) {
                        for (Map.Entry<String, List<PropertyConstraint>> properties : capability.getValue().getProperties().entrySet()) {
                            List<String> constraints = new ArrayList<>();
                            for (PropertyConstraint propertyConstraint : properties.getValue()) {
                                if (propertyConstraint instanceof EqualConstraint) {
                                    constraints.add(((EqualConstraint) propertyConstraint).getEqual());
                                } else if (propertyConstraint instanceof ValidValuesConstraint) {
                                    constraints.addAll(((ValidValuesConstraint) propertyConstraint).getValidValues());
                                } else {
                                    // Constraint not yet managed
                                    System.out.println("Sensors constraint not managed: " + propertyConstraint.toString());
                                }
                            }
                            nodeConstraints.addSensorsConstraint(properties.getKey(), constraints);
                        }
                    }
                    // Find the sensors properties
                    /*if (capability.getKey().equalsIgnoreCase("sensors")) {
                        // TODO: get sensors requirements
                    }*/
                }
                node.addConstraints(nodeConstraints);
            }
        }

        return node;
    }

    public static Map<String, String> getMetadata(ParsingResult<ArchiveRoot> parsingResult) {
        Map<String, String> metadata = new HashMap<>();
        for (Tag tag : parsingResult.getResult().getArchive().getTags()) {
            metadata.put(tag.getName(), tag.getValue());
        }
        return metadata;
    }

    public static Set<String> getListOfCloudsFromMetadata(Map<String, String> metadata) {
        Set<String> clouds = new HashSet<>();
        Set<String> oppositeClouds = new HashSet<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (entry.getKey().contains("ProviderName")) {
                String cloud_id = entry.getKey().split("_")[1];
                for (Map.Entry<String, String> entryCloud : metadata.entrySet()) {
                    if (entryCloud.getKey().contains("ProviderRequired" + "_" + cloud_id)) {
                        if (entryCloud.getValue().equalsIgnoreCase("true")) {
                            clouds.add(entry.getValue().toLowerCase());
                        } else {
                            oppositeClouds.add(entry.getValue().toLowerCase());
                        }
                        break;
                    }
                    if (entryCloud.getKey().contains("ProviderExcluded" + "_" + cloud_id)) {
                        if (entryCloud.getValue().equalsIgnoreCase("false")) {
                            clouds.add(entry.getValue().toLowerCase());
                        } else {
                            oppositeClouds.add(entry.getValue().toLowerCase());
                        }
                        break;
                    }
                }
            }
        }
        return (clouds.size() > 0) ? clouds : oppositeClouds;
    }

    /**
     * Return the two informations separated with a space: "region_name vm_type"
     */
    public static List<String> findBestSuitableRegionAndVMType(ToscaParser parser, String repositoryPath, String cloud, List<String> regions, Map<String, List<String>> hostingConstraints) throws Exception {

        Map<String, VmTypeCostRegistration> selectedTypes = new HashMap<>();
        Map<String, Map<String, Map<String, String>>> VMTypes;
        Path expectedPath = Paths.get(repositoryPath, String.format("%s-vm-templates.yml", cloud));

        if (!Files.exists(expectedPath)) {
//            System.out.println("Cloud of type " + cloud + " not found.");
            throw new IllegalArgumentException("Cloud of type " + cloud + " not found.");
 //           return null;
        }
        VMTypes = getCloudNodesTemplates(parser.parseFile(expectedPath), regions);

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
                            // Memory in GB only
                            if (required_mem_size.split(",")[0].contains("MB")) {
                                required = Math.round(required / 1024);
                            }
                        }
                        else if (required_mem_size.contains("GreaterOrEqual")) {
                            required = Double.valueOf(required_mem_size.split(" ")[1]);
                            // Memory in GB only
                            if (required_mem_size.contains("MB")) {
                                required = Math.round(required / 1024);
                            }
                        }
                        else {
                            required = Double.valueOf(required_mem_size.split(" ")[0]);
                            // Memory in GB only
                            if (required_mem_size.contains("MB")) {
                                required = Math.round(required / 1024);
                            }
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
                            // Disk in GB only
                            if (required_disk_size.split(",")[0].contains("MB")) {
                                required = Math.round(required / 1024);
                            }
                        }
                        else if (required_disk_size.contains("GreaterOrEqual")) {
                            required = Double.valueOf(required_disk_size.split(" ")[1]);
                            // Disk in GB only
                            if (required_disk_size.contains("MB")) {
                                required = Math.round(required / 1024);
                            }
                        }
                        else {
                            required = Double.valueOf(required_disk_size);
                            // Disk in GB only
                            if (required_disk_size.contains("MB")) {
                                required = Math.round(required / 1024);
                            }
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
                            price = true;
                        }
                    }
                }
                else {
                    price = true;
                }
                if (cpu && mem && disk && price) {
                    String region = hostingConstraint.getValue().get("cloud").get("cloud_region");
                    VmTypeCostRegistration vtcr = new VmTypeCostRegistration(hostingConstraint.getValue().get("cloud").get("name"), Double.valueOf(hostingConstraint.getValue().get("host").get("price")));
                    if (selectedTypes.containsKey(region)) {
                        if (vtcr.compareTo(selectedTypes.get(region)) < 0) {
                            selectedTypes.put(region, vtcr);
                        }
                    } else {
                        selectedTypes.put(region, vtcr);
                    }
                }
            }
        }

        if (selectedTypes.isEmpty()) {
            throw new Exception("No suitable type found for hosting constraints: " + hostingConstraints);
        }
        else {
            return selectedTypes.entrySet().parallelStream().map(val -> (val.getKey() + " " + val.getValue().toString())).collect(Collectors.toList());
        }
    }

    public static Map<String, String> getfragmentIds(ParsingResult<ArchiveRoot> parsingResult) {
        Map<String, String> ids = new HashMap<>();

        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        if (nodeTemplates == null) {
            nodeTemplates = new HashMap<>();
        }
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().startsWith("prestocloud.nodes.fragment")) {
                ids.put(nodeTemplateFragment.getValue().getName(), ((ScalarPropertyValue) nodeTemplateFragment.getValue().getProperties().get("id")).getValue());
            }
        }
        return ids;
    }

    public static List<Docker> getDockersCloud(ParsingResult<ArchiveRoot> parsingResult) {
        return getDockers(parsingResult,"docker_cloud");
    }


    public static List<Docker> getDockersEdge(ParsingResult<ArchiveRoot> parsingResult) {
        return getDockers(parsingResult,"docker_edge");
    }

    private static List<Docker> getDockers(ParsingResult<ArchiveRoot> parsingResult, String resourceType) {
        List<Docker> dockers = new ArrayList<>();
        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        if (nodeTemplates == null) {
            nodeTemplates = new HashMap<>();
        }
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().startsWith("prestocloud.nodes.fragment")) {
                Docker docker = new Docker(nodeTemplateFragment.getValue().getName());
                // Look for one of the 'docker' property
                ComplexPropertyValue dockerProperty = (ComplexPropertyValue) nodeTemplateFragment.getValue().getProperties().get(resourceType);
                if (dockerProperty != null) {
                    docker.setResourceType(resourceType);
                    for (String dockerKey : dockerProperty.getValue().keySet()) {
                        if (dockerKey.equalsIgnoreCase("image")) {
                            docker.setImage(dockerProperty.getValue().get(dockerKey).toString());
                        }
                        if (dockerKey.equalsIgnoreCase("registry")) {
                            docker.setRegistry(dockerProperty.getValue().get(dockerKey).toString());
                        }
                        if (dockerKey.equalsIgnoreCase("cmd")) {
                            docker.setCmd(dockerProperty.getValue().get(dockerKey).toString());
                        }
                        if (dockerKey.equalsIgnoreCase("variables")) {
                            for (String variableKey : ((HashMap<String, String>)dockerProperty.getValue().get(dockerKey)).keySet()) {
                                docker.addVariable(variableKey, ((HashMap<String, String>)dockerProperty.getValue().get(dockerKey)).get(variableKey).toString());
                            }
                        }
                        if (dockerKey.equalsIgnoreCase("ports")) {
                            for (HashMap<String, String> port : ((ArrayList<HashMap<String, String>>)dockerProperty.getValue().get(dockerKey))) {
                                docker.addNetworkMapping(port.get("target"),port.get("published"),port.get("protocol"));
                                /*for (String portKey : port.keySet()) {
                                    if (portKey.equalsIgnoreCase("protocol")) {
                                        docker.setPortProtocol(port.get(portKey));
                                    }
                                    if (portKey.equalsIgnoreCase("published")) {
                                        docker.setPortPublished(port.get(portKey));
                                    }
                                    if (portKey.equalsIgnoreCase("target")) {
                                        docker.setPortTarget(port.get(portKey));
                                    }
                                }*/
                            }
                        }
                    }
                    dockers.add(docker);
                }
            }
        }
        return dockers;
    }

    public static List<HealthCheck> getHealthChecks(ParsingResult<ArchiveRoot> parsingResult) {

        List<HealthCheck> healthChecks = new ArrayList<>();

        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        if (nodeTemplates == null) {
            nodeTemplates = new HashMap<>();
        }
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().startsWith("prestocloud.nodes.fragment")) {
                HealthCheck healthCheck = new HealthCheck(nodeTemplateFragment.getValue().getName());
                // Look for the 'health_check' property
                ComplexPropertyValue property = (ComplexPropertyValue) nodeTemplateFragment.getValue().getProperties().get("health_check");
                if (property != null) {
                    for (String key : property.getValue().keySet()) {
                        if (key.equalsIgnoreCase("interval")) {
                            healthCheck.setInterval(Integer.valueOf(property.getValue().get(key).toString()));
                        }
                        if (key.equalsIgnoreCase("url")) {
                            healthCheck.setUrl(property.getValue().get(key).toString());
                        }
                        if (key.equalsIgnoreCase("cmd")) {
                            healthCheck.setCmd(property.getValue().get(key).toString());
                        }
                    }
                    healthChecks.add(healthCheck);
                }
            }
        }
        return healthChecks;
    }

    public static Map<String,SshKey>  getSshKeys(ParsingResult<ArchiveRoot> parsingResult) {
        Map<String,SshKey> sshKeys = new HashMap<>();
        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        if (nodeTemplates == null) {
            nodeTemplates = new HashMap<>();
        }
        String framgentName;
        String nodeName;
        String ssh;
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().startsWith("prestocloud.nodes.fragment")) {
                 framgentName = nodeTemplateFragment.getValue().getName();
                 nodeName = nodeTemplateFragment.getValue().getRelationships().get("execute").getTarget();
                ScalarPropertyValue property = (ScalarPropertyValue) nodeTemplates.get(nodeName).getProperties().get("ssh_pub_key");
                 if (property != null) {
                     ssh = property.getValue().toString();
                 } else {
                     ssh = null;
                 }
                sshKeys.put(framgentName, new SshKey(framgentName, System.getenv().getOrDefault("$credentials_prestocloud_key_" + framgentName, ssh)));
            }
        }
        return sshKeys;
    }

    public static Map<String, Boolean> getScalableFragments(ParsingResult<ArchiveRoot> parsingResult) {
        Map<String, Boolean> result = new HashMap<>();
        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        if (nodeTemplates == null) {
            nodeTemplates = new HashMap<>();
        }
        String fragmentName;
        Boolean isFragmentScalable;
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().startsWith("prestocloud.nodes.fragment")) {
                fragmentName = nodeTemplateFragment.getValue().getName();
                ScalarPropertyValue property = (ScalarPropertyValue) nodeTemplateFragment.getValue().getProperties().get("scalable");
                if (property != null) {
                    isFragmentScalable = property.getValue().toString().equalsIgnoreCase("true");
                } else {
                    isFragmentScalable = false;
                }
                result.put(fragmentName, isFragmentScalable);
            }
        }
        return result;
    }

    public static Map<String, Integer> getOccurencePerFragments(ParsingResult<ArchiveRoot> parsingResult) {
        Map<String, Integer> result = new HashMap<>();
        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        if (nodeTemplates == null) {
            nodeTemplates = new HashMap<>();
        }
        String fragmentName;
        Integer occurence;
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().startsWith("prestocloud.nodes.fragment")) {
                fragmentName = nodeTemplateFragment.getValue().getName();
                ScalarPropertyValue property = (ScalarPropertyValue) nodeTemplateFragment.getValue().getProperties().get("occurrences");
                if (property != null) {
                    occurence = Integer.parseInt(property.getValue());
                } else {
                    occurence = 1;
                }
                result.put(fragmentName, occurence);
            }
        }
        return result;
    }
    public static List<OptimizationVariables> getOptimizationVariables(ParsingResult<ArchiveRoot> parsingResult) {

        List<OptimizationVariables> allOptimizationVariables = new ArrayList<>();

        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        if (nodeTemplates == null) {
            nodeTemplates = new HashMap<>();
        }
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().startsWith("prestocloud.nodes.fragment")) {
                OptimizationVariables optimizationVariables = new OptimizationVariables(nodeTemplateFragment.getValue().getName());
                // Look for the 'optimization_variables' property
                ComplexPropertyValue property = (ComplexPropertyValue) nodeTemplateFragment.getValue().getProperties().get("optimization_variables");
                if (property != null) {
                    for (String key : property.getValue().keySet()) {
                        if (key.equalsIgnoreCase("cost")) {
                            optimizationVariables.setCost(Integer.valueOf(property.getValue().get(key).toString()));
                        }
                        if (key.equalsIgnoreCase("distance")) {
                            optimizationVariables.setDistance(Integer.valueOf(property.getValue().get(key).toString()));
                        }
                        if (key.equalsIgnoreCase("friendliness")) {
                            for (String variableKey : ((HashMap<String, String>)property.getValue().get(key)).keySet()) {
                                optimizationVariables.addFriendliness(variableKey, Integer.valueOf(((HashMap<String, String>)property.getValue().get(key)).get(variableKey).toString()));
                            }
                        }
                    }
                    allOptimizationVariables.add(optimizationVariables);
                }
            }
        }
        return allOptimizationVariables;
    }

    public static GetVMTemplatesDetailsResult getVMTemplatesDetails(ToscaParser parser, String repositoryPath) throws IOException, ParsingException {

        List<VMTemplateDetails> vmTemplatesDetails = new ArrayList<>();
        Map<String,Map<String,Set<RegionCapacityDescriptor>>> regionsPerCloudPerCloudfile = new HashMap<>();
        Map<String,Map<String,List<RegionCapacityDescriptor>>> definitiveRegionPerCloudPerCloudFile = new HashMap<>();
        Pattern vmFilenameMatcher = Pattern.compile("^([\\S]*)-vm-templates.yml$");

        // Let's parse all available cloud resource - I select only specification file matching one kind of spec.
        Path[] listOfCloudsResourceFile = Files.list(Paths.get(repositoryPath)).filter(Files::isRegularFile).filter(path -> (vmFilenameMatcher.matcher(path.toString()).find())).toArray(Path[]::new);

        Map<String,Set<RegionCapacityDescriptor>>  regionsPerCloud;
        String cloudFileName;
        //For each available loud file specificiation
        for(Path cloudFile: listOfCloudsResourceFile) {
            regionsPerCloud = new HashMap<>();
            Map<String, Map<String, Map<String, String>>> vMTypes = getCloudNodesTemplates(parser.parseFile(cloudFile), null);
            // For each available VM type ...
            for (Map.Entry<String, Map<String, Map<String, String>>> hostingConstraint : vMTypes.entrySet()) {
                Double price = null;
                String name = null, type = null, region = null, coordinates = null, cpucapacity = null, memorycapacity = null, diskcapacity = null, instanceName = null;
                for (Map.Entry<String, Map<String, String>> cloudProperties : hostingConstraint.getValue().entrySet()) {
                    if (cloudProperties.getKey().equalsIgnoreCase("host")) {
                        price = Double.valueOf(cloudProperties.getValue().get("price"));
                        cpucapacity = cloudProperties.getValue().get("num_cpus");
                        memorycapacity = cloudProperties.getValue().get("mem_size");
                        diskcapacity = cloudProperties.getValue().get("disk_size");
                    }
                    if (cloudProperties.getKey().equalsIgnoreCase("cloud")) {
                        name = cloudProperties.getValue().get("cloud_name");
                        type = cloudProperties.getValue().get("cloud_type");
                        region = cloudProperties.getValue().get("cloud_region");
                        coordinates = cloudProperties.getValue().get("gps_coordinates");
                        instanceName = cloudProperties.getValue().get("name");
                        if (!regionsPerCloud.keySet().contains(type)) {
                            regionsPerCloud.put(type,new TreeSet<RegionCapacityDescriptor>());
                        }
                        regionsPerCloud.get(type).add(new RegionCapacityDescriptor(region,cpucapacity,memorycapacity,diskcapacity));
                    }
                }
                VMTemplateDetails VMTemplateDetails = new VMTemplateDetails(hostingConstraint.getKey(), name, type, region, coordinates, price, instanceName, cpucapacity, null, memorycapacity, diskcapacity);
                vmTemplatesDetails.add(VMTemplateDetails);
                cloudFileName = cloudFile.getFileName().toString();
                regionsPerCloudPerCloudfile.put(cloudFileName.substring(0,cloudFileName.length() -17),regionsPerCloud);
            }
        }

        GetVMTemplatesDetailsResult result = new GetVMTemplatesDetailsResult();
        result.vmTemplatesDetails = vmTemplatesDetails;
        regionsPerCloudPerCloudfile.forEach((cloudfile, stringSetMap) -> {
            Map<String,List<RegionCapacityDescriptor>> tmp2 = new HashMap<>();
            stringSetMap.forEach((cloudType, regionCapacityDescriptors) -> {
                List<RegionCapacityDescriptor> tmp = new ArrayList<>();
                tmp.addAll(regionCapacityDescriptors);
                tmp2.put(cloudType,tmp);
            });
            definitiveRegionPerCloudPerCloudFile.put(cloudfile,tmp2);
        });
        result.regionsPerCloudPerCloudFile = definitiveRegionPerCloudPerCloudFile;
        return result;
    }

    public static List<EdgeResourceTemplateDetails> getEdgeResourceTemplateDetails(ToscaParser parser, String repositoryPath) throws IOException, ParsingException {
        List<EdgeResourceTemplateDetails> edgeResourceTemplateDetails = new ArrayList<>();
        Pattern edgeFilenameMatcher = Pattern.compile("^([\\S]*)-("+System.getenv("PA_USER")+")?edge-templates.yml$");
        Path[] listOfCloudsResourceFile = Files.list(Paths.get(repositoryPath)).filter(Files::isRegularFile).filter(path -> (edgeFilenameMatcher.matcher(path.toString()).find())).toArray(Path[]::new);

        for (Path edgeFile : listOfCloudsResourceFile) {
            Map<String, Map<String, Map<String, String>>> edgeTypes = getEdgeNodesTemplates(parser.parseFile(edgeFile));
            for (Map.Entry<String, Map<String, Map<String, String>>> hostingConstraint : edgeTypes.entrySet()) {
                // For each properties
                EdgeResourceTemplateDetails ertd = new EdgeResourceTemplateDetails();
                ertd.id = hostingConstraint.getKey().split(" ")[1];
                for (Map.Entry<String, Map<String, String>> cloudProperties : hostingConstraint.getValue().entrySet()) {
                    if (cloudProperties.getKey().equalsIgnoreCase("host")) {
                        ertd.price = Optional.ofNullable(Double.valueOf(cloudProperties.getValue().get("price")) + "");
                        ertd.num_cpus = cloudProperties.getValue().get("num_cpus");
                        ertd.mem_size = cloudProperties.getValue().get("mem_size");
                        ertd.disk_size = Optional.ofNullable(cloudProperties.getValue().get("disk_size"));
                        ertd.cpu_frequency = Optional.ofNullable(cloudProperties.getValue().get("cpu_frequency"));
                    }
                    if (cloudProperties.getKey().equalsIgnoreCase("edge")) {
                        ertd.edgeType = cloudProperties.getValue().get("edge_type");
                        ertd.name = Optional.ofNullable(cloudProperties.getValue().get("name"));
                        ertd.location = cloudProperties.getValue().get("edge_location");
                        ertd.gps_coordinate = Optional.ofNullable(cloudProperties.getValue().get("gps_coordinates"));
                        ertd.username = cloudProperties.getValue().get("username");
                        ertd.password = Optional.ofNullable(cloudProperties.getValue().get("password"));
                        ertd.privatekey = Optional.ofNullable(cloudProperties.getValue().get("privatekey"));
                    }
                    if (cloudProperties.getKey().equalsIgnoreCase("sensors")) {
                        ertd.cameraSensor = Optional.ofNullable(cloudProperties.getValue().get("camera"));
                        ertd.microphoneSensor = Optional.ofNullable(cloudProperties.getValue().get("microphone"));
                        ertd.temperatureSensor = Optional.ofNullable(cloudProperties.getValue().get("temperature"));
                    }
                }
                edgeResourceTemplateDetails.add(ertd);
            }
        }
        return edgeResourceTemplateDetails;
    }
}

