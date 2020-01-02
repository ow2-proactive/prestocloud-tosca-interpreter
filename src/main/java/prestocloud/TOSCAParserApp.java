/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2019 ActiveEon
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import lombok.Getter;
import prestocloud.btrplace.tosca.GetVMTemplatesDetailsResult;
import prestocloud.btrplace.tosca.ParsingUtils;
import prestocloud.btrplace.tosca.model.EdgeResourceTemplateDetails;
import prestocloud.component.ICSARRepositorySearchService;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingException;
import prestocloud.tosca.parser.ParsingResult;
import prestocloud.tosca.parser.ToscaParser;
import prestocloud.tosca.repository.LocalRepositoryImpl;
import prestocloud.workspace.InstanceLevelParsingSpace;
import prestocloud.workspace.TypeLevelParsingSpace;

/**
 * @author ActiveEon Team
 * @since 24/09/18
 */

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@Slf4j
public class TOSCAParserApp {

    @Getter
    @Resource
    private ToscaParser parser;

    @Resource(name = "localRepository")
    private ICSARRepositorySearchService csarRepositorySearchService;

    private Logger logger = LoggerFactory.getLogger(TOSCAParserApp.class);

    public static void main(String[] args) {
        SpringApplication.run(TOSCAParserApp.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            // First argument must be the repository path (example: "src/main/resources/repository/" or "/mnt/glusterfs")
            if (args.length == 8 && args[0].equals("type-level-interpreter")) {

                if (!Paths.get(args[1]).toFile().exists() || !Paths.get(args[2]).toFile().exists() || !Paths.get(args[3]).toFile().exists()) {
                    logger.error("Either the repository directory or the specified file to be parsed is not found.");
                    System.exit(1);
                }
                ((LocalRepositoryImpl) csarRepositorySearchService).setPath(args[1]);

                // Second argument must be the path of the file to parse (example: "src/test/resources/prestocloud/ICCS-example.yml")
                boolean parsingSuccess = processTypeLevelToscaWithBtrPlace(args[2], args[3], args[4], args[5], args[6], args[7]);

                if (parsingSuccess) {
                    logger.info("The parsing ended successfully");
                    System.exit(0);
                } else {
                    logger.error("ERR: The parsing has failed.");
                    System.exit(1);
                }

            } else if (args.length == 4 && args[0].equals("instance-level-interpreter")) {

                if (!Paths.get(args[1]).toFile().exists() || !Paths.get(args[2]).toFile().exists()) {
                    logger.error("Either the repository directory or the specified file to be parsed is not found.");
                    System.exit(1);
                }
                ((LocalRepositoryImpl) csarRepositorySearchService).setPath(args[1]);

                boolean parsingSuccess = processInstanceLevelTosca(args[2], args[3]);

                if (parsingSuccess) {
                    logger.info("The parsing ended successfully");
                    System.exit(0);
                } else {
                    logger.error("ERR: The parsing has failed.");
                    System.exit(1);
                }
            } else {
                logger.error("Missing argument: the expected arguments are:\n" +
                        " - (i) the mode type-level-interpreter, (ii) the TOSCA types directory, (iii) the directory containing the resource description file, (iv) the type level file to proceed, (v) the management output file, (vi) the instance level tosca template to be produce (vii) the file mapping node and deployed node and (viii) the edge status file.\n" +
                        " - (i) the mode instance-level-interpreter, (ii) the TOSCA types directory, (iii) the instance-level TOSCA file, (iv) the outputfile");
                System.exit(1);
            }


        };
    }

