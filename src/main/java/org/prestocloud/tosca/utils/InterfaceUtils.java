package org.prestocloud.tosca.utils;

import static prestocloud.utils.PrestocloudUtils.safe;

import java.util.Map;

import org.prestocloud.tosca.model.definitions.Interface;
import org.prestocloud.tosca.model.definitions.Operation;
import org.apache.commons.lang3.StringUtils;

public final class InterfaceUtils {
    private InterfaceUtils() {
    }

    public static Operation getOperationIfArtifactDefined(Map<String, Interface> interfaceMap, String interfaceName, String operationName) {
        Interface interfaz = safe(interfaceMap).get(interfaceName);
        if (interfaz == null) {
            return null;
        }
        Operation operation = safe(interfaz.getOperations()).get(operationName);
        if (operation == null || operation.getImplementationArtifact() == null || StringUtils.isBlank(operation.getImplementationArtifact().getArtifactRef())) {
            return null;
        }
        return operation;
    }
}
