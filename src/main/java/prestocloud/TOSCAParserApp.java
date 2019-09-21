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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import prestocloud.btrplace.tosca.GetVMTemplatesDetailsResult;
import prestocloud.btrplace.tosca.ParsingUtils;
import prestocloud.component.ICSARRepositorySearchService;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingException;
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
@RunWith(SpringJUnit4ClassRunner.class)
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
            if (!Files.exists(Paths.get(args[0])) || !Files.exists(Paths.get(args[1])) || !Files.exists(Paths.get(args[2]))) {
               System.err.println("Either the repository directory or the specified file to be parsed is not found.");
               System.exit(1);
            }
            ((LocalRepositoryImpl)csarRepositorySearchService).setPath(args[0]);

            // Second argument must be the path of the file to parse (example: "src/test/resources/prestocloud/ICCS-example.yml")
            boolean parsingSuccess = processToscaWithBtrPlace(args[1], args[2], args[3]);

            if (parsingSuccess) {
                logger.info("The parsing ended successfully");
                System.exit(0);
            } else {
                logger.error("ERR: The parsing has failed.");
                System.exit(1);
            }
        };
    }

    public boolean processToscaWithBtrPlace(String resourcesPath, String typeLevelTOSCAFile, String outputFile) {
        try {
            logger.info("(1/) Parsing the type-level TOSCA file");
            ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(typeLevelTOSCAFile));
            logger.info("(2/) Parsing VM cloud resource TOSCA file");
            GetVMTemplatesDetailsResult vmTemplatesParsingResult = ParsingUtils.getVMTemplatesDetails(parser, resourcesPath);
            ParsingSpace ps = new ParsingSpace(parsingResult, vmTemplatesParsingResult ,parser,resourcesPath);
            logger.info("(3/) Interpreting TOSCA specification");
            ps.retrieveResourceFromParsing();
            logger.info("(4/) Determining the best suited cloud VM type for identified computing resources");
            ps.selectBestCloudVmType();
            logger.info("(5/) Preparing APSC context (Btrplace)");
            ps.configureBtrPlace();
            logger.info("(6/) Creating btrplace resources (Vms & Edge)");
            ps.createVmsResourceInBtrPlace();
            logger.info("(7/) Populating the model with regions from public and private cloud");
            ps.populatePublicAndPrivateCloud();
            logger.info("(8/) Configuration of regions computing capability");
            ps.setCapacity();
        } catch (Exception e) {
            logger.error(String.format("Error while parsing the Type-level TOSCA document", e.getMessage()));
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
