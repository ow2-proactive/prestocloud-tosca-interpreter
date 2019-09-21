package prestocloud.btrplace.tosca;

import prestocloud.btrplace.tosca.model.RegionCapacityDescriptor;
import prestocloud.btrplace.tosca.model.VMTemplateDetails;

import java.util.HashMap;
import java.util.List;

public class GetVMTemplatesDetailsResult {

    public HashMap<String, List<RegionCapacityDescriptor>> regionsPerClouds;

    public List<VMTemplateDetails> vmTemplatesDetails;

}
