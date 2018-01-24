package prestocloud.json.deserializer;

import com.fasterxml.jackson.databind.node.JsonNodeType;

import org.prestocloud.tosca.model.definitions.ComplexPropertyValue;
import org.prestocloud.tosca.model.definitions.ConcatPropertyValue;
import org.prestocloud.tosca.model.definitions.FunctionPropertyValue;
import org.prestocloud.tosca.model.definitions.IValue;
import org.prestocloud.tosca.model.definitions.ListPropertyValue;
import org.prestocloud.tosca.model.definitions.PropertyDefinition;
import org.prestocloud.tosca.model.definitions.ScalarPropertyValue;

/**
 * Custom deserializer to handle multiple operation parameters types.
 */
public class OperationParameterDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<IValue> {
    public OperationParameterDeserializer() {
        super(IValue.class);
        addToRegistry("type", PropertyDefinition.class);
        addToRegistry("function", FunctionPropertyValue.class);
        addToRegistry("function_concat", ConcatPropertyValue.class);
        addToRegistry("value", JsonNodeType.STRING.toString(), ScalarPropertyValue.class);
        addToRegistry("value", JsonNodeType.ARRAY.toString(), ListPropertyValue.class);
        addToRegistry("value", JsonNodeType.OBJECT.toString(), ComplexPropertyValue.class);
        setValueStringClass(ScalarPropertyValue.class);
    }
}