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
import prestocloud.btrplace.tosca.model.Constraint;
import prestocloud.btrplace.tosca.model.Docker;
import prestocloud.btrplace.tosca.model.RelationshipFaaS;
import prestocloud.btrplace.tosca.model.RelationshipJPPF;
import prestocloud.model.common.Tag;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingException;
import prestocloud.tosca.parser.ParsingResult;
import prestocloud.tosca.parser.ToscaParser;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ActiveEon Team
 * @since 25/09/18
 */
public class ParsingUtils {

    public static Map<String, Map<String, Map<String, String>>> getCloudNodesTemplates(ParsingResult<ArchiveRoot> parsingResult, String region) throws IOException, ParsingException {

        // First make sure there is no parsing error
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

    public static List<Constraint> getConstraints(ParsingResult<ArchiveRoot> parsingResult) {
        List<Constraint> constraints = new ArrayList<>();

        // Look for placement constraints
        Map<String, PolicyTemplate> policyTemplates = parsingResult.getResult().getTopology().getPolicies();
        if (policyTemplates != null) {
            for (Map.Entry<String, PolicyTemplate> policyTemplate : policyTemplates.entrySet()) {
                Constraint constraint = new Constraint(policyTemplate.getKey());
                constraint.setType(policyTemplate.getValue().getType());
                constraint.setTargets(policyTemplate.getValue().getTargets());
                if (policyTemplate.getValue().getType().equalsIgnoreCase("prestocloud.placement.Ban")) {
                    List<Object> excludedDevices = ((ListPropertyValue) policyTemplate.getValue().getProperties().get("excluded_devices")).getValue();
                    for (Object excludedDevice : excludedDevices) {
                        constraint.addDevice((String) excludedDevice);
                    }
                }
                constraints.add(constraint);
            }
        }
        return constraints;
    }

    // TODO: PARSE MASTER AS WELL!!!
    public static List<RelationshipJPPF> getJPPFRelationships(ParsingResult<ArchiveRoot> parsingResult) {

        List<RelationshipJPPF> relationships = new ArrayList<>();

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
                                RelationshipJPPF relationship = new RelationshipJPPF(nodeTemplateFragment.getKey(), nodeTemplateJPPF.getKey(), nodeTypeJPPF.getKey());
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

    public static List<Docker> getDockerParameters(ParsingResult<ArchiveRoot> parsingResult) {

        List<Docker> dockers = new ArrayList<>();

        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment.faas")) {
                Docker dockerParameters = new Docker(nodeTemplateFragment.getValue().getName());
                // Look for the 'docker' property
                ComplexPropertyValue dockerProperty = (ComplexPropertyValue) nodeTemplateFragment.getValue().getProperties().get("docker");
                if (dockerProperty != null) {
                    for (String dockerKey : dockerProperty.getValue().keySet()) {
                        if (dockerKey.equalsIgnoreCase("image")) {
                            dockerParameters.setImage(dockerProperty.getValue().get(dockerKey).toString());
                        }
                        if (dockerKey.equalsIgnoreCase("registry")) {
                            dockerParameters.setRegistry(dockerProperty.getValue().get(dockerKey).toString());
                        }
                        if (dockerKey.equalsIgnoreCase("cmd")) {
                            dockerParameters.setCmd(dockerProperty.getValue().get(dockerKey).toString());
                        }
                        if (dockerKey.equalsIgnoreCase("variables")) {
                            for (String variableKey : ((HashMap<String, String>)dockerProperty.getValue().get(dockerKey)).keySet()) {
                                dockerParameters.addVariable(variableKey, ((HashMap<String, String>)dockerProperty.getValue().get(dockerKey)).get(variableKey).toString());
                            }
                        }
                        if (dockerKey.equalsIgnoreCase("ports")) {
                            for (HashMap<String, String> port : ((ArrayList<HashMap<String, String>>)dockerProperty.getValue().get(dockerKey))) {
                                for (String portKey : port.keySet()) {
                                    if (portKey.equalsIgnoreCase("protocol")) {
                                        dockerParameters.setPortProtocol(port.get(portKey));
                                    }
                                    if (portKey.equalsIgnoreCase("published")) {
                                        dockerParameters.setPortPublished(port.get(portKey));
                                    }
                                    if (portKey.equalsIgnoreCase("target")) {
                                        dockerParameters.setPortTarget(port.get(portKey));
                                    }
                                }
                            }
                        }
                    }
                    dockers.add(dockerParameters);
                }
            }
        }
        return dockers;
    }

