package prestocloud.btrplace.cost;

import net.minidev.json.JSONObject;
import org.btrplace.json.model.constraint.ConstraintConverter;
import org.btrplace.model.Model;

/**
 * JSON serialisation for {@link MinCostConverter}.
 */
public class MinCostConverter implements ConstraintConverter<MinCost> {

  public static final String ID = "minCost";
  @Override
  public Class<MinCost> getSupportedConstraint() {
    return MinCost.class;
  }

  @Override
  public String getJSONId() {
    return ID;
  }

  @Override
  public MinCost fromJSON(final Model mo, final JSONObject o) {
    return new MinCost();
  }

  @Override
  public JSONObject toJSON(final MinCost o) {
    final JSONObject json = new JSONObject();
    json.put(IDENTIFIER, ID);
    return json;
  }
}
