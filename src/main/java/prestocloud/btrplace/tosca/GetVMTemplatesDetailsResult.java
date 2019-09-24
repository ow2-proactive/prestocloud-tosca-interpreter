package prestocloud.btrplace.tosca;

import prestocloud.btrplace.tosca.model.RegionCapacityDescriptor;
import prestocloud.btrplace.tosca.model.VMTemplateDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetVMTemplatesDetailsResult {


    public Map<String,Map<String, List<RegionCapacityDescriptor>>> regionsPerCloudPerCloudFile;

    public List<VMTemplateDetails> vmTemplatesDetails;

}
