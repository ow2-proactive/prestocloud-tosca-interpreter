package prestocloud.tosca.parser;

import java.util.Set;

import org.prestocloud.tosca.model.Csar;
import org.prestocloud.tosca.model.types.ArtifactType;
import org.prestocloud.tosca.model.types.CapabilityType;
import org.prestocloud.tosca.model.types.DataType;
import org.prestocloud.tosca.model.types.NodeType;
import org.prestocloud.tosca.model.types.RelationshipType;
import org.prestocloud.tosca.normative.constants.NormativeCapabilityTypes;
import org.prestocloud.tosca.normative.constants.NormativeTypesConstant;
import org.mockito.Mockito;

import prestocloud.component.ICSARRepositorySearchService;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for parsers tests.
 */
@Slf4j
public class ParserTestUtil {
    /**
     * Log (with debug level) the various errors from the parsing result.
     * 
     * @param parsingResult The parsing result to log.
     */
    public static void displayErrors(ParsingResult<?> parsingResult) {
        log.debug("\n\nERRORS: \n");
        for (int i = 0; i < parsingResult.getContext().getParsingErrors().size(); i++) {
            ParsingError error = parsingResult.getContext().getParsingErrors().get(i);
            if (error.getErrorLevel().equals(ParsingErrorLevel.ERROR)) {
                log.debug(parsingResult.getContext().getFileName() + "\n" + error);
            }
        }
        log.debug("\n\nWARNING: \n");
        for (int i = 0; i < parsingResult.getContext().getParsingErrors().size(); i++) {
            ParsingError error = parsingResult.getContext().getParsingErrors().get(i);
            if (error.getErrorLevel().equals(ParsingErrorLevel.WARNING)) {
                log.debug(parsingResult.getContext().getFileName() + "\n" + error);
            }
        }
        log.debug("\n\nINFO: \n");
        for (int i = 0; i < parsingResult.getContext().getParsingErrors().size(); i++) {
            ParsingError error = parsingResult.getContext().getParsingErrors().get(i);
            if (error.getErrorLevel().equals(ParsingErrorLevel.INFO)) {
                log.debug(parsingResult.getContext().getFileName() + "\n" + error);
            }
        }
    }

    /**
     * Utility method to mock the access to some normative types nodes and capabilities.
     * 
     * @param repositorySearchService The repositorySearchService to mock.
     */
    public static void mockNormativeTypes(ICSARRepositorySearchService repositorySearchService) {
        Csar csar = new Csar("tosca-normative-types", "1.0.0-ALIEN12");
        Mockito.when(repositorySearchService.getArchive(csar.getName(), csar.getVersion())).thenReturn(csar);
        NodeType mockedNodeRoot = new NodeType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq(NormativeTypesConstant.ROOT_NODE_TYPE),
                Mockito.any(Set.class))).thenReturn(mockedNodeRoot);
        RelationshipType mockedRelationshipRoot = new RelationshipType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class),
                Mockito.eq(NormativeTypesConstant.ROOT_RELATIONSHIP_TYPE), Mockito.any(Set.class))).thenReturn(mockedRelationshipRoot);
        CapabilityType mockedCapabilityType = new CapabilityType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq(NormativeCapabilityTypes.ROOT),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityType);
        DataType mockedDataType = new DataType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(DataType.class), Mockito.eq(NormativeTypesConstant.ROOT_DATA_TYPE),
                Mockito.any(Set.class))).thenReturn(mockedDataType);
        ArtifactType mockedArtifactType = new ArtifactType();
        Mockito.when(repositorySearchService.getElementInDependencies(Mockito.eq(ArtifactType.class), Mockito.eq(NormativeTypesConstant.ROOT_ARTIFACT_TYPE),
                Mockito.any(Set.class))).thenReturn(mockedArtifactType);
    }
}
