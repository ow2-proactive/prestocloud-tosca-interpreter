package prestocloud.btrplace.cost;

import net.minidev.json.JSONObject;
import org.btrplace.model.DefaultModel;
import org.btrplace.model.Model;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link MinCostConverter}.
 */
public class MinCostConverterTest {

  @Test
  public void testBasic() {
    final Model mo = new DefaultModel();
    final MinCost mc = new MinCost();
    final MinCostConverter conv = new MinCostConverter();
    final JSONObject json = conv.toJSON(mc);
    Assert.assertEquals(mc, conv.fromJSON(mo, json));
  }
}
