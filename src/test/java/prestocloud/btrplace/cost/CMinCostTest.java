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

    // The view to declare the scores.
    final CostView cv = new CostView();

    // 3 dimensions for 3 resources.
    final ShareableResource mem = new ShareableResource("memory");
    final ShareableResource cores = new ShareableResource("cores");
    final ShareableResource storage = new ShareableResource("storage");
    mo.attach(cv);
    mo.attach(mem);
    mo.attach(cores);
    mo.attach(storage);

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
      storage.setCapacity(no, 100);
    }

    // 4 public clouds.
    for (int i = 0; i < 4; i++) {
      final Node no = mo.newNode();
      clouds.add(no);
      mo.getMapping().on(no);
      // Pseudo infinite capacity.
      mem.setCapacity(no, Integer.MAX_VALUE / 100);
      cores.setCapacity(no, Integer.MAX_VALUE / 100);
      storage.setCapacity(no, Integer.MAX_VALUE / 100);
    }

    // 10 VMs with different sizes. They represent the fragment replicas but
    // also the proxies.
    // IMHO: the proxies should stay in the edge to save bandwidth and latency
    // as we naturally tend to prefer hosting the fragments in the edge, so
    // with a proxy on a cloud, there will be plenty of unnecessary round trips.
    for (int i = 0; i < 10; i++) {
      final VM vm = mo.newVM();
      vms.add(vm);

      // We size the VMs according to there resource requirements. For the edge,
      // this is required to ensure that we will not saturate the host. For
      // a cloud hosting, the values are only used to compute the hourly hosting
      // costs. So they are not really used as a cloud as a pseudo-infinite
      // hosting capacity.
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
      storage.setConsumption(vm, 4);
      mo.getMapping().addReadyVM(vm);
    }

    // The cost associated to every VM.
    for (final VM vm : vms) {
      int d = 1;
      for (final Node no : clouds) {
        double hourlyCost = mem.getConsumption(vm) * cores.getConsumption(vm);
        if (d == 1) {
          cv.publicHost(no, vm, hourlyCost, d * 100, 0, 1, 1);
        } else {
          cv.publicHost(no, vm, hourlyCost, d * 100, 1, 1, 1);
        }

        d++;
      }
    }

    // Let's deploy all the VMs.
    Instance ii = new Instance(mo, new ArrayList<>(), new MinCost());
    ii.getSatConstraints().addAll(Running.newRunning(vms));

    // TODO: If some VMs
    // Scheduler creation and customisation.
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
    System.out.println(plan);

    System.out.println("Result mapping:");
    System.out.println(plan.getResult().getMapping());

    // No more VMs in the cloud.
    for (final Node no: clouds) {
      Assert.assertTrue(plan.getResult().getMapping().getRunningVMs(no).isEmpty());
    }
  }
}
