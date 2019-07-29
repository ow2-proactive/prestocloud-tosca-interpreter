package prestocloud.btrplace;

import org.btrplace.json.model.InstanceConverter;
import org.btrplace.scheduler.choco.ChocoScheduler;
import org.btrplace.scheduler.choco.DefaultChocoScheduler;
import prestocloud.btrplace.cost.CMinCost;
import prestocloud.btrplace.cost.CostViewConverter;
import prestocloud.btrplace.cost.MinCost;
import prestocloud.btrplace.cost.MinCostConverter;
import prestocloud.btrplace.minUsed.CMinUsed;
import prestocloud.btrplace.minUsed.MinUsed;
import prestocloud.btrplace.minUsed.MinUsedConverter;

/**
 * Utility class to create BtrPlace components.
 *
 * @author Fabien Hermenier
 */
public final class PrestoCloudExtensions {

  /**
   * Make a new scheduler, including PrestoCloud particularities.
   * @return a pre-configured scheduler.
   */
  public static ChocoScheduler newScheduler() {
    final ChocoScheduler sched = new DefaultChocoScheduler();
    sched.getParameters().getMapper().mapConstraint(MinUsed.class, CMinUsed.class);
    sched.getParameters().getMapper().mapConstraint(MinCost.class, CMinCost.class);
    return sched;
  }

  /**
   * Make a new InstanceConverter that includes PrestoCloud particularities.
   * @return the pre-configured JSON converter.
   */
  public static InstanceConverter newInstanceConverter() {
    final InstanceConverter ic = new InstanceConverter();
    ic.getConstraintsConverter().register(new MinUsedConverter());
    ic.getConstraintsConverter().register(new MinCostConverter());
    ic.getModelConverter().getViewsConverter().register(new CostViewConverter());
    return ic;
  }

}
