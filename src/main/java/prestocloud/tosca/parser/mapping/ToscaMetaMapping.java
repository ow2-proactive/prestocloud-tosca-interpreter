package prestocloud.tosca.parser.mapping;

import org.springframework.stereotype.Component;

import prestocloud.tosca.model.ToscaMeta;
import prestocloud.tosca.parser.MappingTarget;
import prestocloud.tosca.parser.impl.base.TypeNodeParser;

@Component
public class ToscaMetaMapping extends AbstractMapper<ToscaMeta> {
    public ToscaMetaMapping() {
        super(new TypeNodeParser<ToscaMeta>(ToscaMeta.class, "Tosca Archive meta file."));
    }

    @Override
    public void initMapping() {
        instance.getYamlToObjectMapping().put("TOSCA-Meta-File-Version", new MappingTarget("", getScalarParser()));
        instance.getYamlToObjectMapping().put("CSAR-Version", new MappingTarget("version", getScalarParser()));
        instance.getYamlToObjectMapping().put("Created-By", new MappingTarget("createdBy", getScalarParser()));
        instance.getYamlToObjectMapping().put("Entry-Definitions", new MappingTarget("entryDefinitions", getScalarParser()));
        instance.getYamlToObjectMapping().put("Name", new MappingTarget("name", getScalarParser()));
        instance.getYamlToObjectMapping().put("Content-Type", new MappingTarget("", getScalarParser()));
    }
}