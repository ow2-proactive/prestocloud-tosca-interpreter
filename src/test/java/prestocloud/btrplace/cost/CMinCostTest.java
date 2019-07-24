package prestocloud.btrplace.cost;

import org.btrplace.model.DefaultModel;
import org.btrplace.model.Instance;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Ready;
import org.btrplace.model.constraint.Running;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.scheduler.choco.ChocoScheduler;
import org.btrplace.scheduler.choco.DefaultChocoScheduler;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unit tests for {@link CMinCost}.
 */
public class CMinCostTest {

  @Test
  public void testDeliverable() {
    final Model mo = new DefaultModel();
    final CostView cv = new CostView();
    final ShareableResource mem = new ShareableResource("memory");
    final ShareableResource cores = new ShareableResource("cores");
    mo.attach(cv);
    mo.attach(mem);
    mo.attach(cores);

    final List<Node> edges = new ArrayList<>();
    final List<Node> clouds = new ArrayList<>();
    final List<VM> vms = new ArrayList<>();

    // 5 edge nodes, 8 GB RAM, 4 cores.
    for (int i = 0; i < 5; i++) {
      final Node no = mo.newNode();
      mo.getMapping().on(no);
      edges.add(no);
      cv.edgeHost(no);
      mem.setCapacity(no, 8);
      cores.setCapacity(no, 4);
    }

    // 4 public clouds.
    for (int i = 0; i < 4; i++) {
      final Node no = mo.newNode();
      clouds.add(no);
      mo.getMapping().on(no);
      // Pseudo infinite capacity.
      mem.setCapacity(no, Integer.MAX_VALUE / 100);
      cores.setCapacity(no, Integer.MAX_VALUE / 100);
    }

    // 10 VMs with different sizes.
    for (int i = 0; i < 10; i++) {
      final VM vm = mo.newVM();
      vms.add(vm);

      if (i < 3) {
        // small VMs.
        mem.setConsumption(vm, 2);
        cores.setConsumption(vm, 1);
      } else if (i < 8) {
        // heavy CPU.
        mem.setConsumption(vm, 2);
        cores.setConsumption(vm, 4);
      } else {
        // regular size.
        mem.setConsumption(vm, 4);
        cores.setConsumption(vm, 2);
      }
      mo.getMapping().addReadyVM(vm);
    }

    // The cost associated to every VM.
    for (final VM vm : vms) {
      int d = 1;
      for (final Node no : clouds) {
        double hourlyCost = mem.getConsumption(vm) * cores.getConsumption(vm);
        cv.publicHost(no, vm, hourlyCost, d * 100, 1, 1, 1);
        d++;
      }
    }

    // Let's deploy all the VMs.
    Instance ii = new Instance(mo, new ArrayList<>(), new MinCost());
    ii.getSatConstraints().addAll(Running.newRunning(vms));

    // the scheduler.
    final ChocoScheduler sched = new DefaultChocoScheduler();
    sched.getParameters().getMapper().mapConstraint(MinCost.class, CMinCost.class);
    sched.doOptimize(true);
    sched.setTimeLimit(5);

    System.out.println("Edge nodes: " + edges);
    System.out.println("cloud nodes: " + clouds);

    System.out.println("Initial deployment:");
    ReconfigurationPlan plan = sched.solve(ii);
    System.out.println(sched.getStatistics());
    //System.out.println(plan);

    System.out.println("Result mapping:");
    System.out.println(plan.getResult().getMapping());


    // Let scale down. Half the VMs are removed.
    System.out.println("------ scale down ---");
    final Set<VM> toRemove = vms.stream()
        .filter(v -> v.id() % 2 == 0).collect(Collectors.toSet());
    System.out.println("VMs to remove: " + toRemove);
    ii = new Instance(plan.getResult(), new ArrayList<>(), new MinCost());
    ii.getSatConstraints().addAll(Ready.newReady(toRemove));

    plan = sched.solve(ii);
    System.out.println(sched.getStatistics());
    //System.out.println(plan);

    System.out.println("Result mapping:");
    System.out.println(plan.getResult().getMapping());

    // No more VMs in the cloud.
    for (final Node no: clouds) {
      Assert.assertTrue(plan.getResult().getMapping().getRunningVMs(no).isEmpty());
    }
  }
}
