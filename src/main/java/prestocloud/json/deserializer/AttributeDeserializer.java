package prestocloud.json.deserializer;

import org.prestocloud.tosca.model.definitions.AttributeDefinition;
import org.prestocloud.tosca.model.definitions.ConcatPropertyValue;
import org.prestocloud.tosca.model.definitions.FunctionPropertyValue;
import org.prestocloud.tosca.model.definitions.IValue;

/**
 * Custom deserializer to handle multiple AttributeValue types
 */
public class AttributeDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<IValue> {
    public AttributeDeserializer() {
        super(IValue.class);
        addToRegistry("type", AttributeDefinition.class);
        addToRegistry("function", FunctionPropertyValue.class);
        addToRegistry("function_concat", ConcatPropertyValue.class);
    }
}