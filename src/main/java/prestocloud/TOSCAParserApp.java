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

package prestocloud;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.prestocloud.tosca.model.definitions.AbstractPropertyValue;
import org.prestocloud.tosca.model.definitions.ComplexPropertyValue;
import org.prestocloud.tosca.model.definitions.ScalarPropertyValue;
import org.prestocloud.tosca.model.templates.Capability;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import lombok.Getter;
import prestocloud.component.ICSARRepositorySearchService;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingResult;
import prestocloud.tosca.parser.ToscaParser;
import prestocloud.tosca.repository.LocalRepositoryImpl;

/**
 * @author ActiveEon Team
 * @since 24/09/18
 */

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class TOSCAParserApp {

    @Getter
    @Resource
    private ToscaParser parser;

    @Resource(name = "localRepository")
    private ICSARRepositorySearchService csarRepositorySearchService;

    public static void main(String[] args) {
        SpringApplication.run(TOSCAParserApp.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            // First argument must be the repository path (example: "src/main/resources/repository/" or "/mnt/glusterfs")
            ((LocalRepositoryImpl)csarRepositorySearchService).setPath(args[0]);

            // Second argument must be the path of the file to parse (example: "src/test/resources/prestocloud/ICCS-example.yml")
            ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(args[1]));

            // Print OK if no error found
            Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
            //System.out.println("OK");
            determine_fragment_instances(args[2],parsingResult);

        };
    }

    /**
     * This function returns a comma separated integer list of instances corresponding to a (string) comma separated list of fragment names
     * @param arg The csv input string of fragment names
     * @param parsingResult The result of the parsing of the instance-level tosca
     */
    private void determine_fragment_instances(String arg, ParsingResult<ArchiveRoot> parsingResult) {
        String DELIMITER = ",";
        String [] fragments_list = arg.split(DELIMITER);
        StringBuilder output_string = new StringBuilder("");
        HashMap<String,Integer> instances_hashmap = new HashMap<>();

        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment.faas")) {
                String fragment_name = nodeTemplateFragment.getKey();
                fragment_name = fragment_name.replaceAll("_\\d$", "");
                instances_hashmap.put(fragment_name,0);
            }
        }
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.agent.faas")){
                String agent_name = nodeTemplateFragment.getKey();
                String fragment_name = agent_name.replaceFirst("deployment_node_","").replaceAll("_\\d$","");
                instances_hashmap.computeIfPresent(fragment_name,(real_fragment_name,fragment_instances)-> ++fragment_instances);
            }
        }

        for (String fragment:fragments_list){
            output_string.append(instances_hashmap.get(fragment)).append(DELIMITER);
        }

        output_string.deleteCharAt(output_string.length()-1);
        System.out.println(output_string);
    }

    private void test_printing_function(ParsingResult<ArchiveRoot> parsingResult) {

        //Parsing code improved from relevant method in PrEstoCloudTest.

        // Look for fragments in the node templates
        Map<String, NodeTemplate> nodeTemplates = parsingResult.getResult().getTopology().getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTemplateFragment : nodeTemplates.entrySet()) {
            // Fragment detected
            if (nodeTemplateFragment.getValue().getType().equalsIgnoreCase("prestocloud.nodes.fragment.faas")) {
                // Look for the corresponding JPPF agent
                for (Map.Entry<String, NodeTemplate> nodeTemplateJPPF  : nodeTemplates.entrySet()) {
                    // Corresponding JPPF agent found
                    if (nodeTemplateJPPF.getValue().getType().equalsIgnoreCase("prestocloud.nodes.agent.faas") &&  nodeTemplateJPPF.getKey().equalsIgnoreCase(nodeTemplateFragment.getValue().getRelationships().get("execute").getTarget())) {
                        // Look for the corresponding host
                        for (Map.Entry<String, NodeTemplate> nodeTemplateHost : nodeTemplates.entrySet()) {
                            // Corresponding host found
                            if (nodeTemplateHost.getKey().equalsIgnoreCase(nodeTemplateJPPF.getValue().getRelationships().get("host").getTarget())) {
                                // Complete dependency found: fragment -> JPPF agent -> host
                                System.out.println("Found fragment: '" + nodeTemplateFragment.getValue().getName() + "' executed on JPPF agent '" + nodeTemplateFragment.getValue().getRelationships().get("execute").getTarget() + "' hosted on '" + nodeTemplateJPPF.getValue().getRelationships().get("host").getTarget() + "'");
                                // Look for capabilities
                                for (Map.Entry<String, Capability> capabilities : nodeTemplateHost.getValue().getCapabilities().entrySet()) {
                                    // Get 'host' capability properties
                                    if (capabilities.getKey().equalsIgnoreCase("host")) {
                                        System.out.println("Host properties: ");
                                        for (Map.Entry<String, AbstractPropertyValue> properties : capabilities.getValue().getProperties().entrySet()) {
                                            if (properties.getKey().equalsIgnoreCase("num_cpus")) {
                                                String num_cpus = ((ScalarPropertyValue)properties.getValue()).getValue();
                                                System.out.println("- " + properties.getKey() + " = " + num_cpus);
                                            }
                                            if (properties.getKey().equalsIgnoreCase("mem_size")) {
                                                String mem_size = ((ScalarPropertyValue)properties.getValue()).getValue();
                                                System.out.println("- " + properties.getKey() + " = " + mem_size);
                                            }
                                            if (properties.getKey().equalsIgnoreCase("disk_size")) {
                                                String disk_size = ((ScalarPropertyValue)properties.getValue()).getValue();
                                                System.out.println("- " + properties.getKey() + " = " + disk_size);
                                            }
                                            if (properties.getKey().equalsIgnoreCase("price")) {
                                                boolean price_denoted = (ScalarPropertyValue)properties.getValue()!=null;
                                                String price = price_denoted? ((ScalarPropertyValue)properties.getValue()).getValue():"0";
                                                System.out.println("- " + properties.getKey() + " = " + price);
                                            }
                                        }
                                    }
                                    // Get 'resource' capability properties
                                    if (capabilities.getKey().equalsIgnoreCase("resource")) {
                                        System.out.println("Resource properties: ");
                                        String type = null;
                                        for (Map.Entry<String, AbstractPropertyValue> properties : capabilities.getValue().getProperties().entrySet()) {
                                            if (properties.getKey().equalsIgnoreCase("type")) {
                                                type = ((ScalarPropertyValue) properties.getValue()).getValue();
                                                System.out.println("- " + properties.getKey() + " = " + type);
                                            }
                                            // Cloud based resource detected
                                            if (type != null && type.equalsIgnoreCase("cloud") && properties.getKey().equalsIgnoreCase("cloud")) {
                                                System.out.println("- " + properties.getKey() + ":");
                                                ComplexPropertyValue cloud = (ComplexPropertyValue) properties.getValue();
                                                for (Map.Entry<String, Object> cloudProperties : cloud.getValue().entrySet()) {
                                                    if (cloudProperties.getKey().equalsIgnoreCase("cloud_type")) {
                                                        System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                                    }
                                                    if (cloudProperties.getKey().equalsIgnoreCase("cloud_region")) {
                                                        System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                                    }
                                                    if (cloudProperties.getKey().equalsIgnoreCase("cloud_name")) {
                                                        System.out.println(" - " + cloudProperties.getKey() + " = " + cloudProperties.getValue());
                                                    }
                                                    // Get networking informations
                                                    if (cloudProperties.getKey().equalsIgnoreCase("cloud_network")) {
                                                        System.out.println(" - " + cloudProperties.getKey() + ":");
                                                        HashMap<String, Object> cloud_network = (HashMap<String, Object>) cloudProperties.getValue();
                                                        for (Map.Entry<String, Object> cloudNetworkProperties : cloud_network.entrySet()) {
                                                            if (cloudNetworkProperties.getKey().equalsIgnoreCase("network_id")) {
                                                                System.out.println("  - " + cloudNetworkProperties.getKey() + " = " + cloudNetworkProperties.getValue());
                                                            }
                                                            if (cloudNetworkProperties.getKey().equalsIgnoreCase("network_name")) {
                                                                System.out.println("  - " + cloudNetworkProperties.getKey() + " = " + cloudNetworkProperties.getValue());
                                                            }
                                                            if (cloudNetworkProperties.getKey().equalsIgnoreCase("addresses")) {
                                                                System.out.println("  - " + cloudNetworkProperties.getKey());
                                                                for (String address : (List<String>) cloudNetworkProperties.getValue()) {
                                                                    System.out.println("    - " + address);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
