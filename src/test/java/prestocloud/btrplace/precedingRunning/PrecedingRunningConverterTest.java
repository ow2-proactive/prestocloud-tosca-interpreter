package prestocloud.btrplace.precedingRunning;

import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.btrplace.json.JSONConverterException;
import org.btrplace.json.model.InstanceConverter;
import org.btrplace.model.DefaultModel;
import org.btrplace.model.Instance;
import org.btrplace.model.Model;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.MinMigrations;
import org.junit.Assert;
import org.junit.Test;
import prestocloud.btrplace.PrestoCloudExtensions;

import java.util.Set;

public class PrecedingRunningConverterTest {

  @Test
  public void test() throws JSONConverterException {
    final Model mo = new DefaultModel();
    final VM vm = mo.newVM();
    final VM o1 = mo.newVM();
    final VM o2 = mo.newVM();

    mo.getMapping().ready(vm, o1, o2);
    final Set<VM> others = Sets.newLinkedHashSet(o1, o2);
    final PrecedingRunning cstr = new PrecedingRunning(vm, others);
    final PrecedingRunningConverter conv = new PrecedingRunningConverter();
    Assert.assertEquals(cstr, conv.fromJSON(mo, conv.toJSON(cstr)));
  }

  @Test
  public void testIntegration() throws JSONConverterException {
    final Model mo = new DefaultModel();
    final VM vm = mo.newVM();
    final VM o1 = mo.newVM();
    final VM o2 = mo.newVM();

    mo.getMapping().ready(vm, o1, o2);
    final Set<VM> others = Sets.newLinkedHashSet(o1, o2);
    final PrecedingRunning cstr = new PrecedingRunning(vm, others);

    final Instance ii = new Instance(mo, Lists.newArrayList(cstr), new MinMigrations());
    final InstanceConverter conv = PrestoCloudExtensions.newInstanceConverter();
    final String json = conv.toJSONString(ii);
    final Instance i2 = conv.fromJSON(json);
    Assert.assertEquals(ii, i2);
  }
}
