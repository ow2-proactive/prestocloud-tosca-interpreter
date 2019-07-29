package prestocloud.btrplace.cost;

import com.google.common.collect.Sets;
import org.btrplace.model.DefaultModel;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link CostView}.
 */
public class CostViewTest {

  @Test
  public void basicTesting() {
    final Model mo = new DefaultModel();
    final CostView cv = new CostView();

    final VM vm1 = mo.newVM();
    final VM vm2 = mo.newVM();

    final Node n1 = mo.newNode();
    final Node n2 = mo.newNode();
    final Node n3 = mo.newNode();

    Assert.assertTrue(cv.get(vm1, n1, null) < 0);

    Assert.assertTrue(cv.publicHost(n1, vm1, 0.1, 10, 2, 3, 4));
    Assert.assertTrue(cv.get(vm1, n1, null) > 0);
    Assert.assertTrue(cv.get(vm1, n2, null) < 0);
    Assert.assertTrue(cv.get(vm1, n1, null) > 0);

    Assert.assertFalse(cv.edgeHost(n1));
    Assert.assertTrue(cv.edgeHost(n2));
    Assert.assertTrue(cv.isEdge(n2));
    Assert.assertNull(cv.publicCost(vm1, n1));

    Assert.assertTrue(cv.publicHost(n3, vm1, 0.5, 1, 2, 3, 4));
    final Cost cc  = cv.publicCost(vm1, n3);
    Assert.assertEquals(1, cc.distance());
    Assert.assertEquals(2, cc.affinity());
    Assert.assertEquals(3, cc.alpha());
    Assert.assertEquals(4, cc.beta());
    Assert.assertEquals(0.1, cv.minHostingCost(), 1E-6);
    Assert.assertEquals(1, cv.minDistance());

    Assert.assertTrue(cv.publicHost(n3, vm1, 0.5, 1, 0, 3, 4));
    Assert.assertEquals(Integer.MAX_VALUE, cv.get(vm1, n3, null));

    Assert.assertEquals(Sets.newHashSet(vm1), cv.registeredVMs());
  }

  @Test
  public void testCopy() {
    final Model mo = new DefaultModel();
    final CostView cv = new CostView();

    final VM vm1 = mo.newVM();
    final VM vm2 = mo.newVM();

    final Node n1 = mo.newNode();
    final Node n2 = mo.newNode();
    cv.publicHost(n1, vm1, 0.1, 1, 2, 3, 4);
    cv.publicHost(n2, vm1, 0.1, 10, 2, 3, 4);
    cv.publicHost(n1, vm2, 0.2, 3, 2, 3, 4);
    cv.publicHost(n2, vm2, 0.2, 7, 2, 3, 4);

    final CostView cv2 = cv.copy();
    Assert.assertEquals(cv, cv2);
    cv2.publicHost(n1, vm1, 0.7, 1, 2, 3, 4);
    Assert.assertNotEquals(cv, cv2);
    cv2.publicHost(n1, vm1, 0.1, 1, 2, 3, 4);
    Assert.assertEquals(cv, cv2);
  }
}
