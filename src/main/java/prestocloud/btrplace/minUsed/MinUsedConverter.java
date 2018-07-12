package prestocloud.btrplace.minUsed;

import net.minidev.json.JSONObject;
import org.btrplace.json.JSONConverterException;
import org.btrplace.json.JSONs;
import org.btrplace.json.model.constraint.ConstraintConverter;
import org.btrplace.model.Model;
import org.btrplace.model.Node;

import java.util.HashSet;
import java.util.Set;

public class MinUsedConverter implements ConstraintConverter<MinUsed> {

  public static final String ID = "minUsed";

  public static final String NODES_LABEL = "nodes";

  @Override
  public Class<MinUsed> getSupportedConstraint() {
    return MinUsed.class;
  }

  @Override
  public String getJSONId() {
    return ID;
  }

  @Override
  public MinUsed fromJSON(final Model mo, final JSONObject json)
      throws JSONConverterException {

    checkId(json);
    Set<Node> nodes = new HashSet<>(JSONs.requiredNodes(mo, json, NODES_LABEL));
    return new MinUsed(nodes);
  }

  @Override
  public JSONObject toJSON(final MinUsed cstr) {
    final JSONObject json = new JSONObject();
    json.put(ConstraintConverter.IDENTIFIER, ID);
    json.put(NODES_LABEL, JSONs.nodesToJSON(cstr.nodes()));
    return json;
  }
}
