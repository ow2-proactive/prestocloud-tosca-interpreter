package prestocloud.workspace;

import java.util.List;
import java.util.Map;

public class GeneratorSpace {

    private Map<String, String> metadata;
    private List<Object> cloudList;

    public GeneratorSpace() {

    }

    public void configureMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String generate() {
        return "";
    }

    private boolean checkConfiguration() {
        return true;
    }
}
