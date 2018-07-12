package prestocloud.btrplace;

import org.btrplace.json.model.InstanceConverter;
import org.btrplace.scheduler.choco.ChocoScheduler;
import org.btrplace.scheduler.choco.DefaultChocoScheduler;
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
    return sched;
  }

  /**
   * Make a new InstanceConverter that includes PrestoCloud particularities.
   * @return the pre-configured JSON converter.
   */
  public static InstanceConverter newInstanceConverter() {
    final InstanceConverter ic = new InstanceConverter();
    ic.getConstraintsConverter().register(new MinUsedConverter());
    return ic;
  }

}
