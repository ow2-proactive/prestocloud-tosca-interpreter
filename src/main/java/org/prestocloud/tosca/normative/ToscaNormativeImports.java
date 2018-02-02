package org.prestocloud.tosca.normative;

import java.util.Map;

import org.prestocloud.tosca.model.CSARDependency;

import com.google.common.collect.Maps;

/**
 * The definition of the TOSCA version implies an automatic import of dependencies.
 *
 * This class contains the CSARDependency definition based on the actual import definition.
 */
public final class ToscaNormativeImports {
    public static final String TOSCA_NORMATIVE_TYPES = "tosca-normative-types";
    public static final Map<String, CSARDependency> IMPORTS;

    static {
        IMPORTS = Maps.newHashMap();
        IMPORTS.put("tosca_prestocloud_mapping_1_2", new CSARDependency(TOSCA_NORMATIVE_TYPES, "1.2"));
        IMPORTS.put("http://docs.oasis-open.org/tosca/ns/simple/yaml/1.2", new CSARDependency(TOSCA_NORMATIVE_TYPES, "1.2"));
    }
}
