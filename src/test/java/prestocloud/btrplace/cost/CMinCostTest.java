package prestocloud.btrplace.cost;

import org.btrplace.json.JSONConverterException;
import org.btrplace.json.model.InstanceConverter;
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
import org.junit.Assert;
import org.junit.Test;
import prestocloud.btrplace.PrestoCloudExtensions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unit tests for {@link CMinCost}.
 */
public class CMinCostTest {

  @Test
  public void testDeliverable() throws JSONConverterException, IOException {
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

    // 2 public clouds.
    for (int i = 0; i < 2; i++) {
      final Node no = mo.newNode();
      clouds.add(no);
      mo.getMapping().on(no);
      // Pseudo infinite capacity.
      mem.setCapacity(no, Integer.MAX_VALUE / 100);
      cores.setCapacity(no, Integer.MAX_VALUE / 100);
      storage.setCapacity(no, Integer.MAX_VALUE / 100);
    }

    List<VM> smalls = new ArrayList<>();
    List<VM> cpuHeavy = new ArrayList<>();
    List<VM> regular = new ArrayList<>();
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
        smalls.add(vm);
      } else if (i < 8) {
        // heavy CPU.
        mem.setConsumption(vm, 2);
        cores.setConsumption(vm, 4);
        cpuHeavy.add(vm);
      } else {
        // regular size.
        mem.setConsumption(vm, 4);
        cores.setConsumption(vm, 2);
        regular.add(vm);
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
          // First cloud is 2.1 times more expensive than the second cloud.
          cv.publicHost(no, vm, 2.1 * hourlyCost, d * 100, 1, 1, 1);
        } else {
          // Second cloud is 2 times further than the first cloud.
          cv.publicHost(no, vm, hourlyCost, d * 100, 1, 1, 1);
        }
        d++;
      }
    }

    // Let's deploy all the VMs.
    Instance ii = new Instance(mo, new ArrayList<>(), new MinCost());
    ii.getSatConstraints().addAll(Running.newRunning(vms));

    // Instance serialisation to enable replay.
    final InstanceConverter ic = PrestoCloudExtensions.newInstanceConverter();

    // Scheduler creation and customisation.
    final ChocoScheduler sched = PrestoCloudExtensions.newScheduler();
    sched.doOptimize(true);
    sched.setTimeLimit(5);

    System.out.println("-- Infrastructure --");
    System.out.println("Edge nodes: " + edges);
    System.out.println("cloud nodes: " + clouds);

    System.out.println("\n-- Managed entities --");
    System.out.println("Small VMs: " + smalls + "; hourly cost=" + cv.publicCost(smalls.get(0), clouds.get(0)).hourlyCost());
    System.out.println("cpu heavy VMs: " + cpuHeavy + "; hourly cost=" + cv.publicCost(cpuHeavy.get(0), clouds.get(0)).hourlyCost());
    System.out.println("regular VMs: " + regular + "; hourly cost=" + cv.publicCost(regular.get(0), clouds.get(0)).hourlyCost());


    System.out.println("\n-- Initial deployment --");
    ReconfigurationPlan plan = sched.solve(ii);
    System.out.println(sched.getStatistics());

    System.out.println("Result mapping:");
    System.out.println(plan.getResult().getMapping());

    // According to the cost model, if the cloud is used, the 2nd cloud must be
    // preferred (distance x2 but 2.1 times cheaper).

    // Let scale down. Half the VMs are removed.
    System.out.println("\n-- scale down --");
    final Set<VM> toRemove = vms.stream()
        .filter(v -> v.id() % 2 == 0).collect(Collectors.toSet());
    System.out.println("VMs to remove: " + toRemove);
    ii = new Instance(plan.getResult(), new ArrayList<>(), new MinCost());
    ii.getSatConstraints().addAll(Ready.newReady(toRemove));

    File tmp = File.createTempFile("presto-", ".json");
    ic.toJSON(ii).writeJSONString(Files.newBufferedWriter(tmp.toPath()));
    System.out.println("Instance solved in " + tmp);
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
