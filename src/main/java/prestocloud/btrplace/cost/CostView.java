package prestocloud.btrplace.cost;

import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.view.ModelView;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A view to be used to state to cost for each possible VM to node assignment.
 */
public class CostView implements ModelView {

  private static final int PRECISION = 100;
  /**
   * View identifier.
   */
  public static final String ID = "costView";

  /**
   * The cost associated to every VM to a public cloud assignment.
   */
  private final Map<Node, Map<VM, Cost>> costs;

  /**
   * The edge nodes.
   */
  private final Set<Node> edges;

  /**
   * The penalty to penalise VM movement inside the edge and prefer to stay
   * on the edge node if it fits.
   */
  private static final int MOBILITY_PENALTY = 10;

  // Cache for the min values.
  private double minCost = -1;

  private int minDistance = -1;

  /**
   * New view.
   */
  public CostView() {
    costs = new HashMap<>();
    edges = new HashSet<>();
  }

  /**
   * Get the view identifier.
   * @return {@link #ID}.
   */
  @Override
  public String getIdentifier() {
    return ID;
  }

  @Override
  public boolean substituteVM(final VM curId, final VM nextId) {
    return true;
  }

  @Override
  public CostView copy() {
    final CostView cv = new CostView();
    costs.forEach((no, map) -> {
      map.forEach((vm, co) -> {
        cv.publicHost(no, vm, co.hourlyCost(), co.distance(), co.affinity(),
            co.alpha(), co.beta());
      });
    });
    edges.forEach(cv::edgeHost);
    return cv;
  }

  /**
   * Declare a new public cloud.
   * @param no the node representing the public cloud
   * @param vm the VM that can go on that cloud
   * @param hourlyCost the hourly hosting cost for the VM in euros
   * @param distance the distance to the public cloud in km (> 0).
   * @param affinity the affinity for that cloud.
   * @param alpha the distance weighting
   * @param beta the cost weighting
   * @return {@code true} if the host is registered. {@code false} is the host
   * was previously declared as an edge node.
   */
  public boolean publicHost(final Node no, final VM vm,
                            final double hourlyCost, final int distance, final int affinity,
                            final int alpha, final int beta) {
    if (isEdge(no)) {
      return false;
    }

    // Cache invalidation.
    minCost = -1;
    minDistance = -1;

    final Cost cc = new Cost(hourlyCost, distance, affinity, alpha, beta);
    costs.putIfAbsent(no, new HashMap<>());
    costs.get(no).put(vm, cc);
    return true;
  }

  /**
   * State if a given node is in the edge.
   * @param no the node
   * @return {@code true} iff the node is in the edge.
   */
  public boolean isEdge(final Node no) {
    return edges.contains(no);
  }

  public Set<Node> edgeHosts() {
    return Collections.unmodifiableSet(edges);
  }

  public Set<Node> cloudHosts() {
    return Collections.unmodifiableSet(costs.keySet());
  }

  /**
   * State if a given node is a public cloud.
   * @param no the node
   * @return {@code true} iff the node is a public cloud.
   */
  public boolean isPublicCloud(final Node no) {
    return costs.containsKey(no);
  }

  /**
   * Declare an edge node.
   * @param no the node
   * @return {@code true} iff the declaration succeeded. {@code false} if the
   * node is already know for being a public host.
   */
  public boolean edgeHost(final Node no) {
    if (costs.containsKey(no)){
      return false;
    }
    edges.add(no);
    return true;
  }

  /**
   * Get the cost associated to a given VM and node.
   * @param vm the VM
   * @param to the destination node
   * @param from the current VM placement. May be {@code null}.
   * @return the associated cost. May be {@code < 0} if unregistered. If it
   * equals to {@code Integer#MAX_VALUE}, this means the destination node is
   * not a candidate.
   */
  public int get(final VM vm, final Node to, final Node from) {
    if (isEdge(to)) {
      if (from == null || to.equals(from)) {
        return 0;
      }
      return mobilityPenalty();
    } else if (!costs.containsKey(to)) {
      return -1;
    }

    final Cost c = costs.get(to).get(vm);
    if (c == null) {
      return -1;
    }
    return (int) (PRECISION * mobilityPenalty() * c.compute(minDistance(), minHostingCost()));
  }

  /**
   * Get the cost model parameters for a given VM to be assigned to a given
   * cloud.
   * @param vm the VM
   * @param to the cloud
   * @return the cost model parameters if declared earlier using {@link #publicHost(Node, VM, double, int, int, int, int)}.
   * {@code null} otherwise
   */
  public Cost publicCost(final VM vm, final Node to) {
    return costs.getOrDefault(to, new HashMap<>()).get(vm);
  }

  /**
   * Get all the known VMs.
   * @return a set of VMs.
   */
  public Set<VM> registeredVMs() {
    final Set<VM> vms = new HashSet<>();
    costs.forEach((no, cc) -> {
      vms.addAll(cc.keySet());
    });
    return vms;
  }

  /**
   * Get the view in a model.
   * @param mo the model to analyse.
   * @return the view if exists. {@code null} otherwise.
   */
  public static CostView get(final Model mo) {
    return (CostView) mo.getView(ID);
  }

  /**
   * Return the minimum hosting cost.
   * @return an hourly cost. {@code -1} iff no host is declared.
   */
  public double minHostingCost() {
    if (costs.isEmpty()) {
      return -1;
    }
    if (minCost >= 0) {
      return minCost;
    }
    cacheMinValues();
    return minCost;
  }

  /**
   * Cache the minimum distance and hosting costs.
   */
  private void cacheMinValues() {
    minDistance = Integer.MAX_VALUE;
    minCost = Double.MAX_VALUE;
    for (Map<VM, Cost> e : costs.values()) {
      for (Map.Entry<VM, Cost> ee : e.entrySet()) {
        minDistance = Math.min(minDistance, ee.getValue().distance());
        minCost = Math.min(minCost, ee.getValue().hourlyCost());
      }
    }
  }

  /**
   * Return the minimum hosting distance
   * @return a distance in kilometer. {@code -1} iff no host is declared.
   */
  public int minDistance() {
    if (costs.isEmpty()) {
      return -1;
    }
    if (minDistance >= 0) {
      return minDistance;
    }

    cacheMinValues();
    return minDistance;
  }

  /**
   * Get the penalty associated to a movement.
   * @return a positive value.
   */
  public int mobilityPenalty() {
    return MOBILITY_PENALTY;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final CostView costView = (CostView) o;
    return Objects.equals(costs, costView.costs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(costs);
  }

  @Override
  public String toString() {
    return "CostView{costs=" + costs + '}';
  }
}
