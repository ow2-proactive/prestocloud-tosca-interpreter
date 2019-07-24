package prestocloud.btrplace.cost;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.btrplace.model.Instance;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.view.ModelView;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.SchedulerModelingException;
import org.btrplace.scheduler.choco.Parameters;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.ChocoConstraint;
import org.btrplace.scheduler.choco.constraint.mttr.VMPlacementUtils;
import org.btrplace.scheduler.choco.transition.NodeTransition;
import org.btrplace.scheduler.choco.view.CShareableResource;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.FirstFail;
import org.chocosolver.solver.search.strategy.selectors.variables.InputOrder;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;
import org.chocosolver.solver.search.strategy.strategy.StrategiesSequencer;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Choco implementation of {@link MinCost}.
 */
public class CMinCost implements ChocoConstraint {

  private CostView cv;

  public CMinCost(final MinCost obj) {
    // Required by {@link ChocoMapper}.
  }

  @Override
  public Set<VM> getMisPlacedVMs(final Instance i) {
    return Collections.emptySet();
  }

  @Override
  public boolean inject(final Parameters ps, final ReconfigurationProblem rp)
      throws SchedulerException {
    cv = CostView.get(rp.getSourceModel());
    if (cv == null) {
      throw SchedulerModelingException.missingView(rp.getSourceModel(), CostView.ID);
    }

    // We force the nodes to stay online.
    for (final NodeTransition nt : rp.getNodeActions()) {
      try {
        nt.getState().setToTrue(Cause.Null);
      } catch(final ContradictionException ex) {
        throw new SchedulerModelingException(rp.getSourceModel(),
            "Can't force " + nt.getNode() + " to stay online", ex);
      }
    }
    final List<IntVar> costs = new ArrayList<>();

    // Create the objective variable, as the sum of all the VM placement costs.
    int ub = 0;
    for (final VM vm : rp.getFutureRunningVMs()) {

      final IntVar place = rp.getVMAction(vm).getDSlice().getHoster();
      final int[] table = costArray(rp, vm, place);
      int max = IntStream.of(table).max().getAsInt();
      final IntVar cost =
          rp.getModel().intVar(rp.makeVarLabel("cost(", vm,")"), 0, max);
      rp.getModel().element(cost, table, place).post();
      costs.add(cost);
      ub += max;
    }
    final IntVar cost = rp.getModel().intVar(rp.makeVarLabel("K"), 0, ub);
    rp.getModel().sum(costs.toArray(new IntVar[0]), "=", cost).post();
    rp.setObjective(true, cost);
    placementHeuristic(rp, cost);
    return true;
  }

  /**
   * Compute the weight of every VM by multiplying its consumption on every
   * dimension.
   * @param rp the problem.
   * @return the weight per VM.
   */
  private static TObjectIntMap<VM> vmWeight(final ReconfigurationProblem rp) {
    final List<CShareableResource> rcs = rp.getSourceModel().getViews().stream()
        .filter(ShareableResource.class::isInstance)
        .map(ModelView::getIdentifier)
        .map(rp::getView)
        .map(CShareableResource.class::cast)
        .collect(Collectors.toList());
    final TObjectIntMap<VM> costs = new TObjectIntHashMap<>();
    for (final VM vm : rp.getFutureRunningVMs()) {
      costs.putIfAbsent(vm, 1);
      for (final CShareableResource rc : rcs) {
        int cc = costs.get(vm) * rc.getSourceResource().getConsumption(vm);
        costs.put(vm, cc);
      }
    }
    return costs;
  }

  private void placementHeuristic(final ReconfigurationProblem rp, final IntVar cost) {
    Model mo = rp.getSourceModel();

    final TObjectIntMap<VM> costs = vmWeight(rp);

    List<AbstractStrategy<?>> strategies = new ArrayList<>();

    List<IntVar> toBoot = new ArrayList<>();
    List<IntVar> toKeep = new ArrayList<>();

    final Map<IntVar, VM> pla = VMPlacementUtils.makePlacementMap(rp);

    for (final VM vm : rp.getFutureRunningVMs()) {
      if (rp.getSourceModel().getMapping().isRunning(vm)) {
        toKeep.add(rp.getVMAction(vm).getDSlice().getHoster());
      } else {
        toBoot.add(rp.getVMAction(vm).getDSlice().getHoster());
      }
    }

    // Biggest VMs first.
    toBoot.sort((v2, v1) -> costs.get(pla.get(v1)) - costs.get(pla.get(v2)));
    toKeep.sort((v2, v1) -> costs.get(pla.get(v1)) - costs.get(pla.get(v2)));

    strategies.add(
        Search.intVarSearch(
            new FirstFail(rp.getModel()), new LowScoreFirst(rp, pla),
            toBoot.toArray(new IntVar[0])));
    strategies.add(
        Search.intVarSearch(
            new FirstFail(rp.getModel()), new LowScoreFirst(rp, pla),
            toKeep.toArray(new IntVar[0]))
    );

    List<IntVar> ends = rp.getVMActions().stream()
        .map(a -> a.getEnd()).collect(
        Collectors.toList());
    strategies.add(
        new IntStrategy(ends.toArray(new IntVar[0]),
            new InputOrder(rp.getModel()),
            new IntDomainMin()));
    strategies.add(
        new IntStrategy(new IntVar[]{rp.getEnd(), cost},
            new InputOrder(rp.getModel()),
            new IntDomainMin()));
    strategies.add(
        new IntStrategy(new IntVar[]{rp.getEnd(), cost},
            new InputOrder(rp.getModel()),
            new IntDomainMin()));
    rp.getSolver().setSearch(
        new StrategiesSequencer(
            rp.getSolver().getEnvironment(),
            strategies.toArray(new AbstractStrategy[strategies.size()])));
  }

  /**
   * Comput the hosting cost associated to every possible placement for a VM.
   * @param rp the problem.
   * @param vm the VM.
   * @param place the placement variable.
   * @return the cost per possible node.
   */
  private int[] costArray(final ReconfigurationProblem rp, final VM vm,
                          final IntVar place) {

    final int[] cost = new int[rp.getNodes().size()];
    int ub = place.getUB();
    final Node currentHost = rp.getSourceModel().getMapping().getVMLocation(vm);
     for (int i = place.getLB(); i <= ub; i = place.nextValue(i)) {
       final Node no = rp.getNode(i);

       final int cc = cv.get(vm, no, currentHost);
       if (cc < 0) {
         throw new SchedulerModelingException(rp.getSourceModel(),
             "No cost associated to the vm " + vm + "->" + no + " assignment");
       }
       cost[i] = cc;
     }
     return cost;
  }

}
