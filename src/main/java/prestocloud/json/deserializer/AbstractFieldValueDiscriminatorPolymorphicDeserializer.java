package prestocloud.json.deserializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

/**
 * Manages polymorphism deserialization for Jackson through discriminator property (based on property value).
 */
public class AbstractFieldValueDiscriminatorPolymorphicDeserializer<T> extends StdDeserializer<T> {
    private Map<String, Class<? extends T>> registry = Maps.newHashMap();

    private String propertyDiscriminatorName;

    public AbstractFieldValueDiscriminatorPolymorphicDeserializer(String fieldDiscriminatorName, Class<T> clazz) {
        super(clazz);
        this.propertyDiscriminatorName = fieldDiscriminatorName;
    }

    protected void addToRegistry(String discriminator, Class<? extends T> clazz) {
        registry.put(discriminator, clazz);
    }

    @Override
    public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        ObjectNode root = mapper.readTree(jp);
        return deserializeAfterRead(jp, ctxt, mapper, root);
    }

    protected T deserializeAfterRead(JsonParser jp, DeserializationContext ctxt, ObjectMapper mapper, ObjectNode root) throws JsonProcessingException {
        Class<? extends T> parameterClass = null;
        Iterator<Map.Entry<String, JsonNode>> elementsIterator = root.fields();
        while (elementsIterator.hasNext()) {
            Map.Entry<String, JsonNode> element = elementsIterator.next();
            String name = element.getKey();
            if (propertyDiscriminatorName.equals(name)) {
                String value = element.getValue().asText();
                if (registry.containsKey(value)) {
                    parameterClass = registry.get(value);
                    break;
                }
            }
        }
        if (parameterClass == null) {
            return null;
        }
        return mapper.treeToValue(root, parameterClass);
    }

    protected void failedToFindImplementation(JsonParser jp, ObjectNode root) throws JsonParseException {
        throw new JsonParseException("Failed to find implementation for node " + root + " from registry " + registry, jp.getCurrentLocation());
    }
}