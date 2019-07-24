package prestocloud.btrplace.cost;

import org.btrplace.model.constraint.OptConstraint;

public class MinCost extends OptConstraint {

  private static final String ID = "minCost()";

  @Override
  public String id() {
    return ID;
  }
}
