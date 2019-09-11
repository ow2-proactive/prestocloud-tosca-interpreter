package prestocloud.btrplace.precedingRunning;

import net.minidev.json.JSONObject;
import org.btrplace.json.JSONConverterException;
import org.btrplace.json.JSONs;
import org.btrplace.json.model.constraint.ConstraintConverter;
import org.btrplace.model.Model;
import org.btrplace.model.VM;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON converter for the {@link PrecedingRunning} constraint.
 * Format: {@code {"id"="precedingRunning","vm"=<vm_id>,"parents"=[vm_ids]}}
 */
public class PrecedingRunningConverter implements
    ConstraintConverter<PrecedingRunning> {

  /**
   * JSON identifier for the constraint.
   */
  public final static String ID = "precedingRunning";

  /**
   * The label for the VM.
   */
  private final String VM_LABEL = "vm";

  /**
   * The label for the parent VMs.
   */
  private final String PARENT_VMS_LABEL = "parents";

  @Override
  public Class<PrecedingRunning> getSupportedConstraint() {
    return PrecedingRunning.class;
  }

  @Override
  public String getJSONId() {
    return ID;
  }

  @Override
  public PrecedingRunning fromJSON(final Model mo, final JSONObject json)
      throws JSONConverterException {

    final Set<VM> parents = new HashSet<>(JSONs.requiredVMs(mo, json, PARENT_VMS_LABEL));
    final VM vm = JSONs.requiredVM(mo, json, VM_LABEL);
    return new PrecedingRunning(vm, parents);
  }

  @Override
  public JSONObject toJSON(final PrecedingRunning o) {
    final JSONObject json = new JSONObject();
    json.put(IDENTIFIER, getJSONId());
    json.put(VM_LABEL, JSONs.elementToJSON(o.vm()));
    json.put(PARENT_VMS_LABEL, JSONs.vmsToJSON(o.parents()));
    return json;
  }
}
