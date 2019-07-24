package prestocloud.btrplace.cost;

import java.util.Objects;

/**
 * Container stating a cost for an assignment.
 */
public class Cost {

  /**
   * The distance to the host in kilometers in euros.
   */
  private final int distance;

  /**
   * The hourly hosting cost.
   */
  private final double cost;

  /**
   * The host affinity.
   */
  private final int affinity;

  /**
   * Distance weighting.
   */
  private final int alpha;

  /**
   * Cost weighting.
   */
  private final int beta;

  /**
   * New cost.
   * @param cost the hourly hosting cost in euros.
   * @param distance the distance to the host in kilometers.
   * @param affinity the affinity with the host.
   * @param alpha the hourly cost weighting.
   * @param beta the distance weighting.
   */
  public Cost(final double cost, final int distance, final int affinity, final int alpha, final int beta) {
    this.distance = distance;
    this.cost = cost;
    this.affinity = affinity;
    this.alpha = alpha;
    this.beta = beta;
  }

  public int distance() {
    return distance;
  }

  public double hourlyCost() {
    return cost;
  }

  public int affinity() {
    return affinity;
  }

  /**
   * Compute the cost associated to the assignment.
   * @param baseDistance the distance to use for normalisation.
   * @param baseCost the hosting cost to use for normalisation.
   * @return the aggregrated cost.
   */
  public double compute(final int baseDistance, final double baseCost) {
    double nd = 1.0 * distance / baseDistance;
    double nc = 1.0 * cost / baseCost;
    return ((alpha * nd + beta * nc) / (alpha + beta)) / affinity;
  }
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Cost cost1 = (Cost) o;
    return distance == cost1.distance &&
        cost == cost1.cost &&
        alpha == cost1.alpha &&
        beta == cost1.beta &&
        Double.compare(cost1.affinity, affinity) == 0;
  }

  public int alpha() {
    return alpha;
  }

  public int beta() {
    return beta;
  }

  @Override
  public int hashCode() {
    return Objects.hash(distance, cost, affinity, alpha, beta);
  }

  @Override
  public String toString() {
    return "Cost{" +
        "distance=" + distance +
        ", cost=" + cost +
        ", affinity=" + affinity +
        ", alpha=" + alpha +
        ", beta=" + beta +
        '}';
  }
}
