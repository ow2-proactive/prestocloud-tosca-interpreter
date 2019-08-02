package prestocloud.btrplace.precedingRunning;

import com.google.common.collect.Sets;
import org.btrplace.model.Instance;
import org.btrplace.model.Model;
import org.btrplace.model.VM;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.Parameters;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.ChocoConstraint;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Choco implementation of {@link PrecedingRunning}.
 */
public class CPrecedingRunning implements ChocoConstraint {

  private final PrecedingRunning cstr;

  public CPrecedingRunning(final PrecedingRunning cstr) {
    this.cstr = cstr;
  }

  @Override
  public Set<VM> getMisPlacedVMs(final Instance i) {
    if (!i.getModel().getMapping().isRunning(cstr.vm())) {
      return Sets.newHashSet(cstr.vm());
    }
    return Collections.emptySet();
  }

  @Override
  public boolean inject(final Parameters ps, final ReconfigurationProblem rp)
      throws SchedulerException {

    final Set<VM> parents = cstr.parents();
    final Model now = rp.getSourceModel();

    if (now.getMapping().isRunning(cstr.vm())) {
      // The VM is already running. the constraint is satisfied.
      return true;
    }
    for (final VM vm : parents) {
      if (now.getMapping().isRunning(vm)) {
        // At least one parent is already running, the constraint is satisfied.
        return true;
      }
    }

    // For every parent VM that are ready to be running (so deployed), we state
    // that the pending VM will necessarily starts after the first parent VM
    // is effectively running. So the start time must be >= the minimum end of
    // the booting time for the parent VMs.
    final List<IntVar> ends = new ArrayList<>();
    final IntVar startAt = rp.getVMAction(cstr.vm()).getStart();
    for (final VM vm : parents) {
      if (now.getMapping().isReady(vm) && rp.getFutureRunningVMs().contains(vm)) {
        ends.add(rp.getVMAction(vm).getEnd());
      }
    }
    if (ends.isEmpty()) {
      // No parent will be running.
      return true;
    }
    final IntVar earliest = rp.makeUnboundedDuration("earliest(", ends, ")");
    rp.getModel().min(earliest, ends.toArray(new IntVar[0])).post();
    rp.getModel().arithm(startAt,">=", earliest).post();
    return true;
  }
}