    // TODO: parse load balancer as well!!
    public static List<RelationshipFaaS> getFaaSRelationships(ParsingResult<ArchiveRoot> parsingResult) {

        List<RelationshipFaaS> relationships = new ArrayList<>();

        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment.faas")) {
                // Look for the corresponding FaaS agent
                for (Map.Entry<String, NodeTemplate> nodeTemplateFaaS : nodeTemplates.entrySet()) {
                    if (nodeTemplateFaaS.getKey().equalsIgnoreCase(nodeTemplateFragment.getValue().getRelationships().get("execute").getTarget())) {
                        // Look for the corresponding node type
                        for (Map.Entry<String, NodeType> nodeTypeFaaS : parsingResult.getResult().getNodeTypes().entrySet()) {
                            if (nodeTypeFaaS.getKey().equalsIgnoreCase(nodeTemplateFaaS.getValue().getType())) {
                                RelationshipFaaS relationship = new RelationshipFaaS(nodeTemplateFragment.getKey(), nodeTemplateFaaS.getKey(), nodeTypeFaaS.getKey());
                                // Look for requirements
                                for (RequirementDefinition requirement : nodeTypeFaaS.getValue().getRequirements()) {
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
                                            // Find the sensors properties
                                            if (capability.getKey().equalsIgnoreCase("sensors")) {
                                                //TODO: get sensors requirements
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

    public static Map<String, String> getMetadata(ParsingResult<ArchiveRoot> parsingResult) {
        Map<String, String> metadata = new HashMap<>();
        for (Tag tag : parsingResult.getResult().getArchive().getTags()) {
            metadata.put(tag.getName(), tag.getValue());
        }
        return metadata;
    }

    public static List<String> getListOfCloudsFromMetadata(Map<String, String> metadata) {
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

    public static String findBestSuitableVMType(ToscaParser parser, String repositoryPath, String cloud, String region, Map<String, List<String>> hostingConstraints) throws IOException, ParsingException {

        // TODO: MAKE A LIST OF CANDIDATES AND SELECT THE LOWEST PRICE

        Map<String, Map<String, Map<String, String>>> VMTypes;
        Map<String, Double> selectedTypes = new HashMap<>();

        if (cloud.equalsIgnoreCase("amazon")) {
            VMTypes = getCloudNodesTemplates(parser.parseFile(Paths.get(repositoryPath,"amazon-vm-templates.yml")), region);
        }
        else if (cloud.equalsIgnoreCase("azure")) {
            VMTypes = getCloudNodesTemplates(parser.parseFile(Paths.get(repositoryPath,"azure-vm-templates.yml")), region);
        }
        else if (cloud.equalsIgnoreCase("openstack")) {
            System.out.println("OpenStack types not yet defined (flavors must be customized).");
            //VMTypes = getCloudNodesTemplates(parser.parseFile(Paths.get(repositoryPath, "openstack-vm-templates.yml")), region);
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
                    //return hostingConstraint.getValue().get("cloud").get("name");
                    selectedTypes.put(hostingConstraint.getValue().get("cloud").get("name"), Double.valueOf(hostingConstraint.getValue().get("host").get("price")));
                }
            }
        }

        if (selectedTypes.isEmpty()) {
            System.out.println("No suitable type found for hosting constraints: " + hostingConstraints);
            return null;
        }
        else {
            return selectedTypes.entrySet().stream().sorted(Map.Entry.comparingByValue()).findFirst().get().getKey();
        }
    }
}
