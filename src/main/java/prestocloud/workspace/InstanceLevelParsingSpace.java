package prestocloud.workspace;

import lombok.extern.slf4j.Slf4j;
import org.prestocloud.tosca.model.templates.NodeTemplate;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import prestocloud.model.ParsedFragmentRegistration;
import prestocloud.tosca.model.ArchiveRoot;
import prestocloud.tosca.parser.ParsingError;
import prestocloud.tosca.parser.ParsingResult;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class InstanceLevelParsingSpace {
    private ParsingResult<ArchiveRoot> parsingResult;
    private ConcurrentHashMap<String, ParsedFragmentRegistration> parsedFragment;
    private Map<String, NodeTemplate> processingNodesPerInstanceName;
    private static Pattern fragmentIdentifier = Pattern.compile("^(\\w*)_[\\d]+$");

    public InstanceLevelParsingSpace(ParsingResult<ArchiveRoot> result) {
        parsingResult = result;
        parsedFragment = new ConcurrentHashMap<>();
    }

    public void startParsing() {
        Set<Map.Entry<String, NodeTemplate>> nodeTemplate = parsingResult.getResult().getTopology().getNodeTemplates().entrySet();
        if (nodeTemplate == null) {
            throw new IllegalStateException("Unable to retrieve node Template");
        }
        // Let's retrieve deployment node from fragment instance nodes
        Map<String, String> deploymentNamePerInstanceName = nodeTemplate.parallelStream()
                .filter(stringNodeTemplateEntry -> stringNodeTemplateEntry.getValue().getType().equals("prestocloud.nodes.fragment.faas"))
                .filter(entry -> (entry.getValue() != null))
                .filter(entry -> (entry.getValue().getRelationships().containsKey("execute")))
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().getRelationships().get("execute").getTarget()));
        // Let's retrieve processing node from deployment node
        log.info(" -- {} instances read", deploymentNamePerInstanceName.size());
        Map<String, String> processingNamePerInstanceName = deploymentNamePerInstanceName.entrySet().parallelStream()
                .collect(Collectors.toMap(entree -> entree.getKey(), entree -> parsingResult.getResult().getTopology().getNodeTemplates().get(entree.getValue()).getRelationships().get("host").getTarget()));
        processingNodesPerInstanceName = processingNamePerInstanceName.entrySet().parallelStream()
                .collect(Collectors.toMap(entree -> entree.getKey(), entree -> parsingResult.getResult().getTopology().getNodeTemplates().get(entree.getValue())));
    }

    public void classify() {
        // We pre-process processingNodesPerInstanceName into a list of Tupple containing FragmentName and the type of the fragment
        List<AbstractMap.SimpleImmutableEntry<String, String>> classified = processingNodesPerInstanceName.entrySet()
                .parallelStream()
                .map(entry -> new AbstractMap.SimpleImmutableEntry<>(fragmentIdentifier.matcher(entry.getKey()), entry.getValue().getType()))
                .filter(entry -> entry.getKey().matches())
                .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey().group(1), entry.getValue()))
                .collect(Collectors.toList());
        // We register instance
        classified.stream().map(Map.Entry::getKey).distinct().forEach(frag -> parsedFragment.put(frag, new ParsedFragmentRegistration(frag)));
        // We register edge device
        classified.parallelStream()
                .filter(entry -> entry.getValue().equals("prestocloud.nodes.compute.edge"))
                .forEach(entry -> parsedFragment.get(entry.getKey()).appendEdge());
        // We register cloud resource
        classified.parallelStream()
                .filter(entry -> entry.getValue().startsWith("prestocloud.nodes.compute.cloud."))
                .forEach(entry -> parsedFragment.get(entry.getKey()).appendCloud());
        log.info(" -- {} fragments were acknowledged", parsedFragment.size());
    }

    public String getResult() {
        String structuredCollectedFragment = parsedFragment.values().parallelStream().map(ParsedFragmentRegistration::toString).collect(Collectors.joining(","));
        return String.format("RESULT:%s", structuredCollectedFragment);
    }

    public boolean isThereParsingError() {
        List<ParsingError> parsingerror = parsingResult.getContext().getParsingErrors();
        if (parsingerror.isEmpty()) {
            return false;
        } else {
            parsingerror.stream().forEach(parsingError -> log.error(parsingError.toString()));
            return true;
        }
    }
}
