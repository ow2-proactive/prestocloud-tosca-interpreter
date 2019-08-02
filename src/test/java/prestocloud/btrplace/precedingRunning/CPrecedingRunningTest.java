package prestocloud.btrplace.precedingRunning;

import com.google.common.collect.Sets;
import org.btrplace.model.DefaultModel;
import org.btrplace.model.Instance;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.MinMigrations;
import org.btrplace.model.constraint.Running;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.BootVM;
import org.btrplace.scheduler.choco.ChocoScheduler;
import org.junit.Assert;
import org.junit.Test;
import prestocloud.btrplace.PrestoCloudExtensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@link CPrecedingRunning}.
 */
public class CPrecedingRunningTest {

  @Test
  public void basicTest() {
    final Model mo = new DefaultModel();
    final Node n0 = mo.newNode();
    final VM vm0 = mo.newVM();
    final VM vm1 = mo.newVM();
    final VM vm2 = mo.newVM();

    mo.getMapping().on(n0).ready(vm0, vm1, vm2);
    final List<SatConstraint> cstrs = new ArrayList<>();
    cstrs.add(new PrecedingRunning(vm0, Sets.newHashSet(vm1, vm2)));
    cstrs.addAll(Running.newRunning(mo.getMapping().getAllVMs()));
    final Instance ii =
        new Instance(mo, cstrs, new MinMigrations());

    mo.getAttributes().put(vm1, "boot", 5);
    mo.getAttributes().put(vm2, "boot", 7);
    final ChocoScheduler sched = PrestoCloudExtensions.newScheduler();
    ReconfigurationPlan plan = sched.solve(ii);
    System.out.println(plan);
    Optional<BootVM> aa = plan.getActions().stream()
        .filter(BootVM.class::isInstance)
        .map(BootVM.class::cast).filter(b -> b.getVM().equals(vm0)).findFirst();
    Assert.assertTrue(aa.isPresent());
    Assert.assertEquals(5, aa.get().getStart());
    Assert.assertEquals(6, aa.get().getEnd());

    // Let's entail the constraint.
    mo.getMapping().run(n0, vm1);
    plan = sched.solve(ii);
    // A duration of 1: vm0 is booted at time 7 as vm1 is already running.
    aa = plan.getActions().stream()
        .filter(BootVM.class::isInstance)
        .map(BootVM.class::cast).filter(b -> b.getVM().equals(vm0)).findFirst();
    Assert.assertEquals(0, aa.get().getStart());
  }


}
