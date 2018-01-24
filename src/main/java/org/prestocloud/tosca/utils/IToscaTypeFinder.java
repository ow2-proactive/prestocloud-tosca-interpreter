package org.prestocloud.tosca.utils;

import org.prestocloud.tosca.model.types.AbstractToscaType;

@FunctionalInterface
public interface IToscaTypeFinder {
    <T extends AbstractToscaType> T findElement(Class<T> clazz, String id);
}
