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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

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

            if (args.length != 5) {
               logger.error("Missing argument: the expected arguments are (i) the TOSCA types directory, (ii) the directory conatining the resource description file, (iii) the type level file to proceed, (iv) the management output file and (v) the file mapping node and deployed node");
               System.exit(1);
            }

            if (!Paths.get(args[0]).toFile().exists() || !Paths.get(args[1]).toFile().exists() || !Paths.get(args[2]).toFile().exists()) {
               logger.error("Either the repository directory or the specified file to be parsed is not found.");
               System.exit(1);
            }
            ((LocalRepositoryImpl)csarRepositorySearchService).setPath(args[0]);

            // Second argument must be the path of the file to parse (example: "src/test/resources/prestocloud/ICCS-example.yml")
            boolean parsingSuccess = processToscaWithBtrPlace(args[1], args[2], args[3], args[4]);

            if (parsingSuccess) {
                logger.info("The parsing ended successfully");
                System.exit(0);
            } else {
                logger.error("ERR: The parsing has failed.");
                System.exit(1);
            }
        };
    }

    public boolean processToscaWithBtrPlace(String resourcesPath, String typeLevelTOSCAFile, String outputFile, String mappingFile) {
        try {
            logger.info("(1/18) Parsing the type-level TOSCA file");
            ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(typeLevelTOSCAFile));
            logger.info("(2/18) Parsing VM cloud resource TOSCA file");
            GetVMTemplatesDetailsResult vmTemplatesParsingResult = ParsingUtils.getVMTemplatesDetails(parser, resourcesPath);
            ParsingSpace ps = new ParsingSpace(parsingResult, vmTemplatesParsingResult ,parser,resourcesPath);
            logger.info("(3/18) Interpreting TOSCA specification");
            ps.retrieveResourceFromParsing();
            logger.info("(4/18) Identifying fragments related to precedence constraints ...");
            ps.identifiesNodeRelatedToPrecedenceConstraints();
            logger.info("(5/18) Determining the best suited cloud VM type for identified computing resources");
            ps.selectBestCloudVmType();
            logger.info("(6/18) Preparing APSC context (Btrplace)");
            ps.configureBtrPlace();
            logger.info("(7/18) Creating btrplace resources (Vms & Edge)");
            ps.createVmsInBtrPlaceModel();
            logger.info("(8/18) Populating the model with regions from public and private cloud");
            ps.populatePublicAndPrivateCloud();
            logger.info("(9/18) Configuration of regions computing capability");
            ps.setCapacity();
            logger.info("(10/18) Configuring constraints from the fragment specification");
            ps.configuringNodeComputingRequirementConstraint();
            logger.info("(11/18) Checking and defining the resource availability");
            ps.detectResourceAvailability();
            logger.info("(12/18) Defining fragment deployability");
            ps.defineFragmentDeployability();
            logger.info("(13/18) Enforcing policy constraint in APSC");
            ps.configurePlacementConstraint();
            logger.info("(14/18) Retrieving cost-related information");
            ps.extractCost();
            logger.info("(15/18) Solving ...");
            if (!ps.performedBtrplaceSolving()) {
                throw new IllegalStateException("No Btrplace reconfiguration plan was determined");
            } else {
                logger.info("(16/18) Writing management plan output");
                writeResutl(ps.generationJsonOutput(),outputFile);
                logger.info("(17/18) Writing the mapping output");
                writeResutl(ps.generateOutputMapping(),mappingFile);
                logger.info("(18/18) The type-level TOSCA processing has ended successfully");
                return true;
            }
        } catch (Exception e) {
            logger.error("Error while parsing the Type-level TOSCA document : {}", e.getMessage());
            logger.error(e.getCause().getMessage());
            return false;
        }
    }

    private void writeResutl(String result, String path) throws IOException {
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
}
