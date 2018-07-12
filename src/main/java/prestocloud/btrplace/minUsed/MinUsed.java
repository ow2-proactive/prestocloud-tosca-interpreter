package prestocloud.btrplace.minUsed;

import org.btrplace.model.Node;
import org.btrplace.model.constraint.OptConstraint;

import java.util.Collections;
import java.util.Set;

/**
 * An objective that minimises the number of used nodes among those given in
 * parameters.
 *
 * @author Fabien Hermenier
 */
public class MinUsed extends OptConstraint {

  /**
   * The nodes we target.
   */
  private final Set<Node> nodes;

  /**
   * Make a new objective.
   * @param nodes the nodes to consider.
   */
  public MinUsed(final Set<Node> nodes) {
    this.nodes = Collections.unmodifiableSet(nodes);
  }

  @Override
  public String id() {
    return "minUsed(" + nodes + ")";
  }

  public Set<Node> nodes() {
    return nodes;
  }

  @Override
  public String toString() {
    return id();
  }
}
