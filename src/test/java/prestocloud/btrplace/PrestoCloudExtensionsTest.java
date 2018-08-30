package prestocloud.btrplace;

import com.google.common.collect.Sets;
import org.btrplace.model.DefaultModel;
import org.btrplace.model.Instance;
import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Gather;
import org.btrplace.model.constraint.Online;
import org.btrplace.model.constraint.Ready;
import org.btrplace.model.constraint.Running;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.constraint.Spread;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.scheduler.choco.ChocoScheduler;
import org.junit.Assert;
import org.junit.Test;
import prestocloud.btrplace.minUsed.MinUsed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Basic tests.
 */
public class PrestoCloudExtensionsTest {

  @Test
  public void testMinUsed() {
    Model mo = new DefaultModel();
    Mapping map = mo.getMapping();
    // 3 code fragments.
    final VM vm0 = mo.newVM();
    final VM vm1 = mo.newVM();
    final VM vm2 = mo.newVM();

    // edge nodes.
    final Node n0 = mo.newNode();
    final Node n1 = mo.newNode();
    final Node n2 = mo.newNode();
    final Node n3 = mo.newNode();

    // 2 public clouds.
    final Node ec2_1 = mo.newNode();
    final Node ec2_2 = mo.newNode();

    System.out.println("Edge nodes: " + Arrays.asList(n0, n1, n2, n3));
    System.out.println("public clouds: " + Arrays.asList(ec2_1, ec2_2));

    ShareableResource mem = new ShareableResource("memory");
    mo.attach(mem);
    mem.setCapacity(n0, 4);
    mem.setCapacity(n1, 4);
    mem.setCapacity(n2, 4);
    mem.setCapacity(n3, 4);

    mem.setConsumption(vm0, 4);
    mem.setConsumption(vm1, 4);
    mem.setConsumption(vm2, 4);

    // Pseudo infinite capacity for the public clouds.
    mem.setCapacity(ec2_1, Integer.MAX_VALUE / 1000);
    mem.setCapacity(ec2_2, Integer.MAX_VALUE / 1000);

    //Initial placement. VMs waiting for being deployed.

    map.on(n0, n1, n2, n3, ec2_1, ec2_2);
    map.ready(vm0, vm1, vm2);

    final List<SatConstraint> cstrs = new ArrayList<>();
    cstrs.addAll(Running.newRunning(mo.getMapping().getAllVMs()));
    cstrs.addAll(Online.newOnline(mo.getMapping().getAllNodes()));
    //VM 1 & VM2 on distinct location (distinct edge or public clouds etc.)
    cstrs.add(new Spread(Sets.newHashSet(vm1, vm2)));

    Instance ii = new Instance(mo, cstrs, new MinUsed(Sets.newHashSet(ec2_1, ec2_2)));

    final ChocoScheduler sched = PrestoCloudExtensions.newScheduler();
    sched.doOptimize(true);
    ReconfigurationPlan p = sched.solve(ii);
    Assert.assertNotNull(p);

    // All fits on edge nodes.
    System.out.println("-- Initial deployment, everyone fit on the edge nodes");
    System.out.println(p);

    mo = p.getResult();
    // On big VM.
    final VM vm3 = mo.newVM();
    mem = ShareableResource.get(mo, mem.getResourceIdentifier());
    mem.setConsumption(vm3, 32);
    mo.getMapping().ready(vm3);

    // VM3 and VM2 must be co-located ... because.
    cstrs.add(new Gather(Sets.newHashSet(vm3, vm2)));
    cstrs.add(new Running(vm3));
    ii = new Instance(mo, cstrs, new MinUsed(Sets.newHashSet(ec2_1, ec2_2)));
    p = sched.solve(ii);
    Assert.assertNotNull(p);
    System.out.println(" -- with the arrival of the big VM (" + vm3 + "), public clouds must be used. " + vm2 + " follows because of the gather --");
    System.out.println(p);

    // VM3 disappears, VM2 should go back to the edge.
    mo = p.getResult();
    cstrs.remove(new Running(vm3));
    cstrs.add(new Ready(vm3));
    ii = new Instance(mo, cstrs, new MinUsed(Sets.newHashSet(ec2_1, ec2_2)));
    p = sched.solve(ii);
    System.out.println(" -- VM3 disappears everyone go back to the edge --");
    System.out.println(p);
  }
}
