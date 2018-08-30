package prestocloud.btrplace.minUsed;

import gnu.trove.map.TObjectIntMap;
import org.btrplace.model.Instance;
import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.Parameters;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.Slice;
import org.btrplace.scheduler.choco.constraint.CObjective;
import org.btrplace.scheduler.choco.constraint.mttr.HostingVariableSelector;
import org.btrplace.scheduler.choco.constraint.mttr.MovementGraph;
import org.btrplace.scheduler.choco.constraint.mttr.MyInputOrder;
import org.btrplace.scheduler.choco.constraint.mttr.OnStableNodeFirst;
import org.btrplace.scheduler.choco.constraint.mttr.StartOnLeafNodes;
import org.btrplace.scheduler.choco.constraint.mttr.VMPlacementUtils;
import org.btrplace.scheduler.choco.constraint.mttr.WorstFit;
import org.btrplace.scheduler.choco.constraint.mttr.load.BiggestDimension;
import org.btrplace.scheduler.choco.transition.RelocatableVM;
import org.btrplace.scheduler.choco.transition.Transition;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.btrplace.scheduler.choco.view.CShareableResource;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMax;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.selectors.variables.FirstFail;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;
import org.chocosolver.solver.search.strategy.strategy.StrategiesSequencer;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Choco implementation of {@link MinUsed}.
 * @author Fabien Hermenier
 */
public class CMinUsed implements CObjective {

  private final MinUsed mu;

  private ReconfigurationProblem rp;

  public CMinUsed(final MinUsed mu) {
    this.mu = mu;
  }

  @Override
  public Set<VM> getMisPlacedVMs(final Instance instance) {

    // the candidate misplaced VMs are those running on the nodes to minimise.
    final Mapping map = instance.getModel().getMapping();
    final Set<VM> vms = new HashSet<>();
    for (final Node node : mu.nodes()) {
      vms.addAll(map.getRunningVMs(node));
    }
    return vms;
  }

  @Override
  public void postCostConstraints() {

  }

  @Override
  public boolean inject(final Parameters ps, final ReconfigurationProblem rp) throws SchedulerException {
    this.rp = rp;
    /*
    final IntVar[] cards = new IntVar[mu.nodes().size()];
    final IntVar total = rp.getModel().intVar(rp.makeVarLabel(mu), 0, rp.getSourceModel().getMapping().getNbVMs());
    int i = 0;
    for (final Node node : mu.nodes()) {
      final IntVar card = rp.getNbRunningVMs().get(rp.getNode(node));
      cards[i++] = card;
    }

    final Constraint cstr  = rp.getModel().sum(cards, "=", total);
    cstr.post();
    rp.setObjective(true, total);
    */
    final Mapping now = rp.getSourceModel().getMapping();

    /*
     *
     */
    // One cost per VM.
    List<IntVar> allCosts = new ArrayList<>();
    for (final VM vm: rp.getFutureRunningVMs()) {
      final int[] costs = new int[rp.getNodes().size()];
      if (now.isReady(vm)) {
        // The VM is not running. a 0 cost for an edge. a XX cost for the public cloud.
        for (final Node node : rp.getNodes()) {
          costs[rp.getNode(node)]= mu.nodes().contains(node) ? 10 : 0;
        }
      } else {
        final Node cur = now.getVMLocation(vm);
        if (!mu.nodes().contains(cur)) {
          // currently on the edge.
          // 0 if stays, 1 if on other edge node, XX on public cloud.
          for (final Node node: rp.getNodes()) {
            if (node.equals(cur)) {
              costs[rp.getNode(node)] = 0;
            } else {
              // public cloud: big cost. Edge, small.
              costs[rp.getNode(node)] = mu.nodes().contains(node) ? 10 : 1;
            }
          }
        }
      }
      IntVar myCost = rp.getModel().intVar("cost(" + vm + ")", 0, 10);
      rp.getModel().element(myCost, costs, rp.getVMAction(vm).getDSlice().getHoster()).post();
      allCosts.add(myCost);
    }
    final IntVar total = rp.getModel().intVar(rp.makeVarLabel(mu), 0, Integer.MAX_VALUE / 100);
    final Constraint cstr  = rp.getModel().sum(allCosts.toArray(new IntVar[0]), "=", total);
    cstr.post();
    rp.setObjective(true, total);
    return true;
  }

