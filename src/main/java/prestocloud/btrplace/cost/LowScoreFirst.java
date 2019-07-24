package prestocloud.btrplace.cost;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.variables.IntVar;

import java.util.Map;

/**
 * Variable select that picks for each VM, the node leading to the lowest score.
 */
public class LowScoreFirst implements IntValueSelector {

  private final ReconfigurationProblem rp;

  private final CostView cv;

  private final Map<IntVar, VM> pla;

  /**
   * New selector.
   * @param rp the problem to solve.
   * @param pla to map a placement variable to its VM.
   */
  public LowScoreFirst(final ReconfigurationProblem rp, Map<IntVar, VM> pla) {

    this.rp = rp;
    this.pla = pla;
    cv = CostView.get(rp.getSourceModel());
  }

  @Override
  public int selectValue(IntVar v) {

    final VM vm = pla.get(v);
    final Node currentHost = rp.getSourceModel().getMapping().getVMLocation(vm);

    // For each candidate node, we compute the associated cost and keep the
    // node leading to the lowest cost.
    int bestHost = v.getLB();
    int bestScore = Integer.MAX_VALUE;
    for (int nId = v.getLB(); nId <= v.getUB(); nId = v.nextValue(nId)) {
      final Node no = rp.getNode(nId);
      int d = cv.get(vm, no, currentHost);
      if (d < bestScore) {
        bestScore = d;
        bestHost = nId;
      }
      if (bestScore == 0) {
        break;
      }
    }
    return bestHost;
  }
}
