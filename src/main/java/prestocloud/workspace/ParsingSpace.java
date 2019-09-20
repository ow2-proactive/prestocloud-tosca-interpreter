package prestocloud.workspace;

import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import prestocloud.btrplace.tosca.ParsingUtils;
import prestocloud.btrplace.tosca.model.*;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@EnableAspectJAutoProxy(proxyTargetClass = true)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class ParsingSpace {

    private ParsingResult<ArchiveRoot> parsingResult;
    private ToscaParser parser;
    private String resourcesPath;

    // We describe couples of element we want to d integrate from our parsing.
    Map<String, String> metadata;
    List<String> supportedClouds;
    List<Relationship> relationships;
    List<PlacementConstraint> placementConstraints;
    List<Docker> dockers;
    List<OptimizationVariables> optimizationVariables;
    List<VMTemplateDetails> vmTemplatesDetails;
    // TODO: deal with health checks
    List<HealthCheck> healthChecks;

    public ParsingSpace(ParsingResult<ArchiveRoot> result, List<VMTemplateDetails> vmTemplatesDetails, ToscaParser parser, String resourcesPath) {
        this.parsingResult = result;
        this.parser = parser;
        this.resourcesPath  = resourcesPath;
        this.vmTemplatesDetails = vmTemplatesDetails;
    }

    public boolean proceed()  {
        // Retrieving main data from the parsed TOSCA.
        metadata = ParsingUtils.getMetadata(parsingResult);
        supportedClouds = ParsingUtils.getListOfCloudsFromMetadata(metadata);
        relationships = ParsingUtils.getRelationships(parsingResult);
        placementConstraints = ParsingUtils.getConstraints(parsingResult);
        dockers = ParsingUtils.getDockers(parsingResult);
        optimizationVariables = ParsingUtils.getOptimizationVariables(parsingResult);
        healthChecks = ParsingUtils.getHealthChecks(parsingResult);
        // Importing the get
        try {
            vmTemplatesDetails = ParsingUtils.getVMTemplatesDetails(parser, resourcesPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ParsingException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
