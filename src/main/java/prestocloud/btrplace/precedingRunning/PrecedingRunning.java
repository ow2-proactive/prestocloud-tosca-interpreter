package prestocloud.btrplace.precedingRunning;

import com.google.common.collect.Sets;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.AllowAllConstraintChecker;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.constraint.SatConstraintChecker;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A constraint to state that a given VM cannot be set to the running state
 * before at least one of the other given VMs is running.
 *
 * Accordingly, if among the parent VMs, at least one VM is already running, the
 * constraint is considered satisfied. The same if the VM is already running.
 */
public class PrecedingRunning implements SatConstraint {

  private final VM vm;

  private final Set<VM> otherVMs;

  /**
   * New constraint.
   * @param vm the VM to boot
   * @param parent the VM that must be set running before.
   */
  public PrecedingRunning(final VM vm, final VM parent) {
    this(vm, Sets.newHashSet(parent));
  }

  /**
   * New constraint.
   * @param vm the VM to boot
   * @param parents the VMs that must be set running before.
   */
  public PrecedingRunning(final VM vm, final Set<VM> parents) {
    this.vm = vm;
    this.otherVMs = Collections.unmodifiableSet(parents);
    if (parents.contains(vm)) {
      throw new IllegalArgumentException(vm + " cannot have a precedence with itself");
    }
  }

  @Override
  public boolean isContinuous() {
    return false;
  }

  @Override
  public boolean setContinuous(final boolean b) {
    return !b;
  }

  @Override
  public SatConstraintChecker<? extends SatConstraint> getChecker() {
    return new AllowAllConstraintChecker<>(this);
  }

  @Override
  public Collection<Node> getInvolvedNodes() {
    return Collections.emptySet();
  }

  @Override
  public Collection<VM> getInvolvedVMs() {
    final Set<VM> allVMs = new HashSet<>(otherVMs);
    allVMs.add(vm);
    return Collections.unmodifiableSet(allVMs);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final PrecedingRunning that = (PrecedingRunning) o;
    return Objects.equals(vm, that.vm) &&
        Objects.equals(otherVMs, that.otherVMs);

  }

  /**
   * Get the VM to be running.
   * @return a VM.
   */
  public VM vm() {
    return vm;
  }

  /**
   * Get the VMs that must be running (at least one) before running {@link #vm()}.
   * @return a set of VMs that should not be empty.
   */
  public Set<VM> parents() {
    return otherVMs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(vm, otherVMs);
  }

  @Override
  public String toString() {
    return "precedingRunning(vm=" + vm + ", parents=" + otherVMs + '}';
  }
}