  private void injectPlacementHeuristic(ReconfigurationProblem rp, Parameters ps, IntVar cost) {

    //TODO : heuristic to rank the nodes. 1st: current node, 2nd: nodes not in minUsed, 3rd: nodes in minUsed.
    List<CShareableResource> rcs = rp.getSourceModel().getViews().stream()
        .filter(v -> v instanceof ShareableResource)
        .map(v -> (CShareableResource) rp.getView(v.getIdentifier()))
        .collect(Collectors.toList());

    Model mo = rp.getSourceModel();
    Mapping map = mo.getMapping();

    OnStableNodeFirst schedHeuristic = new OnStableNodeFirst(rp);

    //Get the VMs to place
    Set<VM> onBadNodes = new HashSet<>(rp.getManageableVMs());

    //Get the VMs that runs and have a pretty low chances to move
    Set<VM> onGoodNodes = map.getRunningVMs(map.getOnlineNodes());
    onGoodNodes.removeAll(onBadNodes);

    List<VMTransition> goodActions = rp.getVMActions(onGoodNodes);
    List<VMTransition> badActions = rp.getVMActions(onBadNodes);

    Solver s = rp.getSolver();

    //Get the VMs to move for exclusion issue
    Set<VM> vmsToExclude = new HashSet<>(rp.getManageableVMs());
    for (Iterator<VM> ite = vmsToExclude.iterator(); ite.hasNext(); ) {
      VM vm = ite.next();
      if (!(map.isRunning(vm) && rp.getFutureRunningVMs().contains(vm))) {
        ite.remove();
      }
    }
    List<AbstractStrategy<?>> strategies = new ArrayList<>();

    Map<IntVar, VM> pla = VMPlacementUtils.makePlacementMap(rp);
    if (!vmsToExclude.isEmpty()) {
      List<VMTransition> actions = new LinkedList<>();
      //Get all the involved slices
      for (VM vm : vmsToExclude) {
        if (rp.getFutureRunningVMs().contains(vm)) {
          actions.add(rp.getVMAction(vm));
        }
      }

      placeVMs(ps, strategies, actions, schedHeuristic, pla);
    }


    TObjectIntMap<VM> costs = CShareableResource.getWeights(rp, rcs);
    badActions.sort((v2, v1) -> costs.get(v1.getVM()) - costs.get(v2.getVM()));
    goodActions.sort((v2, v1) -> costs.get(v1.getVM()) - costs.get(v2.getVM()));
    placeVMs(ps, strategies, badActions, schedHeuristic, pla);
    placeVMs(ps, strategies, goodActions, schedHeuristic, pla);

    //Reinstantations. Try to reinstantiate first
    List<IntVar> migs = new ArrayList<>();
    for (VMTransition t : rp.getVMActions()) {
      if (t instanceof RelocatableVM) {
        migs.add(((RelocatableVM) t).getRelocationMethod());
      }
    }
    strategies.add(
        Search.intVarSearch(
            new FirstFail(rp.getModel()), new IntDomainMax(), migs.toArray(new IntVar[migs.size()]))
    );


    if (!rp.getNodeActions().isEmpty()) {
      //Boot some nodes if needed
      IntVar[] starts = rp.getNodeActions().stream().map(Transition::getStart).toArray(IntVar[]::new);
      strategies.add(new IntStrategy(starts, new FirstFail(rp.getModel()), new IntDomainMin()));
      //Fix the duration. The side effect will be that states will be fixed as well
      //with the objective to not do un-necessary actions
      IntVar[] durations = rp.getNodeActions().stream().map(Transition::getDuration).toArray(IntVar[]::new);
      strategies.add(new IntStrategy(durations, new FirstFail(rp.getModel()), new IntDomainMin()));
    }

    postCostConstraints();
    ///SCHEDULING PROBLEM
    MovementGraph gr = new MovementGraph(rp);
    IntVar[] starts = dSlices(rp.getVMActions()).map(Slice::getStart).filter(v -> !v.isInstantiated()).toArray(IntVar[]::new);
    strategies.add(new IntStrategy(starts, new StartOnLeafNodes(rp, gr), new IntDomainMin()));
    strategies.add(new IntStrategy(schedHeuristic.getScope(), schedHeuristic, new IntDomainMin()));

    IntVar[] ends = rp.getVMActions().stream().map(Transition::getEnd).filter(v -> !v.isInstantiated()).toArray(IntVar[]::new);
    strategies.add(Search.intVarSearch(new MyInputOrder<>(s), new IntDomainMin(), ends));

    //At this stage only it matters to plug the cost constraints
    strategies.add(new IntStrategy(new IntVar[]{rp.getEnd(), cost}, new MyInputOrder<>(s, this), new IntDomainMin()));

    s.setSearch(new StrategiesSequencer(s.getEnvironment(), strategies.toArray(new AbstractStrategy[strategies.size()])));
  }

  /*
   * Try to place the VMs associated on the actions in a random node while trying first to stay on the current node
   */
  private void placeVMs(Parameters ps, List<AbstractStrategy<?>> strategies, List<VMTransition> actions, OnStableNodeFirst schedHeuristic, Map<IntVar, VM> map) {
    IntValueSelector rnd = new WorstFit(map, rp, new BiggestDimension());
    IntVar[] hosts = dSlices(actions).map(Slice::getHoster).filter(v -> !v.isInstantiated()).toArray(IntVar[]::new);
    if (hosts.length > 0) {
      strategies.add(new IntStrategy(hosts, new HostingVariableSelector(rp.getModel(), schedHeuristic), rnd));
    }
  }

  private static Stream<Slice> dSlices(List<VMTransition> l) {
    return l.stream().map(VMTransition::getDSlice).filter(Objects::nonNull);
  }
}
