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
import java.util.List;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
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
import prestocloud.tosca.parser.ParsingResult;
import prestocloud.tosca.parser.ToscaParser;
import prestocloud.tosca.repository.LocalRepositoryImpl;
import prestocloud.workspace.ParsingSpace;

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

            if (args.length != 7) {
                logger.error("Missing argument: the expected arguments are (i) the TOSCA types directory, (ii) the directory conatining the resource description file, (iii) the type level file to proceed, (iv) the management output file, (v) the instance level tosca template to be produce (vi) the file mapping node and deployed node and (vii) the edge status file.");
               System.exit(1);
            }

            if (!Paths.get(args[0]).toFile().exists() || !Paths.get(args[1]).toFile().exists() || !Paths.get(args[2]).toFile().exists()) {
               logger.error("Either the repository directory or the specified file to be parsed is not found.");
               System.exit(1);
            }
            ((LocalRepositoryImpl)csarRepositorySearchService).setPath(args[0]);

            // Second argument must be the path of the file to parse (example: "src/test/resources/prestocloud/ICCS-example.yml")
            boolean parsingSuccess = processToscaWithBtrPlace(args[1], args[2], args[3], args[4], args[5], args[6]);

            if (parsingSuccess) {
                logger.info("The parsing ended successfully");
                System.exit(0);
            } else {
                logger.error("ERR: The parsing has failed.");
                System.exit(1);
            }
        };
    }

    public boolean processToscaWithBtrPlace(String resourcesPath, String typeLevelTOSCAFile, String outputFile, String instanceLevelToscaTemplate, String mappingFile, String edgeStatusFile) {
        try {
            logger.info("(1/22) Parsing the type-level TOSCA file");
            ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(typeLevelTOSCAFile));
            logger.info("(2/22) Parsing VM cloud resource TOSCA file");
            GetVMTemplatesDetailsResult vmTemplatesParsingResult = ParsingUtils.getVMTemplatesDetails(parser, resourcesPath);
            logger.info("(3/22) Parsing Edge resource TOSCA file");
            List<EdgeResourceTemplateDetails> edgeResourceTemplateDetails = ParsingUtils.getEdgeResourceTemplateDetails(parser, resourcesPath);
            ParsingSpace ps = new ParsingSpace(parsingResult, vmTemplatesParsingResult, edgeResourceTemplateDetails, parser, resourcesPath);
            logger.info("(4/22) Interpreting TOSCA specification");
            ps.retrieveResourceFromParsing();
            logger.info("(5/22) Identifying fragments related to precedence constraints ...");
            ps.classifyNodeAccordingToRelationships();
            logger.info("(6/22) Determining the best suited cloud VM type for identified computing resources");
            ps.selectBestCloudVmType();
            logger.info("(7/22) Preparing APSC context (Btrplace)");
            ps.configureBtrPlace();
            logger.info("(8/22) Creating btrplace resources (Vms & Edge)");
            ps.populateVmsInBtrPlaceModel();
            logger.info("(9/22) Populating the model with regions from public and private cloud");
            ps.populateNodesInBtrPlaceModel();
            logger.info("(10/22) Configuration of regions computing capability");
            ps.setCloudNodeToKeep();
            if (Paths.get(mappingFile).toFile().exists()) {
                logger.info("(11/22) Loading mapping file : Interpreting the current fragment deployment");
                ps.loadExistingMapping(readFile(mappingFile));
            } else {
                logger.info("(11/22) Loading mapping file : the file doesn't exist or is empty: Assuming a new fragment deployment");
            }
            logger.info("(12/22) Configuration of regions computing capability");
            ps.setCapacity();
            if (Paths.get(edgeStatusFile).toFile().exists()) {
                logger.info("(13/22) Loading data for edge devices availability");
                ps.loadRunningEdgeNode(readFile(edgeStatusFile));
            } else {
                logger.info("(13/22) Skipping the load of data for edge devices availability: No file for edge device availability found.");
            }
            logger.info("(14/22) Configuring constraints from the fragment specification");
            ps.configuringVmsResourcesRequirementConstraint();
            logger.info("(15/22) Checking and defining the resource availability");
            ps.detectResourceAvailability();
            logger.info("(16/22) Defining fragment deployability");
            ps.defineFragmentDeployability();
            logger.info("(17/22) Enforcing policy constraint in APSC");
            ps.configurePlacementConstraint();
            logger.info("(18/22) Retrieving cost-related information");
            ps.extractCost();
            logger.info("(19/22) Solving ...");
            if (!ps.performedBtrplaceSolving()) {
                throw new IllegalStateException("No Btrplace reconfiguration plan was determined");
            } else {
                double tp = ps.getTimePeriod(), hcod = ps.getHourlyCostOfDeployment(), ct = ps.getCostThreshold();
                if (tp * hcod > ct) {
                    throw new IllegalStateException(String.format("Threshold cost limit has exceeded: Time (h): %s , Hourly cost of deployment (Eur/h): %s , Cost threshold (Eur): %s", tp, hcod, ct));
                }
                logger.info("Threshold cost limit is accepted: Time (h): {}, Hourly cost of deployment (Eur/h): {}, Cost threshold (Eur): {}", tp, hcod, ct);
                logger.info("(20/22) Writing management plan output");
                writeResult(ps.generationJsonOutput(), outputFile);
                logger.info("(21/22) Writing the mapping output");
                writeResult(ps.generateOutputMapping(), mappingFile);
                logger.info("(22/22) Producing instance level TOSCA template");
                try {
                    writeResult(ps.generateInstanceLevelToscaTemplate(), instanceLevelToscaTemplate);
                } catch (IllegalAccessException e) {
                    logger.info(e.getMessage());
                }
                logger.info("(23/22) The type-level TOSCA processing has ended successfully");
                return true;
            }
        } catch (Exception e) {
            logger.error("Error while parsing the Type-level TOSCA document : {}", e.getMessage());
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
}
