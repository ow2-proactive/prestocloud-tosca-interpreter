package prestocloud.btrplace.cost;

import net.minidev.json.JSONObject;
import org.btrplace.json.JSONConverterException;
import org.btrplace.json.model.InstanceConverter;
import org.btrplace.model.DefaultModel;
import org.btrplace.model.Instance;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.MinMigrations;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Unit tests for {@link CostViewConverter}.
 */
public class CostViewConverterTest {

  @Test
  public void testBasic() throws JSONConverterException {
    final Model mo = new DefaultModel();
    final CostView cv = new CostView();
    final Node e1 = mo.newNode();
    final Node e2 = mo.newNode();
    cv.edgeHost(e1);
    cv.edgeHost(e2);
    mo.attach(cv);

    final Node p1 = mo.newNode();
    final Node p2 = mo.newNode();
    final VM vm1 = mo.newVM();
    final VM vm2 = mo.newVM();
    mo.getMapping().on(e1, e2, p1, p2)
        .ready(vm1, vm2);
    cv.publicHost(p1, vm1, 0.1, 2, 3, 4, 5);
    cv.publicHost(p1, vm2, 0.6, 7, 8, 9, 10);
    cv.publicHost(p2, vm1, 0.11, 12, 13, 14, 15);
    cv.publicHost(p2, vm2, 0.16, 17, 18, 19, 20);
    final CostViewConverter conv = new CostViewConverter();
    final JSONObject json = conv.toJSON(cv);
    Assert.assertEquals(cv, conv.fromJSON(mo, json));
    final Instance ii = new Instance(mo, new ArrayList<>(), new MinMigrations());

    final InstanceConverter ic = new InstanceConverter();
    ic.getModelConverter().getViewsConverter().register(new CostViewConverter());
    final String buf = ic.toJSON(ii).toString();
    final Instance i2 = ic.fromJSON(buf);
    Assert.assertEquals(i2, ii);
  }
}
