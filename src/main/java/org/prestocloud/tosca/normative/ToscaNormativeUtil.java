package org.prestocloud.tosca.normative;

import prestocloud.paas.plan.ToscaNodeLifecycleConstants;
import prestocloud.paas.plan.ToscaRelationshipLifecycleConstants;
import prestocloud.utils.PrestocloudUtils;

/**
 * Utility to work with normative constants.
 */
public final class ToscaNormativeUtil {

    private ToscaNormativeUtil() {
    };

    /**
     * Convert a short-named normative interface name to a long one.
     * 
     * @param interfaceName The name of the interface.
     * @return If the interface name is a normative interface shortname then the fullname, if returns the interfaceName.
     */
    public static String getLongInterfaceName(String interfaceName) {
        if (ToscaNodeLifecycleConstants.STANDARD_SHORT.equalsIgnoreCase(interfaceName)) {
            return ToscaNodeLifecycleConstants.STANDARD;
        } else if (ToscaRelationshipLifecycleConstants.CONFIGURE_SHORT.equalsIgnoreCase(interfaceName)) {
            return ToscaRelationshipLifecycleConstants.CONFIGURE;
        }
        return interfaceName;
    }

    public static String formatedOperationOutputName(String nodeName, String interfaceName, String operationName, String output) {
        return PrestocloudUtils.prefixWith(PrestocloudUtils.COLON_SEPARATOR, output, new String[] { nodeName, interfaceName, operationName });
    }
}
