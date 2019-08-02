package prestocloud.btrplace.precedingRunning;

import org.assertj.core.util.Sets;
import org.btrplace.model.DefaultModel;
import org.btrplace.model.Model;
import org.btrplace.model.VM;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * Unit tests for {@link PrecedingRunning}.
 */
public class PrecedingRunningTest {

  @Test
  public void basicTests() {
    final Model mo = new DefaultModel();
    final VM vm = mo.newVM();
    final VM o1 = mo.newVM();
    final VM o2 = mo.newVM();

    final Set<VM> others = Sets.newLinkedHashSet(o1, o2);
    final PrecedingRunning cstr = new PrecedingRunning(vm, others);
    Assert.assertEquals(vm, cstr.vm());
    Assert.assertEquals(others, cstr.parents());
    Assert.assertEquals(Sets.newLinkedHashSet(vm, o1, o2), cstr.getInvolvedVMs());
    Assert.assertEquals(Sets.newHashSet(), cstr.getInvolvedNodes());

    Assert.assertFalse(cstr.setContinuous(true));
    Assert.assertTrue(cstr.setContinuous(false));
    Assert.assertFalse(cstr.isContinuous());

    Assert.assertEquals(cstr, cstr);
    Assert.assertEquals(cstr, new PrecedingRunning(vm, others));
    Assert.assertNotEquals(cstr, new PrecedingRunning(vm, Sets.newLinkedHashSet(o1)));
  }
}