    public boolean processTypeLevelToscaWithBtrPlace(String resourcesPath, String typeLevelTOSCAFile, String outputFile, String instanceLevelToscaTemplate, String mappingFile, String edgeStatusFile) {
        try {
            logger.info("(1/23) Parsing the type-level TOSCA file");
            ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(typeLevelTOSCAFile));
            logger.info("(2/23) Parsing VM cloud resource TOSCA file");
            GetVMTemplatesDetailsResult vmTemplatesParsingResult = ParsingUtils.getVMTemplatesDetails(parser, resourcesPath);
            logger.info("(3/23) Parsing Edge resource TOSCA file");
            List<EdgeResourceTemplateDetails> edgeResourceTemplateDetails = ParsingUtils.getEdgeResourceTemplateDetails(parser, resourcesPath);
            TypeLevelParsingSpace ps = new TypeLevelParsingSpace(parsingResult, vmTemplatesParsingResult, edgeResourceTemplateDetails, parser, resourcesPath);
            logger.info("(4/23) Interpreting TOSCA specification");
            ps.retrieveResourceFromParsing();
            logger.info("(5/23) Identifying fragments related to precedence constraints ...");
            ps.classifyNodeAccordingToRelationships();
            logger.info("(6/23) Determining the best suited cloud VM type for identified computing resources");
            ps.selectBestCloudVmType();
            logger.info("(7/23) Preparing APSC context (Btrplace)");
            ps.configureBtrPlace();
            logger.info("(8/23) Creating btrplace resources (Vms & Edge)");
            ps.populateVmsInBtrPlaceModel();
            logger.info("(9/23) Populating the model with regions from public and private cloud");
            ps.populateNodesInBtrPlaceModel();
            logger.info("(10/23) Configuration of regions computing capability");
            ps.setCloudNodeToKeep();
            if (Paths.get(mappingFile).toFile().exists()) {
                logger.info("(11/23) Loading mapping file : Interpreting the current fragment deployment");
                ps.loadExistingMapping(readFile(mappingFile));
            } else {
                logger.info("(11/23) Loading mapping file : the file doesn't exist or is empty: Assuming a new fragment deployment");
            }
            logger.info("(12/23) Configuration of regions computing capability");
            ps.setCapacity();
            if (Paths.get(edgeStatusFile).toFile().exists()) {
                logger.info("(13/23) Loading data for edge devices availability");
                ps.loadRunningEdgeNode(readFile(edgeStatusFile));
            } else {
                logger.info("(13/23) Skipping the load of data for edge devices availability: No file for edge device availability found.");
            }
            logger.info("(14/23) Configuring constraints from the fragment specification");
            ps.configuringVmsResourcesRequirementConstraint();
            logger.info("(15/23) Checking and defining the resource availability");
            List<Object> availableResources = this.detectResourceAvailability();
            if (availableResources != null) {
                logger.info(" -- ADIAM environment detected. I'll check cloud availability");
                ps.banUnreferencedCloudInCloudList(availableResources);
            } else {
                logger.info(" -- No ADIAM detected. Working standalone, and assume all clouds are available to use.");
            }
            logger.info("(16/23) Defining fragment deployability");
            ps.defineFragmentDeployability();
            logger.info("(17/23) Enforcing policy constraint in APSC");
            ps.configurePlacementConstraint();
            logger.info("(18/23) Retrieving cost-related information");
            ps.extractCost();
            logger.info("(19/23) Solving ...");
            if (!ps.performedBtrplaceSolving()) {
                throw new IllegalStateException("No Btrplace reconfiguration plan was determined");
            } else {
                double tp = ps.getTimePeriod(), hcod = ps.getHourlyCostOfDeployment(), ct = ps.getCostThreshold();
                if (tp * hcod > ct) {
                    throw new IllegalStateException(String.format("Threshold cost limit has exceeded: Time (h): %s , Hourly cost of deployment (Eur/h): %s , Cost threshold (Eur): %s", tp, hcod, ct));
                }
                logger.info("Threshold cost limit is accepted: Time (h): {}, Hourly cost of deployment (Eur/h): {}, Cost threshold (Eur): {}", tp, hcod, ct);
                logger.info("(20/23) Writing management plan output");
                writeResult(ps.generationJsonOutput(), outputFile);
                logger.info("(21/23) Writing the mapping output");
                writeResult(ps.generateOutputMapping(), mappingFile);
                logger.info("(22/23) Producing instance level TOSCA template");
                try {
                    writeResult(ps.generateInstanceLevelToscaTemplate(detectResourceAvailability()), instanceLevelToscaTemplate);
                } catch (IllegalAccessException e) {
                    logger.info(e.getMessage());
                }
                logger.info("(23/23) The type-level TOSCA processing has ended successfully");
                return true;
            }
        } catch (Exception e) {
            logger.error("Error while parsing the Type-level TOSCA document : {}", e.getMessage());
            logger.error(" --> ", e);
            return false;
        }
    }

    public boolean processInstanceLevelTosca(String instanceLevelFile, String outputFile) {
        try {
            logger.info("(1/6) Parsing the instance-level TOSCA file");
            ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(instanceLevelFile));
            logger.info("(2/6) Checking for parsing errors ...");
            InstanceLevelParsingSpace ps = new InstanceLevelParsingSpace(parsingResult);
            if (ps.isThereParsingError()) {
                return false;
            }
            logger.info("(3/6) Collecting instance info");
            ps.startParsing();
            logger.info("(4/6) Classifying");
            ps.classify();
            logger.info("(5/6) Writing results");
            writeResult(ps.getResult(), outputFile);
            logger.info("(6/6) Done");
            return true;
        } catch (ParsingException | IOException e) {
            logger.error("Error while parsing the Instance-level TOSCA document : {}", e.getMessage());
            logger.error(" --> ", e);
            return false;
        }
    }

    private void writeResult(String result, String path) throws IOException {
        FileWriter file = new FileWriter(path);
        try {
            file.write(result);
            file.flush();
        } catch (IOException e) {
            logger.error("Error while writing to {} : {}", path, e.getMessage());
        } finally {
            file.close();
        }
    }

    private String readFile(String path) throws IOException {
        Path filepath = Paths.get(path);
        File file = filepath.toFile();
        if (!file.exists()) {
            throw new IllegalStateException("Unable to access file");
        }
        try {
            return new String(Files.readAllBytes(filepath));
        } catch (IOException e) {
            logger.error("Unable to read the file {} : {}", path, e.getMessage());
            throw e;
        }
    }

    private List<Object> detectResourceAvailability() {
        //We verify if we are running inside the main WF environement
        String cloudList = System.getenv().getOrDefault("variables_CLOUD_LIST", null);
        if (cloudList != null) {
            //If the cloud resource is not detected as online, I'll add a banning constrain.
            if (!JSONValue.isValidJson(cloudList)) {
                throw new IllegalArgumentException("ADIAM CLOUD_LIST argument is not a valid JSON structure");
            }
            JSONArray ja = (JSONArray) JSONValue.parse(cloudList);
            return new ArrayList<Object>(ja);
        } else {
            return null;
        }
        // Edge device availability is already tackled in loadRunningEdgeNode in ParsingSpace
    }
}
