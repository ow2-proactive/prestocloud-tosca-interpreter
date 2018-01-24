package prestocloud.tosca.parser;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KeyValueMappingTarget extends MappingTarget {
    private String keyPath;

    public KeyValueMappingTarget(String keyPath, String path, INodeParser<?> parser) {
        super(path, parser);
        this.keyPath = keyPath;
    }
}