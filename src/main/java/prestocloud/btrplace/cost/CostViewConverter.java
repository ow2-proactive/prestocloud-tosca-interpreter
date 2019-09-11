package prestocloud.btrplace.cost;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.btrplace.json.JSONConverterException;
import org.btrplace.json.JSONs;
import org.btrplace.json.model.view.ModelViewConverter;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;

import java.util.List;

/**
 * JSON serialisation of {@link CostView}.
 */
public class CostViewConverter implements ModelViewConverter<CostView> {

  /**
   * The view identifier.
   */
  public static final String ID = "costView";

  public static final String EDGES_NODES = "edges";

  public static final String PUBLIC_NODES = "public";

  public static final String VM_LABEL = "vm";

  public static final String VMS_LABEL = "vms";

  public static final String NODE_LABEL = "node";

  public static final int DISTANCE_IDX = 0;

  public static final int HOURLY_COST_IDX = 1;

  public static final int AFFINITY_IDX = 2;

  public static final int ALPHA_IDX = 3;

  public static final int BETA_IDX = 4;

  private static final String COSTS_LABEL = "costs";

  @Override
  public Class<CostView> getSupportedView() {
    return CostView.class;
  }

  @Override
  public String getJSONId() {
    return ID;
  }

  @Override
  public CostView fromJSON(final Model mo, final JSONObject json)
      throws JSONConverterException {

    final CostView cv = new CostView();
    final List<Node> edges = JSONs.requiredNodes(mo, json, EDGES_NODES);
    for (final Node no : edges) {
      cv.edgeHost(no);
    }
    final JSONArray pubs = (JSONArray) json.get(PUBLIC_NODES);
    for (final Object ob : pubs) {
      final JSONObject nodeLevel = (JSONObject) ob;
      final Node no = JSONs.requiredNode(mo, nodeLevel, NODE_LABEL);
      for (final Object ob2 : (JSONArray) nodeLevel.get(VMS_LABEL)) {
        final JSONObject jsonCost = (JSONObject) ob2;
        final VM vm = JSONs.requiredVM(mo, jsonCost, VM_LABEL);
        final JSONArray arr = (JSONArray) jsonCost.get(COSTS_LABEL);
        final int distance = (int) arr.get(DISTANCE_IDX);
        final double hourlyCost = (double)arr.get(HOURLY_COST_IDX);
        final int affinity = (int)arr.get(AFFINITY_IDX);
        final int alpha = (int)arr.get(ALPHA_IDX);
        final int beta = (int)arr.get(BETA_IDX);
        cv.publicHost(no, vm, hourlyCost, distance, affinity, alpha, beta);
      }
    }
    return cv;
  }

  @Override
  public JSONObject toJSON(final CostView co) {
    final JSONObject json = new JSONObject();
    json.put(IDENTIFIER, ID);
    json.put(EDGES_NODES, JSONs.nodesToJSON(co.edgeHosts()));
    final JSONArray pub = new JSONArray();
    json.put(PUBLIC_NODES, pub);
    for (final Node no : co.cloudHosts()) {
      final JSONObject nodeLevel = new JSONObject();
      nodeLevel.put(NODE_LABEL, JSONs.elementToJSON(no));
      final JSONArray vms = new JSONArray();
      nodeLevel.put(VMS_LABEL, vms);
      pub.add(nodeLevel);
      for (final VM vm : co.registeredVMs()) {
        final Cost cc = co.publicCost(vm, no);
        final JSONArray values = new JSONArray();
        values.add(cc.distance());
        values.add(cc.hourlyCost());
        values.add(cc.affinity());
        values.add(cc.alpha());
        values.add(cc.beta());
        JSONObject jsonCost = new JSONObject();
        jsonCost.put(VM_LABEL, JSONs.elementToJSON(vm));
        jsonCost.put(COSTS_LABEL, values);
        vms.add(jsonCost);
      }
    }
    return json;
  }
}
